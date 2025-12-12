package com.experiencia.services.domain.followup;

import com.experiencia.models.Cliente;
import com.experiencia.models.Interacao;
import com.experiencia.models.RespostaNps;
import com.experiencia.repositories.ClienteRepository;
import com.experiencia.repositories.InteracaoRepository;
import com.experiencia.repositories.RespostaNpsRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Delegate: Processar Resposta NPS
 *
 * Responsabilidades:
 * - Validar resposta recebida
 * - Classificar score (Detrator/Neutro/Promotor)
 * - Atualizar perfil do cliente
 * - Calcular NPS agregado
 * - Identificar tendências
 */
@Component("processarRespostaNpsDelegate")
public class ProcessarRespostaNpsDelegate implements JavaDelegate {

    @Autowired
    private RespostaNpsRepository respostaNpsRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private InteracaoRepository interacaoRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long respostaId = (Long) execution.getVariable("respostaId");

        // 1. Recuperar resposta
        RespostaNps resposta = respostaNpsRepository.findById(respostaId)
            .orElseThrow(() -> new RuntimeException("Resposta NPS não encontrada: " + respostaId));

        // 2. Validar resposta
        validarResposta(resposta);

        // 3. Classificar score
        String classificacao = classificarScore(resposta.getScore());
        resposta.setClassificacao(classificacao);

        // 4. Processar feedback textual (se houver)
        if (resposta.getFeedback() != null && !resposta.getFeedback().isEmpty()) {
            processarFeedback(resposta);
        }

        // 5. Atualizar cliente
        atualizarPerfilCliente(resposta);

        // 6. Atualizar interação
        atualizarInteracao(resposta);

        // 7. Calcular métricas agregadas
        calcularMetricas(resposta);

        // 8. Salvar resposta processada
        resposta.setProcessado(true);
        resposta.setDataProcessamento(LocalDateTime.now());
        respostaNpsRepository.save(resposta);

        // 9. Setar variáveis de processo
        execution.setVariable("npsScore", resposta.getScore());
        execution.setVariable("npsClassificacao", classificacao);
        execution.setVariable("npsProcessado", true);

        // Determinar se precisa de follow-up especial
        boolean precisaRecuperacao = "DETRATOR".equals(classificacao);
        execution.setVariable("precisaRecuperacao", precisaRecuperacao);
    }

    private void validarResposta(RespostaNps resposta) {
        if (resposta.getScore() < 0 || resposta.getScore() > 10) {
            throw new RuntimeException("Score NPS inválido: " + resposta.getScore());
        }
    }

    private String classificarScore(int score) {
        if (score >= 0 && score <= 6) {
            return "DETRATOR";
        } else if (score >= 7 && score <= 8) {
            return "NEUTRO";
        } else if (score >= 9 && score <= 10) {
            return "PROMOTOR";
        }
        throw new RuntimeException("Score fora do range esperado: " + score);
    }

    private void processarFeedback(RespostaNps resposta) {
        // Extrair palavras-chave
        String feedback = resposta.getFeedback().toLowerCase();

        // Identificar tópicos mencionados
        if (feedback.contains("atendimento")) {
            resposta.addTopico("ATENDIMENTO");
        }
        if (feedback.contains("produto") || feedback.contains("qualidade")) {
            resposta.addTopico("PRODUTO");
        }
        if (feedback.contains("preço") || feedback.contains("caro")) {
            resposta.addTopico("PRECO");
        }
        if (feedback.contains("entrega") || feedback.contains("prazo")) {
            resposta.addTopico("ENTREGA");
        }

        // Preparar para análise de sentimento (próximo delegate)
        resposta.setProntoParaAnalise(true);
    }

    private void atualizarPerfilCliente(RespostaNps resposta) {
        Cliente cliente = clienteRepository.findById(resposta.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Atualizar histórico NPS
        cliente.setUltimoNps(resposta.getScore());
        cliente.setUltimaClassificacaoNps(resposta.getClassificacao());
        cliente.setDataUltimoNps(resposta.getDataResposta());

        // Calcular NPS médio histórico
        Double npsMedia = respostaNpsRepository.calcularMediaPorCliente(cliente.getId());
        cliente.setNpsMedio(npsMedia);

        // Atualizar tendência
        calcularTendenciaNps(cliente);

        clienteRepository.save(cliente);
    }

    private void calcularTendenciaNps(Cliente cliente) {
        // Buscar últimas 3 respostas
        var ultimasRespostas = respostaNpsRepository
            .findTop3ByClienteIdOrderByDataRespostaDesc(cliente.getId());

        if (ultimasRespostas.size() >= 2) {
            int scoreMaisRecente = ultimasRespostas.get(0).getScore();
            int scoreAnterior = ultimasRespostas.get(1).getScore();

            if (scoreMaisRecente > scoreAnterior) {
                cliente.setTendenciaNps("MELHORANDO");
            } else if (scoreMaisRecente < scoreAnterior) {
                cliente.setTendenciaNps("PIORANDO");
            } else {
                cliente.setTendenciaNps("ESTAVEL");
            }
        }
    }

    private void atualizarInteracao(RespostaNps resposta) {
        Interacao interacao = interacaoRepository.findById(resposta.getInteracaoId())
            .orElseThrow(() -> new RuntimeException("Interação não encontrada"));

        interacao.setNpsRespondido(true);
        interacao.setNpsScore(resposta.getScore());
        interacao.setNpsClassificacao(resposta.getClassificacao());
        interacao.setDataRespostaNps(resposta.getDataResposta());

        interacaoRepository.save(interacao);
    }

    private void calcularMetricas(RespostaNps resposta) {
        // Calcular NPS geral do período (último mês)
        LocalDateTime mesAtras = LocalDateTime.now().minusMonths(1);

        long totalRespostas = respostaNpsRepository.countByDataRespostaAfter(mesAtras);
        long promotores = respostaNpsRepository.countByClassificacaoAndDataRespostaAfter("PROMOTOR", mesAtras);
        long detratores = respostaNpsRepository.countByClassificacaoAndDataRespostaAfter("DETRATOR", mesAtras);

        if (totalRespostas > 0) {
            double percentualPromotores = (promotores * 100.0) / totalRespostas;
            double percentualDetratores = (detratores * 100.0) / totalRespostas;
            double npsGeral = percentualPromotores - percentualDetratores;

            // Armazenar métrica (simplificado - na prática, usar tabela de métricas)
            System.out.println("NPS Geral (último mês): " + npsGeral);
        }
    }
}
