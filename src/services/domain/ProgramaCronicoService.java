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
 * Programa Cronico Service - Chronic Disease Program Management Delegate
 *
 * Manages enrollment and tracking for chronic disease management programs
 * including Diabetes, Hypertension, COPD with adherence monitoring.
 *
 * BPMN Coverage:
 * - programaCronicoService.criarGenerico (Create generic program enrollment)
 * - programaCronicoService.definirMetas (Define program goals)
 * - programaCronicoService.avaliarAdesao (Evaluate adherence)
 * - programaCronicoService.consolidarResultados (Consolidate program results)
 * - programaCronicoService.agendarContatoInicial (Schedule initial contact)
 */
@Component("programaCronicoService")
public class ProgramaCronicoService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(ProgramaCronicoService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("programaCronicoMethod");

        switch (method) {
            case "criarGenerico":
                criarGenerico(execution);
                break;
            case "definirMetas":
                definirMetas(execution);
                break;
            case "avaliarAdesao":
                avaliarAdesao(execution);
                break;
            case "consolidarResultados":
                consolidarResultados(execution);
                break;
            case "agendarContatoInicial":
                agendarContatoInicial(execution);
                break;
            default:
                logger.warn("Unknown programa cronico method: {}", method);
                throw new IllegalArgumentException("Invalid programa cronico method: " + method);
        }
    }

    /**
     * Create generic program enrollment
     */
    public void criarGenerico(DelegateExecution execution) throws Exception {
        logger.info("Executing programaCronicoService.criarGenerico for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String tipoPrograma = (String) execution.getVariable("tipoPrograma");
            List<String> condicoes = (List<String>) execution.getVariable("condicoes");

            Map<String, Object> programa = new HashMap<>();
            programa.put("programaId", UUID.randomUUID().toString());
            programa.put("beneficiarioId", beneficiarioId);
            programa.put("tipoPrograma", tipoPrograma);
            programa.put("condicoes", condicoes);
            programa.put("dataInscricao", LocalDateTime.now().toString());
            programa.put("status", "ATIVO");
            programa.put("faseProgramaAtual", "INSCRICAO");

            dataLakeService.salvar(execution, "programas_cronicos",
                (String) programa.get("programaId"), programa);

            execution.setVariable("programaId", programa.get("programaId"));
            execution.setVariable("programaCriado", true);

            logger.info("Chronic program created: id={}, type={}, beneficiary={}",
                programa.get("programaId"), tipoPrograma, beneficiarioId);

        } catch (Exception e) {
            logger.error("Error creating chronic program: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Define program goals
     */
    public void definirMetas(DelegateExecution execution) throws Exception {
        logger.info("Executing programaCronicoService.definirMetas for process {}",
            execution.getProcessInstanceId());

        try {
            String programaId = (String) execution.getVariable("programaId");
            String tipoPrograma = (String) execution.getVariable("tipoPrograma");

            List<Map<String, Object>> metas = new ArrayList<>();

            if ("DIABETES".equals(tipoPrograma)) {
                metas.add(Map.of("metrica", "HbA1c", "alvo", "< 7%", "prazo", "3 meses"));
                metas.add(Map.of("metrica", "Glicemia jejum", "alvo", "< 100 mg/dL", "prazo", "3 meses"));
                metas.add(Map.of("metrica", "Exame fundo olho", "alvo", "Realizado", "prazo", "6 meses"));
            } else if ("HIPERTENSAO".equals(tipoPrograma)) {
                metas.add(Map.of("metrica", "PA sistólica", "alvo", "< 140 mmHg", "prazo", "2 meses"));
                metas.add(Map.of("metrica", "PA diastólica", "alvo", "< 90 mmHg", "prazo", "2 meses"));
                metas.add(Map.of("metrica", "Colesterol total", "alvo", "< 200 mg/dL", "prazo", "3 meses"));
            } else if ("DPOC".equals(tipoPrograma)) {
                metas.add(Map.of("metrica", "VEF1", "alvo", "> 60%", "prazo", "6 meses"));
                metas.add(Map.of("metrica", "Exacerbações", "alvo", "< 2/ano", "prazo", "12 meses"));
            }

            Map<String, Object> updatePrograma = Map.of(
                "metas", metas,
                "metasDefinidadEm", LocalDateTime.now().toString()
            );

            dataLakeService.atualizar(execution, "programas_cronicos", programaId, updatePrograma);

            execution.setVariable("metasDefinidas", true);
            execution.setVariable("metas", metas);

            logger.info("Program goals defined: program={}, goals={}", programaId, metas.size());

        } catch (Exception e) {
            logger.error("Error defining program goals: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Evaluate adherence
     */
    public void avaliarAdesao(DelegateExecution execution) throws Exception {
        logger.info("Executing programaCronicoService.avaliarAdesao for process {}",
            execution.getProcessInstanceId());

        try {
            String programaId = (String) execution.getVariable("programaId");

            // Fetch program data
            Map<String, Object> programa = dataLakeService.consultar(
                execution, "programas_cronicos", programaId);

            int scoreAdesao = 100;
            List<String> fatoresNaoAdesao = new ArrayList<>();

            // Check appointment adherence
            Integer consultasAgendadas = (Integer) programa.getOrDefault("consultasAgendadas", 0);
            Integer consultasRealizadas = (Integer) programa.getOrDefault("consultasRealizadas", 0);

            if (consultasAgendadas > 0) {
                double taxaComparecimento = (consultasRealizadas * 100.0) / consultasAgendadas;
                if (taxaComparecimento < 80) {
                    scoreAdesao -= 20;
                    fatoresNaoAdesao.add("Baixo comparecimento a consultas");
                }
            }

            // Check medication adherence
            Integer adesaoMedicacao = (Integer) programa.getOrDefault("scoreAdesaoMedicacao", 100);
            if (adesaoMedicacao < 80) {
                scoreAdesao -= 20;
                fatoresNaoAdesao.add("Baixa adesão medicamentosa");
            }

            // Check exam compliance
            Integer examesRealizados = (Integer) programa.getOrDefault("examesRealizados", 0);
            Integer examesPrevistos = (Integer) programa.getOrDefault("examesPrevistos", 0);

            if (examesPrevistos > 0) {
                double taxaExames = (examesRealizados * 100.0) / examesPrevistos;
                if (taxaExames < 70) {
                    scoreAdesao -= 15;
                    fatoresNaoAdesao.add("Exames preventivos atrasados");
                }
            }

            String nivelAdesao = scoreAdesao >= 80 ? "ALTA" : scoreAdesao >= 60 ? "MEDIA" : "BAIXA";

            Map<String, Object> avaliacaoAdesao = Map.of(
                "scoreAdesao", scoreAdesao,
                "nivelAdesao", nivelAdesao,
                "fatoresNaoAdesao", fatoresNaoAdesao,
                "dataAvaliacao", LocalDateTime.now().toString()
            );

            dataLakeService.atualizar(execution, "programas_cronicos", programaId, avaliacaoAdesao);

            execution.setVariable("scoreAdesao", scoreAdesao);
            execution.setVariable("nivelAdesao", nivelAdesao);
            execution.setVariable("adesaoAvaliada", true);

            logger.info("Adherence evaluated: program={}, score={}, level={}",
                programaId, scoreAdesao, nivelAdesao);

        } catch (Exception e) {
            logger.error("Error evaluating adherence: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Consolidate program results
     */
    public void consolidarResultados(DelegateExecution execution) throws Exception {
        logger.info("Executing programaCronicoService.consolidarResultados for process {}",
            execution.getProcessInstanceId());

        try {
            String programaId = (String) execution.getVariable("programaId");

            Map<String, Object> programa = dataLakeService.consultar(
                execution, "programas_cronicos", programaId);

            Map<String, Object> resultados = new HashMap<>();
            resultados.put("programaId", programaId);
            resultados.put("dataConsolidacao", LocalDateTime.now().toString());

            // Outcome metrics
            resultados.put("duracaoPrograma", calcularDuracao(programa));
            resultados.put("metasAlcancadas", (Integer) programa.getOrDefault("metasAlcancadas", 0));
            resultados.put("metasTotal", ((List<?>) programa.getOrDefault("metas", new ArrayList<>())).size());
            resultados.put("scoreAdesaoFinal", programa.get("scoreAdesao"));

            // Clinical outcomes
            resultados.put("melhoriaIndicadores", programa.get("melhoriaIndicadores"));
            resultados.put("reducaoInternacoes", programa.get("reducaoInternacoes"));

            // Satisfaction
            resultados.put("satisfacaoPrograma", programa.get("satisfacaoPrograma"));

            dataLakeService.salvar(execution, "programas_cronicos_resultados",
                programaId, resultados);

            kafkaPublisher.publicar(execution, "programa-cronico-concluido", resultados);

            execution.setVariable("resultadosConsolidados", true);
            execution.setVariable("resultados", resultados);

            logger.info("Program results consolidated: program={}, goals={}/{}",
                programaId,
                resultados.get("metasAlcancadas"),
                resultados.get("metasTotal"));

        } catch (Exception e) {
            logger.error("Error consolidating results: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Schedule initial contact
     */
    public void agendarContatoInicial(DelegateExecution execution) throws Exception {
        logger.info("Executing programaCronicoService.agendarContatoInicial for process {}",
            execution.getProcessInstanceId());

        try {
            String programaId = (String) execution.getVariable("programaId");
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");

            LocalDateTime dataContato = LocalDateTime.now().plusDays(7);

            Map<String, Object> agendamento = Map.of(
                "programaId", programaId,
                "beneficiarioId", beneficiarioId,
                "tipoContato", "INICIAL",
                "dataAgendada", dataContato.toString(),
                "canal", "TELEFONE",
                "duracao", "30 minutos",
                "objetivo", "Apresentar programa e alinhar expectativas"
            );

            dataLakeService.salvar(execution, "agendamentos_programas",
                UUID.randomUUID().toString(), agendamento);

            execution.setVariable("contatoInicialAgendado", true);
            execution.setVariable("dataContatoInicial", dataContato.toString());

            logger.info("Initial contact scheduled: program={}, date={}", programaId, dataContato);

        } catch (Exception e) {
            logger.error("Error scheduling initial contact: {}", e.getMessage(), e);
            throw e;
        }
    }

    private int calcularDuracao(Map<String, Object> programa) {
        // Calculate duration in days
        return 90; // Placeholder
    }
}
