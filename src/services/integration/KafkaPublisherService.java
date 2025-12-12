package br.com.austa.experiencia.service.integration;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Java Delegate para publicação de eventos no Kafka
 *
 * Responsabilidades:
 * - Publicar eventos de jornada do beneficiário
 * - Publicar eventos de métricas e observabilidade
 * - Publicar eventos de auditoria
 * - Garantir rastreabilidade com correlation ID
 *
 * Uso no BPMN:
 * <serviceTask id="ServiceTask_PublicarEvento"
 *              name="Publicar Evento Kafka"
 *              camunda:delegateExpression="${kafkaPublisherService}">
 *   <extensionElements>
 *     <camunda:inputOutput>
 *       <camunda:inputParameter name="eventoTipo">BeneficiarioPerfilCompleto</camunda:inputParameter>
 *       <camunda:inputParameter name="topico">austa.jornada</camunda:inputParameter>
 *     </camunda:inputOutput>
 *   </extensionElements>
 * </serviceTask>
 */
@Component("kafkaPublisherService")
public class KafkaPublisherService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPublisherService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String eventoTipo = (String) execution.getVariable("eventoTipo");
        String topico = (String) execution.getVariable("topico");

        if (topico == null) {
            topico = determinarTopico(eventoTipo);
        }

        LOGGER.info("Publicando evento {} no tópico {} - Processo: {}",
                   eventoTipo, topico, execution.getProcessInstanceId());

        try {
            Map<String, Object> evento = construirEvento(execution, eventoTipo);
            String eventoJson = objectMapper.writeValueAsString(evento);

            String chave = (String) execution.getVariable("beneficiarioId");
            if (chave == null) {
                chave = execution.getProcessInstanceId();
            }

            publishWithCallback(topico, chave, eventoJson, execution);

            execution.setVariable("eventoPublicado", true);
            execution.setVariable("eventoId", evento.get("eventoId"));

        } catch (Exception e) {
            LOGGER.error("Erro ao publicar evento no Kafka: {}", e.getMessage(), e);
            execution.setVariable("eventoPublicado", false);
            execution.setVariable("eventoErro", e.getMessage());
            throw new Exception("ERR_KAFKA_PUBLISH", e);
        }
    }

    /**
     * Constrói o payload do evento com metadados padrão
     */
    private Map<String, Object> construirEvento(DelegateExecution execution, String eventoTipo) {
        Map<String, Object> evento = new HashMap<>();

        // Metadados obrigatórios
        evento.put("eventoId", UUID.randomUUID().toString());
        evento.put("eventoTipo", eventoTipo);
        evento.put("timestamp", Instant.now().toString());
        evento.put("processInstanceId", execution.getProcessInstanceId());
        evento.put("processDefinitionKey", execution.getProcessDefinitionId());
        evento.put("activityId", execution.getCurrentActivityId());

        // Correlation ID para rastreabilidade
        String correlationId = (String) execution.getVariable("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            execution.setVariable("correlationId", correlationId);
        }
        evento.put("correlationId", correlationId);

        // Dados do beneficiário
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        if (beneficiarioId != null) {
            evento.put("beneficiarioId", beneficiarioId);
        }

        // Payload específico do evento
        Map<String, Object> payload = construirPayload(execution, eventoTipo);
        evento.put("payload", payload);

        // Metadados adicionais
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "camunda-bpmn");
        metadata.put("version", "1.0");
        metadata.put("tenant", "austa");
        evento.put("metadata", metadata);

        return evento;
    }

    /**
     * Constrói o payload específico baseado no tipo de evento
     */
    private Map<String, Object> construirPayload(DelegateExecution execution, String eventoTipo) {
        Map<String, Object> payload = new HashMap<>();

        switch (eventoTipo) {
            case "BeneficiarioPerfilCompleto":
                payload.put("scoreRisco", execution.getVariable("scoreRisco"));
                payload.put("classificacaoRisco", execution.getVariable("classificacaoRisco"));
                payload.put("statusCPT", execution.getVariable("statusCPT"));
                break;

            case "OnboardingConcluido":
                payload.put("tempoOnboarding", execution.getVariable("tempoOnboarding"));
                payload.put("taxaConclusao", execution.getVariable("taxaConclusao"));
                break;

            case "AutorizacaoProcessada":
                payload.put("numeroAutorizacao", execution.getVariable("numeroAutorizacao"));
                payload.put("decisaoAutorizacao", execution.getVariable("decisaoAutorizacao"));
                payload.put("tipoProcedimento", execution.getVariable("tipoProcedimento"));
                payload.put("valorProcedimento", execution.getVariable("valorProcedimento"));
                break;

            case "JornadaCompleta":
                payload.put("tempoTotal", execution.getVariable("tempoTotal"));
                payload.put("totalTouchpoints", execution.getVariable("totalTouchpoints"));
                payload.put("custoTotal", execution.getVariable("custoTotal"));
                payload.put("npsScore", execution.getVariable("npsScore"));
                break;

            case "AlertaAltoRisco":
                payload.put("scorePredicaoInternacao", execution.getVariable("scorePredicaoInternacao"));
                payload.put("fatoresRisco", execution.getVariable("fatoresRisco"));
                break;

            case "AcoesProativasExecutadas":
                payload.put("gatilhosAtivados", execution.getVariable("gatilhosAtivados"));
                payload.put("acoesExecutadas", execution.getVariable("acoesExecutadas"));
                break;

            default:
                // Incluir todas as variáveis do processo como payload genérico
                execution.getVariables().forEach((key, value) -> {
                    if (!key.startsWith("_") && value != null) {
                        payload.put(key, value);
                    }
                });
        }

        return payload;
    }

    /**
     * Determina o tópico Kafka baseado no tipo de evento
     */
    private String determinarTopico(String eventoTipo) {
        if (eventoTipo.contains("Onboarding") || eventoTipo.contains("Perfil")) {
            return "austa.jornada.onboarding";
        } else if (eventoTipo.contains("Autorizacao")) {
            return "austa.autorizacao";
        } else if (eventoTipo.contains("Alerta") || eventoTipo.contains("Risco")) {
            return "austa.alertas";
        } else if (eventoTipo.contains("Proativ")) {
            return "austa.proatividade";
        } else if (eventoTipo.contains("Jornada")) {
            return "austa.jornada";
        } else {
            return "austa.eventos.geral";
        }
    }

    /**
     * Publica mensagem no Kafka com callback para tratamento de sucesso/erro
     */
    private void publishWithCallback(String topico, String chave, String mensagem,
                                     DelegateExecution execution) {
        ListenableFuture<SendResult<String, String>> future =
            kafkaTemplate.send(topico, chave, mensagem);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                LOGGER.info("Evento publicado com sucesso no tópico {} - Partition: {} - Offset: {}",
                           topico,
                           result.getRecordMetadata().partition(),
                           result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                LOGGER.error("Falha ao publicar evento no tópico {}: {}", topico, ex.getMessage(), ex);
                execution.setVariable("kafkaErro", true);
                execution.setVariable("kafkaErroMensagem", ex.getMessage());
            }
        });
    }
}
