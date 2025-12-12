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
 * Motor Proativo Service - Proactive Health Management Delegate
 *
 * Manages proactive health campaigns with batch beneficiary processing,
 * action tracking, and engagement monitoring.
 *
 * BPMN Coverage:
 * - motorProativoService.carregarBaseAtiva (Load active beneficiary base)
 * - motorProativoService.registrarAcoes (Register executed actions)
 * - motorProativoService.registrarSemAcao (Register no-action beneficiaries)
 */
@Component("motorProativoService")
public class MotorProativoService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(MotorProativoService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private RiscoCalculatorService riscoCalculator;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("motorProativoMethod");

        if ("carregarBaseAtiva".equals(method)) {
            carregarBaseAtiva(execution);
        } else if ("registrarAcoes".equals(method)) {
            registrarAcoes(execution);
        } else if ("registrarSemAcao".equals(method)) {
            registrarSemAcao(execution);
        } else {
            logger.warn("Unknown motor proativo method: {}", method);
            throw new IllegalArgumentException("Invalid motor proativo method: " + method);
        }
    }

    /**
     * Load active beneficiary base
     *
     * Loads and segments beneficiary base for proactive campaigns
     * based on risk stratification and eligibility criteria.
     *
     * @param execution Process execution context
     */
    public void carregarBaseAtiva(DelegateExecution execution) throws Exception {
        logger.info("Executing motorProativoService.carregarBaseAtiva for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract campaign parameters
            String tipoCampanha = (String) execution.getVariable("tipoCampanha");
            String criteriosElegibilidade = (String) execution.getVariable("criteriosElegibilidade");
            Integer limiteBase = (Integer) execution.getVariable("limiteBase");

            // Query Data Lake for eligible beneficiaries
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("tipoCampanha", tipoCampanha);
            queryParams.put("statusContratual", "ATIVO");
            queryParams.put("limite", limiteBase != null ? limiteBase : 10000);

            // Apply eligibility filters
            if ("PREVENCAO_CRONICAS".equals(tipoCampanha)) {
                queryParams.put("condicoesCronicas", Arrays.asList("DIABETES", "HIPERTENSAO", "DPOC"));
                queryParams.put("riscoMinimo", "MEDIO");
            } else if ("VACINAS".equals(tipoCampanha)) {
                queryParams.put("idadeMinima", 60);
                queryParams.put("vacinasAtrasadas", true);
            } else if ("EXAMES_PREVENTIVOS".equals(tipoCampanha)) {
                queryParams.put("tempoUltimoExame", "> 12 meses");
            }

            // Load beneficiaries from Data Lake
            List<Map<String, Object>> baseElegivel = dataLakeService.consultarLista(
                execution, "beneficiarios_ativos", queryParams);

            // Segment base by risk and priority
            List<Map<String, Object>> baseSegmentada = segmentarBase(baseElegivel, execution);

            // Store loaded base
            execution.setVariable("baseAtiva", baseSegmentada);
            execution.setVariable("baseAtivaTotal", baseSegmentada.size());
            execution.setVariable("baseCarregadaEm", LocalDateTime.now().toString());
            execution.setVariable("campanhaAtiva", true);

            logger.info("Active base loaded: campaign={}, eligible={}, segments={}",
                tipoCampanha, baseSegmentada.size(), contarSegmentos(baseSegmentada));

        } catch (Exception e) {
            logger.error("Error loading active base for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("baseCarregamentoErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Register executed actions
     *
     * Records actions executed on beneficiaries during proactive campaign
     * for tracking engagement and follow-up.
     *
     * @param execution Process execution context
     */
    public void registrarAcoes(DelegateExecution execution) throws Exception {
        logger.info("Executing motorProativoService.registrarAcoes for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String tipoAcao = (String) execution.getVariable("tipoAcao");
            String resultadoAcao = (String) execution.getVariable("resultadoAcao");
            String canalAcao = (String) execution.getVariable("canalAcao");

            // Build action record
            Map<String, Object> registroAcao = new HashMap<>();
            registroAcao.put("beneficiarioId", beneficiarioId);
            registroAcao.put("campanhaId", execution.getVariable("campanhaId"));
            registroAcao.put("tipoAcao", tipoAcao);
            registroAcao.put("resultadoAcao", resultadoAcao);
            registroAcao.put("canalAcao", canalAcao);
            registroAcao.put("dataHoraAcao", LocalDateTime.now().toString());
            registroAcao.put("processInstanceId", execution.getProcessInstanceId());

            // Add action details
            registroAcao.put("agendamentoRealizado", execution.getVariable("agendamentoRealizado"));
            registroAcao.put("materialEnviado", execution.getVariable("materialEnviado"));
            registroAcao.put("protocoloGerado", execution.getVariable("protocoloGerado"));
            registroAcao.put("proximoFollowUp", execution.getVariable("proximoFollowUp"));

            // Save action to Data Lake
            dataLakeService.salvar(execution, "acoes_proativas",
                UUID.randomUUID().toString(), registroAcao);

            // Update campaign counters
            incrementarContadores(execution, resultadoAcao);

            execution.setVariable("acaoRegistrada", true);

            logger.info("Action registered: beneficiary={}, type={}, result={}",
                beneficiarioId, tipoAcao, resultadoAcao);

        } catch (Exception e) {
            logger.error("Error registering action for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("registroAcaoErro", e.getMessage());
            // Non-critical - don't throw
        }
    }

    /**
     * Register no-action beneficiaries
     *
     * Records beneficiaries who did not require action during campaign
     * for exclusion tracking and future targeting.
     *
     * @param execution Process execution context
     */
    public void registrarSemAcao(DelegateExecution execution) throws Exception {
        logger.info("Executing motorProativoService.registrarSemAcao for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String motivoSemAcao = (String) execution.getVariable("motivoSemAcao");

            // Build no-action record
            Map<String, Object> registroSemAcao = new HashMap<>();
            registroSemAcao.put("beneficiarioId", beneficiarioId);
            registroSemAcao.put("campanhaId", execution.getVariable("campanhaId"));
            registroSemAcao.put("motivoSemAcao", motivoSemAcao);
            registroSemAcao.put("dataHora", LocalDateTime.now().toString());
            registroSemAcao.put("processInstanceId", execution.getProcessInstanceId());

            // Common reasons: already compliant, recently contacted, opt-out
            registroSemAcao.put("jaEmConformidade", execution.getVariable("jaEmConformidade"));
            registroSemAcao.put("contatoRecente", execution.getVariable("contatoRecente"));
            registroSemAcao.put("optOut", execution.getVariable("optOut"));

            // Save no-action record
            dataLakeService.salvar(execution, "sem_acao_proativa",
                UUID.randomUUID().toString(), registroSemAcao);

            execution.setVariable("semAcaoRegistrado", true);

            logger.info("No-action registered: beneficiary={}, reason={}",
                beneficiarioId, motivoSemAcao);

        } catch (Exception e) {
            logger.error("Error registering no-action for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("registroSemAcaoErro", e.getMessage());
            // Non-critical - don't throw
        }
    }

    /**
     * Segment beneficiary base by risk and priority
     */
    private List<Map<String, Object>> segmentarBase(
        List<Map<String, Object>> base, DelegateExecution execution) {

        List<Map<String, Object>> baseSegmentada = new ArrayList<>();

        for (Map<String, Object> beneficiario : base) {
            // Calculate risk score
            String riscoSaude = riscoCalculator.calcularRisco(execution, beneficiario);
            beneficiario.put("riscoCalculado", riscoSaude);

            // Assign priority
            String prioridade = determinarPrioridade(riscoSaude, beneficiario);
            beneficiario.put("prioridade", prioridade);

            // Assign segment
            String segmento = determinarSegmento(beneficiario);
            beneficiario.put("segmento", segmento);

            baseSegmentada.add(beneficiario);
        }

        // Sort by priority: CRITICA > ALTA > MEDIA > BAIXA
        baseSegmentada.sort((a, b) -> {
            String prioA = (String) a.get("prioridade");
            String prioB = (String) b.get("prioridade");
            return comparePriority(prioB, prioA); // Descending
        });

        return baseSegmentada;
    }

    private String determinarPrioridade(String riscoSaude, Map<String, Object> beneficiario) {
        if ("CRITICO".equals(riscoSaude)) return "CRITICA";
        if ("ALTO".equals(riscoSaude)) return "ALTA";

        // Check other factors
        Integer idade = (Integer) beneficiario.get("idade");
        if (idade != null && idade >= 80) return "ALTA";

        return "MEDIA";
    }

    private String determinarSegmento(Map<String, Object> beneficiario) {
        Integer idade = (Integer) beneficiario.get("idade");

        if (idade != null && idade >= 60) return "IDOSO";
        if (idade != null && idade < 18) return "PEDIATRICO";

        List<String> cronicas = (List<String>) beneficiario.get("condicoesCronicas");
        if (cronicas != null && !cronicas.isEmpty()) return "CRONICO";

        return "GERAL";
    }

    private int comparePriority(String a, String b) {
        Map<String, Integer> pesos = Map.of(
            "CRITICA", 4,
            "ALTA", 3,
            "MEDIA", 2,
            "BAIXA", 1
        );
        return pesos.getOrDefault(a, 0) - pesos.getOrDefault(b, 0);
    }

    private Map<String, Integer> contarSegmentos(List<Map<String, Object>> base) {
        Map<String, Integer> contagem = new HashMap<>();
        for (Map<String, Object> beneficiario : base) {
            String segmento = (String) beneficiario.get("segmento");
            contagem.put(segmento, contagem.getOrDefault(segmento, 0) + 1);
        }
        return contagem;
    }

    private void incrementarContadores(DelegateExecution execution, String resultado) {
        Integer tentativas = (Integer) execution.getVariable("contatosTentados");
        execution.setVariable("contatosTentados", (tentativas != null ? tentativas : 0) + 1);

        if ("SUCESSO".equals(resultado)) {
            Integer realizados = (Integer) execution.getVariable("contatosRealizados");
            execution.setVariable("contatosRealizados", (realizados != null ? realizados : 0) + 1);
        } else if ("FALHA".equals(resultado)) {
            Integer falhados = (Integer) execution.getVariable("contatosFalhados");
            execution.setVariable("contatosFalhados", (falhados != null ? falhados : 0) + 1);
        }
    }
}
