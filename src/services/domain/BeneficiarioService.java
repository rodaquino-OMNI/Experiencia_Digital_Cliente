package br.com.austa.experiencia.services.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de Beneficiários
 */
@Slf4j
@Service
public class BeneficiarioService {

    /**
     * Busca beneficiário por ID
     */
    public BeneficiarioDTO buscarPorId(String beneficiarioId) {
        log.debug("Buscando beneficiário: {}", beneficiarioId);

        // Simula busca no banco
        return BeneficiarioDTO.builder()
            .id(beneficiarioId)
            .nome("Beneficiário " + beneficiarioId)
            .email("beneficiario" + beneficiarioId + "@example.com")
            .telefone("+5511999999999")
            .canalPreferido("WHATSAPP")
            .build();
    }

    /**
     * Busca histórico de interações
     */
    public HistoricoInteracoesDTO buscarHistoricoInteracoes(String beneficiarioId, int dias) {
        log.debug("Buscando histórico de {} dias para beneficiário: {}", dias, beneficiarioId);

        List<Interacao> interacoes = new ArrayList<>();
        interacoes.add(Interacao.builder()
            .tipo("ATENDIMENTO")
            .data(LocalDateTime.now().minusDays(5))
            .canal("WHATSAPP")
            .build());

        return HistoricoInteracoesDTO.builder()
            .beneficiarioId(beneficiarioId)
            .interacoes(interacoes)
            .totalInteracoes(interacoes.size())
            .build();
    }

    /**
     * Atualiza scores preditivos
     */
    public void atualizarScoresPreditivos(String beneficiarioId, Object scores) {
        log.info("Scores preditivos atualizados para beneficiário: {}", beneficiarioId);
        // Em produção: atualiza no banco de dados
    }

    @lombok.Data
    @lombok.Builder
    public static class BeneficiarioDTO {
        private String id;
        private String nome;
        private String email;
        private String telefone;
        private String canalPreferido;
    }

    @lombok.Data
    @lombok.Builder
    public static class HistoricoInteracoesDTO {
        private String beneficiarioId;
        private List<Interacao> interacoes;
        private int totalInteracoes;
    }

    @lombok.Data
    @lombok.Builder
    public static class Interacao {
        private String tipo;
        private LocalDateTime data;
        private String canal;
    }
}
