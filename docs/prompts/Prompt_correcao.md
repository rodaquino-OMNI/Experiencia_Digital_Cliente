# PROMPT TÉCNICO: Completar 100% do Projeto Experiência Digital do Cliente

## Análise de Estado Baseada no GAP Analysis Report

### Status Atual Corrigido

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ESTADO REAL DO PROJETO                               │
├─────────────────────────────────────────────────────────────────────────┤
│ Fase 1 - DMN Files:           [████████████████] 118% ✅ COMPLETO       │
│ Fase 2 - Delegate Beans:      [██████████████░░] 83%  ⚠️ QUASE COMPLETO │
│ Fase 3 - Integration Tests:   [████░░░░░░░░░░░░] 39%  ❌ GAP CRÍTICO    │
├─────────────────────────────────────────────────────────────────────────┤
│ PROJETO TOTAL:                [██████████████░░] 89%                    │
│ PARA 100%:                    12 delegates + 22 test files              │
└─────────────────────────────────────────────────────────────────────────┘
```

### Inventário de Componentes

| Categoria | Encontrados | Requeridos | Status |
|-----------|-------------|------------|--------|
| DMN Files | 13 | 11 | ✅ 118% |
| Delegate Java Files | 78+ | 71 | ✅ 110% |
| JavaDelegate Implementations | 83 | 71 | ✅ 117% |
| Integration Test Files | 14 | 36 | ⚠️ 39% |
| **Delegates Faltantes** | **12** | - | ❌ |
| **Test Files Faltantes** | **22** | - | ❌ |

---

## OBJETIVO

Completar **100%** do projeto implementando:

1. **12 Delegates Faltantes** (SUB-009, SUB-010, Common)
2. **22 Test Files Faltantes** (Workflow, DMN, E2E)
3. **Validação e Integração Final**

**Caminho base do projeto:**
```
/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/
```

---

## PARTE 1: DELEGATES FALTANTES (12 Delegates)

### Estrutura de Localização CORRETA

⚠️ **IMPORTANTE:** O projeto NÃO segue a estrutura originalmente planejada. Os delegates estão em:

```
src/services/domain/
├── onboarding/          ✅ 100% completo
├── proativo/            ✅ 100% completo (incluindo /impl/)
├── recepcao/            ✅ 100% completo
├── selfservice/         ✅ 100% completo
├── agenteia/            ✅ 100% completo
├── autorizacao/         ✅ 100% completo (incluindo /impl/)
├── navegacao/           ✅ 100% completo (incluindo /impl/)
├── cronicos/            ✅ 100% completo (incluindo /impl/)
├── reclamacoes/         ❌ 0% - CRIAR PASTA E 7 DELEGATES
├── followup/            ❌ 0% - CRIAR PASTA E 5 DELEGATES
└── common/              ⚠️ 67% - FALTA 1 DELEGATE
```

---

### SUB-009: Gestão de Reclamações (7 Delegates) - ⚡ CRÍTICO REGULATÓRIO

**Criar pasta:** `src/services/domain/reclamacoes/`

#### 1. RegistrarReclamacaoDelegate.java

```java
package br.com.austa.experiencia.services.domain.reclamacoes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Registra reclamação no sistema CRM e inicia workflow de tratamento.
 * 
 * Referenciado em: SUB-009_Gestao_Reclamacoes.bpmn
 * Activity ID: Activity_RegistrarReclamacao
 * 
 * Variáveis de entrada:
 * - beneficiarioId (String): ID do beneficiário
 * - canalOrigem (String): Canal de origem (WHATSAPP, APP, TELEFONE, ANS, PROCON)
 * - tipoReclamacao (String): Tipo da reclamação
 * - descricao (String): Descrição detalhada
 * - anexos (List<String>): URLs dos anexos
 * 
 * Variáveis de saída:
 * - protocoloReclamacao (String): Número do protocolo gerado
 * - criticidade (String): BAIXA, MEDIA, ALTA, CRITICA
 * - slaHoras (Integer): SLA em horas para resolução
 * - responsavel (String): Área responsável
 */
@Slf4j
@Component("registrarReclamacaoDelegate")
@RequiredArgsConstructor
public class RegistrarReclamacaoDelegate implements JavaDelegate {

    private final ReclamacaoService reclamacaoService;
    private final CrmService crmService;
    private final KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Registrando reclamação - ProcessInstance: {}", 
                 execution.getProcessInstanceId());
        
        try {
            // 1. Extrair variáveis
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String canalOrigem = (String) execution.getVariable("canalOrigem");
            String tipoReclamacao = (String) execution.getVariable("tipoReclamacao");
            String descricao = (String) execution.getVariable("descricao");
            
            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, canalOrigem, tipoReclamacao, descricao);
            
            // 3. Gerar protocolo
            String protocolo = reclamacaoService.gerarProtocolo(canalOrigem);
            
            // 4. Classificar criticidade via DMN (já executado antes)
            String criticidade = (String) execution.getVariable("criticidade");
            Integer slaHoras = (Integer) execution.getVariable("slaHoras");
            String responsavel = (String) execution.getVariable("responsavel");
            
            // 5. Registrar no CRM
            ReclamacaoDTO reclamacao = ReclamacaoDTO.builder()
                .protocolo(protocolo)
                .beneficiarioId(beneficiarioId)
                .canalOrigem(canalOrigem)
                .tipo(tipoReclamacao)
                .descricao(descricao)
                .criticidade(criticidade)
                .slaHoras(slaHoras)
                .responsavel(responsavel)
                .status("ABERTA")
                .dataAbertura(LocalDateTime.now())
                .build();
            
            crmService.registrarReclamacao(reclamacao);
            
            // 6. Publicar evento Kafka
            kafkaPublisher.publish("reclamacao.registrada", reclamacao);
            
            // 7. Definir variáveis de saída
            execution.setVariable("protocoloReclamacao", protocolo);
            execution.setVariable("reclamacaoRegistrada", true);
            execution.setVariable("dataLimiteResolucao", 
                LocalDateTime.now().plusHours(slaHoras));
            
            log.info("Reclamação registrada - Protocolo: {}, Criticidade: {}", 
                     protocolo, criticidade);
            
        } catch (Exception e) {
            log.error("Erro ao registrar reclamação: {}", e.getMessage(), e);
            execution.setVariable("reclamacaoRegistrada", false);
            execution.setVariable("erroRegistro", e.getMessage());
            throw e;
        }
    }
    
    private void validateInputs(String beneficiarioId, String canalOrigem, 
                                String tipoReclamacao, String descricao) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (canalOrigem == null || canalOrigem.isBlank()) {
            throw new IllegalArgumentException("canalOrigem é obrigatório");
        }
        if (tipoReclamacao == null || tipoReclamacao.isBlank()) {
            throw new IllegalArgumentException("tipoReclamacao é obrigatório");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descricao é obrigatória");
        }
    }
}
```

#### 2. AnalisarCausaRaizDelegate.java

```java
/**
 * Analisa a causa raiz da reclamação usando IA/NLP.
 * 
 * Variáveis de entrada:
 * - protocoloReclamacao (String)
 * - tipoReclamacao (String)
 * - descricao (String)
 * - historicoReclamacoes (List<ReclamacaoDTO>)
 * 
 * Variáveis de saída:
 * - causaRaizIdentificada (String): Causa raiz detectada
 * - categoriaCausaRaiz (String): PROCESSO, SISTEMA, HUMANO, EXTERNO
 * - confiancaAnalise (Double): 0.0 a 1.0
 * - recomendacoes (List<String>): Recomendações de ação
 */
