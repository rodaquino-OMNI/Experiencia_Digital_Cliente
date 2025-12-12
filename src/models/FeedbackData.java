package br.com.austa.experiencia.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelo para dados de feedback do beneficiário
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackData {
    private String beneficiarioId;
    private Integer notaNps;
    private String sentimento;
    private List<String> temas;
    private String tipoInteracao;
    private Long tempoResolucao;
    private LocalDateTime timestamp;
}
