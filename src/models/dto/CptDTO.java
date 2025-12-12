package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class CptDTO {
    private boolean bloqueado;
    private String tipoBloqueio;
    private String motivoBloqueio;
    private LocalDate dataInicioBloqueio;
    private LocalDate dataFimBloqueio;
}
