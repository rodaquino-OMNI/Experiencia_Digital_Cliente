package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class Perfil360DTO {
    private String beneficiarioId;
    private Map<String, Object> dadosCompletos;
    private LocalDateTime ultimaInteracao;
    private Integer scoreSaude;
}
