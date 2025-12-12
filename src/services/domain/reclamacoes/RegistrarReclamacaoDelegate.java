package br.com.austa.experiencia.services.domain.reclamacoes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Registra reclamação no sistema CRM e inicia workflow de tratamento.
 *
 * Referenciado em: SUB-009_Gestao_Reclamacoes.bpmn
 * Activity ID: Activity_RegistrarReclamacao
 *
 * Variáveis de entrada:
 * - beneficiarioId (String): ID do beneficiário
 * - canalOrigem (String): Canal de origem (WHATSAPP, APP, TELEFONE, ANS, PROCON)
 * - tipoReclamacao (String): Tipo da reclamação
 * - descricao (String): Descrição detalhada
 * - anexos (List<String>): URLs dos anexos
 *
 * Variáveis de saída:
 * - protocoloReclamacao (String): Número do protocolo gerado
 * - criticidade (String): BAIXA, MEDIA, ALTA, CRITICA
 * - slaHoras (Integer): SLA em horas para resolução
 * - responsavel (String): Área responsável
 */
@Slf4j
@Component("registrarReclamacaoDelegate")
@RequiredArgsConstructor
public class RegistrarReclamacaoDelegate implements JavaDelegate {

    private final ReclamacaoService reclamacaoService;
    private final CrmService crmService;
    private final KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Registrando reclamação - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String canalOrigem = (String) execution.getVariable("canalOrigem");
            String tipoReclamacao = (String) execution.getVariable("tipoReclamacao");
            String descricao = (String) execution.getVariable("descricao");

            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, canalOrigem, tipoReclamacao, descricao);

            // 3. Gerar protocolo
            String protocolo = reclamacaoService.gerarProtocolo(canalOrigem);

            // 4. Classificar criticidade via DMN (já executado antes)
            String criticidade = (String) execution.getVariable("criticidade");
            Integer slaHoras = (Integer) execution.getVariable("slaHoras");
            String responsavel = (String) execution.getVariable("responsavel");

            // 5. Registrar no CRM
            ReclamacaoDTO reclamacao = ReclamacaoDTO.builder()
                .protocolo(protocolo)
                .beneficiarioId(beneficiarioId)
                .canalOrigem(canalOrigem)
                .tipo(tipoReclamacao)
                .descricao(descricao)
                .criticidade(criticidade)
                .slaHoras(slaHoras)
                .responsavel(responsavel)
                .status("ABERTA")
                .dataAbertura(LocalDateTime.now())
                .build();

            crmService.registrarReclamacao(reclamacao);

            // 6. Publicar evento Kafka
            kafkaPublisher.publish("reclamacao.registrada", reclamacao);

            // 7. Definir variáveis de saída
            execution.setVariable("protocoloReclamacao", protocolo);
            execution.setVariable("reclamacaoRegistrada", true);
            execution.setVariable("dataLimiteResolucao",
                LocalDateTime.now().plusHours(slaHoras));

            log.info("Reclamação registrada - Protocolo: {}, Criticidade: {}",
                     protocolo, criticidade);

        } catch (Exception e) {
            log.error("Erro ao registrar reclamação: {}", e.getMessage(), e);
            execution.setVariable("reclamacaoRegistrada", false);
            execution.setVariable("erroRegistro", e.getMessage());
            throw e;
        }
    }

    private void validateInputs(String beneficiarioId, String canalOrigem,
                                String tipoReclamacao, String descricao) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (canalOrigem == null || canalOrigem.isBlank()) {
            throw new IllegalArgumentException("canalOrigem é obrigatório");
        }
        if (tipoReclamacao == null || tipoReclamacao.isBlank()) {
            throw new IllegalArgumentException("tipoReclamacao é obrigatório");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descricao é obrigatória");
        }
    }
}
