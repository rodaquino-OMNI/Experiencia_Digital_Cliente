package br.com.austa.experiencia.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CarterinhaDigitalDTO {
    private String url;
    private String qrCodeData;
    private LocalDateTime dataGeracao;
    private String downloadToken;
}
