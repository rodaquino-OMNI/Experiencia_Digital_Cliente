package br.com.austa.experiencia.service.domain.onboarding;

import com.healthplan.services.risk.RiskCalculatorService;
import com.healthplan.models.BeneficiaryProfile;
import com.healthplan.models.RiskScore;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Calcular Score Risco Delegate
 *
 * Calcula score de risco clínico e financeiro do beneficiário.
 *
 * INPUT:
 * - beneficiaryProfile (Object): Perfil completo do beneficiário
 *
 * OUTPUT:
 * - riskScore (Double): Score de risco (0-100)
 * - riskLevel (String): Nível de risco (LOW/MEDIUM/HIGH/CRITICAL)
 * - riskFactors (List): Fatores de risco identificados
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-001 Onboarding)
 */
@Slf4j
@Component("calcularScoreRiscoDelegate")
public class CalcularScoreRiscoDelegate implements JavaDelegate {

    @Autowired
    private RiskCalculatorService riskCalculator;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        BeneficiaryProfile profile = (BeneficiaryProfile) execution.getVariable("beneficiaryProfile");
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");

        log.info("Calculando score de risco - Beneficiário: {}", beneficiaryId);

        try {
            // Calcular risco
            RiskScore riskScore = riskCalculator.calculateRiskScore(profile);

            // Armazenar resultados
            execution.setVariable("riskScore", riskScore.getTotalScore());
            execution.setVariable("riskLevel", riskScore.getRiskLevel());
            execution.setVariable("riskFactors", riskScore.getFactors());
            execution.setVariable("demographicRisk", riskScore.getDemographicComponent());
            execution.setVariable("clinicalRisk", riskScore.getClinicalComponent());
            execution.setVariable("behavioralRisk", riskScore.getBehavioralComponent());
            execution.setVariable("historicalRisk", riskScore.getHistoricalComponent());

            // Classificação para plano de cuidados
            execution.setVariable("needsIntensiveCare", riskScore.getTotalScore() >= 75);
            execution.setVariable("needsPreventiveCare", riskScore.getTotalScore() >= 50);

            log.info("Score de risco calculado - Score: {:.2f}, Nível: {}",
                riskScore.getTotalScore(), riskScore.getRiskLevel());

        } catch (Exception e) {
            log.error("Erro ao calcular score de risco - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("riskCalculationError", e.getMessage());
            throw e;
        }
    }
}