@Slf4j
@Component("analisarCausaRaizDelegate")
@RequiredArgsConstructor
public class AnalisarCausaRaizDelegate implements JavaDelegate {

    private final NlpService nlpService;
    private final ReclamacaoService reclamacaoService;
    private final AnalyticsService analyticsService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Analisando causa raiz - Protocolo: {}", 
                 execution.getVariable("protocoloReclamacao"));
        
        String descricao = (String) execution.getVariable("descricao");
        String tipoReclamacao = (String) execution.getVariable("tipoReclamacao");
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        
        // 1. Buscar histórico de reclamações similares
        List<ReclamacaoDTO> historico = reclamacaoService
            .buscarReclamacoesSimilares(tipoReclamacao, 30);
        
        // 2. Analisar padrões com IA
        RootCauseAnalysis analysis = nlpService.analyzeRootCause(
            descricao, 
            tipoReclamacao,
            historico
        );
        
        // 3. Identificar tendências
        TrendAnalysis trends = analyticsService.analyzeTrends(
            tipoReclamacao, 
            LocalDate.now().minusDays(90),
            LocalDate.now()
        );
        
        // 4. Gerar recomendações
        List<String> recomendacoes = generateRecommendations(analysis, trends);
        
        // 5. Definir variáveis de saída
        execution.setVariable("causaRaizIdentificada", analysis.getRootCause());
        execution.setVariable("categoriaCausaRaiz", analysis.getCategory());
        execution.setVariable("confiancaAnalise", analysis.getConfidence());
        execution.setVariable("recomendacoes", recomendacoes);
        execution.setVariable("tendenciaRecorrencia", trends.isRecurring());
        
        log.info("Causa raiz identificada: {} (confiança: {})", 
                 analysis.getRootCause(), analysis.getConfidence());
    }
    
    private List<String> generateRecommendations(RootCauseAnalysis analysis, 
                                                  TrendAnalysis trends) {
        List<String> recommendations = new ArrayList<>();
        
        switch (analysis.getCategory()) {
            case "PROCESSO":
                recommendations.add("Revisar procedimento operacional");
                recommendations.add("Atualizar documentação");
                break;
            case "SISTEMA":
                recommendations.add("Escalar para TI");
                recommendations.add("Verificar integrações");
                break;
            case "HUMANO":
                recommendations.add("Capacitar equipe");
                recommendations.add("Revisar scripts de atendimento");
                break;
            case "EXTERNO":
                recommendations.add("Comunicar parceiro/prestador");
                recommendations.add("Avaliar penalidades contratuais");
                break;
        }
        
        if (trends.isRecurring()) {
            recommendations.add("ALERTA: Problema recorrente - escalar para gestão");
        }
        
        return recommendations;
    }
}
```

#### 3. BuscarSolucoesAnterioresDelegate.java

```java
/**
 * Busca soluções aplicadas em reclamações similares anteriores.
 * 
 * Variáveis de entrada:
 * - tipoReclamacao (String)
 * - causaRaizIdentificada (String)
 * - categoriaCausaRaiz (String)
 * 
 * Variáveis de saída:
 * - solucoesAnteriores (List<SolucaoDTO>)
 * - solucaoRecomendada (SolucaoDTO)
 * - taxaSucessoSolucao (Double)
 */
@Slf4j
@Component("buscarSolucoesAnterioresDelegate")
@RequiredArgsConstructor
public class BuscarSolucoesAnterioresDelegate implements JavaDelegate {

    private final ReclamacaoService reclamacaoService;
    private final KnowledgeBaseService knowledgeBase;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String tipoReclamacao = (String) execution.getVariable("tipoReclamacao");
        String causaRaiz = (String) execution.getVariable("causaRaizIdentificada");
        
        // 1. Buscar soluções no histórico
        List<SolucaoDTO> solucoesHistorico = reclamacaoService
            .buscarSolucoesPorTipoECausa(tipoReclamacao, causaRaiz);
        
        // 2. Buscar na base de conhecimento
        List<SolucaoDTO> solucoesKB = knowledgeBase
            .searchSolutions(tipoReclamacao, causaRaiz);
        
        // 3. Consolidar e rankear por taxa de sucesso
        List<SolucaoDTO> todasSolucoes = Stream
            .concat(solucoesHistorico.stream(), solucoesKB.stream())
            .distinct()
            .sorted(Comparator.comparing(SolucaoDTO::getTaxaSucesso).reversed())
            .collect(Collectors.toList());
        
        // 4. Selecionar melhor solução
        SolucaoDTO melhorSolucao = todasSolucoes.isEmpty() ? null : todasSolucoes.get(0);
        
        // 5. Definir variáveis
        execution.setVariable("solucoesAnteriores", todasSolucoes);
        execution.setVariable("solucaoRecomendada", melhorSolucao);
        execution.setVariable("taxaSucessoSolucao", 
            melhorSolucao != null ? melhorSolucao.getTaxaSucesso() : 0.0);
        execution.setVariable("existeSolucaoConhecida", !todasSolucoes.isEmpty());
        
        log.info("Encontradas {} soluções anteriores. Melhor taxa de sucesso: {}%", 
                 todasSolucoes.size(), 
                 melhorSolucao != null ? melhorSolucao.getTaxaSucesso() * 100 : 0);
    }
}
```

#### 4. ProporSolucaoDelegate.java

```java
/**
 * Propõe solução ao beneficiário baseado na análise.
 * 
 * Variáveis de entrada:
 * - beneficiarioId (String)
 * - protocoloReclamacao (String)
 * - solucaoRecomendada (SolucaoDTO)
 * - canalOrigem (String)
 * 
 * Variáveis de saída:
 * - solucaoProposta (SolucaoDTO)
 * - mensagemProposta (String)
 * - aguardandoAceite (Boolean)
 */
@Slf4j
@Component("proporSolucaoDelegate")
@RequiredArgsConstructor
public class ProporSolucaoDelegate implements JavaDelegate {

