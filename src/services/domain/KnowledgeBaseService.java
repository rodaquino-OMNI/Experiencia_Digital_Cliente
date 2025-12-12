package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Knowledge Base Service - FAQ and Knowledge Repository Delegate
 *
 * Provides intelligent FAQ responses using NLP query matching
 * and confidence scoring for self-service support.
 *
 * BPMN Coverage:
 * - knowledgeBaseService.responder (Answer FAQ queries)
 */
@Component("knowledgeBaseService")
public class KnowledgeBaseService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${knowledgebase.api.url:http://localhost:8080/api/kb}")
    private String knowledgeBaseApiUrl;

    private static final double CONFIDENCE_THRESHOLD = 0.7;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("knowledgeBaseMethod");

        if ("responder".equals(method)) {
            responder(execution);
        } else {
            logger.warn("Unknown knowledge base method: {}", method);
            throw new IllegalArgumentException("Invalid knowledge base method: " + method);
        }
    }

    /**
     * Answer FAQ queries
     *
     * Uses NLP to match user queries against knowledge base articles
     * and returns best matching answers with confidence scores.
     *
     * @param execution Process execution context
     */
    public void responder(DelegateExecution execution) throws Exception {
        logger.info("Executing knowledgeBaseService.responder for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract query parameters
            String pergunta = (String) execution.getVariable("pergunta");
            String categoria = (String) execution.getVariable("categoriaFAQ");
            String contexto = (String) execution.getVariable("contextoUsuario");

            if (pergunta == null || pergunta.trim().isEmpty()) {
                logger.warn("Empty FAQ query received");
                execution.setVariable("respostaEncontrada", false);
                execution.setVariable("respostaMensagem", "Pergunta não fornecida");
                return;
            }

            logger.info("Processing FAQ query: '{}'", pergunta);

            // Build query request
            Map<String, Object> queryRequest = new HashMap<>();
            queryRequest.put("query", pergunta);
            queryRequest.put("categoria", categoria);
            queryRequest.put("contexto", contexto);
            queryRequest.put("maxResults", 3);
            queryRequest.put("minConfidence", CONFIDENCE_THRESHOLD);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(queryRequest, headers);

            // Query knowledge base
            ResponseEntity<Map> response = restTemplate.exchange(
                knowledgeBaseApiUrl + "/search",
                HttpMethod.POST,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> resultados =
                    (List<Map<String, Object>>) responseBody.get("results");

                if (resultados != null && !resultados.isEmpty()) {
                    // Get best match
                    Map<String, Object> melhorResposta = resultados.get(0);
                    double confianca = (double) melhorResposta.get("confidence");

                    if (confianca >= CONFIDENCE_THRESHOLD) {
                        String respostaTitulo = (String) melhorResposta.get("title");
                        String respostaConteudo = (String) melhorResposta.get("content");
                        String respostaId = (String) melhorResposta.get("articleId");

                        execution.setVariable("respostaEncontrada", true);
                        execution.setVariable("respostaTitulo", respostaTitulo);
                        execution.setVariable("respostaConteudo", respostaConteudo);
                        execution.setVariable("respostaId", respostaId);
                        execution.setVariable("respostaConfianca", confianca);
                        execution.setVariable("respostasAlternativas",
                            resultados.size() > 1 ? resultados.subList(1, resultados.size()) : new ArrayList<>());

                        logger.info("FAQ answer found: '{}' with confidence {}",
                            respostaTitulo, confianca);

                    } else {
                        logger.info("No confident answer found. Best confidence: {}", confianca);
                        execution.setVariable("respostaEncontrada", false);
                        execution.setVariable("respostaMensagem", "Confiança insuficiente");
                        execution.setVariable("sugestaoEscalacao", true);
                    }

                } else {
                    logger.info("No FAQ matches found for query: '{}'", pergunta);
                    execution.setVariable("respostaEncontrada", false);
                    execution.setVariable("respostaMensagem", "Nenhuma resposta encontrada");
                    execution.setVariable("sugestaoEscalacao", true);
                }

            } else {
                logger.error("Knowledge base API returned non-OK status: {}",
                    response.getStatusCode());
                execution.setVariable("respostaEncontrada", false);
                execution.setVariable("respostaMensagem", "Erro na consulta");
            }

        } catch (Exception e) {
            logger.error("Error querying knowledge base for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("respostaEncontrada", false);
            execution.setVariable("respostaErro", e.getMessage());

            // Fallback: use simple keyword matching
            tentarRespostaFallback(execution);
        }
    }

    /**
     * Fallback: simple keyword-based response matching
     */
    private void tentarRespostaFallback(DelegateExecution execution) {
        String pergunta = (String) execution.getVariable("pergunta");
        if (pergunta == null) return;

        String perguntaLower = pergunta.toLowerCase();

        // Simple keyword matching for common questions
        Map<String, String> respostasProntas = Map.of(
            "cobertura", "Para consultar sua cobertura, acesse o portal do beneficiário ou ligue 0800-XXX-XXXX.",
            "autorização", "Autorizações podem ser solicitadas através do portal ou app. Prazo de resposta: 48h úteis.",
            "reembolso", "Solicitações de reembolso devem ser enviadas em até 30 dias após o atendimento.",
            "carência", "Períodos de carência variam por procedimento. Consulte seu contrato ou fale conosco.",
            "rede credenciada", "Consulte a rede credenciada no portal ou app, filtrada por especialidade e localização."
        );

        for (Map.Entry<String, String> entry : respostasProntas.entrySet()) {
            if (perguntaLower.contains(entry.getKey())) {
                execution.setVariable("respostaEncontrada", true);
                execution.setVariable("respostaConteudo", entry.getValue());
                execution.setVariable("respostaConfianca", 0.6);
                execution.setVariable("respostaFallback", true);
                logger.info("Fallback response provided for keyword: {}", entry.getKey());
                return;
            }
        }

        logger.info("No fallback response available");
        execution.setVariable("sugestaoEscalacao", true);
    }
}
