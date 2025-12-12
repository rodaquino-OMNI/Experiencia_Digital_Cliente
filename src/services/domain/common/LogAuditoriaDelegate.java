package com.experiencia.services.domain.common;

import com.experiencia.models.LogAuditoria;
import com.experiencia.repositories.LogAuditoriaRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate: Log de Auditoria
 *
 * Responsabilidades:
 * - Registrar eventos importantes do processo
 * - Capturar contexto completo (variáveis, usuário, timestamps)
 * - Permitir rastreabilidade e compliance
 * - Facilitar debugging e análise de processos
 */
@Component("logAuditoriaDelegate")
public class LogAuditoriaDelegate implements JavaDelegate {

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LogAuditoria log = new LogAuditoria();

        // Dados do processo
        log.setProcessInstanceId(execution.getProcessInstanceId());
        log.setProcessDefinitionId(execution.getProcessDefinitionId());
        log.setActivityId(execution.getCurrentActivityId());
        log.setActivityName(execution.getCurrentActivityName());

        // Timestamp
        log.setDataEvento(LocalDateTime.now());

        // Tipo de evento (configurável via variável)
        String tipoEvento = (String) execution.getVariable("tipoEvento");
        log.setTipoEvento(tipoEvento != null ? tipoEvento : "EXECUCAO_ATIVIDADE");

        // Capturar variáveis relevantes
        Map<String, Object> variaveis = capturarVariaveisRelevantes(execution);
        log.setVariaveis(variaveis);

        // Usuário (se disponível)
        String usuario = (String) execution.getVariable("usuario");
        log.setUsuario(usuario);

        // Salvar log
        logAuditoriaRepository.save(log);
    }

    private Map<String, Object> capturarVariaveisRelevantes(DelegateExecution execution) {
        Map<String, Object> variaveis = new HashMap<>();

        // Lista de variáveis a capturar (configurável)
        String[] variaveisParaCapturar = {
            "clienteId", "interacaoId", "npsScore", "classificacao",
            "sentimento", "urgencia", "severidade", "ticketId"
        };

        for (String nomeVariavel : variaveisParaCapturar) {
            if (execution.hasVariable(nomeVariavel)) {
                variaveis.put(nomeVariavel, execution.getVariable(nomeVariavel));
            }
        }

        return variaveis;
    }
}
