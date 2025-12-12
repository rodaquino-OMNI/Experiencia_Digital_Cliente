package com.experiencia.services.domain.followup;

import com.experiencia.clients.NlpClient;
import com.experiencia.models.RespostaNps;
import com.experiencia.models.AnaliseSentimento;
import com.experiencia.repositories.RespostaNpsRepository;
import com.experiencia.repositories.AnaliseSentimentoRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Delegate: Analisar Sentimento
 *
 * Responsabilidades:
 * - Aplicar NLP no feedback textual
 * - Classificar sentimento (positivo/neutro/negativo)
 * - Extrair entidades e tópicos
 * - Identificar pontos de dor e elogios
 * - Alimentar base de conhecimento
 */
@Component("analisarSentimentoDelegate")
public class AnalisarSentimentoDelegate implements JavaDelegate {

    @Autowired
    private RespostaNpsRepository respostaNpsRepository;

    @Autowired
    private AnaliseSentimentoRepository analiseSentimentoRepository;

    @Autowired
    private NlpClient nlpClient;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long respostaId = (Long) execution.getVariable("respostaId");

        // 1. Recuperar resposta NPS
        RespostaNps resposta = respostaNpsRepository.findById(respostaId)
            .orElseThrow(() -> new RuntimeException("Resposta NPS não encontrada: " + respostaId));

        // 2. Verificar se há feedback para analisar
        if (resposta.getFeedback() == null || resposta.getFeedback().trim().isEmpty()) {
            execution.setVariable("semFeedback", true);
            return;
        }

        // 3. Criar registro de análise
        AnaliseSentimento analise = new AnaliseSentimento();
        analise.setRespostaNpsId(respostaId);
        analise.setTextoOriginal(resposta.getFeedback());
        analise.setDataAnalise(LocalDateTime.now());

        // 4. Análise de Sentimento
        Map<String, Object> resultadoSentimento = nlpClient.analisarSentimento(resposta.getFeedback());

        analise.setSentimento((String) resultadoSentimento.get("sentimento"));
        analise.setConfiancaSentimento((Double) resultadoSentimento.get("confianca"));
        analise.setPolaridade((Double) resultadoSentimento.get("polaridade"));

        // 5. Extração de Entidades
        List<Map<String, String>> entidades = nlpClient.extrairEntidades(resposta.getFeedback());
        analise.setEntidades(entidades);

        // 6. Extração de Tópicos
        List<String> topicos = nlpClient.extrairTopicos(resposta.getFeedback());
        analise.setTopicos(topicos);

        // 7. Identificar Aspectos (o que foi mencionado)
        List<String> aspectos = identificarAspectos(resposta.getFeedback());
        analise.setAspectos(aspectos);

        // 8. Classificar Urgência baseado em palavras-chave
        String urgencia = classificarUrgencia(resposta.getFeedback(), analise.getSentimento());
        analise.setUrgencia(urgencia);

        // 9. Identificar se há solicitação de contato
        boolean solicitaContato = detectarSolicitacaoContato(resposta.getFeedback());
        analise.setSolicitaContato(solicitaContato);

        // 10. Salvar análise
        analiseSentimentoRepository.save(analise);

        // 11. Atualizar resposta NPS com resultado da análise
        resposta.setSentimento(analise.getSentimento());
        resposta.setAnalisado(true);
        respostaNpsRepository.save(resposta);

        // 12. Setar variáveis de processo
        execution.setVariable("sentimento", analise.getSentimento());
        execution.setVariable("urgencia", urgencia);
        execution.setVariable("solicitaContato", solicitaContato);
        execution.setVariable("topicos", topicos);

        // 13. Determinar próximas ações
        boolean precisaAcaoImediata = "ALTA".equals(urgencia) || solicitaContato;
        execution.setVariable("precisaAcaoImediata", precisaAcaoImediata);
    }

    private List<String> identificarAspectos(String feedback) {
        // Aspectos típicos do negócio
        List<String> aspectos = new java.util.ArrayList<>();

        String feedbackLower = feedback.toLowerCase();

        if (feedbackLower.contains("atendimento") || feedbackLower.contains("atendente")) {
            aspectos.add("ATENDIMENTO");
        }
        if (feedbackLower.contains("produto") || feedbackLower.contains("qualidade")) {
            aspectos.add("QUALIDADE_PRODUTO");
        }
        if (feedbackLower.contains("preço") || feedbackLower.contains("caro") || feedbackLower.contains("valor")) {
            aspectos.add("PRECO");
        }
        if (feedbackLower.contains("entrega") || feedbackLower.contains("prazo") || feedbackLower.contains("atraso")) {
            aspectos.add("ENTREGA");
        }
        if (feedbackLower.contains("site") || feedbackLower.contains("app") || feedbackLower.contains("sistema")) {
            aspectos.add("PLATAFORMA_DIGITAL");
        }
        if (feedbackLower.contains("pagamento") || feedbackLower.contains("cobrança")) {
            aspectos.add("PAGAMENTO");
        }

        return aspectos;
    }

    private String classificarUrgencia(String feedback, String sentimento) {
        String feedbackLower = feedback.toLowerCase();

        // Palavras que indicam urgência alta
        String[] palavrasUrgentes = {
            "urgente", "imediato", "agora", "já",
            "cancelar", "processso", "reclamação",
            "péssimo", "horrível", "terrível"
        };

        for (String palavra : palavrasUrgentes) {
            if (feedbackLower.contains(palavra)) {
                return "ALTA";
            }
        }

        // Se sentimento é muito negativo
        if ("NEGATIVO".equals(sentimento)) {
            return "MEDIA";
        }

        return "BAIXA";
    }

    private boolean detectarSolicitacaoContato(String feedback) {
        String feedbackLower = feedback.toLowerCase();

        String[] frasesContato = {
            "me liguem", "entrem em contato", "quero falar",
            "preciso de ajuda", "resolver", "solucionar",
            "quero cancelar", "retorno"
        };

        for (String frase : frasesContato) {
            if (feedbackLower.contains(frase)) {
                return true;
            }
        }

        return false;
    }
}
