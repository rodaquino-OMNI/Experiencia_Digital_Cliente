package br.com.austa.experiencia.services.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Serviço de Analytics para análise de tendências
 */
@Slf4j
@Service
public class AnalyticsService {

    /**
     * Analisa tendências de reclamações em um período
     */
    public TrendAnalysis analyzeTrends(String tipoReclamacao, LocalDate dataInicio, LocalDate dataFim) {
        log.debug("Analisando tendências para: {} de {} até {}", tipoReclamacao, dataInicio, dataFim);

        // Simula análise de tendências
        boolean isRecurring = Math.random() > 0.7; // 30% chance de ser recorrente
        double growthRate = (Math.random() - 0.5) * 0.4; // -20% a +20%
        int occurrences = (int) (Math.random() * 50) + 10;

        return TrendAnalysis.builder()
            .tipoReclamacao(tipoReclamacao)
            .dataInicio(dataInicio)
            .dataFim(dataFim)
            .recurring(isRecurring)
            .growthRate(growthRate)
            .occurrences(occurrences)
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class TrendAnalysis {
        private String tipoReclamacao;
        private LocalDate dataInicio;
        private LocalDate dataFim;
        private boolean recurring;
        private double growthRate;
        private int occurrences;
    }
}