    private final WhatsAppService whatsAppService;
    private final NotificationService notificationService;
    private final ReclamacaoService reclamacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String protocolo = (String) execution.getVariable("protocoloReclamacao");
        String canalOrigem = (String) execution.getVariable("canalOrigem");
        SolucaoDTO solucaoRecomendada = (SolucaoDTO) execution.getVariable("solucaoRecomendada");
        
        // 1. Preparar mensagem de proposta
        String mensagem = buildMensagemProposta(protocolo, solucaoRecomendada);
        
        // 2. Enviar pelo canal de origem
        boolean enviado = false;
        switch (canalOrigem) {
            case "WHATSAPP":
                enviado = whatsAppService.sendMessage(beneficiarioId, mensagem);
                break;
            case "APP":
                enviado = notificationService.sendPush(beneficiarioId, mensagem);
                break;
            case "EMAIL":
                enviado = notificationService.sendEmail(beneficiarioId, 
                    "Solução para sua reclamação - Protocolo " + protocolo, mensagem);
                break;
            default:
                enviado = notificationService.sendSms(beneficiarioId, mensagem);
        }
        
        // 3. Atualizar status da reclamação
        reclamacaoService.atualizarStatus(protocolo, "AGUARDANDO_ACEITE");
        
        // 4. Definir variáveis
        execution.setVariable("solucaoProposta", solucaoRecomendada);
        execution.setVariable("mensagemProposta", mensagem);
        execution.setVariable("aguardandoAceite", true);
        execution.setVariable("propostaEnviada", enviado);
        execution.setVariable("dataPropostaEnviada", LocalDateTime.now());
        
        log.info("Solução proposta enviada - Protocolo: {}, Canal: {}", protocolo, canalOrigem);
    }
    
    private String buildMensagemProposta(String protocolo, SolucaoDTO solucao) {
        return String.format("""
            Olá! Analisamos sua reclamação (Protocolo: %s).
            
            📋 Nossa proposta de solução:
            %s
            
            ✅ Para aceitar esta solução, responda SIM
            ❌ Para recusar e falar com um especialista, responda NAO
            
            Estamos à disposição!
            """, protocolo, solucao.getDescricao());
    }
}
```

#### 5. AplicarCompensacaoDelegate.java

```java
/**
 * Aplica compensação ao beneficiário quando aprovada.
 * 
 * Variáveis de entrada:
 * - beneficiarioId (String)
 * - protocoloReclamacao (String)
 * - tipoCompensacao (String): DESCONTO, CREDITO, SERVICO_EXTRA, REEMBOLSO
 * - valorCompensacao (BigDecimal)
 * - aprovadoPor (String)
 * 
 * Variáveis de saída:
 * - compensacaoAplicada (Boolean)
 * - codigoCompensacao (String)
 * - dataVigencia (LocalDate)
 */
@Slf4j
@Component("aplicarCompensacaoDelegate")
@RequiredArgsConstructor
public class AplicarCompensacaoDelegate implements JavaDelegate {

    private final FinanceiroService financeiroService;
    private final TasyService tasyService;
    private final ReclamacaoService reclamacaoService;
    private final KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String protocolo = (String) execution.getVariable("protocoloReclamacao");
        String tipoCompensacao = (String) execution.getVariable("tipoCompensacao");
        BigDecimal valor = (BigDecimal) execution.getVariable("valorCompensacao");
        String aprovadoPor = (String) execution.getVariable("aprovadoPor");
        
        log.info("Aplicando compensação - Protocolo: {}, Tipo: {}, Valor: {}", 
                 protocolo, tipoCompensacao, valor);
        
        CompensacaoDTO compensacao = CompensacaoDTO.builder()
            .beneficiarioId(beneficiarioId)
            .protocoloReclamacao(protocolo)
            .tipo(tipoCompensacao)
            .valor(valor)
            .aprovadoPor(aprovadoPor)
            .dataAplicacao(LocalDateTime.now())
            .build();
        
        String codigoCompensacao = null;
        LocalDate dataVigencia = null;
        
        switch (tipoCompensacao) {
            case "DESCONTO":
                codigoCompensacao = financeiroService.aplicarDesconto(
                    beneficiarioId, valor, 3); // 3 meses
                dataVigencia = LocalDate.now().plusMonths(3);
                break;
                
            case "CREDITO":
                codigoCompensacao = financeiroService.adicionarCredito(
                    beneficiarioId, valor);
                dataVigencia = LocalDate.now().plusYears(1);
                break;
                
            case "SERVICO_EXTRA":
                codigoCompensacao = tasyService.liberarServicoExtra(
                    beneficiarioId, (String) execution.getVariable("servicoExtra"));
                dataVigencia = LocalDate.now().plusMonths(6);
                break;
                
            case "REEMBOLSO":
                codigoCompensacao = financeiroService.processarReembolso(
                    beneficiarioId, valor, 
                    (String) execution.getVariable("dadosBancarios"));
                dataVigencia = LocalDate.now();
                break;
        }
        
        compensacao.setCodigo(codigoCompensacao);
        compensacao.setDataVigencia(dataVigencia);
        
        // Registrar compensação
        reclamacaoService.registrarCompensacao(protocolo, compensacao);
        
        // Publicar evento
        kafkaPublisher.publish("reclamacao.compensacao.aplicada", compensacao);
        
        // Definir variáveis
        execution.setVariable("compensacaoAplicada", true);
        execution.setVariable("codigoCompensacao", codigoCompensacao);
        execution.setVariable("dataVigencia", dataVigencia);
        
        log.info("Compensação aplicada com sucesso - Código: {}", codigoCompensacao);
    }
}
```

#### 6. EscalarOuvidoriaDelegate.java

```java
/**
 * Escala reclamação para ouvidoria em casos críticos.
 * 
 * Variáveis de entrada:
 * - protocoloReclamacao (String)
 * - motivoEscalacao (String)
 * - criticidade (String)
 * - canalOrigem (String): Se ANS ou PROCON, prioridade máxima
 * 
 * Variáveis de saída:
 * - protocoloOuvidoria (String)
 * - responsavelOuvidoria (String)
 * - prazoResposta (LocalDateTime)
 */
@Slf4j
@Component("escalarOuvidoriaDelegate")
@RequiredArgsConstructor
public class EscalarOuvidoriaDelegate implements JavaDelegate {

    private final OuvidoriaService ouvidoriaService;
    private final NotificationService notificationService;
    private final AnsService ansService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String protocolo = (String) execution.getVariable("protocoloReclamacao");
        String canalOrigem = (String) execution.getVariable("canalOrigem");
        String motivoEscalacao = (String) execution.getVariable("motivoEscalacao");
        
        log.warn("Escalando para ouvidoria - Protocolo: {}, Canal: {}", 
                 protocolo, canalOrigem);
        
        // 1. Determinar prioridade
        PrioridadeOuvidoria prioridade = determinePrioridade(canalOrigem);
        
