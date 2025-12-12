package br.com.austa.experiencia.services.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de Recuperação de Detratores NPS
 */
@Slf4j
@Service
public class RecuperacaoService {

    /**
     * Determina estratégia de recuperação baseada no contexto
     */
    public EstrategiaRecuperacao determinarEstrategia(Integer notaNps, String sentimento,
                                                       List<String> temas, Object historico) {
        log.debug("Determinando estratégia para NPS: {}, Sentimento: {}", notaNps, sentimento);

        if (notaNps <= 3 || "CRITICO".equals(sentimento)) {
            return EstrategiaRecuperacao.CONTATO_IMEDIATO_GERENCIA;
        } else if (notaNps <= 6) {
            return EstrategiaRecuperacao.CONTATO_COMERCIAL_RETENCAO;
        } else {
            return EstrategiaRecuperacao.PESQUISA_APROFUNDAMENTO;
        }
    }

    /**
     * Atribui responsável pela recuperação
     */
    public String atribuirResponsavel(EstrategiaRecuperacao estrategia) {
        return switch (estrategia) {
            case CONTATO_IMEDIATO_GERENCIA -> "gerencia.relacionamento@austa.com.br";
            case CONTATO_COMERCIAL_RETENCAO -> "retencao@austa.com.br";
            case PESQUISA_APROFUNDAMENTO -> "pesquisa.satisfacao@austa.com.br";
            case OFERTAR_BENEFICIO -> "relacionamento@austa.com.br";
        };
    }

    /**
     * Cria caso de recuperação
     */
    public CasoRecuperacaoDTO criarCaso(String beneficiarioId, String idPesquisa,
                                         EstrategiaRecuperacao estrategia, String responsavel,
                                         LocalDateTime prazoContato) {
        String id = UUID.randomUUID().toString();

        log.info("Caso de recuperação criado: {} para beneficiário: {} com estratégia: {}",
                 id, beneficiarioId, estrategia);

        return CasoRecuperacaoDTO.builder()
            .id(id)
            .beneficiarioId(beneficiarioId)
            .idPesquisa(idPesquisa)
            .estrategia(estrategia)
            .responsavel(responsavel)
            .prazoContato(prazoContato)
            .status("ABERTO")
            .dataCriacao(LocalDateTime.now())
            .build();
    }

    public enum EstrategiaRecuperacao {
        CONTATO_IMEDIATO_GERENCIA,
        CONTATO_COMERCIAL_RETENCAO,
        PESQUISA_APROFUNDAMENTO,
        OFERTAR_BENEFICIO
    }

    @lombok.Data
    @lombok.Builder
    public static class CasoRecuperacaoDTO {
        private String id;
        private String beneficiarioId;
        private String idPesquisa;
        private EstrategiaRecuperacao estrategia;
        private String responsavel;
        private LocalDateTime prazoContato;
        private String status;
        private LocalDateTime dataCriacao;
    }
}
