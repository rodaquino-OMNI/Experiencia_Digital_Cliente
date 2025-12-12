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
 * Plano Cuidados Service - Care Plan Management Delegate
 *
 * Creates and validates personalized care plans for chronic disease
 * management with clinical goals and tracking.
 *
 * BPMN Coverage:
 * - planoCuidadosService.criar (Create care plan)
 * - planoCuidadosService.validar (Validate care plan)
 */
@Component("planoCuidadosService")
public class PlanoCuidadosService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(PlanoCuidadosService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("planoCuidadosMethod");

        if ("criar".equals(method)) {
            criar(execution);
        } else if ("validar".equals(method)) {
            validar(execution);
        } else {
            logger.warn("Unknown plano cuidados method: {}", method);
            throw new IllegalArgumentException("Invalid plano cuidados method: " + method);
        }
    }

    /**
     * Create care plan
     *
     * Generates personalized care plan based on beneficiary conditions,
     * risk profile, and clinical guidelines.
     *
     * @param execution Process execution context
     */
    public void criar(DelegateExecution execution) throws Exception {
        logger.info("Executing planoCuidadosService.criar for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            List<String> condicoesCronicas = (List<String>) execution.getVariable("condicoesCronicas");

            // Fetch beneficiary clinical data
            Map<String, Object> dadosClinicos = dataLakeService.consultar(
                execution, "beneficiarios_analytics", beneficiarioId);

            // Create care plan structure
            Map<String, Object> planoCuidados = new HashMap<>();
            planoCuidados.put("planoCuidadosId", UUID.randomUUID().toString());
            planoCuidados.put("beneficiarioId", beneficiarioId);
            planoCuidados.put("dataCriacao", LocalDateTime.now().toString());
            planoCuidados.put("status", "RASCUNHO");
            planoCuidados.put("condicoes", condicoesCronicas);

            // Define care goals based on conditions
            List<Map<String, Object>> metas = definirMetas(condicoesCronicas, dadosClinicos);
            planoCuidados.put("metas", metas);

            // Define interventions
            List<Map<String, Object>> intervencoes = definirIntervencoes(condicoesCronicas);
            planoCuidados.put("intervencoes", intervencoes);

            // Define monitoring schedule
            Map<String, Object> cronogramaMonitoramento = definirCronogramaMonitoramento(condicoesCronicas);
            planoCuidados.put("cronogramaMonitoramento", cronogramaMonitoramento);

            // Assign care team
            Map<String, String> equipeCuidados = atribuirEquipe(condicoesCronicas, dadosClinicos);
            planoCuidados.put("equipeCuidados", equipeCuidados);

            // Define medication reconciliation
            List<String> medicamentos = (List<String>) dadosClinicos.get("medicamentosUso");
            planoCuidados.put("medicamentos", medicamentos != null ? medicamentos : new ArrayList<>());
            planoCuidados.put("reconciliacaoMedicamentosRequerida", true);

            // Calculate complexity score
            int complexidade = calcularComplexidadePlano(condicoesCronicas, dadosClinicos);
            planoCuidados.put("scoreComplexidade", complexidade);

            // Save draft care plan
            dataLakeService.salvar(execution, "planos_cuidados",
                (String) planoCuidados.get("planoCuidadosId"), planoCuidados);

            execution.setVariable("planoCuidados", planoCuidados);
            execution.setVariable("planoCuidadosId", planoCuidados.get("planoCuidadosId"));
            execution.setVariable("planoCriadoSucesso", true);

            logger.info("Care plan created: beneficiary={}, conditions={}, goals={}, complexity={}",
                beneficiarioId, condicoesCronicas.size(), metas.size(), complexidade);

        } catch (Exception e) {
            logger.error("Error creating care plan for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("planoCriacaoErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Validate care plan
     *
     * Performs clinical validation of care plan including guideline
     * compliance, goal appropriateness, and care team approval.
     *
     * @param execution Process execution context
     */
    public void validar(DelegateExecution execution) throws Exception {
        logger.info("Executing planoCuidadosService.validar for process {}",
            execution.getProcessInstanceId());

        try {
            String planoCuidadosId = (String) execution.getVariable("planoCuidadosId");

            // Fetch care plan
            Map<String, Object> planoCuidados = dataLakeService.consultar(
                execution, "planos_cuidados", planoCuidadosId);

            List<String> validacoes = new ArrayList<>();
            List<String> alertas = new ArrayList<>();
            boolean validado = true;

            // Validate goals
            List<Map<String, Object>> metas = (List<Map<String, Object>>) planoCuidados.get("metas");
            if (metas == null || metas.isEmpty()) {
                validacoes.add("FALHA: Nenhuma meta definida");
                validado = false;
            } else {
                validacoes.add("OK: " + metas.size() + " metas definidas");
            }

            // Validate interventions
            List<Map<String, Object>> intervencoes =
                (List<Map<String, Object>>) planoCuidados.get("intervencoes");
            if (intervencoes == null || intervencoes.isEmpty()) {
                validacoes.add("FALHA: Nenhuma intervenção definida");
                validado = false;
            } else {
                validacoes.add("OK: " + intervencoes.size() + " intervenções definidas");
            }

            // Validate care team assignment
            Map<String, String> equipe = (Map<String, String>) planoCuidados.get("equipeCuidados");
            if (equipe == null || !equipe.containsKey("navegador")) {
                validacoes.add("FALHA: Navegador de cuidados não atribuído");
                validado = false;
            } else {
                validacoes.add("OK: Equipe de cuidados completa");
            }

            // Validate monitoring schedule
            Map<String, Object> cronograma =
                (Map<String, Object>) planoCuidados.get("cronogramaMonitoramento");
            if (cronograma == null || cronograma.isEmpty()) {
                alertas.add("ALERTA: Cronograma de monitoramento não definido");
            } else {
                validacoes.add("OK: Cronograma de monitoramento configurado");
            }

            // Clinical guideline compliance check
            List<String> condicoes = (List<String>) planoCuidados.get("condicoes");
            boolean aderenteGuias = validarAderenciaGuias(condicoes, metas, intervencoes);
            if (!aderenteGuias) {
                alertas.add("ALERTA: Plano pode não estar aderente a todas as diretrizes clínicas");
            } else {
                validacoes.add("OK: Aderente às diretrizes clínicas");
            }

            // Update plan status
            String novoStatus = validado ? "VALIDADO" : "REVISAO_NECESSARIA";
            planoCuidados.put("status", novoStatus);
            planoCuidados.put("dataValidacao", LocalDateTime.now().toString());
            planoCuidados.put("resultadoValidacao", validacoes);
            planoCuidados.put("alertasValidacao", alertas);
            planoCuidados.put("validadoPor", execution.getVariable("validadorId"));

            // Save validated plan
            dataLakeService.atualizar(execution, "planos_cuidados", planoCuidadosId, planoCuidados);

            // Publish validation event
            kafkaPublisher.publicar(execution, "plano-cuidados-validado", Map.of(
                "planoCuidadosId", planoCuidadosId,
                "status", novoStatus,
                "validado", validado
            ));

            execution.setVariable("planoValidado", validado);
            execution.setVariable("statusPlano", novoStatus);
            execution.setVariable("validacoes", validacoes);
            execution.setVariable("alertas", alertas);

            logger.info("Care plan validated: id={}, status={}, checks={}, alerts={}",
                planoCuidadosId, novoStatus, validacoes.size(), alertas.size());

        } catch (Exception e) {
            logger.error("Error validating care plan for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("planoValidacaoErro", e.getMessage());
            throw e;
        }
    }

    private List<Map<String, Object>> definirMetas(List<String> condicoes, Map<String, Object> dados) {
        List<Map<String, Object>> metas = new ArrayList<>();

        for (String condicao : condicoes) {
            if ("DIABETES".equals(condicao)) {
                metas.add(Map.of(
                    "condicao", "DIABETES",
                    "metrica", "HbA1c",
                    "alvo", "< 7.0%",
                    "prazo", "3 meses"
                ));
            } else if ("HIPERTENSAO".equals(condicao)) {
                metas.add(Map.of(
                    "condicao", "HIPERTENSAO",
                    "metrica", "Pressão Arterial",
                    "alvo", "< 140/90 mmHg",
                    "prazo", "2 meses"
                ));
            } else if ("DPOC".equals(condicao)) {
                metas.add(Map.of(
                    "condicao", "DPOC",
                    "metrica", "Espirometria",
                    "alvo", "VEF1 > 60%",
                    "prazo", "6 meses"
                ));
            }
        }

        return metas;
    }

    private List<Map<String, Object>> definirIntervencoes(List<String> condicoes) {
        List<Map<String, Object>> intervencoes = new ArrayList<>();

        for (String condicao : condicoes) {
            intervencoes.add(Map.of(
                "condicao", condicao,
                "tipo", "EDUCACAO",
                "descricao", "Educação sobre autogerenciamento",
                "frequencia", "Mensal"
            ));

            intervencoes.add(Map.of(
                "condicao", condicao,
                "tipo", "MONITORAMENTO",
                "descricao", "Monitoramento de sinais vitais",
                "frequencia", "Semanal"
            ));
        }

        return intervencoes;
    }

    private Map<String, Object> definirCronogramaMonitoramento(List<String> condicoes) {
        return Map.of(
            "contatoInicial", "7 dias",
            "followUpRegular", "30 dias",
            "avaliacaoTrimestral", "90 dias",
            "tipoContato", "TELEFONE_VIDEOCHAMADA"
        );
    }

    private Map<String, String> atribuirEquipe(List<String> condicoes, Map<String, Object> dados) {
        Map<String, String> equipe = new HashMap<>();
        equipe.put("navegador", "NAV-" + UUID.randomUUID().toString().substring(0, 8));

        if (condicoes.contains("DIABETES")) {
            equipe.put("endocrinologista", "ENDO-001");
        }
        if (condicoes.contains("HIPERTENSAO")) {
            equipe.put("cardiologista", "CARDIO-001");
        }

        equipe.put("farmaceutico", "FARM-001");
        equipe.put("nutricionista", "NUTRI-001");

        return equipe;
    }

    private int calcularComplexidadePlano(List<String> condicoes, Map<String, Object> dados) {
        int score = condicoes.size() * 20;

        Integer medicamentos = (Integer) dados.get("numeroMedicamentos");
        if (medicamentos != null) {
            score += medicamentos * 5;
        }

        return Math.min(score, 100);
    }

    private boolean validarAderenciaGuias(List<String> condicoes,
                                         List<Map<String, Object>> metas,
                                         List<Map<String, Object>> intervencoes) {
        // Simplified guideline compliance check
        return metas.size() >= condicoes.size() && intervencoes.size() >= (condicoes.size() * 2);
    }
}