        // 2. Criar caso na ouvidoria
        CasoOuvidoria caso = ouvidoriaService.criarCaso(
            protocolo, motivoEscalacao, prioridade);
        
        // 3. Se origem é ANS, registrar NIP
        if ("ANS".equals(canalOrigem)) {
            String protocoloAns = ansService.registrarNip(protocolo, caso);
            execution.setVariable("protocoloAns", protocoloAns);
        }
        
        // 4. Notificar responsáveis
        notificationService.alertOuvidoria(caso);
        
        // 5. Calcular prazo conforme regulamentação
        LocalDateTime prazoResposta = calculatePrazo(canalOrigem, prioridade);
        
        // 6. Definir variáveis
        execution.setVariable("protocoloOuvidoria", caso.getProtocolo());
        execution.setVariable("responsavelOuvidoria", caso.getResponsavel());
        execution.setVariable("prazoResposta", prazoResposta);
        execution.setVariable("escaladoParaOuvidoria", true);
        execution.setVariable("prioridadeOuvidoria", prioridade.name());
        
        log.info("Caso escalado para ouvidoria - Protocolo: {}, Prazo: {}", 
                 caso.getProtocolo(), prazoResposta);
    }
    
    private PrioridadeOuvidoria determinePrioridade(String canalOrigem) {
        return switch (canalOrigem) {
            case "ANS", "PROCON" -> PrioridadeOuvidoria.CRITICA;
            case "RECLAME_AQUI" -> PrioridadeOuvidoria.ALTA;
            default -> PrioridadeOuvidoria.NORMAL;
        };
    }
    
    private LocalDateTime calculatePrazo(String canalOrigem, PrioridadeOuvidoria prioridade) {
        return switch (canalOrigem) {
            case "ANS" -> LocalDateTime.now().plusDays(5);  // NIP: 5 dias úteis
            case "PROCON" -> LocalDateTime.now().plusDays(10); // PROCON: 10 dias
            default -> LocalDateTime.now().plusDays(
                prioridade == PrioridadeOuvidoria.CRITICA ? 1 : 
                prioridade == PrioridadeOuvidoria.ALTA ? 3 : 7);
        };
    }
}
```

#### 7. RegistrarResolucaoDelegate.java

```java
/**
 * Registra a resolução final da reclamação.
 * 
 * Variáveis de entrada:
 * - protocoloReclamacao (String)
 * - statusFinal (String): RESOLVIDA, PROCEDENTE, IMPROCEDENTE
 * - descricaoResolucao (String)
 * - compensacaoAplicada (Boolean)
 * - satisfacaoBeneficiario (Integer): 1-5
 * 
 * Variáveis de saída:
 * - reclamacaoEncerrada (Boolean)
 * - dataEncerramento (LocalDateTime)
 * - tempoResolucao (Long): em horas
 */
@Slf4j
@Component("registrarResolucaoDelegate")
@RequiredArgsConstructor
public class RegistrarResolucaoDelegate implements JavaDelegate {

    private final ReclamacaoService reclamacaoService;
    private final MetricasService metricasService;
    private final KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String protocolo = (String) execution.getVariable("protocoloReclamacao");
        String statusFinal = (String) execution.getVariable("statusFinal");
        String descricaoResolucao = (String) execution.getVariable("descricaoResolucao");
        
        // 1. Buscar reclamação
        ReclamacaoDTO reclamacao = reclamacaoService.buscarPorProtocolo(protocolo);
        
        // 2. Calcular tempo de resolução
        LocalDateTime agora = LocalDateTime.now();
        long tempoResolucaoHoras = ChronoUnit.HOURS.between(
            reclamacao.getDataAbertura(), agora);
        
        // 3. Verificar se cumpriu SLA
        boolean dentroDosla = tempoResolucaoHoras <= reclamacao.getSlaHoras();
        
        // 4. Atualizar reclamação
        ResolucaoDTO resolucao = ResolucaoDTO.builder()
            .protocolo(protocolo)
            .statusFinal(statusFinal)
            .descricao(descricaoResolucao)
            .dataEncerramento(agora)
            .tempoResolucaoHoras(tempoResolucaoHoras)
            .dentroDosla(dentroDosla)
            .resolvidoPor((String) execution.getVariable("resolvidoPor"))
            .build();
        
        reclamacaoService.registrarResolucao(resolucao);
        
        // 5. Atualizar métricas
        metricasService.registrarResolucao(
            reclamacao.getTipo(),
            tempoResolucaoHoras,
            dentroDosla,
            statusFinal
        );
        
        // 6. Publicar evento
        kafkaPublisher.publish("reclamacao.resolvida", resolucao);
        
        // 7. Definir variáveis
        execution.setVariable("reclamacaoEncerrada", true);
        execution.setVariable("dataEncerramento", agora);
        execution.setVariable("tempoResolucao", tempoResolucaoHoras);
        execution.setVariable("dentroDosla", dentroDosla);
        
        log.info("Reclamação encerrada - Protocolo: {}, Status: {}, Tempo: {}h, SLA: {}", 
                 protocolo, statusFinal, tempoResolucaoHoras, 
                 dentroDosla ? "CUMPRIDO" : "EXCEDIDO");
    }
}
```

---

### SUB-010: Follow-up e Feedback (5 Delegates)

**Criar pasta:** `src/services/domain/followup/`

#### 8. EnviarPesquisaNpsDelegate.java

```java
/**
 * Envia pesquisa NPS ao beneficiário após interação.
 * 
 * Variáveis de entrada:
 * - beneficiarioId (String)
 * - tipoInteracao (String): ATENDIMENTO, AUTORIZACAO, RECLAMACAO, INTERNACAO
 * - protocoloReferencia (String)
 * - canalPreferido (String)
 * 
 * Variáveis de saída:
 * - pesquisaEnviada (Boolean)
 * - idPesquisa (String)
 * - dataEnvio (LocalDateTime)
 * - dataLimiteResposta (LocalDateTime)
 */
@Slf4j
@Component("enviarPesquisaNpsDelegate")
@RequiredArgsConstructor
public class EnviarPesquisaNpsDelegate implements JavaDelegate {

    private final NpsService npsService;
    private final WhatsAppService whatsAppService;
    private final NotificationService notificationService;
    private final BeneficiarioService beneficiarioService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String tipoInteracao = (String) execution.getVariable("tipoInteracao");
        String protocoloReferencia = (String) execution.getVariable("protocoloReferencia");
        
        // 1. Verificar se já não enviou pesquisa recentemente
        if (npsService.pesquisaRecenteExiste(beneficiarioId, 7)) {
            log.info("Pesquisa NPS já enviada recentemente para beneficiário: {}", 
                     beneficiarioId);
            execution.setVariable("pesquisaEnviada", false);
            execution.setVariable("motivoNaoEnvio", "PESQUISA_RECENTE");
            return;
        }
        
