package br.com.austa.experiencia.service.domain.onboarding;

import com.healthplan.services.screening.ScreeningService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Processar Resposta Screening Delegate
 *
 * Processa respostas do questionário de screening de saúde inicial.
 *
 * INPUT:
 * - screeningResponses (Map): Respostas do questionário
 * - beneficiaryId (String): ID do beneficiário
 *
 * OUTPUT:
 * - screeningScore (Integer): Pontuação do screening
 * - healthFlags (List): Flags de saúde identificadas
 * - requiresFollowUp (Boolean): Requer acompanhamento?
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-001 Onboarding)
 */
@Slf4j
@Component("processarRespostaScreeningDelegate")
public class ProcessarRespostaScreeningDelegate implements JavaDelegate {

    @Autowired
    private ScreeningService screeningService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        @SuppressWarnings("unchecked")
        Map<String, Object> responses = (Map<String, Object>) execution.getVariable("screeningResponses");

        log.info("Processando screening - Beneficiário: {}, {} respostas",
            beneficiaryId, responses.size());

        try {
            // Processar respostas
            var result = screeningService.processScreening(beneficiaryId, responses);

            // Armazenar resultados
            execution.setVariable("screeningScore", result.getScore());
            execution.setVariable("healthFlags", result.getHealthFlags());
            execution.setVariable("requiresFollowUp", result.isRequiresFollowUp());
            execution.setVariable("chronicConditions", result.getChronicConditions());
            execution.setVariable("riskFactors", result.getRiskFactors());

            log.info("Screening processado - Score: {}, Flags: {}",
                result.getScore(), result.getHealthFlags().size());

        } catch (Exception e) {
            log.error("Erro ao processar screening - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("screeningError", e.getMessage());
            throw e;
        }
    }
}
