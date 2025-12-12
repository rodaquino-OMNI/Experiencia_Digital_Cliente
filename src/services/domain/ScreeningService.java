package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Screening Service - Health Assessment Screening Delegate
 *
 * Distributes health screening modules and validates responses
 * for risk identification and care planning.
 *
 * BPMN Coverage:
 * - screeningService.enviarModulo (Send screening module)
 * - screeningService.validarRespostas (Validate screening responses)
 */
@Component("screeningService")
public class ScreeningService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(ScreeningService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("screeningMethod");

        if ("enviarModulo".equals(method)) {
            enviarModulo(execution);
        } else if ("validarRespostas".equals(method)) {
            validarRespostas(execution);
        } else {
            logger.warn("Unknown screening method: {}", method);
            throw new IllegalArgumentException("Invalid screening method: " + method);
        }
    }

    /**
     * Send screening module
     *
     * Distributes health screening questionnaires to beneficiaries
     * via preferred channels with tracking and reminders.
     *
     * @param execution Process execution context
     */
    public void enviarModulo(DelegateExecution execution) throws Exception {
        logger.info("Executing screeningService.enviarModulo for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String tipoScreening = (String) execution.getVariable("tipoScreening");
            String canal = (String) execution.getVariable("canalEnvio");

            // Select appropriate screening module
            Map<String, Object> moduloScreening = selecionarModulo(tipoScreening);

            // Build screening distribution record
            Map<String, Object> envioScreening = new HashMap<>();
            envioScreening.put("screeningId", UUID.randomUUID().toString());
            envioScreening.put("beneficiarioId", beneficiarioId);
            envioScreening.put("tipoScreening", tipoScreening);
            envioScreening.put("moduloId", moduloScreening.get("moduloId"));
            envioScreening.put("nomeModulo", moduloScreening.get("nome"));
            envioScreening.put("numeroQuestoes", moduloScreening.get("numeroQuestoes"));
            envioScreening.put("canalEnvio", canal);
            envioScreening.put("dataEnvio", LocalDateTime.now().toString());
            envioScreening.put("dataExpiracao", LocalDateTime.now().plusDays(7).toString());
            envioScreening.put("status", "ENVIADO");
            envioScreening.put("tentativasLembrete", 0);

            // Store screening distribution
            dataLakeService.salvar(execution, "screenings_enviados",
                (String) envioScreening.get("screeningId"), envioScreening);

            // Publish screening sent event
            kafkaPublisher.publicar(execution, "screening-enviado", Map.of(
                "screeningId", envioScreening.get("screeningId"),
                "beneficiarioId", beneficiarioId,
                "tipoScreening", tipoScreening,
                "canal", canal
            ));

            execution.setVariable("screeningId", envioScreening.get("screeningId"));
            execution.setVariable("screeningEnviado", true);
            execution.setVariable("dataExpiracaoScreening", envioScreening.get("dataExpiracao"));

            logger.info("Screening module sent: id={}, type={}, beneficiary={}, channel={}",
                envioScreening.get("screeningId"), tipoScreening, beneficiarioId, canal);

        } catch (Exception e) {
            logger.error("Error sending screening module for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("screeningEnvioErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Validate screening responses
     *
     * Validates completed screening responses, calculates risk scores,
     * and identifies flags requiring clinical attention.
     *
     * @param execution Process execution context
     */
    public void validarRespostas(DelegateExecution execution) throws Exception {
        logger.info("Executing screeningService.validarRespostas for process {}",
            execution.getProcessInstanceId());

        try {
            String screeningId = (String) execution.getVariable("screeningId");
            Map<String, Object> respostas = (Map<String, Object>) execution.getVariable("respostasScreening");

            // Fetch screening module for validation rules
            Map<String, Object> screening = dataLakeService.consultar(
                execution, "screenings_enviados", screeningId);

            String tipoScreening = (String) screening.get("tipoScreening");

            // Validate completeness
            boolean completo = validarCompletude(respostas, screening);
            List<String> questoesPendentes = identificarQuestoesPendentes(respostas, screening);

            // Calculate scores and identify risks
            Map<String, Object> analiseRespostas = analisarRespostas(respostas, tipoScreening);

            // Build validation result
            Map<String, Object> validacao = new HashMap<>();
            validacao.put("screeningId", screeningId);
            validacao.put("dataValidacao", LocalDateTime.now().toString());
            validacao.put("completo", completo);
            validacao.put("questoesPendentes", questoesPendentes);
            validacao.put("scoreTotal", analiseRespostas.get("scoreTotal"));
            validacao.put("nivelRisco", analiseRespostas.get("nivelRisco"));
            validacao.put("flagsIdentificadas", analiseRespostas.get("flags"));
            validacao.put("recomendacoes", analiseRespostas.get("recomendacoes"));

            // Update screening record
            Map<String, Object> updateScreening = new HashMap<>();
            updateScreening.put("status", completo ? "COMPLETO" : "PARCIAL");
            updateScreening.put("dataResposta", LocalDateTime.now().toString());
            updateScreening.put("respostas", respostas);
            updateScreening.put("validacao", validacao);

            dataLakeService.atualizar(execution, "screenings_enviados", screeningId, updateScreening);

            // If high risk, trigger care coordination
            String nivelRisco = (String) analiseRespostas.get("nivelRisco");
            boolean requerIntervencao = "ALTO".equals(nivelRisco) || "CRITICO".equals(nivelRisco);

            if (requerIntervencao) {
                kafkaPublisher.publicar(execution, "screening-risco-identificado", Map.of(
                    "screeningId", screeningId,
                    "beneficiarioId", screening.get("beneficiarioId"),
                    "nivelRisco", nivelRisco,
                    "flags", analiseRespostas.get("flags")
                ));
            }

            execution.setVariable("screeningValidado", true);
            execution.setVariable("screeningCompleto", completo);
            execution.setVariable("scoreScreening", analiseRespostas.get("scoreTotal"));
            execution.setVariable("nivelRiscoScreening", nivelRisco);
            execution.setVariable("requerIntervencao", requerIntervencao);
            execution.setVariable("flagsScreening", analiseRespostas.get("flags"));

            logger.info("Screening responses validated: id={}, complete={}, risk={}, intervention={}",
                screeningId, completo, nivelRisco, requerIntervencao);

        } catch (Exception e) {
            logger.error("Error validating screening responses for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("screeningValidacaoErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Select appropriate screening module
     */
    private Map<String, Object> selecionarModulo(String tipo) {
        Map<String, Object> modulo = new HashMap<>();

        switch (tipo) {
            case "SAUDE_GERAL":
                modulo.put("moduloId", "MOD-001");
                modulo.put("nome", "Avaliação de Saúde Geral");
                modulo.put("numeroQuestoes", 25);
                break;
            case "CRONICO":
                modulo.put("moduloId", "MOD-002");
                modulo.put("nome", "Rastreamento Condições Crônicas");
                modulo.put("numeroQuestoes", 30);
                break;
            case "MENTAL":
                modulo.put("moduloId", "MOD-003");
                modulo.put("nome", "Avaliação Saúde Mental (PHQ-9)");
                modulo.put("numeroQuestoes", 15);
                break;
            case "PREVENTIVO":
                modulo.put("moduloId", "MOD-004");
                modulo.put("nome", "Triagem Preventiva");
                modulo.put("numeroQuestoes", 20);
                break;
            default:
                modulo.put("moduloId", "MOD-000");
                modulo.put("nome", "Módulo Padrão");
                modulo.put("numeroQuestoes", 20);
        }

        return modulo;
    }

    /**
     * Validate response completeness
     */
    private boolean validarCompletude(Map<String, Object> respostas, Map<String, Object> screening) {
        Integer numeroQuestoes = (Integer) screening.get("numeroQuestoes");
        return respostas != null && respostas.size() >= numeroQuestoes;
    }

    /**
     * Identify unanswered questions
     */
    private List<String> identificarQuestoesPendentes(Map<String, Object> respostas,
                                                      Map<String, Object> screening) {
        List<String> pendentes = new ArrayList<>();
        // Simplified - would compare against full questionnaire
        return pendentes;
    }

    /**
     * Analyze responses and calculate risk scores
     */
    private Map<String, Object> analisarRespostas(Map<String, Object> respostas, String tipo) {
        Map<String, Object> analise = new HashMap<>();

        int scoreTotal = 0;
        List<String> flags = new ArrayList<>();
        List<String> recomendacoes = new ArrayList<>();

        // Simplified scoring logic
        for (Map.Entry<String, Object> entry : respostas.entrySet()) {
            if (entry.getValue() instanceof Integer) {
                scoreTotal += (Integer) entry.getValue();
            }
        }

        // Determine risk level
        String nivelRisco;
        if (scoreTotal >= 80) {
            nivelRisco = "CRITICO";
            flags.add("Score crítico - requer avaliação imediata");
            recomendacoes.add("Contato de navegador em 24h");
        } else if (scoreTotal >= 60) {
            nivelRisco = "ALTO";
            flags.add("Múltiplos fatores de risco identificados");
            recomendacoes.add("Agendar consulta preventiva");
        } else if (scoreTotal >= 40) {
            nivelRisco = "MEDIO";
            recomendacoes.add("Monitoramento regular");
        } else {
            nivelRisco = "BAIXO";
            recomendacoes.add("Manter cuidados preventivos");
        }

        analise.put("scoreTotal", scoreTotal);
        analise.put("nivelRisco", nivelRisco);
        analise.put("flags", flags);
        analise.put("recomendacoes", recomendacoes);

        return analise;
    }
}
