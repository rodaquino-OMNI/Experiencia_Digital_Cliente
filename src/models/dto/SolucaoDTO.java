package br.com.austa.experiencia.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para Solução de Reclamação
 * Usado no processo SUB-009: Gestão de Reclamações
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolucaoDTO {

    private String id;
    private String tipoReclamacao;
    private String causaRaiz;
    private String descricao;

    // Efetividade
    private Double taxaSucesso;
    private Integer vezesAplicada;
    private Integer vezesResolvida;

    // Metadata
    private String fonte; // HISTORICO, KNOWLEDGE_BASE
    private LocalDateTime ultimaAplicacao;
    private String categoria;

    // Tempo estimado
    private Integer tempoMedioResolucao; // em horas
}
