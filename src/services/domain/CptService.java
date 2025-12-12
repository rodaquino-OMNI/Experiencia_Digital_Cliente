package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * CPT Service - Pre-existing Conditions Management Delegate
 *
 * Manages CPT (Cobertura Parcial Temporária) periods and coverage rules
 * for pre-existing conditions according to ANS regulations.
 *
 * BPMN Coverage:
 * - cptService.aplicar (Apply CPT coverage rules)
 * - cptService.liberarCobertura (Release coverage after carência period)
 */
@Component("cptService")
public class CptService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(CptService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DataLakeService dataLakeService;

    // ANS regulated carência periods (in months)
    private static final int CARENCIA_CPT_PADRAO = 24; // 24 months for CPT
    private static final int CARENCIA_URGENCIA = 0; // No carência for emergencies
    private static final int CARENCIA_PARTO = 10; // 10 months for childbirth

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("cptMethod");

        if ("aplicar".equals(method)) {
            aplicar(execution);
        } else if ("liberarCobertura".equals(method)) {
            liberarCobertura(execution);
        } else {
            logger.warn("Unknown CPT method: {}", method);
            throw new IllegalArgumentException("Invalid CPT method: " + method);
        }
    }

    /**
     * Apply CPT coverage rules
     *
     * Evaluates if procedure is affected by CPT restrictions and applies
     * appropriate carência period enforcement based on ANS regulations.
     *
     * @param execution Process execution context
     */
    public void aplicar(DelegateExecution execution) throws Exception {
        logger.info("Executing cptService.aplicar for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract procedure and beneficiary info
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String procedimentoCodigo = (String) execution.getVariable("procedimentoCodigo");
            String procedimentoNome = (String) execution.getVariable("procedimentoNome");
            LocalDate dataContratacao = LocalDate.parse((String) execution.getVariable("dataContratacao"));
            LocalDate dataSolicitacao = LocalDate.now();

            // Check if beneficiary has CPT declared
            Map<String, Object> beneficiarioData = dataLakeService.consultar(
                execution, "beneficiarios", beneficiarioId);

            boolean possuiCpt = (boolean) beneficiarioData.getOrDefault("possuiCpt", false);
            List<String> condicoesCpt = (List<String>) beneficiarioData.getOrDefault("condicoesCpt", new ArrayList<>());

            // Calculate elapsed months since contract start
            long mesesDesdeContratacao = ChronoUnit.MONTHS.between(dataContratacao, dataSolicitacao);

            boolean cptAplicavel = false;
            String motivoCpt = null;
            int mesesRestantesCarencia = 0;

            if (possuiCpt && mesesDesdeContratacao < CARENCIA_CPT_PADRAO) {
                // Check if procedure is related to declared CPT conditions
                cptAplicavel = isProcedimentoRelacionadoCpt(procedimentoCodigo, procedimentoNome, condicoesCpt);

                if (cptAplicavel) {
                    mesesRestantesCarencia = CARENCIA_CPT_PADRAO - (int) mesesDesdeContratacao;
                    motivoCpt = String.format(
                        "Procedimento relacionado a CPT declarada. Carência de %d meses restantes.",
                        mesesRestantesCarencia
                    );

                    logger.info("CPT applicable for procedure {} - {} months remaining",
                        procedimentoCodigo, mesesRestantesCarencia);
                }
            }

            // Check for emergency/urgency exception (no CPT applies)
            boolean isUrgencia = (boolean) execution.getVariable("isUrgencia");
            if (isUrgencia) {
                cptAplicavel = false;
                motivoCpt = "Atendimento de urgência/emergência - CPT não aplicável";
                logger.info("CPT waived due to emergency/urgency");
            }

            // Store CPT evaluation results
            execution.setVariable("cptAplicavel", cptAplicavel);
            execution.setVariable("motivoCpt", motivoCpt);
            execution.setVariable("mesesRestantesCarencia", mesesRestantesCarencia);
            execution.setVariable("mesesDesdeContratacao", mesesDesdeContratacao);
            execution.setVariable("condicoesCptDeclaradas", condicoesCpt);

            // If CPT applies, flag for denial or manual review
            if (cptAplicavel) {
                execution.setVariable("negadoPorCpt", true);
                execution.setVariable("dataLiberacaoCpt",
                    dataSolicitacao.plusMonths(mesesRestantesCarencia).toString());
            }

        } catch (Exception e) {
            logger.error("Error applying CPT rules for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("cptErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Release coverage after carência period
     *
     * Checks if carência period has elapsed and releases CPT restrictions
     * for the beneficiary's declared conditions.
     *
     * @param execution Process execution context
     */
    public void liberarCobertura(DelegateExecution execution) throws Exception {
        logger.info("Executing cptService.liberarCobertura for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            LocalDate dataContratacao = LocalDate.parse((String) execution.getVariable("dataContratacao"));
            LocalDate dataAtual = LocalDate.now();

            long mesesDesdeContratacao = ChronoUnit.MONTHS.between(dataContratacao, dataAtual);

            boolean coberturaLiberada = false;
            String mensagem;

            if (mesesDesdeContratacao >= CARENCIA_CPT_PADRAO) {
                // Carência period complete - release CPT restrictions
                coberturaLiberada = true;
                mensagem = "Período de carência CPT completo. Cobertura total liberada.";

                // Update beneficiary record to remove CPT flag
                Map<String, Object> updateData = Map.of(
                    "cptLiberada", true,
                    "dataLiberacaoCpt", dataAtual.toString(),
                    "coberturaStatus", "COMPLETA"
                );

                dataLakeService.atualizar(execution, "beneficiarios", beneficiarioId, updateData);

                logger.info("CPT coverage released for beneficiary {} after {} months",
                    beneficiarioId, mesesDesdeContratacao);

            } else {
                int mesesRestantes = CARENCIA_CPT_PADRAO - (int) mesesDesdeContratacao;
                mensagem = String.format("Cobertura ainda em carência. %d meses restantes.", mesesRestantes);
                logger.info("CPT coverage not yet released - {} months remaining", mesesRestantes);
            }

            execution.setVariable("coberturaLiberada", coberturaLiberada);
            execution.setVariable("mensagemLiberacao", mensagem);
            execution.setVariable("mesesCarenciaDecorridos", mesesDesdeContratacao);

        } catch (Exception e) {
            logger.error("Error releasing CPT coverage for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("cptErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if procedure code/name is related to declared CPT conditions
     */
    private boolean isProcedimentoRelacionadoCpt(String codigo, String nome, List<String> condicoesCpt) {
        if (condicoesCpt == null || condicoesCpt.isEmpty()) {
            return false;
        }

        // CID-10 and TUSS code matching logic
        String searchText = (codigo + " " + nome).toLowerCase();

        for (String condicao : condicoesCpt) {
            String condicaoLower = condicao.toLowerCase();

            // Direct keyword matching for common CPT conditions
            if (condicaoLower.contains("diabetes") && searchText.contains("diabet")) return true;
            if (condicaoLower.contains("hipertens") && searchText.contains("hiperten")) return true;
            if (condicaoLower.contains("cardio") && searchText.contains("cardio")) return true;
            if (condicaoLower.contains("oncol") && searchText.contains("neo")) return true;
            if (condicaoLower.contains("renal") && searchText.contains("ren")) return true;
        }

        return false;
    }
}