        // 2. Buscar dados do beneficiário
        BeneficiarioDTO beneficiario = beneficiarioService.buscarPorId(beneficiarioId);
        String canalPreferido = (String) execution.getVariable("canalPreferido");
        if (canalPreferido == null) {
            canalPreferido = beneficiario.getCanalPreferido();
        }
        
        // 3. Criar pesquisa
        PesquisaNpsDTO pesquisa = npsService.criarPesquisa(
            beneficiarioId, tipoInteracao, protocoloReferencia);
        
        // 4. Construir mensagem
        String mensagem = buildMensagemNps(pesquisa, beneficiario.getNome());
        
        // 5. Enviar pelo canal preferido
        boolean enviado = enviarPorCanal(canalPreferido, beneficiario, mensagem, pesquisa);
        
        // 6. Definir variáveis
        execution.setVariable("pesquisaEnviada", enviado);
        execution.setVariable("idPesquisa", pesquisa.getId());
        execution.setVariable("dataEnvio", LocalDateTime.now());
        execution.setVariable("dataLimiteResposta", LocalDateTime.now().plusDays(7));
        
        log.info("Pesquisa NPS enviada - ID: {}, Beneficiário: {}, Canal: {}", 
                 pesquisa.getId(), beneficiarioId, canalPreferido);
    }
    
    private String buildMensagemNps(PesquisaNpsDTO pesquisa, String nome) {
        return String.format("""
            Olá %s! 👋
            
            Queremos saber sua opinião sobre nosso atendimento.
            
            De 0 a 10, o quanto você recomendaria a AUSTA para um amigo ou familiar?
            
            Responda com um número de 0 a 10.
            
            Sua opinião é muito importante para nós! 💙
            """, nome.split(" ")[0]);
    }
    
    private boolean enviarPorCanal(String canal, BeneficiarioDTO beneficiario, 
                                   String mensagem, PesquisaNpsDTO pesquisa) {
        return switch (canal) {
            case "WHATSAPP" -> whatsAppService.sendMessage(
                beneficiario.getTelefone(), mensagem);
            case "SMS" -> notificationService.sendSms(
                beneficiario.getTelefone(), mensagem);
            case "EMAIL" -> notificationService.sendEmail(
                beneficiario.getEmail(), "Sua opinião importa - AUSTA", mensagem);
            case "APP" -> notificationService.sendPush(
                beneficiario.getId(), mensagem);
            default -> whatsAppService.sendMessage(
                beneficiario.getTelefone(), mensagem);
        };
    }
}
```

#### 9. ProcessarRespostaNpsDelegate.java

```java
/**
 * Processa resposta da pesquisa NPS.
 * 
 * Variáveis de entrada:
 * - idPesquisa (String)
 * - notaNps (Integer): 0-10
 * - comentario (String): opcional
 * 
 * Variáveis de saída:
 * - categoriaNps (String): DETRATOR, NEUTRO, PROMOTOR
 * - acaoRequerida (String)
 * - prioridadeContato (String)
 */
@Slf4j
@Component("processarRespostaNpsDelegate")
@RequiredArgsConstructor
public class ProcessarRespostaNpsDelegate implements JavaDelegate {

    private final NpsService npsService;
    private final MetricasService metricasService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String idPesquisa = (String) execution.getVariable("idPesquisa");
        Integer notaNps = (Integer) execution.getVariable("notaNps");
        String comentario = (String) execution.getVariable("comentario");
        
        // 1. Classificar NPS
        String categoriaNps = classificarNps(notaNps);
        
        // 2. Registrar resposta
        RespostaNpsDTO resposta = npsService.registrarResposta(
            idPesquisa, notaNps, comentario);
        
        // 3. Determinar ação necessária
        String acaoRequerida = determinarAcao(categoriaNps, comentario);
        String prioridadeContato = determinarPrioridade(categoriaNps);
        
        // 4. Atualizar métricas
        String tipoInteracao = (String) execution.getVariable("tipoInteracao");
        metricasService.registrarNps(tipoInteracao, notaNps, categoriaNps);
        
        // 5. Definir variáveis
        execution.setVariable("categoriaNps", categoriaNps);
        execution.setVariable("acaoRequerida", acaoRequerida);
        execution.setVariable("prioridadeContato", prioridadeContato);
        execution.setVariable("respostaProcessada", true);
        
        log.info("Resposta NPS processada - ID: {}, Nota: {}, Categoria: {}", 
                 idPesquisa, notaNps, categoriaNps);
    }
    
    private String classificarNps(int nota) {
        if (nota <= 6) return "DETRATOR";
        if (nota <= 8) return "NEUTRO";
        return "PROMOTOR";
    }
    
    private String determinarAcao(String categoria, String comentario) {
        return switch (categoria) {
            case "DETRATOR" -> comentario != null && !comentario.isBlank() 
                ? "CONTATO_IMEDIATO" : "CONTATO_24H";
            case "NEUTRO" -> "ANALISAR_COMENTARIO";
            case "PROMOTOR" -> "SOLICITAR_INDICACAO";
            default -> "MONITORAR";
        };
    }
    
    private String determinarPrioridade(String categoria) {
        return switch (categoria) {
            case "DETRATOR" -> "CRITICA";
            case "NEUTRO" -> "BAIXA";
            case "PROMOTOR" -> "BAIXA";
            default -> "MEDIA";
        };
    }
}
```

#### 10. AnalisarSentimentoDelegate.java

```java
/**
 * Analisa sentimento do comentário NPS usando NLP.
 * 
 * Variáveis de entrada:
 * - idPesquisa (String)
 * - comentario (String)
 * - notaNps (Integer)
 * 
 * Variáveis de saída:
 * - sentimento (String): POSITIVO, NEUTRO, NEGATIVO, CRITICO
 * - scoreSentimento (Double): -1.0 a 1.0
 * - temasIdentificados (List<String>)
 * - urgenciaDetectada (Boolean)
 */
@Slf4j
@Component("analisarSentimentoDelegate")
@RequiredArgsConstructor
public class AnalisarSentimentoDelegate implements JavaDelegate {

    private final NlpService nlpService;
    private final NpsService npsService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String comentario = (String) execution.getVariable("comentario");
        
        if (comentario == null || comentario.isBlank()) {
            execution.setVariable("sentimento", "NAO_INFORMADO");
            execution.setVariable("scoreSentimento", 0.0);
            execution.setVariable("temasIdentificados", List.of());
            execution.setVariable("urgenciaDetectada", false);
            return;
        }
        
        // 1. Analisar sentimento
        SentimentAnalysis analysis = nlpService.analyzeSentiment(comentario);
        
        // 2. Extrair temas
        List<String> temas = nlpService.extractTopics(comentario);
        
