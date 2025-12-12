package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Java Delegate para preparação de templates de comunicação
 *
 * Responsabilidades:
 * - Preparar templates personalizados de WhatsApp
 * - Preparar templates de email
 * - Preparar templates de SMS
 * - Substituir variáveis nos templates
 *
 * Uso no BPMN:
 * <serviceTask id="ServiceTask_PrepararTemplate"
 *              name="Preparar Template de Boas-vindas"
 *              camunda:delegateExpression="${templateService}">
 * </serviceTask>
 */
@Component("templateService")
public class TemplateService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateService.class);

    // Templates HSM aprovados pelo WhatsApp
    private static final Map<String, String> TEMPLATES = new HashMap<String, String>() {{
        put("boas_vindas_v2",
            "Olá {{1}}! 👋\n\n" +
            "Bem-vindo(a) à AUSTA Saúde! 🎉\n\n" +
            "Seu plano {{2}} está ativo e você já pode começar a usar todos os benefícios.\n\n" +
            "Para conhecer melhor seu perfil de saúde, vamos fazer um breve screening. " +
            "Leva apenas 5 minutos e você ganha pontos! 🎁\n\n" +
            "Responda SIM para começar agora.");

        put("lembrete_onboarding_v1",
            "Oi {{1}},\n\n" +
            "Notamos que você ainda não completou seu screening de saúde.\n\n" +
            "É rápido e importante para personalizarmos seu atendimento! " +
            "Você já completou {{2}} de 5 módulos.\n\n" +
            "Responda CONTINUAR para retomar de onde parou.");

        put("resumo_onboarding_v2",
            "Parabéns {{1}}! 🎉\n\n" +
            "Seu perfil de saúde está completo.\n\n" +
            "📊 Classificação: {{2}}\n" +
            "🎯 Próximos passos:\n{{3}}\n\n" +
            "Conte conosco para cuidar da sua saúde! 💙");

        put("checkup_pendente_v1",
            "Oi {{1}},\n\n" +
            "Está na hora do seu check-up anual! 🏥\n\n" +
            "Identificamos que faz mais de {{2}} meses desde sua última consulta de rotina.\n\n" +
            "Posso agendar para você na {{3}}? Responda SIM ou escolha outra clínica.");

        put("medicamento_acabando_v1",
            "Atenção {{1}}! ⚠️\n\n" +
            "Seu medicamento {{2}} está acabando (restam {{3}} dias).\n\n" +
            "Precisa renovar sua receita? Responda SIM e vou te ajudar a facilitar o processo.");

        put("exame_alterado_v1",
            "{{1}}, detectamos que seu exame {{2}} apresentou alteração.\n\n" +
            "É importante fazer uma consulta de retorno.\n\n" +
            "Posso agendar com {{3}} para esta semana? Responda SIM.");
    }};

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String templateId = (String) execution.getVariable("templateId");

        if (templateId == null) {
            templateId = determinarTemplate(execution);
        }

        LOGGER.info("Preparando template {} para processo: {}",
                   templateId, execution.getProcessInstanceId());

        try {
            String mensagemPersonalizada = prepararTemplate(execution, templateId);

            execution.setVariable("mensagemPersonalizada", mensagemPersonalizada);
            execution.setVariable("templateUtilizado", templateId);
            execution.setVariable("templatePreparado", true);

            LOGGER.info("Template preparado com sucesso");

        } catch (Exception e) {
            LOGGER.error("Erro ao preparar template: {}", e.getMessage(), e);
            execution.setVariable("templatePreparado", false);
            throw e;
        }
    }

    /**
     * Determina qual template usar baseado no contexto do processo
     */
    private String determinarTemplate(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();

        if (activityId.contains("BoasVindas")) {
            return "boas_vindas_v2";
        } else if (activityId.contains("Lembrete")) {
            return "lembrete_onboarding_v1";
        } else if (activityId.contains("Resumo")) {
            return "resumo_onboarding_v2";
        } else if (activityId.contains("Checkup")) {
            return "checkup_pendente_v1";
        } else if (activityId.contains("Medicamento")) {
            return "medicamento_acabando_v1";
        } else if (activityId.contains("Exame")) {
            return "exame_alterado_v1";
        }

        return "boas_vindas_v2"; // Template padrão
    }

    /**
     * Prepara template substituindo variáveis pelo contexto real
     */
    private String prepararTemplate(DelegateExecution execution, String templateId) {
        String template = TEMPLATES.get(templateId);

        if (template == null) {
            throw new IllegalArgumentException("Template não encontrado: " + templateId);
        }

        Map<String, Object> variaveis = extrairVariaveis(execution, templateId);
        String mensagem = template;

        // Substituir variáveis {{1}}, {{2}}, etc.
        for (int i = 1; i <= variaveis.size(); i++) {
            String placeholder = "{{" + i + "}}";
            Object valor = variaveis.get("var" + i);
            if (valor != null) {
                mensagem = mensagem.replace(placeholder, valor.toString());
            }
        }

        return mensagem;
    }

    /**
     * Extrai variáveis do contexto do processo para substituir no template
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extrairVariaveis(DelegateExecution execution, String templateId) {
        Map<String, Object> variaveis = new HashMap<>();

        // Nome do beneficiário (sempre {{1}})
        String nome = obterNomeBeneficiario(execution);
        variaveis.put("var1", nome);

        switch (templateId) {
            case "boas_vindas_v2":
                // {{2}} = nome do plano
                Map<String, Object> dadosCadastrais =
                    (Map<String, Object>) execution.getVariable("dadosCadastrais");
                String plano = dadosCadastrais != null ?
                    (String) dadosCadastrais.get("plano") : "Seu plano";
                variaveis.put("var2", plano);
                break;

            case "lembrete_onboarding_v1":
                // {{2}} = módulos completados
                Integer modulosCompletos = (Integer) execution.getVariable("modulosCompletos");
                variaveis.put("var2", modulosCompletos != null ? modulosCompletos : 0);
                break;

            case "resumo_onboarding_v2":
                // {{2}} = classificação de risco
                String classificacao = (String) execution.getVariable("classificacaoRisco");
                variaveis.put("var2", traduzirClassificacao(classificacao));

                // {{3}} = próximos passos
                String proximosPassos = gerarProximosPassos(execution);
                variaveis.put("var3", proximosPassos);
                break;

            case "checkup_pendente_v1":
                // {{2}} = meses desde último checkup
                Integer diasCheckup = (Integer) execution.getVariable("diasUltimoCheckup");
                Integer meses = diasCheckup != null ? diasCheckup / 30 : 12;
                variaveis.put("var2", meses);

                // {{3}} = clínica preferencial
                String clinica = (String) execution.getVariable("clinicaPreferencial");
                variaveis.put("var3", clinica != null ? clinica : "rede credenciada");
                break;

            case "medicamento_acabando_v1":
                // {{2}} = nome do medicamento
                String medicamento = (String) execution.getVariable("nomeMedicamento");
                variaveis.put("var2", medicamento);

                // {{3}} = dias restantes
                Integer diasRestantes = (Integer) execution.getVariable("diasRestantesMedicamento");
                variaveis.put("var3", diasRestantes);
                break;

            case "exame_alterado_v1":
                // {{2}} = tipo de exame
                String tipoExame = (String) execution.getVariable("tipoExame");
                variaveis.put("var2", tipoExame);

                // {{3}} = especialidade médica
                String especialidade = (String) execution.getVariable("especialidadeSugerida");
                variaveis.put("var3", especialidade);
                break;
        }

        return variaveis;
    }

    /**
     * Obtém o primeiro nome do beneficiário
     */
    @SuppressWarnings("unchecked")
    private String obterNomeBeneficiario(DelegateExecution execution) {
        String nomeCompleto = null;

        Map<String, Object> dadosCadastrais =
            (Map<String, Object>) execution.getVariable("dadosCadastrais");
        if (dadosCadastrais != null) {
            nomeCompleto = (String) dadosCadastrais.get("nome");
        }

        if (nomeCompleto == null) {
            Map<String, Object> dadosBeneficiario =
                (Map<String, Object>) execution.getVariable("dadosBeneficiario");
            if (dadosBeneficiario != null) {
                nomeCompleto = (String) dadosBeneficiario.get("nome");
            }
        }

        if (nomeCompleto != null) {
            return nomeCompleto.split(" ")[0]; // Retorna primeiro nome
        }

        return "Cliente"; // Fallback
    }

    /**
     * Traduz classificação de risco para linguagem amigável
     */
    private String traduzirClassificacao(String classificacao) {
        if (classificacao == null) return "Risco Normal";

        switch (classificacao.toUpperCase()) {
            case "BAIXO":
                return "Baixo Risco - Você está bem! 💚";
            case "MODERADO":
                return "Risco Moderado - Vamos cuidar juntos 💛";
            case "ALTO":
                return "Alto Risco - Acompanhamento especial 🧡";
            case "COMPLEXO":
                return "Risco Complexo - Cuidado intensivo ❤️";
            default:
                return "Risco " + classificacao;
        }
    }

    /**
     * Gera lista de próximos passos baseado no perfil de risco
     */
    private String gerarProximosPassos(DelegateExecution execution) {
        String classificacao = (String) execution.getVariable("classificacaoRisco");

        if (classificacao == null || "BAIXO".equals(classificacao)) {
            return "• Check-up anual\n" +
                   "• App para acompanhar sua saúde\n" +
                   "• Dicas de prevenção";
        } else if ("MODERADO".equals(classificacao)) {
            return "• Acompanhamento semestral\n" +
                   "• Programa de hábitos saudáveis\n" +
                   "• Exames periódicos";
        } else {
            return "• Acompanhamento trimestral\n" +
                   "• Navegador de saúde dedicado\n" +
                   "• Programa de gestão de crônicos";
        }
    }
}
