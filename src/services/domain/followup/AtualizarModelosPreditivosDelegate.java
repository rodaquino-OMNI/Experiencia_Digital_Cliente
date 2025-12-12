package com.experiencia.services.domain.followup;

import com.experiencia.clients.MlClient;
import com.experiencia.models.Cliente;
import com.experiencia.models.RespostaNps;
import com.experiencia.models.DadosTreinamento;
import com.experiencia.repositories.ClienteRepository;
import com.experiencia.repositories.RespostaNpsRepository;
import com.experiencia.repositories.DadosTreinamentoRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate: Atualizar Modelos Preditivos
 *
 * Responsabilidades:
 * - Extrair features dos dados do cliente e interação
 * - Alimentar modelos de ML com novos dados
 * - Recalcular scores preditivos:
 *   * Churn Score (probabilidade de cancelamento)
 *   * Health Score (saúde da conta)
 *   * NPS Previsto (tendência futura)
 *   * LTV (valor vitalício)
 * - Identificar padrões e anomalias
 * - Atualizar segmentação dinâmica
 */
@Component("atualizarModelosPreditivosDelegate")
public class AtualizarModelosPreditivosDelegate implements JavaDelegate {

    @Autowired
    private RespostaNpsRepository respostaNpsRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DadosTreinamentoRepository dadosTreinamentoRepository;

    @Autowired
    private MlClient mlClient;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long clienteId = (Long) execution.getVariable("clienteId");
        Long respostaId = (Long) execution.getVariable("respostaId");

