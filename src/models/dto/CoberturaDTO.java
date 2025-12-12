package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CoberturaDTO {
    private boolean coberto;
    private boolean emCarencia;
    private Integer diasCarenciaRestantes;
    private String motivoNaoCobertura;
    private BigDecimal valorCoparticipacao;
}
