package br.com.austa.experiencia.service.domain.agenteia;

import com.healthplan.services.ai.AiTriageService;
import com.healthplan.models.HealthRecommendation;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gerar Recomendação IA Delegate
 *
 * Gera recomendação de saúde personalizada baseada na triagem.
 *
 * INPUT:
 * - triageSessionId (String): ID da sessão de triagem
 *
 * OUTPUT:
 * - recommendation (String): Recomendação gerada
 * - severity (String): Gravidade avaliada
 * - suggestedActions (List): Ações sugeridas
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-005 Agentes IA)
 */
@Slf4j
@Component("gerarRecomendacaoIaDelegate")
public class GerarRecomendacaoIaDelegate implements JavaDelegate {

    @Autowired
    private AiTriageService aiTriageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String sessionId = (String) execution.getVariable("triageSessionId");

        log.info("Gerando recomendação IA - Sessão: {}", sessionId);

        try {
            // Gerar recomendação
            HealthRecommendation recommendation = aiTriageService.generateRecommendation(sessionId);

            // Armazenar resultados
            execution.setVariable("recommendation", recommendation.getRecommendation());
            execution.setVariable("severity", recommendation.getSeverity());
            execution.setVariable("suggestedActions", recommendation.getSuggestedActions());
            execution.setVariable("protocols", recommendation.getProtocols());

            log.info("Recomendação gerada - Gravidade: {}", recommendation.getSeverity());

        } catch (Exception e) {
            log.error("Erro ao gerar recomendação IA - Sessão: {}", sessionId, e);
            execution.setVariable("recommendationError", e.getMessage());
            throw e;
        }
    }
}
