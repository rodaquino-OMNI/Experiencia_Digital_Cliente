package br.com.austa.experiencia.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para Reclamação de Beneficiário
 * Usado no processo SUB-009: Gestão de Reclamações
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReclamacaoDTO {

    private String id;
    private String protocolo;
    private String beneficiarioId;
    private String canalOrigem;
    private String tipo;
    private String descricao;
    private List<String> anexos;

    // Classificação
    private String criticidade;
    private Integer slaHoras;
    private String responsavel;

    // Status
    private String status;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataLimiteResolucao;
    private LocalDateTime dataEncerramento;

    // Análise
    private String causaRaiz;
    private String categoriaCausaRaiz;
    private Double confiancaAnalise;
    private List<String> recomendacoes;

    // Resolução
    private String statusFinal;
    private String descricaoResolucao;
    private Long tempoResolucaoHoras;
    private Boolean dentroDosla;
    private String resolvidoPor;

    // Compensação (se aplicável)
    private Boolean compensacaoAplicada;
    private String tipoCompensacao;
    private String codigoCompensacao;
}
