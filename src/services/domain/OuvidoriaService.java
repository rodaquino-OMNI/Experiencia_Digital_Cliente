package br.com.austa.experiencia.services.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço de Ouvidoria para escalação de casos críticos
 */
@Slf4j
@Service
public class OuvidoriaService {

    /**
     * Cria caso na ouvidoria
     */
    public CasoOuvidoria criarCaso(String protocoloReclamacao, String motivo, PrioridadeOuvidoria prioridade) {
        String protocoloOuvidoria = gerarProtocoloOuvidoria();
        String responsavel = atribuirResponsavel(prioridade);

        log.info("Caso de ouvidoria criado: {} para reclamação: {} com prioridade: {}",
                 protocoloOuvidoria, protocoloReclamacao, prioridade);

        return CasoOuvidoria.builder()
            .protocolo(protocoloOuvidoria)
            .protocoloReclamacao(protocoloReclamacao)
            .motivo(motivo)
            .prioridade(prioridade)
            .responsavel(responsavel)
            .status("ABERTO")
            .dataAbertura(LocalDateTime.now())
            .build();
    }

    private String gerarProtocoloOuvidoria() {
        return "OUV-" + System.currentTimeMillis() + "-" +
               UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String atribuirResponsavel(PrioridadeOuvidoria prioridade) {
        return switch (prioridade) {
            case CRITICA -> "ouvidoria.geral@austa.com.br";
            case ALTA -> "ouvidoria.alta@austa.com.br";
            case NORMAL -> "ouvidoria.atendimento@austa.com.br";
        };
    }

    public enum PrioridadeOuvidoria {
        NORMAL, ALTA, CRITICA
    }

    @lombok.Data
    @lombok.Builder
    public static class CasoOuvidoria {
        private String protocolo;
        private String protocoloReclamacao;
        private String motivo;
        private PrioridadeOuvidoria prioridade;
        private String responsavel;
        private String status;
        private LocalDateTime dataAbertura;
    }
}
