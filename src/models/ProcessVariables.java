package br.com.austa.models;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Modelos de dados para variáveis de processo BPMN
 *
 * Estas classes representam os dados que transitam entre os
 * processos e subprocessos do Camunda
 */
public class ProcessVariables {

    /**
     * Perfil de Risco do Beneficiário
     */
    public static class PerfilRisco implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer scoreRisco;
        private String classificacaoRisco; // BAIXO, MODERADO, ALTO, COMPLEXO
        private Integer scoreComportamental;
        private Integer scorePredicaoInternacao;
        private List<String> fatoresRisco;
        private String statusCPT; // SEM_CPT, SUSPEITA_BAIXA, SUSPEITA_ALTA, CPT_PROVAVEL
        private List<String> condicoesSuspeitas;
        private Integer nivelConfianca;

        // Getters e Setters
        public Integer getScoreRisco() { return scoreRisco; }
        public void setScoreRisco(Integer scoreRisco) { this.scoreRisco = scoreRisco; }

        public String getClassificacaoRisco() { return classificacaoRisco; }
        public void setClassificacaoRisco(String classificacaoRisco) {
            this.classificacaoRisco = classificacaoRisco;
        }

        public Integer getScoreComportamental() { return scoreComportamental; }
        public void setScoreComportamental(Integer scoreComportamental) {
            this.scoreComportamental = scoreComportamental;
        }

        public Integer getScorePredicaoInternacao() { return scorePredicaoInternacao; }
        public void setScorePredicaoInternacao(Integer scorePredicaoInternacao) {
            this.scorePredicaoInternacao = scorePredicaoInternacao;
        }

        public List<String> getFatoresRisco() { return fatoresRisco; }
        public void setFatoresRisco(List<String> fatoresRisco) { this.fatoresRisco = fatoresRisco; }

        public String getStatusCPT() { return statusCPT; }
        public void setStatusCPT(String statusCPT) { this.statusCPT = statusCPT; }

        public List<String> getCondicoesSuspeitas() { return condicoesSuspeitas; }
        public void setCondicoesSuspeitas(List<String> condicoesSuspeitas) {
            this.condicoesSuspeitas = condicoesSuspeitas;
        }

