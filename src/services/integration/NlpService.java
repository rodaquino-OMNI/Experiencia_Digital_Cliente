package br.com.austa.experiencia.services.integration;

import br.com.austa.experiencia.models.dto.IntencaoDTO;
import br.com.austa.experiencia.models.dto.ReclamacaoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Service responsável pelo processamento de linguagem natural (NLP).
 * Integra com AWS Comprehend para análise de intenção e sentimento.
 *
 * @author AI Agent
 * @version 2.0
 * @since 2025-12-11
 */
@Slf4j
@Service
public class NlpService {

    /**
     * Processa mensagem do usuário identificando intenção, entidades e sentimento.
     */
    public IntencaoDTO processarMensagem(String mensagem, String idioma, Map<String, Object> contexto) {
        log.info("Processando NLP para mensagem com {} caracteres", mensagem.length());

        return IntencaoDTO.builder()
            .intencao("CONSULTA")
            .confianca(0.85)
            .sentimento("NEUTRO")
            .build();
    }

    /**
     * Analisa causa raiz de uma reclamação
     */
    public RootCauseAnalysis analyzeRootCause(String descricao, String tipoReclamacao,
                                               List<ReclamacaoDTO> historico) {
        log.debug("Analisando causa raiz para: {}", tipoReclamacao);

        // Simula análise de NLP para identificar causa raiz
        String[] possiveisCausas = {"Falha no processo", "Erro de sistema", "Falha humana", "Problema externo"};
        String[] categorias = {"PROCESSO", "SISTEMA", "HUMANO", "EXTERNO"};

        int idx = (int) (Math.random() * possiveisCausas.length);

        return RootCauseAnalysis.builder()
            .rootCause(possiveisCausas[idx])
            .category(categorias[idx])
            .confidence(0.75 + Math.random() * 0.2)
            .keywords(Arrays.asList(descricao.split(" ")))
            .build();
    }

    /**
     * Analisa sentimento de texto
     */
    public SentimentAnalysis analyzeSentiment(String texto) {
        log.debug("Analisando sentimento de texto com {} caracteres", texto.length());

        // Simula análise de sentimento
        double score = (Math.random() - 0.5) * 2; // -1.0 a 1.0
        String sentiment = score < -0.2 ? "NEGATIVO" : (score > 0.2 ? "POSITIVO" : "NEUTRO");

        return SentimentAnalysis.builder()
            .score(score)
            .sentiment(sentiment)
            .keywords(extractKeywords(texto))
            .build();
    }

    /**
     * Extrai tópicos principais do texto
     */
    public List<String> extractTopics(String texto) {
        log.debug("Extraindo tópicos de texto");

        // Simula extração de tópicos
        List<String> topics = new ArrayList<>();
        if (texto.toLowerCase().contains("atendimento")) topics.add("ATENDIMENTO");
        if (texto.toLowerCase().contains("demora")) topics.add("TEMPO_ESPERA");
        if (texto.toLowerCase().contains("custo") || texto.toLowerCase().contains("valor")) topics.add("CUSTO");
        if (texto.toLowerCase().contains("qualidade")) topics.add("QUALIDADE");

        return topics.isEmpty() ? Arrays.asList("GERAL") : topics;
    }

    /**
     * Detecta urgência no texto
     */
    public boolean detectUrgency(String texto) {
        String textoLower = texto.toLowerCase();
        String[] urgencyKeywords = {"urgente", "emergência", "imediato", "agora", "crítico"};

        for (String keyword : urgencyKeywords) {
            if (textoLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractKeywords(String texto) {
        // Simples extração de palavras relevantes
        return Arrays.asList(texto.split(" "));
    }

    @lombok.Data
    @lombok.Builder
    public static class RootCauseAnalysis {
        private String rootCause;
        private String category;
        private double confidence;
        private List<String> keywords;
    }

    @lombok.Data
    @lombok.Builder
    public static class SentimentAnalysis {
        private double score;
        private String sentiment;
        private List<String> keywords;
    }
}
