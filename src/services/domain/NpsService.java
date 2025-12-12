package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * NPS Service - Net Promoter Score Management Delegate
 *
 * Handles NPS non-response tracking, follow-up campaigns,
 * and sentiment analysis for customer satisfaction monitoring.
 *
 * BPMN Coverage:
 * - npsService.registrarNaoRespondeu (Register NPS non-response)
 */
@Component("npsService")
public class NpsService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(NpsService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("npsMethod");

        if ("registrarNaoRespondeu".equals(method)) {
            registrarNaoRespondeu(execution);
        } else {
            logger.warn("Unknown NPS method: {}", method);
            throw new IllegalArgumentException("Invalid NPS method: " + method);
        }
    }

    /**
     * Register NPS non-response
     *
     * Tracks beneficiaries who did not respond to NPS surveys
     * for follow-up campaigns and engagement analysis.
     *
     * @param execution Process execution context
     */
    public void registrarNaoRespondeu(DelegateExecution execution) throws Exception {
        logger.info("Executing npsService.registrarNaoRespondeu for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String pesquisaId = (String) execution.getVariable("pesquisaId");
            String canalEnvio = (String) execution.getVariable("canalEnvio");
            String etapaJornada = (String) execution.getVariable("etapaJornada");

            // Build non-response record
            Map<String, Object> registroNaoResposta = new HashMap<>();
            registroNaoResposta.put("beneficiarioId", beneficiarioId);
            registroNaoResposta.put("pesquisaId", pesquisaId);
            registroNaoResposta.put("canalEnvio", canalEnvio);
            registroNaoResposta.put("etapaJornada", etapaJornada);
            registroNaoResposta.put("dataEnvio", execution.getVariable("dataEnvioPesquisa"));
            registroNaoResposta.put("dataExpiracao", execution.getVariable("dataExpiracaoPesquisa"));
            registroNaoResposta.put("registradoEm", LocalDateTime.now().toString());
            registroNaoResposta.put("processInstanceId", execution.getProcessInstanceId());

            // Track reminder attempts
            Integer tentativasLembrete = (Integer) execution.getVariable("tentativasLembrete");
            registroNaoResposta.put("tentativasLembrete", tentativasLembrete != null ? tentativasLembrete : 0);

            // Check beneficiary engagement history
            Map<String, Object> historicoEngajamento = dataLakeService.consultar(
                execution, "beneficiarios_analytics", beneficiarioId);

            Integer naoRespostasAnteriores = (Integer) historicoEngajamento.getOrDefault("npsNaoRespostas", 0);
            Integer totalPesquisasEnviadas = (Integer) historicoEngajamento.getOrDefault("npsTotalEnviadas", 0);

            // Calculate response rate
            double taxaRespostaHistorica = 0.0;
            if (totalPesquisasEnviadas > 0) {
                int respostasHistoricas = totalPesquisasEnviadas - naoRespostasAnteriores;
                taxaRespostaHistorica = (respostasHistoricas * 100.0) / totalPesquisasEnviadas;
            }

            registroNaoResposta.put("naoRespostasAnteriores", naoRespostasAnteriores);
            registroNaoResposta.put("taxaRespostaHistorica", taxaRespostaHistorica);

            // Determine follow-up strategy
            String estrategiaFollowUp = determinarEstrategiaFollowUp(
                naoRespostasAnteriores, taxaRespostaHistorica, canalEnvio);

            registroNaoResposta.put("estrategiaFollowUp", estrategiaFollowUp);

            // Save non-response record
            dataLakeService.salvar(execution, "nps_nao_respostas",
                pesquisaId + "_" + beneficiarioId, registroNaoResposta);

            // Publish non-response event
            kafkaPublisher.publicar(execution, "nps-nao-resposta", registroNaoResposta);

            // Update beneficiary engagement metrics
            Map<String, Object> updateEngajamento = new HashMap<>();
            updateEngajamento.put("npsNaoRespostas", naoRespostasAnteriores + 1);
            updateEngajamento.put("ultimaNaoResposta", LocalDateTime.now().toString());
            updateEngajamento.put("taxaRespostaNPS", taxaRespostaHistorica);

            dataLakeService.atualizar(execution, "beneficiarios_analytics",
                beneficiarioId, updateEngajamento);

            execution.setVariable("naoRespostaRegistrada", true);
            execution.setVariable("estrategiaFollowUp", estrategiaFollowUp);
            execution.setVariable("requireFollowUp", shouldScheduleFollowUp(estrategiaFollowUp));

            logger.info("NPS non-response registered: beneficiary={}, survey={}, strategy={}",
                beneficiarioId, pesquisaId, estrategiaFollowUp);

        } catch (Exception e) {
            logger.error("Error registering NPS non-response for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("naoRespostaErro", e.getMessage());
            // Non-critical error - don't throw
        }
    }

    /**
     * Determine follow-up strategy based on history
     */
    private String determinarEstrategiaFollowUp(
        int naoRespostasAnteriores, double taxaResposta, String canalOriginal) {

        // High non-response rate - change approach
        if (taxaResposta < 20 && naoRespostasAnteriores >= 3) {
            return "MUDAR_CANAL_INCENTIVO";
        }

        // Moderate non-response - try different channel
        if (naoRespostasAnteriores >= 2) {
            if ("EMAIL".equals(canalOriginal)) {
                return "TENTAR_SMS";
            } else if ("SMS".equals(canalOriginal)) {
                return "TENTAR_TELEFONE";
            } else {
                return "TENTAR_APP";
            }
        }

        // First non-response - gentle reminder
        if (naoRespostasAnteriores == 0) {
            return "LEMBRETE_SIMPLES";
        }

        // Default - try same channel with different timing
        return "REENVIAR_HORARIO_ALTERNATIVO";
    }

    /**
     * Determine if follow-up should be scheduled
     */
    private boolean shouldScheduleFollowUp(String estrategia) {
        // Don't schedule follow-up if giving up
        return !"NAO_INSISTIR".equals(estrategia) &&
               !"MUDAR_CANAL_INCENTIVO".equals(estrategia);
    }
}
