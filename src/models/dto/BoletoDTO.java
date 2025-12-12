package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BoletoDTO {
    private String url;
    private String linhaDigitavel;
    private String codigoBarras;
    private LocalDate dataVencimento;
    private BigDecimal valor;
}