        // 3. Detectar urgência
        boolean urgencia = nlpService.detectUrgency(comentario);
        
        // 4. Classificar sentimento
        String sentimento = classificarSentimento(analysis.getScore());
        
        // 5. Registrar análise
        String idPesquisa = (String) execution.getVariable("idPesquisa");
        npsService.registrarAnaliseSentimento(idPesquisa, analysis, temas);
        
        // 6. Definir variáveis
        execution.setVariable("sentimento", sentimento);
        execution.setVariable("scoreSentimento", analysis.getScore());
        execution.setVariable("temasIdentificados", temas);
        execution.setVariable("urgenciaDetectada", urgencia);
        execution.setVariable("palavrasChave", analysis.getKeywords());
        
        log.info("Sentimento analisado - Score: {}, Temas: {}, Urgência: {}", 
                 analysis.getScore(), temas, urgencia);
    }
    
    private String classificarSentimento(double score) {
        if (score < -0.6) return "CRITICO";
        if (score < -0.2) return "NEGATIVO";
        if (score < 0.2) return "NEUTRO";
        return "POSITIVO";
    }
}
```

#### 11. AcionarRecuperacaoDetratoresDelegate.java

```java
/**
 * Aciona workflow de recuperação de detratores.
 * 
 * Variáveis de entrada:
 * - beneficiarioId (String)
 * - idPesquisa (String)
 * - notaNps (Integer)
 * - comentario (String)
 * - sentimento (String)
 * - temasIdentificados (List<String>)
 * 
 * Variáveis de saída:
 * - recuperacaoIniciada (Boolean)
 * - responsavelRecuperacao (String)
 * - prazoContato (LocalDateTime)
 * - estrategiaRecuperacao (String)
 */
@Slf4j
@Component("acionarRecuperacaoDetratoresDelegate")
@RequiredArgsConstructor
public class AcionarRecuperacaoDetratoresDelegate implements JavaDelegate {

    private final RecuperacaoService recuperacaoService;
    private final BeneficiarioService beneficiarioService;
    private final NotificationService notificationService;
    private final TaskService taskService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        Integer notaNps = (Integer) execution.getVariable("notaNps");
        String sentimento = (String) execution.getVariable("sentimento");
        @SuppressWarnings("unchecked")
        List<String> temas = (List<String>) execution.getVariable("temasIdentificados");
        
        log.info("Iniciando recuperação de detrator - Beneficiário: {}, NPS: {}", 
                 beneficiarioId, notaNps);
        
        // 1. Buscar histórico do beneficiário
        BeneficiarioDTO beneficiario = beneficiarioService.buscarPorId(beneficiarioId);
        HistoricoInteracoesDTO historico = beneficiarioService
            .buscarHistoricoInteracoes(beneficiarioId, 90);
        
        // 2. Determinar estratégia
        EstrategiaRecuperacao estrategia = recuperacaoService.determinarEstrategia(
            notaNps, sentimento, temas, historico);
        
        // 3. Atribuir responsável
        String responsavel = recuperacaoService.atribuirResponsavel(estrategia);
        
        // 4. Calcular prazo
        LocalDateTime prazoContato = calcularPrazo(sentimento);
        
        // 5. Criar tarefa de recuperação
        CasoRecuperacaoDTO caso = recuperacaoService.criarCaso(
            beneficiarioId,
            (String) execution.getVariable("idPesquisa"),
            estrategia,
            responsavel,
            prazoContato
        );
        
        // 6. Notificar responsável
        notificationService.alertarResponsavel(responsavel, caso);
        
        // 7. Criar task no Camunda
        taskService.createTask(
            "Recuperar Detrator - " + beneficiario.getNome(),
            responsavel,
            prazoContato,
            Map.of(
                "beneficiarioId", beneficiarioId,
                "notaNps", notaNps,
                "estrategia", estrategia.name()
            )
        );
        
        // 8. Definir variáveis
        execution.setVariable("recuperacaoIniciada", true);
        execution.setVariable("responsavelRecuperacao", responsavel);
        execution.setVariable("prazoContato", prazoContato);
        execution.setVariable("estrategiaRecuperacao", estrategia.name());
        execution.setVariable("casoRecuperacaoId", caso.getId());
        
        log.info("Recuperação iniciada - Caso: {}, Estratégia: {}, Responsável: {}", 
                 caso.getId(), estrategia, responsavel);
    }
    
    private LocalDateTime calcularPrazo(String sentimento) {
        return switch (sentimento) {
            case "CRITICO" -> LocalDateTime.now().plusHours(4);
            case "NEGATIVO" -> LocalDateTime.now().plusHours(24);
            default -> LocalDateTime.now().plusHours(48);
        };
    }
}
```

#### 12. AtualizarModelosPreditivosDelegate.java

```java
/**
 * Atualiza modelos preditivos com dados de NPS e feedback.
 * 
 * Variáveis de entrada:
 * - beneficiarioId (String)
 * - notaNps (Integer)
 * - sentimento (String)
 * - temasIdentificados (List<String>)
 * - tipoInteracao (String)
 * - tempoResolucao (Long)
 * 
 * Variáveis de saída:
 * - modelosAtualizados (Boolean)
 * - novoScoreRisco (Double)
 * - novoScoreSatisfacao (Double)
 * - predicaoCancelamento (Double)
 */
@Slf4j
@Component("atualizarModelosPreditivosDelegate")
@RequiredArgsConstructor
public class AtualizarModelosPreditivosDelegate implements JavaDelegate {

    private final MlService mlService;
    private final DataLakeService dataLakeService;
    private final BeneficiarioService beneficiarioService;
    private final KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        Integer notaNps = (Integer) execution.getVariable("notaNps");
        String sentimento = (String) execution.getVariable("sentimento");
        
        log.info("Atualizando modelos preditivos - Beneficiário: {}", beneficiarioId);
        
        // 1. Coletar dados do contexto
        FeedbackData feedbackData = FeedbackData.builder()
            .beneficiarioId(beneficiarioId)
            .notaNps(notaNps)
            .sentimento(sentimento)
            .temas((List<String>) execution.getVariable("temasIdentificados"))
            .tipoInteracao((String) execution.getVariable("tipoInteracao"))
            .tempoResolucao((Long) execution.getVariable("tempoResolucao"))
            .timestamp(LocalDateTime.now())
            .build();
        
        // 2. Enviar para Data Lake
        dataLakeService.ingestFeedbackData(feedbackData);
        
        // 3. Atualizar features do beneficiário
        BeneficiarioFeatures features = mlService.updateBeneficiarioFeatures(
            beneficiarioId, feedbackData);
        
        // 4. Recalcular scores preditivos
        PredictiveScores scores = mlService.calculateScores(beneficiarioId, features);
        
