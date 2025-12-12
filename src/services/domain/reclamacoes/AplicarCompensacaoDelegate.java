package br.com.austa.experiencia.services.domain.reclamacoes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aplica compensação ao beneficiário quando aprovada.
 *
 * Variáveis de entrada:
 * - beneficiarioId (String)
 * - protocoloReclamacao (String)
 * - tipoCompensacao (String): DESCONTO, CREDITO, SERVICO_EXTRA, REEMBOLSO
 * - valorCompensacao (BigDecimal)
 * - aprovadoPor (String)
 *
 * Variáveis de saída:
 * - compensacaoAplicada (Boolean)
 * - codigoCompensacao (String)
 * - dataVigencia (LocalDate)
 */
@Slf4j
@Component("aplicarCompensacaoDelegate")
@RequiredArgsConstructor
public class AplicarCompensacaoDelegate implements JavaDelegate {

    private final FinanceiroService financeiroService;
    private final TasyService tasyService;
    private final ReclamacaoService reclamacaoService;
    private final KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String protocolo = (String) execution.getVariable("protocoloReclamacao");
        String tipoCompensacao = (String) execution.getVariable("tipoCompensacao");
        BigDecimal valor = (BigDecimal) execution.getVariable("valorCompensacao");
        String aprovadoPor = (String) execution.getVariable("aprovadoPor");

        log.info("Aplicando compensação - Protocolo: {}, Tipo: {}, Valor: {}",
                 protocolo, tipoCompensacao, valor);

        CompensacaoDTO compensacao = CompensacaoDTO.builder()
            .beneficiarioId(beneficiarioId)
            .protocoloReclamacao(protocolo)
            .tipo(tipoCompensacao)
            .valor(valor)
            .aprovadoPor(aprovadoPor)
            .dataAplicacao(LocalDateTime.now())
            .build();

        String codigoCompensacao = null;
        LocalDate dataVigencia = null;

        switch (tipoCompensacao) {
            case "DESCONTO":
                codigoCompensacao = financeiroService.aplicarDesconto(
                    beneficiarioId, valor, 3); // 3 meses
                dataVigencia = LocalDate.now().plusMonths(3);
                break;

            case "CREDITO":
                codigoCompensacao = financeiroService.adicionarCredito(
                    beneficiarioId, valor);
                dataVigencia = LocalDate.now().plusYears(1);
                break;

            case "SERVICO_EXTRA":
                codigoCompensacao = tasyService.liberarServicoExtra(
                    beneficiarioId, (String) execution.getVariable("servicoExtra"));
                dataVigencia = LocalDate.now().plusMonths(6);
                break;

            case "REEMBOLSO":
                codigoCompensacao = financeiroService.processarReembolso(
                    beneficiarioId, valor,
                    (String) execution.getVariable("dadosBancarios"));
                dataVigencia = LocalDate.now();
                break;
        }

        compensacao.setCodigo(codigoCompensacao);
        compensacao.setDataVigencia(dataVigencia);

        // Registrar compensação
        reclamacaoService.registrarCompensacao(protocolo, compensacao);

        // Publicar evento
        kafkaPublisher.publish("reclamacao.compensacao.aplicada", compensacao);

        // Definir variáveis
        execution.setVariable("compensacaoAplicada", true);
        execution.setVariable("codigoCompensacao", codigoCompensacao);
        execution.setVariable("dataVigencia", dataVigencia);

        log.info("Compensação aplicada com sucesso - Código: {}", codigoCompensacao);
    }
}
