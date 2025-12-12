package br.com.austa.experiencia.service.domain.selfservice;

import br.com.austa.experiencia.service.AutorizacaoService;
import br.com.austa.experiencia.model.dto.StatusAutorizacaoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por consultar status de autorização/guia médica.
 *
 * Referenciado em: SUB-004_AutoAtendimento_Inteligente.bpmn
 * Activity ID: Activity_ConsultarStatusAutorizacao
 *
 * Variáveis de entrada:
 * - numeroGuia (String): Número da guia/autorização
 * - beneficiarioId (String, opcional): ID do beneficiário para validação
 *
 * Variáveis de saída:
 * - statusAutorizacao (String): Status atual (PENDENTE, APROVADA, NEGADA, etc)
 * - dataAtualizacao (LocalDateTime): Data da última atualização
 * - observacoes (String): Observações sobre a autorização
 * - podeRecorrer (Boolean): Se é possível recurso em caso de negativa
 * - statusEncontrado (Boolean): Flag de sucesso da consulta
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("consultarStatusAutorizacaoDelegate")
@RequiredArgsConstructor
public class ConsultarStatusAutorizacaoDelegate implements JavaDelegate {

    private final AutorizacaoService autorizacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando consulta de status de autorização - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String numeroGuia = (String) execution.getVariable("numeroGuia");
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");

            // 2. Validar dados obrigatórios
            validateInputs(numeroGuia);

            // 3. Consultar status da autorização
            StatusAutorizacaoDTO status = autorizacaoService.consultarStatus(numeroGuia, beneficiarioId);

            // 4. Definir variáveis de saída
            execution.setVariable("statusAutorizacao", status.getStatus());
            execution.setVariable("dataAtualizacao", status.getDataAtualizacao());
            execution.setVariable("observacoes", status.getObservacoes());
            execution.setVariable("podeRecorrer", status.isPodeRecorrer());
            execution.setVariable("statusEncontrado", true);

            log.info("Status de autorização consultado - Guia: {}, Status: {}",
                     numeroGuia, status.getStatus());

        } catch (Exception e) {
            log.error("Erro ao consultar status de autorização: {}", e.getMessage(), e);
            execution.setVariable("statusEncontrado", false);
            execution.setVariable("erroConsulta", e.getMessage());
            throw new RuntimeException("Falha ao consultar status de autorização", e);
        }
    }

    private void validateInputs(String numeroGuia) {
        if (numeroGuia == null || numeroGuia.isBlank()) {
            throw new IllegalArgumentException("numeroGuia é obrigatório");
        }
    }
}
