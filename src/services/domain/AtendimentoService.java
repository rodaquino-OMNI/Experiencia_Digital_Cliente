package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Java Delegate para registro de atendimentos
 *
 * Responsabilidades:
 * - Registrar atendimentos realizados
 * - Consolidar informações do atendimento
 * - Gerar métricas de atendimento
 *
 * Uso no BPMN:
 * <serviceTask id="Task_RegistrarAtendimento"
 *              name="Registrar Atendimento"
 *              camunda:delegateExpression="${atendimentoService.registrar}">
 * </serviceTask>
 */
@Component("atendimentoService")
public class AtendimentoService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtendimentoService.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.info("Registrando atendimento para processo: {}", execution.getProcessInstanceId());

        try {
            registrar(execution);
        } catch (Exception e) {
            LOGGER.error("Erro ao registrar atendimento: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Registra atendimento realizado
     *
     * Input:
     * - beneficiarioId (String)
     * - camadaAtendimento (String): SELF_SERVICE, AGENTE_IA, NAVEGADOR
     * - demandaResolvida (Boolean)
     * - interacaoId (String)
     *
     * Output:
     * - atendimentoId (String): ID do atendimento registrado
     * - dataAtendimento (String): Data do atendimento
     */
    private void registrar(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String camadaAtendimento = (String) execution.getVariable("camadaDestino");
        Boolean demandaResolvida = (Boolean) execution.getVariable("demandaResolvida");
        String interacaoId = (String) execution.getVariable("interacaoId");

        if (demandaResolvida == null) {
            demandaResolvida = false;
        }

        LOGGER.info("Registrando atendimento - Beneficiário: {}, Camada: {}, Resolvida: {}",
                   beneficiarioId, camadaAtendimento, demandaResolvida);

        String atendimentoId = "ATD-" + System.currentTimeMillis();
        String dataAtendimento = LocalDateTime.now().toString();

        Map<String, Object> dadosAtendimento = new HashMap<>();
        dadosAtendimento.put("atendimentoId", atendimentoId);
        dadosAtendimento.put("beneficiarioId", beneficiarioId);
        dadosAtendimento.put("camadaAtendimento", camadaAtendimento);
        dadosAtendimento.put("demandaResolvida", demandaResolvida);
        dadosAtendimento.put("interacaoId", interacaoId);
        dadosAtendimento.put("dataAtendimento", dataAtendimento);

        execution.setVariable("atendimentoId", atendimentoId);
        execution.setVariable("dataAtendimento", dataAtendimento);
        execution.setVariable("dadosAtendimento", dadosAtendimento);

        LOGGER.info("Atendimento registrado com sucesso - ID: {}", atendimentoId);
    }
}