        // 1. Recuperar dados
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clienteId));

        RespostaNps resposta = respostaNpsRepository.findById(respostaId)
            .orElseThrow(() -> new RuntimeException("Resposta NPS não encontrada: " + respostaId));

        // 2. Extrair features para ML
        Map<String, Object> features = extrairFeatures(cliente, resposta);

        // 3. Criar registro de treinamento
        DadosTreinamento dadosTreino = criarDadosTreinamento(cliente, resposta, features);
        dadosTreinamentoRepository.save(dadosTreino);

        // 4. Atualizar modelo de Churn
        Double churnScore = atualizarModeloChurn(cliente, features);
        cliente.setChurnScore(churnScore);

        // 5. Atualizar Health Score
        Double healthScore = calcularHealthScore(cliente, resposta);
        cliente.setHealthScore(healthScore);

        // 6. Prever próximo NPS
        Integer npsPrevistoProximo = preverProximoNps(cliente, features);
        cliente.setNpsPrevistoProximo(npsPrevistoProximo);

        // 7. Recalcular LTV
        Double ltv = recalcularLtv(cliente);
        cliente.setLtv(ltv);

        // 8. Atualizar segmentação dinâmica
        String novoSegmento = atualizarSegmentacao(cliente);
        if (!novoSegmento.equals(cliente.getSegmento())) {
            cliente.setSegmentoAnterior(cliente.getSegmento());
            cliente.setSegmento(novoSegmento);
            cliente.setDataMudancaSegmento(LocalDateTime.now());
        }

        // 9. Detectar anomalias
        boolean anomaliaDetectada = detectarAnomalias(cliente, resposta, features);

        // 10. Salvar cliente atualizado
        clienteRepository.save(cliente);

        // 11. Setar variáveis de processo
        execution.setVariable("churnScore", churnScore);
        execution.setVariable("healthScore", healthScore);
        execution.setVariable("npsPrevistoProximo", npsPrevistoProximo);
        execution.setVariable("anomaliaDetectada", anomaliaDetectada);
        execution.setVariable("modelosAtualizados", true);
    }

    private Map<String, Object> extrairFeatures(Cliente cliente, RespostaNps resposta) {
        Map<String, Object> features = new HashMap<>();

        // Features demográficas
        features.put("idade", cliente.getIdade());
        features.put("segmento", cliente.getSegmento());
        features.put("tempoCliente", cliente.getTempoClienteDias());

        // Features de comportamento
        features.put("npsAtual", resposta.getScore());
        features.put("npsMedio", cliente.getNpsMedio());
        features.put("tendenciaNps", cliente.getTendenciaNps());
        features.put("totalInteracoes", cliente.getTotalInteracoes());
        features.put("interacoesMes", cliente.getInteracoesUltimoMes());

        // Features financeiras
        features.put("ltv", cliente.getLtv());
        features.put("valorMedioCompra", cliente.getValorMedioCompra());
        features.put("frequenciaCompra", cliente.getFrequenciaCompraDias());
        features.put("diasDesdeUltimaCompra", cliente.getDiasDesdeUltimaCompra());

        // Features de engajamento
        features.put("taxaAberturasEmail", cliente.getTaxaAberturasEmail());
        features.put("taxaCliquesEmail", cliente.getTaxaCliquesEmail());
        features.put("acessosApp", cliente.getAcessosAppMes());

        // Features de suporte
        features.put("ticketsAbertos", cliente.getTicketsAbertos());
        features.put("ticketsResolvidosMes", cliente.getTicketsResolvidosMes());
        features.put("tempoMedioResolucao", cliente.getTempoMedioResolucaoTickets());

        // Features de sentimento
        if (resposta.getSentimento() != null) {
            features.put("sentimento", resposta.getSentimento());
            features.put("topicos", resposta.getTopicos());
        }

        return features;
    }

    private DadosTreinamento criarDadosTreinamento(Cliente cliente, RespostaNps resposta,
                                                    Map<String, Object> features) {
        DadosTreinamento dados = new DadosTreinamento();
        dados.setClienteId(cliente.getId());
        dados.setRespostaNpsId(resposta.getId());
        dados.setFeatures(features);
        dados.setTarget(resposta.getScore());
        dados.setDataColeta(LocalDateTime.now());
        dados.setUsadoTreinamento(false);

        return dados;
    }

    private Double atualizarModeloChurn(Cliente cliente, Map<String, Object> features) {
        try {
            // Chamar serviço de ML para prever churn
            Map<String, Object> predicao = mlClient.preverChurn(features);

            Double probabilidadeChurn = (Double) predicao.get("probabilidade");

            // Classificar risco
            String riscoChurn;
            if (probabilidadeChurn >= 0.7) {
                riscoChurn = "ALTO";
            } else if (probabilidadeChurn >= 0.4) {
                riscoChurn = "MEDIO";
            } else {
                riscoChurn = "BAIXO";
            }

            cliente.setRiscoChurn(riscoChurn);

            return probabilidadeChurn;

        } catch (Exception e) {
            // Fallback: calcular score simplificado
            return calcularChurnScoreSimplificado(cliente);
        }
    }

    private Double calcularChurnScoreSimplificado(Cliente cliente) {
        // Modelo simplificado baseado em regras
        double score = 0.0;

        // Fatores de risco
        if (cliente.getNpsMedio() != null && cliente.getNpsMedio() <= 6) {
            score += 0.3;
        }
        if ("PIORANDO".equals(cliente.getTendenciaNps())) {
            score += 0.2;
        }
        if (cliente.getDiasDesdeUltimaCompra() > 90) {
            score += 0.2;
        }
        if (cliente.getTicketsAbertos() > 2) {
            score += 0.15;
        }
        if (cliente.getTaxaAberturasEmail() < 0.1) {
            score += 0.15;
        }

        return Math.min(score, 1.0);
    }

    private Double calcularHealthScore(Cliente cliente, RespostaNps resposta) {
        // Health Score combina múltiplos fatores (0-100)
        double score = 100.0;

        // NPS (peso 30%)
        double fatorNps = (resposta.getScore() / 10.0) * 30;

        // Engajamento (peso 25%)
        double fatorEngajamento = calcularFatorEngajamento(cliente) * 25;

        // Adoção de produto (peso 20%)
        double fatorAdocao = calcularFatorAdocao(cliente) * 20;

        // Suporte (peso 15%)
        double fatorSuporte = calcularFatorSuporte(cliente) * 15;

        // Financeiro (peso 10%)
        double fatorFinanceiro = calcularFatorFinanceiro(cliente) * 10;

        score = fatorNps + fatorEngajamento + fatorAdocao + fatorSuporte + fatorFinanceiro;

        return Math.max(0, Math.min(100, score));
    }

    private double calcularFatorEngajamento(Cliente cliente) {
        // 0-1 baseado em engajamento
        double taxaAberturas = cliente.getTaxaAberturasEmail() != null ? cliente.getTaxaAberturasEmail() : 0;
        double acessosApp = cliente.getAcessosAppMes() != null ? Math.min(cliente.getAcessosAppMes() / 30.0, 1.0) : 0;

        return (taxaAberturas + acessosApp) / 2.0;
    }

    private double calcularFatorAdocao(Cliente cliente) {
        // Simplificado - na prática, medir features utilizadas
        return 0.7; // placeholder
    }

    private double calcularFatorSuporte(Cliente cliente) {
        // Menos tickets = melhor
        int tickets = cliente.getTicketsAbertos() != null ? cliente.getTicketsAbertos() : 0;
        return Math.max(0, 1.0 - (tickets * 0.2));
    }

    private double calcularFatorFinanceiro(Cliente cliente) {
        // Baseado em recência de compra
        int dias = cliente.getDiasDesdeUltimaCompra() != null ? cliente.getDiasDesdeUltimaCompra() : 999;
        if (dias <= 30) return 1.0;
        if (dias <= 60) return 0.7;
        if (dias <= 90) return 0.4;
        return 0.1;
    }

    private Integer preverProximoNps(Cliente cliente, Map<String, Object> features) {
        try {
            Map<String, Object> predicao = mlClient.preverProximoNps(features);
            return (Integer) predicao.get("nps_previsto");
        } catch (Exception e) {
            // Fallback: usar tendência
            if ("MELHORANDO".equals(cliente.getTendenciaNps())) {
                return Math.min(10, cliente.getUltimoNps() + 1);
            } else if ("PIORANDO".equals(cliente.getTendenciaNps())) {
                return Math.max(0, cliente.getUltimoNps() - 1);
            }
            return cliente.getUltimoNps();
        }
    }

    private Double recalcularLtv(Cliente cliente) {
        // LTV = Valor Médio Compra × Frequência Anual × Tempo Esperado Como Cliente

        Double valorMedio = cliente.getValorMedioCompra() != null ? cliente.getValorMedioCompra() : 0.0;
        Integer frequenciaDias = cliente.getFrequenciaCompraDias() != null ? cliente.getFrequenciaCompraDias() : 90;

        double comprasAnuais = 365.0 / frequenciaDias;
        double tempoEsperadoAnos = calcularTempoEsperadoCliente(cliente);

        return valorMedio * comprasAnuais * tempoEsperadoAnos;
    }

    private double calcularTempoEsperadoCliente(Cliente cliente) {
        // Baseado em churn score
        Double churnScore = cliente.getChurnScore() != null ? cliente.getChurnScore() : 0.5;

        // Quanto menor o churn, maior o tempo esperado
        double taxaRetencao = 1 - churnScore;

        // Fórmula simplificada: 1 / taxa_churn_anual
        return 1 / Math.max(0.1, 1 - taxaRetencao);
    }

    private String atualizarSegmentacao(Cliente cliente) {
        // Segmentação dinâmica baseada em comportamento e valor

        Double healthScore = cliente.getHealthScore();
        Double ltv = cliente.getLtv();

        if (ltv > 10000 && healthScore > 80) {
            return "PREMIUM";
        } else if (ltv > 5000 && healthScore > 60) {
            return "GOLD";
        } else if (healthScore < 40 || cliente.getChurnScore() > 0.7) {
            return "EM_RISCO";
        } else {
            return "STANDARD";
        }
    }

    private boolean detectarAnomalias(Cliente cliente, RespostaNps resposta, Map<String, Object> features) {
        // Detectar comportamentos anômalos

        boolean anomalia = false;

        // 1. Queda brusca de NPS
        if (cliente.getNpsMedio() != null && cliente.getNpsMedio() >= 8 && resposta.getScore() <= 3) {
            anomalia = true;
        }

        // 2. Aumento súbito de tickets
        if (cliente.getTicketsAbertos() > 5) {
            anomalia = true;
        }

        // 3. Parada de compras (cliente ativo que parou)
        if (cliente.getFrequenciaCompraDias() < 30 && cliente.getDiasDesdeUltimaCompra() > 90) {
            anomalia = true;
        }

        if (anomalia) {
            // Registrar anomalia para análise
            System.out.println("ANOMALIA DETECTADA: Cliente " + cliente.getId());
        }

        return anomalia;
    }
}
