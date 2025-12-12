package br.com.austa.experiencia.service.domain.autorizacao;

import br.com.austa.experiencia.service.AutorizacaoService;
import br.com.austa.experiencia.model.dto.AutorizacaoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Delegate responsável por aprovar automaticamente autorizações baseado em regras.
 * Utiliza motor de regras de negócio para decisão automática.
 *
 * Referenciado em: SUB-006_Autorizacao_Hibrida.bpmn
 * Activity ID: Activity_AprovarAutomaticamente
 *
 * Variáveis de entrada:
 * - numeroGuia (String): Número da guia/autorização
 * - dadosGuia (Map): Dados estruturados da guia TISS
 * - beneficiarioId (String): ID do beneficiário
 * - procedimentoId (String): Código do procedimento (TUSS)
 * - valorProcedimento (BigDecimal): Valor do procedimento
 * - urgencia (Boolean, default: false): Se é atendimento de urgência
 *
 * Variáveis de saída:
 * - autorizacaoAprovada (Boolean): Se foi aprovada automaticamente
 * - numeroAutorizacao (String): Número da autorização gerada
 * - regraAplicada (String): Código da regra que autorizou
 * - validadeAutorizacao (LocalDate): Validade da autorização
 * - motivoReprovacao (String): Motivo se não foi aprovada
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("aprovarAutomaticamenteDelegate")
@RequiredArgsConstructor
public class AprovarAutomaticamenteDelegate implements JavaDelegate {

    private final AutorizacaoService autorizacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando aprovação automática de autorização - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String numeroGuia = (String) execution.getVariable("numeroGuia");

            @SuppressWarnings("unchecked")
            Map<String, Object> dadosGuia =
                (Map<String, Object>) execution.getVariable("dadosGuia");

            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String procedimentoId = (String) execution.getVariable("procedimentoId");
            java.math.BigDecimal valorProcedimento =
                (java.math.BigDecimal) execution.getVariable("valorProcedimento");
            Boolean urgencia = execution.getVariable("urgencia") != null
                ? (Boolean) execution.getVariable("urgencia") : false;

            // 2. Validar dados obrigatórios
            validateInputs(numeroGuia, dadosGuia, beneficiarioId, procedimentoId);

            // 3. Tentar aprovação automática via motor de regras
            AutorizacaoDTO autorizacao = autorizacaoService.tentarAprovacaoAutomatica(
                numeroGuia, dadosGuia, beneficiarioId, procedimentoId, valorProcedimento, urgencia);

            // 4. Definir variáveis de saída
            execution.setVariable("autorizacaoAprovada", autorizacao.isAprovada());
            execution.setVariable("numeroAutorizacao", autorizacao.getNumeroAutorizacao());
            execution.setVariable("regraAplicada", autorizacao.getRegraAplicada());
            execution.setVariable("validadeAutorizacao", autorizacao.getValidadeAutorizacao());
            execution.setVariable("motivoReprovacao", autorizacao.getMotivoReprovacao());

            if (autorizacao.isAprovada()) {
                log.info("Autorização APROVADA automaticamente - Número: {}, Guia: {}, Regra: {}",
                         autorizacao.getNumeroAutorizacao(), numeroGuia, autorizacao.getRegraAplicada());
            } else {
                log.info("Autorização NÃO aprovada automaticamente - Guia: {}, Motivo: {}",
                         numeroGuia, autorizacao.getMotivoReprovacao());
            }

        } catch (Exception e) {
            log.error("Erro ao tentar aprovação automática: {}", e.getMessage(), e);
            execution.setVariable("autorizacaoAprovada", false);
            execution.setVariable("motivoReprovacao", "Erro no processamento automático");
            // Não lança exceção - fluxo continua para análise manual
        }
    }

    private void validateInputs(String numeroGuia, Map<String, Object> dadosGuia,
                                 String beneficiarioId, String procedimentoId) {
        if (numeroGuia == null || numeroGuia.isBlank()) {
            throw new IllegalArgumentException("numeroGuia é obrigatório");
        }
        if (dadosGuia == null || dadosGuia.isEmpty()) {
            throw new IllegalArgumentException("dadosGuia é obrigatório");
        }
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (procedimentoId == null || procedimentoId.isBlank()) {
            throw new IllegalArgumentException("procedimentoId é obrigatório");
        }
    }
}
