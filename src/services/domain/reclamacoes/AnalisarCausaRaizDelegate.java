package br.com.austa.experiencia.services.domain.reclamacoes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Analisa a causa raiz da reclamação usando IA/NLP.
 *
 * Variáveis de entrada:
 * - protocoloReclamacao (String)
 * - tipoReclamacao (String)
 * - descricao (String)
 * - historicoReclamacoes (List<ReclamacaoDTO>)
 *
 * Variáveis de saída:
 * - causaRaizIdentificada (String): Causa raiz detectada
 * - categoriaCausaRaiz (String): PROCESSO, SISTEMA, HUMANO, EXTERNO
 * - confiancaAnalise (Double): 0.0 a 1.0
 * - recomendacoes (List<String>): Recomendações de ação
 */
@Slf4j
@Component("analisarCausaRaizDelegate")
@RequiredArgsConstructor
public class AnalisarCausaRaizDelegate implements JavaDelegate {

    private final NlpService nlpService;
    private final ReclamacaoService reclamacaoService;
    private final AnalyticsService analyticsService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Analisando causa raiz - Protocolo: {}",
                 execution.getVariable("protocoloReclamacao"));

        String descricao = (String) execution.getVariable("descricao");
        String tipoReclamacao = (String) execution.getVariable("tipoReclamacao");
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        // 1. Buscar histórico de reclamações similares
        List<ReclamacaoDTO> historico = reclamacaoService
            .buscarReclamacoesSimilares(tipoReclamacao, 30);

        // 2. Analisar padrões com IA
        RootCauseAnalysis analysis = nlpService.analyzeRootCause(
            descricao,
            tipoReclamacao,
            historico
        );

        // 3. Identificar tendências
        TrendAnalysis trends = analyticsService.analyzeTrends(
            tipoReclamacao,
            LocalDate.now().minusDays(90),
            LocalDate.now()
        );

        // 4. Gerar recomendações
        List<String> recomendacoes = generateRecommendations(analysis, trends);

        // 5. Definir variáveis de saída
        execution.setVariable("causaRaizIdentificada", analysis.getRootCause());
        execution.setVariable("categoriaCausaRaiz", analysis.getCategory());
        execution.setVariable("confiancaAnalise", analysis.getConfidence());
        execution.setVariable("recomendacoes", recomendacoes);
        execution.setVariable("tendenciaRecorrencia", trends.isRecurring());

        log.info("Causa raiz identificada: {} (confiança: {})",
                 analysis.getRootCause(), analysis.getConfidence());
    }

    private List<String> generateRecommendations(RootCauseAnalysis analysis,
                                                  TrendAnalysis trends) {
        List<String> recommendations = new ArrayList<>();

        switch (analysis.getCategory()) {
            case "PROCESSO":
                recommendations.add("Revisar procedimento operacional");
                recommendations.add("Atualizar documentação");
                break;
            case "SISTEMA":
                recommendations.add("Escalar para TI");
                recommendations.add("Verificar integrações");
                break;
            case "HUMANO":
                recommendations.add("Capacitar equipe");
                recommendations.add("Revisar scripts de atendimento");
                break;
            case "EXTERNO":
                recommendations.add("Comunicar parceiro/prestador");
                recommendations.add("Avaliar penalidades contratuais");
                break;
        }

        if (trends.isRecurring()) {
            recommendations.add("ALERTA: Problema recorrente - escalar para gestão");
        }

        return recommendations;
    }
}
