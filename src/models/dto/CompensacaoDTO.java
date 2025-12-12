package br.com.austa.experiencia.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para Compensação de Reclamação
 * Usado no processo SUB-009: Gestão de Reclamações
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensacaoDTO {

    private String id;
    private String beneficiarioId;
    private String protocoloReclamacao;

    // Tipo de compensação
    private String tipo; // DESCONTO, CREDITO, SERVICO_EXTRA, REEMBOLSO
    private BigDecimal valor;

    // Aprovação
    private String aprovadoPor;
    private LocalDateTime dataAprovacao;

    // Aplicação
    private String codigo;
    private LocalDateTime dataAplicacao;
    private LocalDate dataVigencia;
    private String status;

    // Dados bancários (para reembolso)
    private String dadosBancarios;
    private String servicoExtra;

    // Rastreabilidade
    private LocalDateTime dataCriacao;
    private String criadoPor;
}
