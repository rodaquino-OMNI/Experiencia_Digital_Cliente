package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElegibilidadeDTO {
    private boolean elegivel;
    private String motivoInelegibilidade;
    private String statusPlano;
    private String beneficiarioId;
    private String nomeBeneficiario;
}
