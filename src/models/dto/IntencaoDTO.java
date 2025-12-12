package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class IntencaoDTO {
    private String intencao;
    private Double confianca;
    private List<Map<String, Object>> entidades;
    private String sentimento;
    private List<String> topicos;
}
