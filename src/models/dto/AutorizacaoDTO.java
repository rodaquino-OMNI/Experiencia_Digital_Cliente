package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class AutorizacaoDTO {
    private boolean aprovada;
    private String numeroAutorizacao;
    private String regraAplicada;
    private LocalDate validadeAutorizacao;
    private String motivoReprovacao;
}
