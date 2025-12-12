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
 * RCA Service - Root Cause Analysis Management Delegate
 *
 * Registers root cause analysis for problems, tracks corrective
 * actions, and enables continuous improvement.
 *
 * BPMN Coverage:
 * - rcaService.registrar (Register RCA record)
 */
@Component("rcaService")
public class RcaService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(RcaService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("rcaMethod");

        if ("registrar".equals(method)) {
            registrar(execution);
        } else {
            logger.warn("Unknown RCA method: {}", method);
            throw new IllegalArgumentException("Invalid RCA method: " + method);
        }
    }

    /**
     * Register RCA record
     *
     * Documents root cause analysis including problem description,
     * root causes identified, corrective actions, and preventive measures.
     *
     * @param execution Process execution context
     */
    public void registrar(DelegateExecution execution) throws Exception {
        logger.info("Executing rcaService.registrar for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract RCA data
            String tipoProblema = (String) execution.getVariable("tipoProblema");
            String descricaoProblema = (String) execution.getVariable("descricaoProblema");
            String severidade = (String) execution.getVariable("severidadeProblema");
            String processo = (String) execution.getVariable("processoAfetado");

            // Build RCA record
            Map<String, Object> registroRCA = new HashMap<>();
            registroRCA.put("rcaId", UUID.randomUUID().toString());
            registroRCA.put("processInstanceId", execution.getProcessInstanceId());
            registroRCA.put("tipoProblema", tipoProblema);
            registroRCA.put("descricaoProblema", descricaoProblema);
            registroRCA.put("severidade", severidade);
            registroRCA.put("processoAfetado", processo);
            registroRCA.put("dataOcorrencia", execution.getVariable("dataOcorrenciaProblema"));
            registroRCA.put("dataRegistro", LocalDateTime.now().toString());

            // Problem categorization
            String categoria = categorizarProblema(tipoProblema);
            registroRCA.put("categoria", categoria);

            // Root causes identification
            List<String> causasRaiz = (List<String>) execution.getVariable("causasRaiz");
            if (causasRaiz == null || causasRaiz.isEmpty()) {
                causasRaiz = identificarCausasRaiz(tipoProblema, descricaoProblema);
            }
            registroRCA.put("causasRaiz", causasRaiz);

            // 5 Whys analysis
            List<String> cincoPorques = (List<String>) execution.getVariable("cincoPorques");
            if (cincoPorques != null) {
                registroRCA.put("analise5Porques", cincoPorques);
            }

            // Corrective actions
            List<Map<String, Object>> acoesCorretivas = definirAcoesCorretivas(causasRaiz, severidade);
            registroRCA.put("acoesCorretivas", acoesCorretivas);

            // Preventive measures
            List<String> medidasPreventivas = definirMedidasPreventivas(categoria, causasRaiz);
            registroRCA.put("medidasPreventivas", medidasPreventivas);

            // Impact analysis
            Map<String, Object> analiseImpacto = analisarImpacto(execution);
            registroRCA.put("analiseImpacto", analiseImpacto);

            // Responsible parties
            registroRCA.put("responsavelRCA", execution.getVariable("responsavelRCA"));
            registroRCA.put("responsavelAcoes", execution.getVariable("responsavelAcoes"));

            // Timeline
            registroRCA.put("prazoImplementacao", calcularPrazoImplementacao(severidade));
            registroRCA.put("dataRevisao", LocalDateTime.now().plusDays(30).toString());

            // Status tracking
            registroRCA.put("status", "ABERTA");
            registroRCA.put("acoesImplementadas", 0);
            registroRCA.put("acoesTotal", acoesCorretivas.size());

            // Save RCA record
            dataLakeService.salvar(execution, "rca_registros",
                (String) registroRCA.get("rcaId"), registroRCA);

            // Publish RCA event
            Map<String, Object> rcaEvent = new HashMap<>();
            rcaEvent.put("rcaId", registroRCA.get("rcaId"));
            rcaEvent.put("tipoProblema", tipoProblema);
            rcaEvent.put("severidade", severidade);
            rcaEvent.put("categoria", categoria);
            rcaEvent.put("numeroCausas", causasRaiz.size());
            rcaEvent.put("numeroAcoes", acoesCorretivas.size());

            kafkaPublisher.publicar(execution, "rca-registrada", rcaEvent);

            execution.setVariable("rcaId", registroRCA.get("rcaId"));
            execution.setVariable("rcaRegistrada", true);
            execution.setVariable("acoesCorretivas", acoesCorretivas);

            logger.info("RCA registered: id={}, problem={}, severity={}, causes={}, actions={}",
                registroRCA.get("rcaId"), tipoProblema, severidade,
                causasRaiz.size(), acoesCorretivas.size());

        } catch (Exception e) {
            logger.error("Error registering RCA for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("rcaErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Categorize problem type
     */
    private String categorizarProblema(String tipoProblema) {
        if (tipoProblema.contains("SISTEMA") || tipoProblema.contains("TECNICO")) {
            return "TECNOLOGIA";
        } else if (tipoProblema.contains("PROCESSO") || tipoProblema.contains("FLUXO")) {
            return "PROCESSO";
        } else if (tipoProblema.contains("TREINAMENTO") || tipoProblema.contains("CONHECIMENTO")) {
            return "PESSOAS";
        } else if (tipoProblema.contains("INTEGRACAO") || tipoProblema.contains("INTERFACE")) {
            return "INTEGRACAO";
        }
        return "OUTROS";
    }

    /**
     * Identify root causes based on problem analysis
     */
    private List<String> identificarCausasRaiz(String tipo, String descricao) {
        List<String> causas = new ArrayList<>();

        // Simplified root cause identification
        if (tipo.contains("SLA")) {
            causas.add("Falta de automação no processo");
            causas.add("Recursos insuficientes");
        } else if (tipo.contains("ERRO")) {
            causas.add("Falta de validação de dados");
            causas.add("Tratamento inadequado de exceções");
        } else if (tipo.contains("INTEGRACAO")) {
            causas.add("Timeout em API externa");
            causas.add("Falta de mecanismo de retry");
        } else {
            causas.add("Causa a ser investigada");
        }

        return causas;
    }

    /**
     * Define corrective actions based on root causes
     */
    private List<Map<String, Object>> definirAcoesCorretivas(List<String> causas, String severidade) {
        List<Map<String, Object>> acoes = new ArrayList<>();

        for (String causa : causas) {
            Map<String, Object> acao = new HashMap<>();
            acao.put("causa", causa);
            acao.put("descricao", "Implementar correção para: " + causa);
            acao.put("tipo", "CORRETIVA");
            acao.put("prioridade", severidade);
            acao.put("status", "PENDENTE");
            acoes.add(acao);
        }

        return acoes;
    }

    /**
     * Define preventive measures
     */
    private List<String> definirMedidasPreventivas(String categoria, List<String> causas) {
        List<String> medidas = new ArrayList<>();

        if ("TECNOLOGIA".equals(categoria)) {
            medidas.add("Implementar monitoramento proativo");
            medidas.add("Adicionar testes automatizados");
        } else if ("PROCESSO".equals(categoria)) {
            medidas.add("Revisar e documentar processo");
            medidas.add("Implementar controles preventivos");
        } else if ("PESSOAS".equals(categoria)) {
            medidas.add("Treinamento e capacitação");
            medidas.add("Atualizar documentação");
        }

        return medidas;
    }

    /**
     * Analyze problem impact
     */
    private Map<String, Object> analisarImpacto(DelegateExecution execution) {
        Map<String, Object> impacto = new HashMap<>();

        Integer beneficiariosAfetados = (Integer) execution.getVariable("beneficiariosAfetados");
        Integer processosAfetados = (Integer) execution.getVariable("processosAfetados");
        Double custoEstimado = (Double) execution.getVariable("custoEstimadoProblema");

        impacto.put("beneficiariosAfetados", beneficiariosAfetados != null ? beneficiariosAfetados : 0);
        impacto.put("processosAfetados", processosAfetados != null ? processosAfetados : 0);
        impacto.put("custoEstimado", custoEstimado != null ? custoEstimado : 0.0);
        impacto.put("impactoReputacional", execution.getVariable("impactoReputacional"));

        return impacto;
    }

    /**
     * Calculate implementation deadline based on severity
     */
    private String calcularPrazoImplementacao(String severidade) {
        LocalDateTime prazo;

        switch (severidade) {
            case "CRITICA":
                prazo = LocalDateTime.now().plusDays(7);
                break;
            case "ALTA":
                prazo = LocalDateTime.now().plusDays(15);
                break;
            case "MEDIA":
                prazo = LocalDateTime.now().plusDays(30);
                break;
            default:
                prazo = LocalDateTime.now().plusDays(60);
        }

        return prazo.toString();
    }
}
