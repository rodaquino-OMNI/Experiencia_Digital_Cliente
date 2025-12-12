package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java Delegate para cálculo de scores de risco
 *
 * Responsabilidades:
 * - Calcular score de risco de internação
 * - Calcular score comportamental
 * - Identificar fatores de risco
 * - Calcular índices como BMI
 *
 * Uso no BPMN:
 * <serviceTask id="ServiceTask_CalcularRisco"
 *              name="Calcular Score de Risco"
 *              camunda:delegateExpression="${riscoCalculatorService}">
 * </serviceTask>
 */
@Component("riscoCalculatorService")
public class RiscoCalculatorService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiscoCalculatorService.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String tipoCalculo = (String) execution.getVariable("tipoCalculo");

        if (tipoCalculo == null) {
            tipoCalculo = "completo";
        }

        LOGGER.info("Calculando risco (tipo: {}) para processo: {}",
                   tipoCalculo, execution.getProcessInstanceId());

        try {
            switch (tipoCalculo.toLowerCase()) {
                case "completo":
                    calcularRiscoCompleto(execution);
                    break;
                case "internacao":
                    calcularRiscoInternacao(execution);
                    break;
                case "comportamental":
                    calcularScoreComportamental(execution);
                    break;
                case "bmi":
                    calcularBMI(execution);
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de cálculo inválido: " + tipoCalculo);
            }

            execution.setVariable("calculoRiscoSucesso", true);

        } catch (Exception e) {
            LOGGER.error("Erro ao calcular risco: {}", e.getMessage(), e);
            execution.setVariable("calculoRiscoSucesso", false);
            throw e;
        }
    }

    /**
     * Cálculo completo de risco incluindo todos os scores
     */
    private void calcularRiscoCompleto(DelegateExecution execution) {
        LOGGER.info("Executando cálculo completo de risco");

        // Calcular BMI
        calcularBMI(execution);

        // Calcular score comportamental
        calcularScoreComportamental(execution);

        // Calcular risco de internação
        calcularRiscoInternacao(execution);

        // Identificar fatores de risco
        identificarFatoresRisco(execution);
    }

    /**
     * Calcula score de predição de internação
     *
     * Input:
     * - idade, comorbidades, utilizacoesRecentes, scoreComportamental
     *
     * Output:
     * - scorePredicaoInternacao (0-100)
     * - riscoInternacaoCategoria (BAIXO/MEDIO/ALTO/CRITICO)
     */
    private void calcularRiscoInternacao(DelegateExecution execution) {
        Integer idade = (Integer) execution.getVariable("idade");
        Integer qtdComorbidades = (Integer) execution.getVariable("qtdComorbidades");
        Integer scoreComportamental = (Integer) execution.getVariable("scoreComportamental");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> utilizacoesRecentes =
            (List<Map<String, Object>>) execution.getVariable("historicoUtilizacoes");

        // Algoritmo simplificado de predição
        int score = 0;

        // Idade (0-25 pontos)
        if (idade != null) {
            if (idade >= 75) score += 25;
            else if (idade >= 65) score += 20;
            else if (idade >= 50) score += 10;
            else score += 5;
        }

        // Comorbidades (0-30 pontos)
        if (qtdComorbidades != null) {
            score += Math.min(qtdComorbidades * 10, 30);
        }

        // Histórico de utilizações (0-25 pontos)
        if (utilizacoesRecentes != null && !utilizacoesRecentes.isEmpty()) {
            long internacoes = utilizacoesRecentes.stream()
                .filter(u -> "INTERNACAO".equals(u.get("tipo")))
                .count();

            long emergencias = utilizacoesRecentes.stream()
                .filter(u -> "PRONTO_SOCORRO".equals(u.get("tipo")))
                .count();

            score += Math.min((int)(internacoes * 10 + emergencias * 5), 25);
        }

        // Score comportamental (0-20 pontos)
        if (scoreComportamental != null) {
            score += Math.min(scoreComportamental / 5, 20);
        }

        // Normalizar para 0-100
        score = Math.min(score, 100);

        String categoria;
        if (score >= 70) categoria = "CRITICO";
        else if (score >= 50) categoria = "ALTO";
        else if (score >= 30) categoria = "MEDIO";
        else categoria = "BAIXO";

        execution.setVariable("scorePredicaoInternacao", score);
        execution.setVariable("riscoInternacaoCategoria", categoria);

        LOGGER.info("Score de predição de internação calculado: {} ({})", score, categoria);
    }

    /**
     * Calcula score comportamental baseado em padrões de resposta
     *
     * Input:
     * - respostasScreening (Map)
     *
     * Output:
     * - scoreComportamental (0-100)
     */
    @SuppressWarnings("unchecked")
    private void calcularScoreComportamental(DelegateExecution execution) {
        Map<String, Object> respostasScreening =
            (Map<String, Object>) execution.getVariable("respostasScreening");

        if (respostasScreening == null) {
            execution.setVariable("scoreComportamental", 0);
            return;
        }

        int score = 50; // Base neutra

        // Tabagismo (-20 pontos)
        Boolean tabagista = (Boolean) respostasScreening.get("tabagista");
        if (Boolean.TRUE.equals(tabagista)) {
            score -= 20;
        }

        // Sedentarismo (-15 pontos)
        Boolean sedentario = (Boolean) respostasScreening.get("sedentario");
        if (Boolean.TRUE.equals(sedentario)) {
            score -= 15;
        }

        // Etilismo (-10 pontos)
        String consumoAlcool = (String) respostasScreening.get("consumoAlcool");
        if ("ALTO".equals(consumoAlcool)) {
            score -= 10;
        }

        // Alimentação saudável (+10 pontos)
        Boolean alimentacaoSaudavel = (Boolean) respostasScreening.get("alimentacaoSaudavel");
        if (Boolean.TRUE.equals(alimentacaoSaudavel)) {
            score += 10;
        }

        // Atividade física regular (+15 pontos)
        Boolean atividadeFisica = (Boolean) respostasScreening.get("atividadeFisicaRegular");
        if (Boolean.TRUE.equals(atividadeFisica)) {
            score += 15;
        }

        // Acompanhamento médico regular (+10 pontos)
        Boolean acompanhamentoMedico = (Boolean) respostasScreening.get("acompanhamentoMedico");
        if (Boolean.TRUE.equals(acompanhamentoMedico)) {
            score += 10;
        }

        // Normalizar para 0-100
        score = Math.max(0, Math.min(100, score));

        execution.setVariable("scoreComportamental", score);

        LOGGER.info("Score comportamental calculado: {}", score);
    }

    /**
     * Calcula o Índice de Massa Corporal (BMI)
     *
     * Input:
     * - peso (Double, em kg)
     * - altura (Double, em metros)
     *
     * Output:
     * - bmi (Double)
     * - categoriaBMI (String)
     */
    private void calcularBMI(DelegateExecution execution) {
        Double peso = (Double) execution.getVariable("peso");
        Double altura = (Double) execution.getVariable("altura");

        if (peso == null || altura == null || altura == 0) {
            LOGGER.warn("Dados insuficientes para calcular BMI");
            return;
        }

        double bmi = peso / (altura * altura);
        bmi = Math.round(bmi * 10.0) / 10.0; // Arredondar para 1 casa decimal

        String categoria;
        if (bmi < 18.5) categoria = "ABAIXO_DO_PESO";
        else if (bmi < 25) categoria = "PESO_NORMAL";
        else if (bmi < 30) categoria = "SOBREPESO";
        else if (bmi < 35) categoria = "OBESIDADE_GRAU_I";
        else if (bmi < 40) categoria = "OBESIDADE_GRAU_II";
        else categoria = "OBESIDADE_GRAU_III";

        execution.setVariable("bmi", bmi);
        execution.setVariable("categoriaBMI", categoria);

        LOGGER.info("BMI calculado: {} ({})", bmi, categoria);
    }

    /**
     * Identifica e lista fatores de risco do beneficiário
     *
     * Output:
     * - fatoresRisco (List<String>)
     */
    @SuppressWarnings("unchecked")
    private void identificarFatoresRisco(DelegateExecution execution) {
        List<String> fatoresRisco = new java.util.ArrayList<>();

        // Idade avançada
        Integer idade = (Integer) execution.getVariable("idade");
        if (idade != null && idade >= 65) {
            fatoresRisco.add("IDADE_AVANCADA");
        }

        // BMI elevado
        String categoriaBMI = (String) execution.getVariable("categoriaBMI");
        if ("OBESIDADE_GRAU_I".equals(categoriaBMI) ||
            "OBESIDADE_GRAU_II".equals(categoriaBMI) ||
            "OBESIDADE_GRAU_III".equals(categoriaBMI)) {
            fatoresRisco.add("OBESIDADE");
        }

        // Comorbidades múltiplas
        Integer qtdComorbidades = (Integer) execution.getVariable("qtdComorbidades");
        if (qtdComorbidades != null && qtdComorbidades >= 3) {
            fatoresRisco.add("COMORBIDADES_MULTIPLAS");
        }

        // Fatores comportamentais
        Boolean tabagista = (Boolean) execution.getVariable("tabagista");
        if (Boolean.TRUE.equals(tabagista)) {
            fatoresRisco.add("TABAGISMO");
        }

        Boolean sedentario = (Boolean) execution.getVariable("sedentario");
        if (Boolean.TRUE.equals(sedentario)) {
            fatoresRisco.add("SEDENTARISMO");
        }

        // Histórico familiar
        Boolean historicoFamiliarPositivo =
            (Boolean) execution.getVariable("historicoFamiliarPositivo");
        if (Boolean.TRUE.equals(historicoFamiliarPositivo)) {
            fatoresRisco.add("HISTORICO_FAMILIAR_POSITIVO");
        }

        // Utilizações recentes de alto custo
        List<Map<String, Object>> utilizacoesRecentes =
            (List<Map<String, Object>>) execution.getVariable("historicoUtilizacoes");

        if (utilizacoesRecentes != null) {
            long internacoes = utilizacoesRecentes.stream()
                .filter(u -> "INTERNACAO".equals(u.get("tipo")))
                .count();

            if (internacoes > 0) {
                fatoresRisco.add("INTERNACAO_RECENTE");
            }
        }

        execution.setVariable("fatoresRisco", fatoresRisco);
        execution.setVariable("qtdFatoresRisco", fatoresRisco.size());

        LOGGER.info("Identificados {} fatores de risco: {}", fatoresRisco.size(), fatoresRisco);
    }
}
