package br.com.austa.experiencia.integration.support;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class for creating test data used across integration tests
 *
 * Provides standardized test data builders for:
 * - Workflow variables (all 10 sub-processes)
 * - DMN decision inputs
 * - Event payloads
 * - Entity builders
 *
 * Ensures consistency across test suites and reduces boilerplate code.
 *
 * @see PROMPT_TECNICO_3.MD Lines 1391-1419
 */
public class TestDataFactory {

    // ========== Workflow Variables Factories ==========

    /**
     * Creates variables for Motor Proativo Workflow (WF_02)
     */
    public static Map<String, Object> createMotorProativoVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("tipoEvento", "ALTERACAO_EXAMES");
        variables.put("nivelRisco", "MEDIO");
        variables.put("condicaoCronica", "DIABETES");
        variables.put("dadosExame", Map.of(
            "glicemia", 180,
            "pressaoArterial", "140/90",
            "dataColeta", LocalDateTime.now().minusDays(1)
        ));
        return variables;
    }

    /**
     * Creates variables for Recepção e Classificação Workflow (WF_03)
     */
    public static Map<String, Object> createRecepcaoClassificacaoVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("canal", "TELEFONE");
        variables.put("tipoDemanda", "ORIENTACAO_CLINICA");
        variables.put("sintomas", Map.of(
            "febre", true,
            "tosse", false,
            "intensidade", "LEVE"
        ));
        variables.put("tempoEspera", 0);
        return variables;
    }

    /**
     * Creates variables for Self-Service Workflow (WF_04)
     */
    public static Map<String, Object> createSelfServiceVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("tipoSolicitacao", "SEGUNDA_VIA_CARTEIRINHA");
        variables.put("canal", "APP");
        variables.put("dadosEntrega", Map.of(
            "endereco", "Rua Teste, 123",
            "cep", "01234-567",
            "complemento", "Apto 45"
        ));
        return variables;
    }

    /**
     * Creates variables for Agentes IA Workflow (WF_05)
     */
    public static Map<String, Object> createAgentesIaVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("canal", "CHAT");
        variables.put("mensagemBeneficiario", "Preciso agendar uma consulta");
        variables.put("idioma", "PT_BR");
        variables.put("contextoConversacional", Map.of(
            "interacoesAnteriores", 0,
            "topicoAtual", "AGENDAMENTO"
        ));
        return variables;
    }

    /**
     * Creates variables for Navegação no Cuidado Workflow (WF_06)
     */
    public static Map<String, Object> createNavegacaoCuidadoVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("condicoes", List.of("DIABETES", "HIPERTENSAO"));
        variables.put("numeroEspecialistas", 2);
        variables.put("complexidadeCaso", "MEDIA");
        variables.put("necessitaCoordenacao", true);
        return variables;
    }

    /**
     * Creates variables for Gestão de Crônicos Workflow (WF_07)
     */
    public static Map<String, Object> createGestaoCronicosVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("condicao", "DIABETES");
        variables.put("hba1c", 7.5);
        variables.put("comorbidades", 1);
        variables.put("internacoes12meses", 0);
        variables.put("elegivelPrograma", true);
        return variables;
    }

    /**
     * Creates variables for Gestão de Reclamações Workflow (WF_08)
     */
    public static Map<String, Object> createGestaoReclamacoesVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("canal", "TELEFONE");
        variables.put("descricaoReclamacao", "Atendimento inadequado");
        variables.put("categoria", "ATENDIMENTO");
        variables.put("gravidade", "MEDIA");
        variables.put("dataOcorrencia", LocalDateTime.now().minusDays(1));
        return variables;
    }

    /**
     * Creates variables for Follow-up e Feedback Workflow (WF_09)
     */
    public static Map<String, Object> createFollowUpFeedbackVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("interacaoId", "INT123");
        variables.put("tipoInteracao", "CONSULTA_MEDICA");
        variables.put("canal", "WHATSAPP");
        variables.put("diasAposAtendimento", 2);
        return variables;
    }

    /**
     * Creates variables for Autorização Workflow (WF_10)
     */
    public static Map<String, Object> createAutorizacaoVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN001");
        variables.put("procedimento", "RESSONANCIA_MAGNETICA");
        variables.put("prestadorId", "PREST001");
        variables.put("urgencia", "ELETIVA");
        variables.put("justificativa", "Investigação de dor crônica");
        variables.put("cid10", "M54.5");
        return variables;
    }

    // ========== DMN Input Factories ==========

    /**
     * Creates inputs for Classificação de Urgência DMN (DMN_01)
     */
    public static Map<String, Object> createClassificacaoUrgenciaInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("sintomas", List.of("FEBRE", "TOSSE"));
        inputs.put("intensidade", "MODERADA");
        inputs.put("duracao", 3);
        inputs.put("idade", 45);
        inputs.put("comorbidades", List.of("DIABETES"));
        return inputs;
    }

    /**
     * Creates inputs for Roteamento de Demanda DMN (DMN_02)
     */
    public static Map<String, Object> createRoteamentoDemandaInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("tipoDemanda", "AUTORIZACAO");
        inputs.put("complexidade", "BAIXA");
        inputs.put("urgencia", "MEDIA");
        inputs.put("horaAtendimento", 14);
        inputs.put("canalOrigem", "APP");
        return inputs;
    }

    /**
     * Creates inputs for Protocolo Clínico DMN (DMN_03)
     */
    public static Map<String, Object> createProtocoloClinicoInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("sintomas", List.of("DOR_PEITO", "FALTA_AR"));
        inputs.put("idade", 60);
        inputs.put("fatoresRisco", List.of("TABAGISMO", "HIPERTENSAO"));
        inputs.put("sinaisVitais", Map.of(
            "pressao", "160/100",
            "frequenciaCardiaca", 95
        ));
        return inputs;
    }

    /**
     * Creates inputs for Identificação de Gatilhos DMN (DMN_04)
     */
    public static Map<String, Object> createIdentificacaoGatilhosInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("condicao", "DIABETES");
        inputs.put("glicemia", 250);
        inputs.put("hba1c", 9.0);
        inputs.put("ultimaConsulta", 120); // days ago
        inputs.put("aderenciaMedicacao", 60.0); // percentage
        return inputs;
    }

    /**
     * Creates inputs for Elegibilidade para Programa DMN (DMN_05)
     */
    public static Map<String, Object> createElegibilidadeProgramaInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("condicaoCronica", "DIABETES");
        inputs.put("tempoCondicao", 24); // months
        inputs.put("internacoes12meses", 1);
        inputs.put("nivelControle", "INADEQUADO");
        inputs.put("consentimento", true);
        return inputs;
    }

    /**
     * Creates inputs for Prioridade de Atendimento DMN (DMN_06)
     */
    public static Map<String, Object> createPrioridadeAtendimentoInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("urgencia", "ALTA");
        inputs.put("tempoEspera", 30); // minutes
        inputs.put("tipoCliente", "PREMIUM");
        inputs.put("historicoReclamacoes", 2);
        inputs.put("idadeBeneficiario", 75);
        return inputs;
    }

    /**
     * Creates inputs for Classificação de Reclamação DMN (DMN_07)
     */
    public static Map<String, Object> createClassificacaoReclamacaoInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("descricao", "Negativa de autorização indevida");
        inputs.put("categoria", "AUTORIZACAO");
        inputs.put("impactoSaude", "ALTO");
        inputs.put("valorEnvolvido", 5000.00);
        inputs.put("recorrente", true);
        return inputs;
    }

    /**
     * Creates inputs for Cálculo de NPS DMN (DMN_08)
     */
    public static Map<String, Object> createCalculoNpsInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("nota", 9);
        inputs.put("tipoInteracao", "CONSULTA");
        inputs.put("canalAtendimento", "PRESENCIAL");
        inputs.put("tempoResolucao", 15); // minutes
        return inputs;
    }

    // ========== Event Payload Factories ==========

    /**
     * Creates a standard Kafka event payload
     */
    public static Map<String, Object> createEventPayload(String eventType, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("timestamp", LocalDateTime.now());
        payload.put("data", data);
        payload.put("correlationId", java.util.UUID.randomUUID().toString());
        return payload;
    }

    // ========== Utility Methods ==========

    /**
     * Creates a beneficiary ID for testing
     */
    public static String createBeneficiarioId(int index) {
        return String.format("BEN%06d", index);
    }

    /**
     * Creates a timestamp for testing (relative to now)
     */
    public static LocalDateTime createTimestamp(int daysOffset) {
        return LocalDateTime.now().plusDays(daysOffset);
    }

    /**
     * Creates a map with common audit fields
     */
    public static Map<String, Object> createAuditFields(String userId) {
        Map<String, Object> audit = new HashMap<>();
        audit.put("criadoPor", userId);
        audit.put("dataCriacao", LocalDateTime.now());
        audit.put("modificadoPor", userId);
        audit.put("dataModificacao", LocalDateTime.now());
        return audit;
    }
}
