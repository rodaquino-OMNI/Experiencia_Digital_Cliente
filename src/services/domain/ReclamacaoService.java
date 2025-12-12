package br.com.austa.experiencia.services.domain;

import br.com.austa.experiencia.models.dto.CompensacaoDTO;
import br.com.austa.experiencia.models.dto.ReclamacaoDTO;
import br.com.austa.experiencia.models.dto.SolucaoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serviço de Gestão de Reclamações
 * Gerencia o ciclo completo de reclamações de beneficiários
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReclamacaoService {

    private final ConcurrentHashMap<String, ReclamacaoDTO> reclamacoesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompensacaoDTO> compensacoesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SolucaoDTO>> solucoesCache = new ConcurrentHashMap<>();

    /**
     * Gera protocolo único de reclamação
     */
    public String gerarProtocolo(String canalOrigem) {
        String prefixo = switch (canalOrigem) {
            case "ANS" -> "ANS";
            case "PROCON" -> "PRC";
            case "WHATSAPP" -> "WPP";
            case "APP" -> "APP";
            default -> "RCL";
        };

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        return String.format("%s-%s-%s", prefixo, timestamp, random);
    }

    /**
     * Busca reclamações similares para análise de padrões
     */
    public List<ReclamacaoDTO> buscarReclamacoesSimilares(String tipoReclamacao, int ultimosDias) {
        log.debug("Buscando reclamações similares do tipo: {} dos últimos {} dias", tipoReclamacao, ultimosDias);

        LocalDateTime dataLimite = LocalDateTime.now().minusDays(ultimosDias);

        return reclamacoesCache.values().stream()
            .filter(r -> r.getTipo().equals(tipoReclamacao))
            .filter(r -> r.getDataAbertura() != null && r.getDataAbertura().isAfter(dataLimite))
            .collect(Collectors.toList());
    }

    /**
     * Busca soluções aplicadas anteriormente por tipo e causa raiz
     */
    public List<SolucaoDTO> buscarSolucoesPorTipoECausa(String tipoReclamacao, String causaRaiz) {
        log.debug("Buscando soluções para tipo: {} e causa: {}", tipoReclamacao, causaRaiz);

        String chave = tipoReclamacao + "|" + causaRaiz;
        return solucoesCache.getOrDefault(chave, new ArrayList<>());
    }

    /**
     * Atualiza status da reclamação
     */
    public void atualizarStatus(String protocolo, String novoStatus) {
        ReclamacaoDTO reclamacao = reclamacoesCache.get(protocolo);
        if (reclamacao != null) {
            reclamacao.setStatus(novoStatus);
            log.info("Status da reclamação {} atualizado para: {}", protocolo, novoStatus);
        } else {
            log.warn("Reclamação não encontrada para protocolo: {}", protocolo);
        }
    }

    /**
     * Busca reclamação por protocolo
     */
    public ReclamacaoDTO buscarPorProtocolo(String protocolo) {
        ReclamacaoDTO reclamacao = reclamacoesCache.get(protocolo);
        if (reclamacao == null) {
            log.warn("Reclamação não encontrada: {}", protocolo);
            // Em produção, buscaria do banco de dados
            reclamacao = ReclamacaoDTO.builder()
                .protocolo(protocolo)
                .dataAbertura(LocalDateTime.now().minusHours(2))
                .slaHoras(24)
                .build();
        }
        return reclamacao;
    }

    /**
     * Registra compensação aplicada
     */
    public void registrarCompensacao(String protocolo, CompensacaoDTO compensacao) {
        compensacoesCache.put(protocolo, compensacao);

        // Atualizar reclamação
        ReclamacaoDTO reclamacao = reclamacoesCache.get(protocolo);
        if (reclamacao != null) {
            reclamacao.setCompensacaoAplicada(true);
            reclamacao.setTipoCompensacao(compensacao.getTipo());
            reclamacao.setCodigoCompensacao(compensacao.getCodigo());
        }

        log.info("Compensação registrada para protocolo {}: {} - {}",
                 protocolo, compensacao.getTipo(), compensacao.getCodigo());
    }

    /**
     * Registra resolução da reclamação
     */
    public void registrarResolucao(ResolucaoDTO resolucao) {
        ReclamacaoDTO reclamacao = reclamacoesCache.get(resolucao.getProtocolo());
        if (reclamacao != null) {
            reclamacao.setStatusFinal(resolucao.getStatusFinal());
            reclamacao.setDescricaoResolucao(resolucao.getDescricao());
            reclamacao.setDataEncerramento(resolucao.getDataEncerramento());
            reclamacao.setTempoResolucaoHoras(resolucao.getTempoResolucaoHoras());
            reclamacao.setDentroDosla(resolucao.getDentroDosla());
            reclamacao.setResolvidoPor(resolucao.getResolvidoPor());
            reclamacao.setStatus("ENCERRADA");

            log.info("Resolução registrada para protocolo {}: {}",
                     resolucao.getProtocolo(), resolucao.getStatusFinal());
        }
    }

    /**
     * Salva reclamação (simula persistência)
     */
    public void salvar(ReclamacaoDTO reclamacao) {
        reclamacoesCache.put(reclamacao.getProtocolo(), reclamacao);
        log.debug("Reclamação salva: {}", reclamacao.getProtocolo());
    }

    /**
     * DTO interno para Resolução
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ResolucaoDTO {
        private String protocolo;
        private String statusFinal;
        private String descricao;
        private LocalDateTime dataEncerramento;
        private Long tempoResolucaoHoras;
        private Boolean dentroDosla;
        private String resolvidoPor;
    }
}