        // 5. Atualizar perfil do beneficiário
        beneficiarioService.atualizarScoresPreditivos(beneficiarioId, scores);
        
        // 6. Publicar evento para pipelines de ML
        kafkaPublisher.publish("ml.feedback.received", feedbackData);
        
        // 7. Se risco de cancelamento alto, alertar
        if (scores.getChurnProbability() > 0.7) {
            log.warn("ALERTA: Alto risco de cancelamento - Beneficiário: {}, Prob: {}", 
                     beneficiarioId, scores.getChurnProbability());
            kafkaPublisher.publish("beneficiario.churn.risk.high", Map.of(
                "beneficiarioId", beneficiarioId,
                "probability", scores.getChurnProbability()
            ));
        }
        
        // 8. Definir variáveis
        execution.setVariable("modelosAtualizados", true);
        execution.setVariable("novoScoreRisco", scores.getRiskScore());
        execution.setVariable("novoScoreSatisfacao", scores.getSatisfactionScore());
        execution.setVariable("predicaoCancelamento", scores.getChurnProbability());
        
        log.info("Modelos atualizados - Risco: {}, Satisfação: {}, Churn: {}", 
                 scores.getRiskScore(), scores.getSatisfactionScore(), 
                 scores.getChurnProbability());
    }
}
```

---

### Common: LogAuditoriaDelegate (1 Delegate - Opcional)

**Localização:** `src/services/domain/common/`

#### 13. LogAuditoriaDelegate.java

```java
/**
 * Registra log de auditoria para ações críticas.
 * 
 * Variáveis de entrada:
 * - tipoAcao (String)
 * - entidade (String)
 * - entidadeId (String)
 * - dadosAntes (Map)
 * - dadosDepois (Map)
 * - usuario (String)
 * 
 * Variáveis de saída:
 * - auditLogId (String)
 * - logRegistrado (Boolean)
 */
@Slf4j
@Component("logAuditoriaDelegate")
@RequiredArgsConstructor
public class LogAuditoriaDelegate implements JavaDelegate {

    private final AuditService auditService;
    private final KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        AuditLog auditLog = AuditLog.builder()
            .processInstanceId(execution.getProcessInstanceId())
            .activityId(execution.getCurrentActivityId())
            .tipoAcao((String) execution.getVariable("tipoAcao"))
            .entidade((String) execution.getVariable("entidade"))
            .entidadeId((String) execution.getVariable("entidadeId"))
            .dadosAntes((Map<String, Object>) execution.getVariable("dadosAntes"))
            .dadosDepois((Map<String, Object>) execution.getVariable("dadosDepois"))
            .usuario((String) execution.getVariable("usuario"))
            .ip((String) execution.getVariable("ipOrigem"))
            .timestamp(LocalDateTime.now())
            .build();
        
        String auditLogId = auditService.registrar(auditLog);
        
        // Publicar para sistema de auditoria centralizado
        kafkaPublisher.publish("audit.log.created", auditLog);
        
        execution.setVariable("auditLogId", auditLogId);
        execution.setVariable("logRegistrado", true);
        
        log.debug("Audit log registrado: {} - {}", auditLogId, auditLog.getTipoAcao());
    }
}
```

---

## PARTE 2: TESTES DE INTEGRAÇÃO (22 Test Files)

### Estrutura de Testes

```
src/test/java/br/com/austa/experiencia/
├── integration/
│   ├── workflow/                    # 11 arquivos (1 por subprocesso)
│   │   ├── OrquestracaoWorkflowIT.java       ✅ EXISTE
│   │   ├── OnboardingWorkflowIT.java         ✅ EXISTE
│   │   ├── MotorProativoWorkflowIT.java      ❌ CRIAR
│   │   ├── RecepcaoClassificacaoWorkflowIT.java  ❌ CRIAR
│   │   ├── SelfServiceWorkflowIT.java        ❌ CRIAR
│   │   ├── AgentesIaWorkflowIT.java          ❌ CRIAR
│   │   ├── AutorizacaoWorkflowIT.java        ✅ EXISTE
│   │   ├── NavegacaoCuidadoWorkflowIT.java   ❌ CRIAR
│   │   ├── GestaoCronicosWorkflowIT.java     ❌ CRIAR
│   │   ├── GestaoReclamacoesWorkflowIT.java  ❌ CRIAR
│   │   └── FollowUpFeedbackWorkflowIT.java   ❌ CRIAR
│   ├── dmn/                         # 11 arquivos (1 por DMN)
│   │   ├── EstratificacaoRiscoDmnIT.java     ✅ EXISTE
│   │   ├── DeteccaoCptDmnIT.java             ✅ EXISTE
│   │   ├── ClassificacaoUrgenciaDmnIT.java   ❌ CRIAR
│   │   ├── RoteamentoDemandaDmnIT.java       ❌ CRIAR
│   │   ├── RegrasAutorizacaoDmnIT.java       ✅ EXISTE
│   │   ├── ProtocoloClinicoDmnIT.java        ❌ CRIAR
│   │   ├── IdentificacaoGatilhosDmnIT.java   ❌ CRIAR
│   │   ├── ElegibilidadeProgramaDmnIT.java   ❌ CRIAR
│   │   ├── PrioridadeAtendimentoDmnIT.java   ❌ CRIAR
│   │   ├── ClassificacaoReclamacaoDmnIT.java ❌ CRIAR
│   │   └── CalculoNpsDmnIT.java              ❌ CRIAR
│   └── e2e/                         # 4 arquivos
│       ├── JornadaBeneficiarioE2EIT.java     ✅ EXISTE
│       ├── AutorizacaoE2EIT.java             ✅ EXISTE
│       ├── ReclamacaoE2EIT.java              ❌ CRIAR
│       └── NpsE2EIT.java                     ❌ CRIAR
└── support/
    ├── TestContainersConfig.java             ✅ EXISTE
    ├── CamundaTestConfig.java                ✅ EXISTE
    ├── MockServersConfig.java                ❌ CRIAR
    └── TestDataFactory.java                  ❌ CRIAR
```

### Testes Faltantes a Criar (22 arquivos)

#### Workflow Integration Tests (8 faltantes)

**Template padrão:**

```java
@SpringBootTest
@Testcontainers
@CamundaSpringBootTest
@ActiveProfiles("test")
class [NomeSubprocesso]WorkflowIT {

    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private HistoryService historyService;
    
    @Autowired
    private TaskService taskService;

    @Test
    @DisplayName("Deve completar fluxo happy path")
    void deveCompletarFluxoHappyPath() {
        // Given
        Map<String, Object> variables = TestDataFactory.criarVariaveis[Subprocesso]();
        
        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "[PROCESS_KEY]", variables);
        
