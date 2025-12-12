package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class CanalDTO {
    private String tipo;
    private String nome;
    private Map<String, Object> atributos;
    private boolean suportaMultimidia;
    private boolean suportaRichContent;
}
