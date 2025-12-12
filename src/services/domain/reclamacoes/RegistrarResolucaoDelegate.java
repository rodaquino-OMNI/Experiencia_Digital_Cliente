package br.com.austa.experiencia.services.domain.reclamacoes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Registra a resolução final da reclamação.
 *
 * Variáveis de entrada:
 * - protocoloReclamacao (String)
 * - statusFinal (String): RESOLVIDA, PROCEDENTE, IMPROCEDENTE
 * - descricaoResolucao (String)
 * - compensacaoAplicada (Boolean)
 * - satisfacaoBeneficiario (Integer): 1-5
 *
 * Variáveis de saída:
 * - reclamacaoEncerrada (Boolean)
 * - dataEncerramento (LocalDateTime)
 * - tempoResolucao (Long): em horas
 */
@Slf4j
@Component("registrarResolucaoDelegate")
@RequiredArgsConstructor
public class RegistrarResolucaoDelegate implements JavaDelegate {

    private final ReclamacaoService reclamacaoService;
    private final MetricasService metricasService;
    private final KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String protocolo = (String) execution.getVariable("protocoloReclamacao");
        String statusFinal = (String) execution.getVariable("statusFinal");
        String descricaoResolucao = (String) execution.getVariable("descricaoResolucao");

        // 1. Buscar reclamação
        ReclamacaoDTO reclamacao = reclamacaoService.buscarPorProtocolo(protocolo);

        // 2. Calcular tempo de resolução
        LocalDateTime agora = LocalDateTime.now();
        long tempoResolucaoHoras = ChronoUnit.HOURS.between(
            reclamacao.getDataAbertura(), agora);

        // 3. Verificar se cumpriu SLA
        boolean dentroDosla = tempoResolucaoHoras <= reclamacao.getSlaHoras();

        // 4. Atualizar reclamação
        ResolucaoDTO resolucao = ResolucaoDTO.builder()
            .protocolo(protocolo)
            .statusFinal(statusFinal)
            .descricao(descricaoResolucao)
            .dataEncerramento(agora)
            .tempoResolucaoHoras(tempoResolucaoHoras)
            .dentroDosla(dentroDosla)
            .resolvidoPor((String) execution.getVariable("resolvidoPor"))
            .build();

        reclamacaoService.registrarResolucao(resolucao);

        // 5. Atualizar métricas
        metricasService.registrarResolucao(
            reclamacao.getTipo(),
            tempoResolucaoHoras,
            dentroDosla,
            statusFinal
        );

        // 6. Publicar evento
        kafkaPublisher.publish("reclamacao.resolvida", resolucao);

        // 7. Definir variáveis
        execution.setVariable("reclamacaoEncerrada", true);
        execution.setVariable("dataEncerramento", agora);
        execution.setVariable("tempoResolucao", tempoResolucaoHoras);
        execution.setVariable("dentroDosla", dentroDosla);

        log.info("Reclamação encerrada - Protocolo: {}, Status: {}, Tempo: {}h, SLA: {}",
                 protocolo, statusFinal, tempoResolucaoHoras,
                 dentroDosla ? "CUMPRIDO" : "EXCEDIDO");
    }
}