        public Integer getNivelConfianca() { return nivelConfianca; }
        public void setNivelConfianca(Integer nivelConfianca) { this.nivelConfianca = nivelConfianca; }
    }

    /**
     * Dados da Interação Omnichannel
     */
    public static class DadosInteracao implements Serializable {
        private static final long serialVersionUID = 1L;

        private String canal; // WHATSAPP, APP, PORTAL, TELEFONE, EMAIL
        private String intencaoDetectada;
        private String nivelUrgencia; // BAIXA, MEDIA, ALTA, CRITICA
        private Integer slaResposta; // em minutos
        private String complexidadeInteracao; // BAIXA, MEDIA, ALTA
        private Boolean sentimentoNegativo;
        private String camadaDestino; // SELF_SERVICE, AGENTE_IA, NAVEGACAO, AUTORIZACAO
        private String tipoSubprocesso; // SUB-004, SUB-005, SUB-006, SUB-007
        private List<String> palavrasChaveUrgencia;
        private Map<String, Object> contextoEnriquecido;

        // Getters e Setters
        public String getCanal() { return canal; }
        public void setCanal(String canal) { this.canal = canal; }

        public String getIntencaoDetectada() { return intencaoDetectada; }
        public void setIntencaoDetectada(String intencaoDetectada) {
            this.intencaoDetectada = intencaoDetectada;
        }

        public String getNivelUrgencia() { return nivelUrgencia; }
        public void setNivelUrgencia(String nivelUrgencia) { this.nivelUrgencia = nivelUrgencia; }

        public Integer getSlaResposta() { return slaResposta; }
        public void setSlaResposta(Integer slaResposta) { this.slaResposta = slaResposta; }

        public String getComplexidadeInteracao() { return complexidadeInteracao; }
        public void setComplexidadeInteracao(String complexidadeInteracao) {
            this.complexidadeInteracao = complexidadeInteracao;
        }

        public Boolean getSentimentoNegativo() { return sentimentoNegativo; }
        public void setSentimentoNegativo(Boolean sentimentoNegativo) {
            this.sentimentoNegativo = sentimentoNegativo;
        }

        public String getCamadaDestino() { return camadaDestino; }
        public void setCamadaDestino(String camadaDestino) { this.camadaDestino = camadaDestino; }

        public String getTipoSubprocesso() { return tipoSubprocesso; }
        public void setTipoSubprocesso(String tipoSubprocesso) {
            this.tipoSubprocesso = tipoSubprocesso;
        }

        public List<String> getPalavrasChaveUrgencia() { return palavrasChaveUrgencia; }
        public void setPalavrasChaveUrgencia(List<String> palavrasChaveUrgencia) {
            this.palavrasChaveUrgencia = palavrasChaveUrgencia;
        }

        public Map<String, Object> getContextoEnriquecido() { return contextoEnriquecido; }
        public void setContextoEnriquecido(Map<String, Object> contextoEnriquecido) {
            this.contextoEnriquecido = contextoEnriquecido;
        }
    }

    /**
     * Dados de Autorização
     */
    public static class DadosAutorizacao implements Serializable {
        private static final long serialVersionUID = 1L;

        private String numeroAutorizacao;
        private String tipoProcedimento;
        private Double valorProcedimento;
        private Integer diasDesdeAdesao;
        private Boolean atendeProtocolo;
        private Boolean prestadorRede;
        private String decisaoAutorizacao; // APROVADO, NEGADO, PENDENTE
        private String motivoDecisao;
        private Boolean requerAnaliseTecnica;
        private String dataAutorizacao;
        private String validadeAutorizacao;

        // Getters e Setters
        public String getNumeroAutorizacao() { return numeroAutorizacao; }
        public void setNumeroAutorizacao(String numeroAutorizacao) {
            this.numeroAutorizacao = numeroAutorizacao;
        }

        public String getTipoProcedimento() { return tipoProcedimento; }
        public void setTipoProcedimento(String tipoProcedimento) {
            this.tipoProcedimento = tipoProcedimento;
        }

        public Double getValorProcedimento() { return valorProcedimento; }
        public void setValorProcedimento(Double valorProcedimento) {
            this.valorProcedimento = valorProcedimento;
        }

        public Integer getDiasDesdeAdesao() { return diasDesdeAdesao; }
        public void setDiasDesdeAdesao(Integer diasDesdeAdesao) {
            this.diasDesdeAdesao = diasDesdeAdesao;
        }

        public Boolean getAtendeProtocolo() { return atendeProtocolo; }
        public void setAtendeProtocolo(Boolean atendeProtocolo) {
            this.atendeProtocolo = atendeProtocolo;
        }

        public Boolean getPrestadorRede() { return prestadorRede; }
        public void setPrestadorRede(Boolean prestadorRede) { this.prestadorRede = prestadorRede; }

        public String getDecisaoAutorizacao() { return decisaoAutorizacao; }
        public void setDecisaoAutorizacao(String decisaoAutorizacao) {
            this.decisaoAutorizacao = decisaoAutorizacao;
        }

        public String getMotivoDecisao() { return motivoDecisao; }
        public void setMotivoDecisao(String motivoDecisao) { this.motivoDecisao = motivoDecisao; }

        public Boolean getRequerAnaliseTecnica() { return requerAnaliseTecnica; }
        public void setRequerAnaliseTecnica(Boolean requerAnaliseTecnica) {
            this.requerAnaliseTecnica = requerAnaliseTecnica;
        }

        public String getDataAutorizacao() { return dataAutorizacao; }
        public void setDataAutorizacao(String dataAutorizacao) {
            this.dataAutorizacao = dataAutorizacao;
        }

        public String getValidadeAutorizacao() { return validadeAutorizacao; }
        public void setValidadeAutorizacao(String validadeAutorizacao) {
            this.validadeAutorizacao = validadeAutorizacao;
        }
    }

    /**
     * Gatilho Proativo
     */
    public static class GatilhoProativo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String gatilhoId; // GAT-001, GAT-002, etc.
        private String prioridade; // BAIXA, MEDIA, ALTA, CRITICA
        private String acaoSugerida;
        private Map<String, Object> parametros;
        private Boolean executado;
        private String dataExecucao;

        // Getters e Setters
        public String getGatilhoId() { return gatilhoId; }
        public void setGatilhoId(String gatilhoId) { this.gatilhoId = gatilhoId; }

        public String getPrioridade() { return prioridade; }
        public void setPrioridade(String prioridade) { this.prioridade = prioridade; }

        public String getAcaoSugerida() { return acaoSugerida; }
        public void setAcaoSugerida(String acaoSugerida) { this.acaoSugerida = acaoSugerida; }

        public Map<String, Object> getParametros() { return parametros; }
        public void setParametros(Map<String, Object> parametros) { this.parametros = parametros; }

        public Boolean getExecutado() { return executado; }
        public void setExecutado(Boolean executado) { this.executado = executado; }

        public String getDataExecucao() { return dataExecucao; }
        public void setDataExecucao(String dataExecucao) { this.dataExecucao = dataExecucao; }
    }

    /**
     * Métricas de Jornada
     */
    public static class MetricasJornada implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long tempoTotal; // em segundos
        private Integer totalTouchpoints;
        private List<String> canaisUtilizados;
        private String desfecho;
        private Integer npsScore;
        private Integer cesScore;
        private Double custoTotal;
        private List<String> estadosTransitados;
        private List<String> subprocessosExecutados;

        // Getters e Setters
        public Long getTempoTotal() { return tempoTotal; }
        public void setTempoTotal(Long tempoTotal) { this.tempoTotal = tempoTotal; }

        public Integer getTotalTouchpoints() { return totalTouchpoints; }
        public void setTotalTouchpoints(Integer totalTouchpoints) {
            this.totalTouchpoints = totalTouchpoints;
        }

        public List<String> getCanaisUtilizados() { return canaisUtilizados; }
        public void setCanaisUtilizados(List<String> canaisUtilizados) {
            this.canaisUtilizados = canaisUtilizados;
        }

        public String getDesfecho() { return desfecho; }
        public void setDesfecho(String desfecho) { this.desfecho = desfecho; }

        public Integer getNpsScore() { return npsScore; }
        public void setNpsScore(Integer npsScore) { this.npsScore = npsScore; }

        public Integer getCesScore() { return cesScore; }
        public void setCesScore(Integer cesScore) { this.cesScore = cesScore; }

        public Double getCustoTotal() { return custoTotal; }
        public void setCustoTotal(Double custoTotal) { this.custoTotal = custoTotal; }

        public List<String> getEstadosTransitados() { return estadosTransitados; }
        public void setEstadosTransitados(List<String> estadosTransitados) {
            this.estadosTransitados = estadosTransitados;
        }

        public List<String> getSubprocessosExecutados() { return subprocessosExecutados; }
        public void setSubprocessosExecutados(List<String> subprocessosExecutados) {
            this.subprocessosExecutados = subprocessosExecutados;
        }
    }
}
