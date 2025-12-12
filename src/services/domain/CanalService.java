package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Java Delegate para identificação de canal de origem
 *
 * Responsabilidades:
 * - Identificar canal de origem da interação
 * - Normalizar dados do canal
 *
 * Uso no BPMN:
 * <serviceTask id="Task_IdentificarCanal"
 *              name="Identificar Canal"
 *              camunda:delegateExpression="${canalService.identificar}">
 * </serviceTask>
 */
@Component("canalService")
public class CanalService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanalService.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.info("Identificando canal de origem para processo: {}", execution.getProcessInstanceId());

        try {
            identificar(execution);
        } catch (Exception e) {
            LOGGER.error("Erro ao identificar canal: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Identifica canal de origem da interação
     *
     * Input:
     * - canalOrigem (String): Canal bruto (whatsapp, email, telefone, app)
     *
     * Output:
     * - canalIdentificado (String): Canal normalizado
     * - tipoCanal (String): Tipo do canal (DIGITAL, VOICE, etc.)
     */
    private void identificar(DelegateExecution execution) {
        String canalOrigem = (String) execution.getVariable("canalOrigem");

        LOGGER.info("Identificando canal: {}", canalOrigem);

        String canalIdentificado;
        String tipoCanal;

        if (canalOrigem != null) {
            String canalLower = canalOrigem.toLowerCase();

            if (canalLower.contains("whatsapp") || canalLower.contains("wpp")) {
                canalIdentificado = "WHATSAPP";
                tipoCanal = "DIGITAL";
            } else if (canalLower.contains("email") || canalLower.contains("mail")) {
                canalIdentificado = "EMAIL";
                tipoCanal = "DIGITAL";
            } else if (canalLower.contains("telefone") || canalLower.contains("phone") || canalLower.contains("ligacao")) {
                canalIdentificado = "TELEFONE";
                tipoCanal = "VOICE";
            } else if (canalLower.contains("app") || canalLower.contains("aplicativo")) {
                canalIdentificado = "APP_MOBILE";
                tipoCanal = "DIGITAL";
            } else if (canalLower.contains("portal") || canalLower.contains("web")) {
                canalIdentificado = "PORTAL_WEB";
                tipoCanal = "DIGITAL";
            } else {
                canalIdentificado = "OUTRO";
                tipoCanal = "INDEFINIDO";
            }
        } else {
            canalIdentificado = "DESCONHECIDO";
            tipoCanal = "INDEFINIDO";
        }

        execution.setVariable("canalIdentificado", canalIdentificado);
        execution.setVariable("tipoCanal", tipoCanal);

        LOGGER.info("Canal identificado: {} - Tipo: {}", canalIdentificado, tipoCanal);
    }
}
