package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class GuiaTissDTO {
    private boolean valida;
    private String numeroGuia;
    private List<String> errosValidacao;
    private Map<String, Object> dadosEstruturados;
    private String versaoTiss;
}
