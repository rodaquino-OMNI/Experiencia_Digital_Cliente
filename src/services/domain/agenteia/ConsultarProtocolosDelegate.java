package br.com.austa.experiencia.service.domain.agenteia;

import com.healthplan.services.ai.AiTriageService;
import com.healthplan.models.ClinicalProtocol;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Consultar Protocolos Delegate
 *
 * Consulta protocolos clínicos baseados nos sintomas identificados.
 *
 * INPUT:
 * - symptoms (List<String>): Sintomas identificados
 * - patientAge (Integer): Idade do paciente
 *
 * OUTPUT:
 * - matchedProtocols (List): Protocolos clínicos compatíveis
 * - protocolCount (Integer): Número de protocolos encontrados
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-005 Agentes IA)
 */
@Slf4j
@Component("consultarProtocolosDelegate")
public class ConsultarProtocolosDelegate implements JavaDelegate {

    @Autowired
    private AiTriageService aiTriageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        @SuppressWarnings("unchecked")
        List<String> symptoms = (List<String>) execution.getVariable("symptoms");
        Integer patientAge = (Integer) execution.getVariable("patientAge");

        log.info("Consultando protocolos clínicos - {} sintomas, idade: {}",
            symptoms.size(), patientAge);

        try {
            // Consultar protocolos
            List<ClinicalProtocol> protocols = aiTriageService.queryProtocols(
                symptoms,
                patientAge
            );

            // Armazenar resultados
            execution.setVariable("matchedProtocols", protocols);
            execution.setVariable("protocolCount", protocols.size());
            execution.setVariable("hasProtocols", !protocols.isEmpty());

            log.info("Protocolos consultados - {} encontrados", protocols.size());

        } catch (Exception e) {
            log.error("Erro ao consultar protocolos clínicos", e);
            execution.setVariable("protocolError", e.getMessage());
            throw e;
        }
    }
}
