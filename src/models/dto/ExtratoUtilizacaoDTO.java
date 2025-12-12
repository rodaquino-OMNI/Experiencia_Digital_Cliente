package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ExtratoUtilizacaoDTO {
    private List<Map<String, Object>> utilizacoes;
    private Integer totalUtilizacoes;
    private BigDecimal valorTotal;
}
