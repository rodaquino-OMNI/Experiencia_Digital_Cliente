package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

/**
 * Java Delegate para verificação de cobertura contratual
 *
 * Responsabilidades:
 * - Verificar cobertura de procedimentos
 * - Validar carências
 * - Consultar rol ANS
 *
 * Uso no BPMN:
 * <serviceTask id="Task_VerificarCobertura"
 *              name="Verificar Cobertura"
 *              camunda:delegateExpression="${coberturaService.verificar}">
 * </serviceTask>
 */
@Component("coberturaService")
public class CoberturaService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoberturaService.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.info("Verificando cobertura para processo: {}", execution.getProcessInstanceId());

        try {
            verificar(execution);
        } catch (Exception e) {
            LOGGER.error("Erro ao verificar cobertura: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Verifica cobertura contratual do procedimento
     *
     * Input:
     * - beneficiarioId (String)
     * - codigoProcedimento (String)
     * - tipoGuia (String)
     *
     * Output:
     * - coberturaValida (Boolean): Se tem cobertura
     * - carenciaAtiva (Boolean): Se está em carência
     * - diasCarenciaRestantes (Integer)
     */
    private void verificar(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String codigoProcedimento = (String) execution.getVariable("codigoProcedimento");
        String tipoGuia = (String) execution.getVariable("tipoGuia");

        LOGGER.info("Verificando cobertura - Beneficiário: {}, Procedimento: {}",
                   beneficiarioId, codigoProcedimento);

        // Simulação de verificação de cobertura
        boolean coberturaValida = true; // Simulado
        boolean carenciaAtiva = false; // Simulado
        int diasCarenciaRestantes = 0; // Simulado

        Map<String, Object> detalhesCobertura = new HashMap<>();
        detalhesCobertura.put("codigoProcedimento", codigoProcedimento);
        detalhesCobertura.put("tipoGuia", tipoGuia);
        detalhesCobertura.put("rolANS", true);
        detalhesCobertura.put("coberturaContratual", true);

        execution.setVariable("coberturaValida", coberturaValida);
        execution.setVariable("carenciaAtiva", carenciaAtiva);
        execution.setVariable("diasCarenciaRestantes", diasCarenciaRestantes);
        execution.setVariable("detalhesCobertura", detalhesCobertura);

        LOGGER.info("Cobertura verificada - Válida: {}, Carência: {}",
                   coberturaValida, carenciaAtiva);
    }
}