        // Then
        assertThat(instance).isEnded();
        // Validações específicas
    }
    
    @Test
    @DisplayName("Deve tratar exceção corretamente")
    void deveTratarExcecaoCorretamente() {
        // Given
        Map<String, Object> variables = TestDataFactory.criarVariaveisComErro();
        
        // When/Then
        assertThrows(BpmnError.class, () -> {
            runtimeService.startProcessInstanceByKey("[PROCESS_KEY]", variables);
        });
    }
}
```

#### DMN Integration Tests (8 faltantes)

**Template padrão:**

```java
@SpringBootTest
@CamundaSpringBootTest
@ActiveProfiles("test")
class [NomeDmn]DmnIT {

    @Autowired
    private DecisionService decisionService;

    @Test
    @DisplayName("Deve retornar resultado correto para cenário X")
    void deveRetornarResultadoCorretoCenarioX() {
        // Given
        Map<String, Object> variables = Map.of(
            "input1", valor1,
            "input2", valor2
        );
        
        // When
        DmnDecisionTableResult result = decisionService
            .evaluateDecisionTableByKey("[DMN_KEY]", variables);
        
        // Then
        assertThat(result.getSingleResult().getEntry("output")).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
        "input1,input2,expectedOutput",
        // ... mais cenários
    })
    @DisplayName("Deve cobrir todas as regras da tabela de decisão")
    void deveCobrirTodasAsRegras(String input1, String input2, String expected) {
        // ...
    }
}
```

#### E2E Tests (2 faltantes)

**ReclamacaoE2EIT.java:**

```java
@SpringBootTest
@Testcontainers
@CamundaSpringBootTest
class ReclamacaoE2EIT {

    @Test
    @DisplayName("Jornada completa: Reclamação → Análise → Resolução → NPS")
    void jornadaCompletaReclamacao() {
        // 1. Registrar reclamação
        // 2. Verificar classificação DMN
        // 3. Simular análise de causa raiz
        // 4. Propor solução
        // 5. Simular aceite do beneficiário
        // 6. Verificar resolução
        // 7. Verificar envio de NPS
    }
    
    @Test
    @DisplayName("Jornada: Reclamação crítica ANS → Ouvidoria → Prazo")
    void jornadaReclamacaoAns() {
        // 1. Registrar reclamação via ANS
        // 2. Verificar escalação automática para ouvidoria
        // 3. Verificar prazo de 5 dias úteis
    }
}
```

#### Support Classes (2 faltantes)

**MockServersConfig.java:**

```java
@TestConfiguration
public class MockServersConfig {

    @RegisterExtension
    static WireMockExtension tasyMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @RegisterExtension
    static WireMockExtension whatsappMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @RegisterExtension
    static WireMockExtension nlpMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();
        
    // Métodos para configurar stubs
}
```

**TestDataFactory.java:**

```java
public class TestDataFactory {

    public static Map<String, Object> criarVariaveisOnboarding() { /* ... */ }
    public static Map<String, Object> criarVariaveisReclamacao() { /* ... */ }
    public static Map<String, Object> criarVariaveisNps() { /* ... */ }
    public static BeneficiarioDTO criarBeneficiarioTeste() { /* ... */ }
    public static ReclamacaoDTO criarReclamacaoTeste() { /* ... */ }
    // ... mais métodos
}
```

---

## PARTE 3: VALIDAÇÃO FINAL

### Checklist de Completude

```
DELEGATES (12 faltantes):
[ ] SUB-009: RegistrarReclamacaoDelegate
[ ] SUB-009: AnalisarCausaRaizDelegate
[ ] SUB-009: BuscarSolucoesAnterioresDelegate
[ ] SUB-009: ProporSolucaoDelegate
[ ] SUB-009: AplicarCompensacaoDelegate
[ ] SUB-009: EscalarOuvidoriaDelegate
[ ] SUB-009: RegistrarResolucaoDelegate
[ ] SUB-010: EnviarPesquisaNpsDelegate
[ ] SUB-010: ProcessarRespostaNpsDelegate
[ ] SUB-010: AnalisarSentimentoDelegate
[ ] SUB-010: AcionarRecuperacaoDetratoresDelegate
[ ] SUB-010: AtualizarModelosPreditivosDelegate
[ ] Common: LogAuditoriaDelegate (opcional)

TESTES (22 faltantes):
[ ] Workflow: MotorProativoWorkflowIT
[ ] Workflow: RecepcaoClassificacaoWorkflowIT
[ ] Workflow: SelfServiceWorkflowIT
[ ] Workflow: AgentesIaWorkflowIT
[ ] Workflow: NavegacaoCuidadoWorkflowIT
[ ] Workflow: GestaoCronicosWorkflowIT
[ ] Workflow: GestaoReclamacoesWorkflowIT
[ ] Workflow: FollowUpFeedbackWorkflowIT
[ ] DMN: ClassificacaoUrgenciaDmnIT
[ ] DMN: RoteamentoDemandaDmnIT
[ ] DMN: ProtocoloClinicoDmnIT
[ ] DMN: IdentificacaoGatilhosDmnIT
[ ] DMN: ElegibilidadeProgramaDmnIT
[ ] DMN: PrioridadeAtendimentoDmnIT
[ ] DMN: ClassificacaoReclamacaoDmnIT
[ ] DMN: CalculoNpsDmnIT
[ ] E2E: ReclamacaoE2EIT
[ ] E2E: NpsE2EIT
[ ] Support: MockServersConfig
[ ] Support: TestDataFactory
```

### Comandos de Validação

```bash
# 1. Compilar projeto
./mvnw compile

# 2. Executar testes
./mvnw test

# 3. Verificar cobertura
./mvnw verify jacoco:report

# 4. Validar BPMN
./mvnw camunda:validate

# 5. Deploy local
./mvnw spring-boot:run
```

---

## ESTIMATIVA DE ESFORÇO

| Componente | Quantidade | Tempo Estimado |
|------------|------------|----------------|
| Delegates SUB-009 | 7 | 3-4 dias |
| Delegates SUB-010 | 5 | 2-3 dias |
| Delegate Common | 1 | 0.5 dia |
| Workflow Tests | 8 | 4-5 dias |
| DMN Tests | 8 | 3-4 dias |
| E2E Tests | 2 | 2 dias |
| Support Classes | 2 | 1 dia |
| **TOTAL** | **33 itens** | **15-20 dias** |

---

## PRIORIDADE DE EXECUÇÃO

### Sprint 1 : Funcionalidade Crítica
1. ⚡ Delegates SUB-009 (Reclamações) - REGULATÓRIO
2. ⚡ Delegates SUB-010 (Follow-up) - NPS/FEEDBACK
3. Support classes (TestDataFactory, MockServersConfig)

### Sprint 2 
1. Workflow Integration Tests
2. DMN Integration Tests
3. E2E Tests

### Sprint 3  Validação e Ajustes
1. Code review
2. Correções de bugs
3. Documentação
4. Deploy final
