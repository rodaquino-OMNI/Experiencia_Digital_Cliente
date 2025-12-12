package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Dados Service - Contextual Data Collection Delegate
 *
 * Collects and enriches beneficiary context from multiple sources:
 * - Tasy ERP system
 * - Data Lake analytics
 * - External health APIs
 * - Cached historical data
 *
 * BPMN Coverage:
 * - dadosService.coletarContexto (Collect updated beneficiary context)
 */
@Component("dadosService")
public class DadosService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(DadosService.class);

    @Autowired
    private TasyBeneficiarioService tasyService;

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private ContextoService contextoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("dadosMethod");

        if ("coletarContexto".equals(method)) {
            coletarContexto(execution);
        } else {
            logger.warn("Unknown dados method: {}", method);
            throw new IllegalArgumentException("Invalid dados method: " + method);
        }
    }

    /**
     * Collect updated beneficiary context
     *
     * Aggregates data from multiple sources to build comprehensive
     * beneficiary profile for decision-making and personalization.
     *
     * Data sources:
     * 1. Tasy ERP - Demographic, plan, claims
     * 2. Data Lake - Analytics, risk scores, utilization patterns
     * 3. External APIs - Health data, pharmacy, care coordination
     * 4. Cache - Recent interactions and preferences
     *
     * @param execution Process execution context
     */
    @Cacheable(value = "beneficiaryContext", key = "#execution.getVariable('beneficiarioId')", unless = "#result == null")
    public void coletarContexto(DelegateExecution execution) throws Exception {
        logger.info("Executing dadosService.coletarContexto for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String cpf = (String) execution.getVariable("cpf");

            Map<String, Object> contextoCompleto = new HashMap<>();
            List<String> fontesColetadas = new ArrayList<>();

            // 1. Collect Tasy ERP data
            logger.info("Collecting Tasy data for beneficiary {}", beneficiarioId);
            try {
                Map<String, Object> dadosTasy = tasyService.consultar(execution, beneficiarioId);
                contextoCompleto.put("dadosCadastrais", dadosTasy.get("dadosCadastrais"));
                contextoCompleto.put("planoVigente", dadosTasy.get("plano"));
                contextoCompleto.put("dependentes", dadosTasy.get("dependentes"));
                contextoCompleto.put("statusContratual", dadosTasy.get("status"));
                fontesColetadas.add("TASY_ERP");
            } catch (Exception e) {
                logger.error("Error collecting Tasy data: {}", e.getMessage());
                contextoCompleto.put("tasyErro", e.getMessage());
            }

            // 2. Collect Data Lake analytics
            logger.info("Collecting Data Lake analytics for beneficiary {}", beneficiarioId);
            try {
                Map<String, Object> dadosDataLake = dataLakeService.consultar(
                    execution, "beneficiarios_analytics", beneficiarioId);

                contextoCompleto.put("riscoSaude", dadosDataLake.get("riscoSaude"));
                contextoCompleto.put("scoreUtilizacao", dadosDataLake.get("scoreUtilizacao"));
                contextoCompleto.put("padraoConsultas", dadosDataLake.get("padraoConsultas"));
                contextoCompleto.put("medicamentosUso", dadosDataLake.get("medicamentosUso"));
                contextoCompleto.put("historicoInternacoes", dadosDataLake.get("historicoInternacoes"));
                contextoCompleto.put("doencasCronicas", dadosDataLake.get("doencasCronicas"));
                fontesColetadas.add("DATA_LAKE");
            } catch (Exception e) {
                logger.error("Error collecting Data Lake data: {}", e.getMessage());
                contextoCompleto.put("dataLakeErro", e.getMessage());
            }

            // 3. Collect external health data
            logger.info("Collecting external health data for CPF {}", cpf);
            try {
                Map<String, Object> dadosExternos = coletarDadosExternos(cpf);
                contextoCompleto.put("dadosRnds", dadosExternos.get("rnds")); // National Health Data Network
                contextoCompleto.put("dadosPep", dadosExternos.get("pep")); // Electronic Patient Record
                contextoCompleto.put("prescricoesExternas", dadosExternos.get("prescricoes"));
                fontesColetadas.add("EXTERNAL_APIS");
            } catch (Exception e) {
                logger.error("Error collecting external data: {}", e.getMessage());
                contextoCompleto.put("externosErro", e.getMessage());
            }

            // 4. Collect interaction history and preferences
            logger.info("Collecting interaction history for beneficiary {}", beneficiarioId);
            try {
                Map<String, Object> historicoInteracoes = contextoService.consultar(
                    execution, "historico_" + beneficiarioId);

                contextoCompleto.put("ultimasInteracoes", historicoInteracoes.get("interacoes"));
                contextoCompleto.put("canaisPreferencia", historicoInteracoes.get("canaisPreferidos"));
                contextoCompleto.put("horariosPreferencia", historicoInteracoes.get("horariosPreferidos"));
                contextoCompleto.put("nps", historicoInteracoes.get("npsScore"));
                contextoCompleto.put("satisfacaoGeral", historicoInteracoes.get("satisfacaoGeral"));
                fontesColetadas.add("INTERACTION_HISTORY");
            } catch (Exception e) {
                logger.error("Error collecting interaction history: {}", e.getMessage());
                contextoCompleto.put("historicoErro", e.getMessage());
            }

            // 5. Enrich with computed insights
            enrichContextoComputado(contextoCompleto);
            fontesColetadas.add("COMPUTED_INSIGHTS");

            // Store complete context
            contextoCompleto.put("fontesColetadas", fontesColetadas);
            contextoCompleto.put("coletadoEm", LocalDateTime.now().toString());
            contextoCompleto.put("beneficiarioId", beneficiarioId);

            execution.setVariable("contextoCompleto", contextoCompleto);
            execution.setVariable("contextoFontes", fontesColetadas);
            execution.setVariable("contextoColetadoSucesso", true);

            logger.info("Context collection complete. Sources: {}, Data points: {}",
                fontesColetadas.size(), contextoCompleto.size());

        } catch (Exception e) {
            logger.error("Critical error collecting context for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("contextoColetadoSucesso", false);
            execution.setVariable("contextoErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Collect external health data from RNDS and other sources
     */
    private Map<String, Object> coletarDadosExternos(String cpf) {
        Map<String, Object> dadosExternos = new HashMap<>();

        // Placeholder for RNDS (Rede Nacional de Dados em Saúde) integration
        dadosExternos.put("rnds", Map.of(
            "disponivel", false,
            "mensagem", "Integração RNDS não configurada"
        ));

        // Placeholder for PEP (Prontuário Eletrônico do Paciente)
        dadosExternos.put("pep", Map.of(
            "disponivel", false,
            "mensagem", "Integração PEP não configurada"
        ));

        dadosExternos.put("prescricoes", new ArrayList<>());

        return dadosExternos;
    }

    /**
     * Enrich context with computed insights and aggregations
     */
    private void enrichContextoComputado(Map<String, Object> contexto) {
        // Risk stratification
        Object riscoSaude = contexto.get("riscoSaude");
        if (riscoSaude != null) {
            contexto.put("estratoRisco", calcularEstratoRisco(riscoSaude.toString()));
        }

        // Care needs assessment
        List<String> doencasCronicas = (List<String>) contexto.getOrDefault("doencasCronicas", new ArrayList<>());
        contexto.put("elegibilidadeCarePlan", doencasCronicas.size() >= 2);

        // Engagement score
        Object nps = contexto.get("nps");
        Object satisfacao = contexto.get("satisfacaoGeral");
        if (nps != null && satisfacao != null) {
            contexto.put("scoreEngajamento", calcularEngajamento(nps, satisfacao));
        }

        // Completeness score
        int camposPreenchidos = (int) contexto.values().stream()
            .filter(v -> v != null && !v.toString().isEmpty())
            .count();
        contexto.put("percentualCompletudeContexto", (camposPreenchidos * 100) / 20); // 20 expected fields
    }

    private String calcularEstratoRisco(String riscoSaude) {
        try {
            int score = Integer.parseInt(riscoSaude);
            if (score >= 80) return "CRITICO";
            if (score >= 60) return "ALTO";
            if (score >= 40) return "MEDIO";
            return "BAIXO";
        } catch (Exception e) {
            return "DESCONHECIDO";
        }
    }

    private int calcularEngajamento(Object nps, Object satisfacao) {
        // Simple engagement calculation from NPS and satisfaction
        return 75; // Placeholder
    }
}
