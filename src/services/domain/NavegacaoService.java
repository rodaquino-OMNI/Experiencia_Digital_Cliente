package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Navegacao Service - Care Navigator Complexity Assessment Delegate
 *
 * Evaluates case complexity for care navigator assignment
 * and workload balancing across navigation teams.
 *
 * BPMN Coverage:
 * - navegacaoService.avaliarComplexidade (Assess navigation complexity)
 */
@Component("navegacaoService")
public class NavegacaoService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(NavegacaoService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("navegacaoMethod");

        if ("avaliarComplexidade".equals(method)) {
            avaliarComplexidade(execution);
        } else {
            logger.warn("Unknown navegacao method: {}", method);
            throw new IllegalArgumentException("Invalid navegacao method: " + method);
        }
    }

    /**
     * Assess navigation complexity
     *
     * Evaluates case complexity based on medical, social, and logistical
     * factors to assign appropriate care navigator and estimate resources.
     *
     * Complexity factors:
     * - Number and severity of comorbidities
     * - Social determinants of health (housing, support)
     * - Care coordination requirements (specialists, facilities)
     * - Adherence history
     * - Geographic accessibility
     *
     * @param execution Process execution context
     */
    public void avaliarComplexidade(DelegateExecution execution) throws Exception {
        logger.info("Executing navegacaoService.avaliarComplexidade for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");

            // Fetch beneficiary data for complexity assessment
            Map<String, Object> beneficiarioData = dataLakeService.consultar(
                execution, "beneficiarios_analytics", beneficiarioId);

            int scoreComplexidade = 0;
            List<String> fatoresComplexidade = new ArrayList<>();

            // 1. Medical complexity (40 points max)
            scoreComplexidade += avaliarComplexidadeMedica(beneficiarioData, fatoresComplexidade);

            // 2. Social determinants (30 points max)
            scoreComplexidade += avaliarDeterminantesSociais(beneficiarioData, fatoresComplexidade);

            // 3. Care coordination needs (20 points max)
            scoreComplexidade += avaliarNecessidadeCoordenacao(beneficiarioData, fatoresComplexidade);

            // 4. Adherence and engagement (10 points max)
            scoreComplexidade += avaliarAdesao(beneficiarioData, fatoresComplexidade);

            // Determine complexity level and navigator tier
            String nivelComplexidade;
            String tierNavigator;
            int tempoEstimado; // minutes per week

            if (scoreComplexidade >= 80) {
                nivelComplexidade = "ALTA";
                tierNavigator = "SENIOR";
                tempoEstimado = 120; // 2 hours/week
            } else if (scoreComplexidade >= 60) {
                nivelComplexidade = "MEDIA_ALTA";
                tierNavigator = "PLENO";
                tempoEstimado = 60; // 1 hour/week
            } else if (scoreComplexidade >= 40) {
                nivelComplexidade = "MEDIA";
                tierNavigator = "JUNIOR";
                tempoEstimado = 30; // 30 min/week
            } else {
                nivelComplexidade = "BAIXA";
                tierNavigator = "JUNIOR";
                tempoEstimado = 15; // 15 min/week
            }

            // Store complexity assessment
            execution.setVariable("scoreComplexidade", scoreComplexidade);
            execution.setVariable("nivelComplexidade", nivelComplexidade);
            execution.setVariable("fatoresComplexidade", fatoresComplexidade);
            execution.setVariable("tierNavigator", tierNavigator);
            execution.setVariable("tempoEstimadoSemanal", tempoEstimado);

            // Recommend navigator assignment
            String navigatorRecomendado = selecionarNavigator(tierNavigator, execution);
            execution.setVariable("navigatorRecomendado", navigatorRecomendado);

            execution.setVariable("complexidadeAvaliada", true);

            logger.info("Complexity assessed: score={}, level={}, tier={}, navigator={}",
                scoreComplexidade, nivelComplexidade, tierNavigator, navigatorRecomendado);

        } catch (Exception e) {
            logger.error("Error assessing complexity for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("complexidadeErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Assess medical complexity (max 40 points)
     */
    private int avaliarComplexidadeMedica(Map<String, Object> data, List<String> fatores) {
        int score = 0;

        // Chronic conditions (20 points)
        List<String> condicoes = (List<String>) data.getOrDefault("condicoesCronicas", new ArrayList<>());
        if (condicoes.size() >= 3) {
            score += 20;
            fatores.add("3+ condições crônicas");
        } else if (condicoes.size() == 2) {
            score += 15;
            fatores.add("2 condições crônicas");
        } else if (condicoes.size() == 1) {
            score += 10;
            fatores.add("1 condição crônica");
        }

        // Hospitalization history (10 points)
        Integer internacoes = (Integer) data.getOrDefault("internacoesUltimoAno", 0);
        if (internacoes >= 2) {
            score += 10;
            fatores.add("Múltiplas internações recentes");
        } else if (internacoes == 1) {
            score += 5;
        }

        // ER visits (5 points)
        Integer urgencias = (Integer) data.getOrDefault("urgenciasUltimoAno", 0);
        if (urgencias >= 3) {
            score += 5;
            fatores.add("Uso frequente de emergência");
        }

        // Polypharmacy (5 points)
        Integer medicamentos = (Integer) data.getOrDefault("numeroMedicamentos", 0);
        if (medicamentos >= 5) {
            score += 5;
            fatores.add("Polifarmácia (5+ medicamentos)");
        }

        return score;
    }

    /**
     * Assess social determinants (max 30 points)
     */
    private int avaliarDeterminantesSociais(Map<String, Object> data, List<String> fatores) {
        int score = 0;

        // Living situation (10 points)
        String moradia = (String) data.get("situacaoMoradia");
        if ("INSTAVEL".equals(moradia)) {
            score += 10;
            fatores.add("Situação de moradia instável");
        } else if ("RISCO".equals(moradia)) {
            score += 5;
        }

        // Social support (10 points)
        String suporteSocial = (String) data.get("suporteSocial");
        if ("BAIXO".equals(suporteSocial)) {
            score += 10;
            fatores.add("Suporte social baixo");
        } else if ("MEDIO".equals(suporteSocial)) {
            score += 5;
        }

        // Transportation barriers (5 points)
        Boolean barreirasTransporte = (Boolean) data.getOrDefault("barreirasTransporte", false);
        if (Boolean.TRUE.equals(barreirasTransporte)) {
            score += 5;
            fatores.add("Barreiras de transporte");
        }

        // Financial constraints (5 points)
        String situacaoFinanceira = (String) data.get("situacaoFinanceira");
        if ("VULNERAVEL".equals(situacaoFinanceira)) {
            score += 5;
            fatores.add("Vulnerabilidade financeira");
        }

        return score;
    }

    /**
     * Assess care coordination needs (max 20 points)
     */
    private int avaliarNecessidadeCoordenacao(Map<String, Object> data, List<String> fatores) {
        int score = 0;

        // Multiple specialists (10 points)
        Integer especialistas = (Integer) data.getOrDefault("numeroEspecialistas", 0);
        if (especialistas >= 3) {
            score += 10;
            fatores.add("3+ especialistas envolvidos");
        } else if (especialistas >= 2) {
            score += 5;
        }

        // Complex treatment plans (5 points)
        Boolean tratamentoComplexo = (Boolean) data.getOrDefault("tratamentoComplexo", false);
        if (Boolean.TRUE.equals(tratamentoComplexo)) {
            score += 5;
            fatores.add("Plano terapêutico complexo");
        }

        // Multiple facilities (5 points)
        Integer estabelecimentos = (Integer) data.getOrDefault("numeroEstabelecimentos", 0);
        if (estabelecimentos >= 3) {
            score += 5;
            fatores.add("Múltiplos estabelecimentos");
        }

        return score;
    }

    /**
     * Assess adherence and engagement (max 10 points)
     */
    private int avaliarAdesao(Map<String, Object> data, List<String> fatores) {
        int score = 0;

        // Medication adherence (5 points)
        Integer adesaoMedicacao = (Integer) data.getOrDefault("scoreAdesaoMedicacao", 100);
        if (adesaoMedicacao < 70) {
            score += 5;
            fatores.add("Baixa adesão medicamentosa");
        } else if (adesaoMedicacao < 85) {
            score += 3;
        }

        // Appointment no-shows (5 points)
        Integer faltas = (Integer) data.getOrDefault("faltasUltimoAno", 0);
        if (faltas >= 3) {
            score += 5;
            fatores.add("Múltiplas faltas a consultas");
        } else if (faltas >= 2) {
            score += 3;
        }

        return score;
    }

    /**
     * Select navigator based on tier and availability
     */
    private String selecionarNavigator(String tier, DelegateExecution execution) {
        // Simplified navigator selection - in production, query workload data
        // and assign to least loaded navigator of appropriate tier

        Map<String, List<String>> navigatorsByTier = Map.of(
            "SENIOR", Arrays.asList("NAV-001-SENIOR", "NAV-002-SENIOR"),
            "PLENO", Arrays.asList("NAV-003-PLENO", "NAV-004-PLENO", "NAV-005-PLENO"),
            "JUNIOR", Arrays.asList("NAV-006-JUNIOR", "NAV-007-JUNIOR", "NAV-008-JUNIOR")
        );

        List<String> navigators = navigatorsByTier.getOrDefault(tier, new ArrayList<>());

        if (!navigators.isEmpty()) {
            // Round-robin assignment (simplified)
            return navigators.get(0);
        }

        return "NAV-POOL-" + tier;
    }
}
