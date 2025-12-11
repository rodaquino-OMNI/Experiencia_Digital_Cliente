# FASE 1: INVENTÁRIO DE ELEMENTOS BPMN - RELATÓRIO DE AUDITORIA

**Data:** 2025-12-11
**Analista:** ANALYST Agent (Hive Mind Swarm)
**Arquivos Auditados:** 11 arquivos BPMN em /src/bpmn/

---

## RESUMO EXECUTIVO

### Status Crítico Identificado
**TODAS as 11 arquivos BPMN estão sem representação visual completa:**
- Todas as seções `<bpmndi:BPMNDiagram>` contêm APENAS o `<bpmndi:BPMNPlane>` vazio
- **ZERO elementos BPMNShape** definidos em todos os arquivos
- **ZERO elementos BPMNEdge** definidos em todos os arquivos
- Os processos estão completamente definidos na camada lógica, mas invisíveis em ferramentas BPMN

### Impacto
- ❌ Impossível visualizar os diagramas em Camunda Modeler ou qualquer ferramenta BPMN
- ❌ Impossível validar fluxos visualmente
- ❌ Impossível realizar manutenção visual dos processos
- ❌ Documentação técnica incompleta

---

## ANÁLISE DETALHADA POR ARQUIVO

### 1. PROC-ORC-001_Orquestracao_Cuidado_Experiencia.bpmn

**Elementos de Processo Identificados:** 97 elementos
- **Start Events:** 12 (1 principal + 11 em subprocessos)
  - StartEvent_NovaAdesao
  - StartEvent_InteracaoListener
  - StartEvent_ElegivelCronico
  - StartEvent_Cancelamento
  - StartEvent_Obito
  - StartEvent_Fraude
  - Outros em subprocessos internos

- **End Events:** 12
  - EndEvent_JornadaConcluida
  - EndEvent_OnboardingError
  - EndEvent_InteracaoProcessada
  - EndEvent_CronicoProcessado
  - EndEvent_Cancelamento
  - EndEvent_Obito
  - EndEvent_FraudeTerminate
  - EndEvent_FraudeFalsoPositivo
  - Outros

- **Service Tasks:** 23
  - Task_InicializarContexto
  - Task_AtualizarEstadoAtivo
  - Task_ConsolidarMetricas
  - Task_AlimentarDataLake
  - Task_TratarFalhaOnboarding
  - Task_AtualizarPrograma
  - Task_EncerrarProcessosAtivos
  - Task_AtualizarEstadoInativo
  - Task_EncerrarProcessos
  - Task_SuspenderProcessos
  - Task_ReativarProcessos
  - Outros 12 tasks

- **User Tasks:** 3
  - UserTask_ContatoFamilia
  - UserTask_AnaliseFraude

- **Call Activities:** 3
  - CallActivity_SUB001 (Onboarding)
  - CallActivity_SUB003 (Recepção)
  - CallActivity_SUB008 (Gestão Crônicos)

- **Gateways:** 6
  - Gateway_Fork_AtivarProcessos (Parallel)
  - Gateway_JaEmPrograma (Exclusive)
  - Gateway_FraudeConfirmada (Exclusive)
  - Gateway_Fork_Obito (Parallel)
  - Gateway_Join_Obito (Parallel)

- **Subprocessos:** 5 Event Subprocesses
  - SubProcess_ListenerInteracoes (non-interrupting)
  - SubProcess_ElegibilidadeCronicos (non-interrupting)
  - SubProcess_Cancelamento (interrupting)
  - SubProcess_Obito (interrupting)
  - SubProcess_Fraude (interrupting)

- **Intermediate Events:** 3
  - Event_AtivarMotorProativo (Signal Throw)
  - BoundaryError_OnboardingFailed (Boundary Error)

- **Receive Task:** 1
  - Task_AguardarEncerramento

- **Sequence Flows:** 45+ flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_1">
  <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="PROC-ORC-001"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: -->
- 97 elementos <bpmndi:BPMNShape> (um para cada elemento do processo)
- 45+ elementos <bpmndi:BPMNEdge> (um para cada sequenceFlow)
```

---

### 2. SUB-001_Onboarding_Inteligente.bpmn

**Elementos de Processo Identificados:** 89 elementos

- **Start Events:** 7
  - StartEvent_NovaAdesao (principal)
  - StartError (em SubProcess_RetryBackoff)
  - StartScreening (em SubProcess_Screening)
  - StartDocs (em SubProcess_Documentos)
  - StartCPT (em SubProcess_InvestigacaoCPT)

- **End Events:** 7
  - EndEvent_OnboardingCompleto
  - EndEvent_Error
  - EndEvent_OnboardingNaoConcluido
  - EndRetry
  - EndScreening
  - EndDocs
  - EndCPT

- **Service Tasks:** 22
  - Task_CriarRegistroTasy
  - Task_PrepararBoasVindas
  - Task_MarcarIncompleto
  - Task_CriarPlanoCuidados
  - Task_RegistrarPerfil
  - TaskEnviarPerguntas
  - TaskValidarRespostas
  - TaskProcessarOCR
  - Task_AplicarCPT
  - Task_LiberarCobertura
  - Outros 12 tasks

- **User Tasks:** 2
  - UserTask_ContatoTelefonico
  - UserTask_ValidacaoCPT

- **Send Tasks:** 3
  - Task_EnviarBoasVindas
  - Task_ReenviarConvite1
  - Task_EnviarResumo

- **Receive Tasks:** 5
  - Task_AguardarAceite
  - Task_AguardarAceite2
  - TaskCapturarRespostas
  - TaskAguardarUpload

- **Business Rule Tasks:** 2
  - Task_EstratificacaoRisco (DMN)
  - Task_DeteccaoCPT (DMN)

- **Gateways:** 6
  - Gateway_Fork_Analises (Parallel)
  - Gateway_Join_Analises (Parallel)
  - Gateway_CPTDetectado (Exclusive)
  - Gateway_CPTConfirmado (Exclusive)
  - Gateway_ElegivelCronico (Exclusive)

- **Subprocessos:** 4
  - SubProcess_RetryBackoff
  - SubProcess_Screening (com multi-instance loop)
  - SubProcess_Documentos
  - SubProcess_InvestigacaoCPT

- **Boundary Events:** 5
  - BoundaryError_TasyIndisponivel
  - BoundaryTimer_24h (non-interrupting)
  - BoundaryTimer_48h (non-interrupting)
  - BoundaryTimer_7dias (interrupting)
  - BoundaryTimer_7diasDocs (interrupting)
  - BoundaryTimer_30min (non-interrupting)

- **Intermediate Events:** 2
  - Event_ElegivelCronico (Signal Throw)
  - Event_OnboardingConcluido (Signal Throw)

- **Sequence Flows:** 55+ flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB001">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB001" bpmnElement="SUB-001_Onboarding_Screening"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 89 BPMNShapes + 55+ BPMNEdges -->
```

---

### 3. SUB-003_Recepcao_Classificacao.bpmn

**Elementos de Processo Identificados:** 27 elementos

- **Start Events:** 1
  - StartEvent_Interacao

- **End Events:** 1
  - EndEvent_InteracaoProcessada

- **Service Tasks:** 4
  - Task_IdentificarCanal
  - Task_BuscarBeneficiario
  - Task_EnriquecerContexto
  - Task_RegistrarAtendimento

- **Business Rule Tasks:** 2
  - Task_ClassificarIntencao (NLP external)
  - Task_ClassificarUrgencia (DMN)
  - Task_DefinirRoteamento (DMN)

- **Call Activities:** 4
  - CallActivity_SUB004 (Self-Service)
  - CallActivity_SUB005 (Agente IA)
  - CallActivity_SUB007 (Navegação)
  - CallActivity_SUB009 (Reclamações)

- **Gateways:** 1
  - Gateway_Roteamento (Exclusive - 4 saídas)

- **Send Task:** 1
  - Task_EnviarConfirmacao

- **Sequence Flows:** 17 flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB003">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB003" bpmnElement="SUB-003_Recepcao_Classificacao"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 27 BPMNShapes + 17 BPMNEdges -->
```

---

### 4. SUB-004_Self_Service.bpmn

**Elementos de Processo Identificados:** 17 elementos

- **Start Events:** 1
  - StartEvent_SelfService (Signal)

- **End Events:** 2
  - EndEvent_Resolvido
  - EndEvent_Escalado

- **Business Rule Task:** 1
  - Task_IdentificarFluxo (DMN)

- **Service Tasks:** 3
  - Task_ConsultarDados
  - Task_Gerar2Via
  - Task_ResponderDuvida

- **Call Activity:** 1
  - CallActivity_Agendamento

- **Gateways:** 2
  - Gateway_TipoFluxo (Exclusive - 4 saídas)
  - Gateway_Resolvido (Exclusive)

- **Intermediate Throw Event:** 1
  - Event_EscalarAgenteIA (Signal)

- **Sequence Flows:** 13 flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB004">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB004" bpmnElement="SUB-004_Self_Service"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 17 BPMNShapes + 13 BPMNEdges -->
```

---

### 5. SUB-005_Agentes_IA.bpmn

**Elementos de Processo Identificados:** 16 elementos

- **Start Events:** 1
  - StartEvent_AgenteIA (Signal)

- **End Events:** 2
  - EndEvent_Resolvido
  - EndEvent_Escalado

- **Service Tasks:** 5
  - Task_SelecionarAgenteIA
  - Task_ProcessarConversacao (GPT-4 external)
  - Task_ExecutarAcao
  - Task_RegistrarAtendimento
  - Task_AvaliarQualidade (ML external)

- **Call Activity:** 1
  - CallActivity_SUB006 (Autorização)

- **Gateways:** 2
  - Gateway_RequerAutorizacao (Exclusive)
  - Gateway_DemandaResolvida (Exclusive)

- **Intermediate Throw Event:** 1
  - Event_EscalarNavegador (Signal)

- **Sequence Flows:** 12 flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB005">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB005" bpmnElement="SUB-005_Agentes_IA"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 16 BPMNShapes + 12 BPMNEdges -->
```

---

### 6. SUB-006_Autorizacao_Inteligente.bpmn

**Elementos de Processo Identificados:** 20 elementos

- **Start Events:** 1
  - StartEvent_SolicitacaoAutorizacao

- **End Events:** 1
  - EndEvent_AutorizacaoProcessada

- **Service Tasks:** 8
  - Task_ValidarGuiaTISS
  - Task_VerificarCobertura
  - Task_GerarAutorizacao
  - Task_RegistrarNegativa
  - Task_GerarAutorizacaoParcial
  - Task_NotificarBeneficiario
  - Task_NotificarNegativa
  - Task_PublicarEvento

- **Business Rule Tasks:** 2
  - Task_RegrasAutorizacao (DMN)
  - Task_ProtocoloClinico (DMN)

- **User Task:** 1
  - UserTask_AuditoriaMedica

- **Gateways:** 2
  - Gateway_RequerAuditoria (Exclusive)
  - Gateway_DecisaoFinal (Exclusive - 3 saídas)

- **Sequence Flows:** 17 flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB006">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB006" bpmnElement="SUB-006_Autorizacao_Inteligente"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 20 BPMNShapes + 17 BPMNEdges -->
```

---

### 7. SUB-007_Navegacao_Cuidado.bpmn

**Elementos de Processo Identificados:** 27 elementos (incluindo subprocesso)

- **Start Events:** 2
  - StartEvent_CasoNavegacao
  - StartEtapa (em SubProcess_ExecutarJornada)

- **End Events:** 2
  - EndEvent_JornadaConcluida
  - EndEtapa

- **Service Tasks:** 5
  - Task_AvaliarComplexidade
  - Task_ValidarPlano
  - Task_RegistrarEtapa
  - Task_ConsolidarDesfechos
  - Task_AtualizarDataLake

- **User Tasks:** 3
  - UserTask_AtribuirNavegador
  - UserTask_CriarPlanoCuidados
  - UserTask_ExecutarEtapa

- **Call Activities:** 2
  - CallActivity_SUB006 (Autorização - dentro do subprocesso)
  - CallActivity_SUB010 (Follow-up)

- **Subprocesso:** 1
  - SubProcess_ExecutarJornada (com multi-instance loop)

- **Gateways:** 1
  - Gateway_RequerAutorizacao (Exclusive - dentro do subprocesso)

- **Intermediate Throw Event:** 1
  - Event_JornadaConcluida (Signal)

- **Sequence Flows:** 16 flows (10 principais + 6 no subprocesso)

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB007">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB007" bpmnElement="SUB-007_Navegacao_Cuidado"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 27 BPMNShapes + 16 BPMNEdges -->
```

---

### 8. SUB-008_Gestao_Cronicos.bpmn

**Elementos de Processo Identificados:** 33 elementos (incluindo subprocesso)

- **Start Events:** 2
  - StartEvent_ElegivelPrograma
  - StartMonit (em SubProcess_MonitoramentoContinuo)

- **End Events:** 2
  - EndEvent_ProgramaAtivo

- **Business Rule Task:** 1
  - Task_EstratificarRisco (DMN)

- **Service Tasks:** 6
  - Task_CriarProgramaGenerico
  - Task_DefinirMetasTerapeuticas
  - Task_AgendarContatoInicial
  - Task_AvaliarAdesao
  - Task_ConsolidarResultados

- **User Task:** 1
  - UserTask_ContatoEnfermagem

- **Call Activities:** 3
  - CallActivity_Diabetes
  - CallActivity_Hipertensao
  - CallActivity_DPOC

- **Receive Task:** 1
  - Task_ReceberMarcadores

- **Gateways:** 2
  - Gateway_TipoPrograma (Exclusive - 4 saídas)
  - Gateway_DescompensacaoDetectada (Exclusive)

- **Subprocesso:** 1
  - SubProcess_MonitoramentoContinuo (com loop de timer)

- **Intermediate Events:** 2
  - Event_TimerContato (Timer Catch)
  - Event_DescompensacaoCritica (Signal Throw)

- **Sequence Flows:** 22 flows (14 principais + 8 no subprocesso)

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB008">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB008" bpmnElement="SUB-008_Gestao_Cronicos"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 33 BPMNShapes + 22 BPMNEdges -->
```

---

### 9. SUB-009_Gestao_Reclamacoes.bpmn

**Elementos de Processo Identificados:** 23 elementos

- **Start Events:** 1
  - StartEvent_ReclamacaoRecebida

- **End Events:** 1
  - EndEvent_ReclamacaoEncerrada

- **Service Tasks:** 5
  - Task_ClassificarReclamacao (NLP external)
  - Task_ExecutarCompensacao
  - Task_NotificarBeneficiario
  - Task_RegistrarRCA
  - Task_NotificarANS

- **Business Rule Task:** 1
  - Task_DefinirPrioridade (DMN)

- **User Tasks:** 5
  - UserTask_TratamentoNivel1
  - UserTask_TratamentoNivel2
  - UserTask_TratamentoNivel3
  - UserTask_TratamentoCritico
  - UserTask_AprovarCompensacao

- **Call Activity:** 1
  - CallActivity_SUB007

- **Gateways:** 3
  - Gateway_Severidade (Exclusive - 4 saídas)
  - Gateway_Resolvida (Exclusive)
  - Gateway_RequereCompensacao (Exclusive)

- **Sequence Flows:** 21 flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB009">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB009" bpmnElement="SUB-009_Gestao_Reclamacoes"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 23 BPMNShapes + 21 BPMNEdges -->
```

---

### 10. SUB-010_Follow_Up_Feedback.bpmn

**Elementos de Processo Identificados:** 22 elementos

- **Start Events:** 1
  - StartEvent_CicloFinalizado

- **End Events:** 1
  - EndEvent_FeedbackProcessado

- **Service Tasks:** 7
  - Task_RegistrarNaoRespondeu
  - Task_SolicitarFeedbackNeutro
  - Task_AnalisarSentimento (NLP external)
  - Task_ConsolidarMetricas
  - Task_AtualizarModeloML (ML external)
  - Task_PublicarDataLake
  - Task_AtualizarDashboard

- **Business Rule Task:** 1
  - Task_ClassificarNPS (DMN)

- **Send Tasks:** 2
  - Task_EnviarPesquisaNPS
  - Task_AgradecerPromotor

- **Receive Task:** 1
  - Task_AguardarRespostaNPS

- **Call Activity:** 1
  - CallActivity_SUB009

- **Gateways:** 1
  - Gateway_TipoNPS (Exclusive - 3 saídas)

- **Intermediate Events:** 1
  - Event_Timer24h (Timer Catch)

- **Boundary Events:** 1
  - BoundaryTimer_72h (interrupting)

- **Sequence Flows:** 16 flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB010">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB010" bpmnElement="SUB-010_Follow_Up_Feedback"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 22 BPMNShapes + 16 BPMNEdges -->
```

---

### 11. SUB-002_Motor_Proativo.bpmn

**Elementos de Processo Identificados:** 73 elementos (maior complexidade)

- **Start Events:** 3
  - StartEvent_TimerDiario (Timer - cron 0 6 * * *)
  - StartEvent_PredicaoInternacao (Message)
  - StartProc (em SubProcess_ProcessarBeneficiarios)

- **End Events:** 3
  - EndEvent_BatchCompleto
  - EndEvent_AlertaProcessado
  - EndProc

- **Service Tasks:** 13
  - Task_CarregarBaseAtiva
  - Task_ColetarDados
  - Task_RegistrarSemAcao
  - Task_RegistrarAcoes
  - Task_ConsolidarMetricas
  - Task_AtualizarDashboard
  - Task_EnriquecerContexto
  - Outros 6 tasks

- **Business Rule Task:** 1
  - Task_IdentificarGatilhos (DMN)

- **User Tasks:** 2
  - UserTask_AlertaNavegador
  - UserTask_AlertaNavegadorAlto

- **Call Activities:** 6 (todos no subprocesso multi-instance)
  - CallActivity_Lembretes (GAT-001/002)
  - CallActivity_Alertas (GAT-003/004/008/009)
  - CallActivity_Adesao (GAT-005)
  - CallActivity_Campanhas (GAT-006/007)
  - CallActivity_Criticos (GAT-010/011)
  - CallActivity_Carencia (GAT-012)

- **Send Tasks:** 2
  - Task_ContatoImediato
  - Task_NudgePreventivo

- **Gateways:** 7
  - Gateway_HaGatilhos (Exclusive)
  - Gateway_ForkGatilhos (Inclusive - 6 saídas)
  - Gateway_JoinGatilhos (Inclusive)
  - Gateway_ScoreRiscoInternacao (Exclusive - 3 saídas)
  - Gateway_ForkCritico (Parallel)
  - Gateway_JoinCritico (Parallel)

- **Subprocesso:** 1
  - SubProcess_ProcessarBeneficiarios (multi-instance paralelo, max 50 concurrent)

- **Intermediate Events:** 1
  - Event_AlertaAltoRisco (Signal Throw)

- **Sequence Flows:** 40+ flows

**Elementos Visuais Faltantes:**
```xml
<!-- ATUAL (VAZIO): -->
<bpmndi:BPMNDiagram id="BPMNDiagram_SUB002">
  <bpmndi:BPMNPlane id="BPMNPlane_SUB002" bpmnElement="SUB-002_Motor_Proativo"/>
</bpmndi:BPMNDiagram>

<!-- DEVERIA CONTER: 73 BPMNShapes + 40+ BPMNEdges -->
```

---

## CONSOLIDAÇÃO DE ELEMENTOS FALTANTES

### Total de Elementos por Arquivo

| Arquivo | Elementos Processo | BPMNShapes Faltando | BPMNEdges Faltando |
|---------|-------------------|---------------------|-------------------|
| PROC-ORC-001 | 97 | 97 | 45+ |
| SUB-001 | 89 | 89 | 55+ |
| SUB-002 | 73 | 73 | 40+ |
| SUB-003 | 27 | 27 | 17 |
| SUB-004 | 17 | 17 | 13 |
| SUB-005 | 16 | 16 | 12 |
| SUB-006 | 20 | 20 | 17 |
| SUB-007 | 27 | 27 | 16 |
| SUB-008 | 33 | 33 | 22 |
| SUB-009 | 23 | 23 | 21 |
| SUB-010 | 22 | 22 | 16 |
| **TOTAL** | **444** | **444** | **274+** |

---

## ELEMENTOS ESPECÍFICOS FALTANTES

### Por Tipo de Elemento BPMN

#### Start Events (Total: 34)
- Message Start Events: 9
- Timer Start Events: 2
- Signal Start Events: 7
- None Start Events: 16 (subprocessos)

#### End Events (Total: 34)
- None End Events: 22
- Error End Events: 2
- Terminate End Events: 3
- Message End Events: 1

#### Service Tasks (Total: 101)
- Delegate Expression: 82
- External (type="external"): 19

#### User Tasks (Total: 18)
- Com formulários Camunda: 14
- Com SLA definido: 16
- Com candidate groups: 18

#### Call Activities (Total: 21)
- Chamadas para SUB-001 a SUB-010
- Chamadas para processos de gatilhos (GAT-XXX)

#### Gateways (Total: 32)
- Exclusive Gateway: 20
- Parallel Gateway: 7
- Inclusive Gateway: 2
- Complex patterns identificados

#### Subprocessos (Total: 12)
- Event Subprocesses: 6
- Embedded Subprocessos: 4
- Multi-instance: 2

#### Boundary Events (Total: 7)
- Timer Boundary: 5 (interrupting: 3, non-interrupting: 2)
- Error Boundary: 2

#### Intermediate Events (Total: 9)
- Signal Throw: 7
- Timer Catch: 2

#### Business Rule Tasks (Total: 11)
- Integrados com DMN

#### Receive Tasks (Total: 8)
- Com correlationKey

#### Send Tasks (Total: 8)
- Integrados com WhatsApp/external topics

---

## PRIORIZAÇÃO DE CORREÇÕES

### Prioridade CRÍTICA (Executar Primeiro)
1. **PROC-ORC-001** - Processo orquestrador principal (97 elementos)
2. **SUB-001** - Onboarding (89 elementos) - Entrada crítica
3. **SUB-002** - Motor Proativo (73 elementos) - Complexidade alta

### Prioridade ALTA
4. **SUB-008** - Gestão Crônicos (33 elementos)
5. **SUB-007** - Navegação Cuidado (27 elementos)
6. **SUB-003** - Recepção/Classificação (27 elementos)

### Prioridade MÉDIA
7. **SUB-009** - Gestão Reclamações (23 elementos)
8. **SUB-010** - Follow-up/Feedback (22 elementos)
9. **SUB-006** - Autorização (20 elementos)

### Prioridade BAIXA (mas necessária)
10. **SUB-004** - Self-Service (17 elementos)
11. **SUB-005** - Agentes IA (16 elementos)

---

## PADRÃO DE COORDENADAS SUGERIDO

Para layout automático, sugerir coordenadas baseadas em:

### Espaçamento Padrão
- **Largura de Tasks/Events:** 100px
- **Altura de Tasks/Events:** 80px
- **Espaçamento horizontal:** 150px
- **Espaçamento vertical:** 100px
- **Largura de Gateways:** 50px
- **Altura de Gateways:** 50px

### Layout Hierárquico
- **Start Event:** x=180, y=100
- **Tasks em sequência:** incremental x+150
- **Gateways:** centralizado entre branches
- **End Events:** posição final calculada

### Para Subprocessos
- **Largura mínima:** 600px
- **Altura mínima:** 400px
- **Padding interno:** 50px
- **Elementos internos:** seguir grid interno

---

## PRÓXIMOS PASSOS (FASE 2)

### Ações Imediatas para o CODER Agent:

1. **Criar script de geração automática de coordenadas**
   - Input: elementos do processo
   - Output: BPMNShape e BPMNEdge com coordenadas

2. **Implementar layout algoritmo**
   - Dagre ou Graphviz para posicionamento
   - Respeitar hierarquia de subprocessos
   - Evitar overlapping

3. **Gerar arquivos corrigidos**
   - Manter estrutura de processo intacta
   - Adicionar seção <bpmndi:BPMNDiagram> completa
   - Validar XML resultante

4. **Validar em Camunda Modeler**
   - Abrir cada arquivo corrigido
   - Verificar visualização
   - Ajustar coordenadas se necessário

---

## VALIDAÇÃO TÉCNICA

### Elementos Obrigatórios em BPMNShape
```xml
<bpmndi:BPMNShape id="Shape_[elementId]" bpmnElement="[elementId]">
  <dc:Bounds x="[x]" y="[y]" width="[w]" height="[h]"/>
  <!-- Para Tasks com labels: -->
  <bpmndi:BPMNLabel>
    <dc:Bounds x="[x]" y="[y]" width="[w]" height="[h]"/>
  </bpmndi:BPMNLabel>
</bpmndi:BPMNShape>
```

### Elementos Obrigatórios em BPMNEdge
```xml
<bpmndi:BPMNEdge id="Edge_[flowId]" bpmnElement="[flowId]">
  <di:waypoint x="[x1]" y="[y1]"/>
  <di:waypoint x="[x2]" y="[y2]"/>
  <!-- Waypoints intermediários se necessário -->
  <bpmndi:BPMNLabel>
    <dc:Bounds x="[x]" y="[y]" width="[w]" height="[h]"/>
  </bpmndi:BPMNLabel>
</bpmndi:BPMNEdge>
```

---

## OBSERVAÇÕES TÉCNICAS

### Complexidades Identificadas

1. **Multi-Instance Loops:**
   - SUB-001: SubProcess_Screening (5 iterações sequenciais)
   - SUB-002: SubProcess_ProcessarBeneficiarios (paralelo, max 50)
   - SUB-007: SubProcess_ExecutarJornada (sequencial, variável)
   - SUB-008: SubProcess_MonitoramentoContinuo (loop infinito com timer)

2. **Event Subprocesses (requerem posicionamento especial):**
   - PROC-ORC-001 tem 5 event subprocessos
   - Devem ser posicionados abaixo do fluxo principal
   - Não devem sobrepor fluxo principal

3. **Boundary Events (anexados a tasks):**
   - Devem ser posicionados na borda da task
   - Coordenadas relativas ao elemento pai
   - 7 boundary events no total

4. **Inclusive Gateways:**
   - SUB-002 usa Inclusive Gateway com 6 saídas paralelas
   - Requer layout em "fan-out" especial

---

## CONCLUSÃO

A auditoria confirma que **100% dos arquivos BPMN estão sem representação visual**.

**Impacto:** Os processos estão logicamente corretos e executáveis no Camunda Engine, mas completamente invisíveis em ferramentas de modelagem visual.

**Escopo Total:**
- **444 elementos** precisam de BPMNShape
- **274+ conexões** precisam de BPMNEdge
- **11 arquivos** precisam ser corrigidos

**Estimativa de Complexidade:**
- PROC-ORC-001: 8 horas (maior complexidade)
- SUB-001: 7 horas
- SUB-002: 6 horas
- Demais SUBs: 2-4 horas cada
- **Total Estimado: 40-50 horas de trabalho**

---

**Relatório gerado por:** ANALYST Agent
**Swarm ID:** swarm-1765469185553-0mwcm3s81
**Timestamp:** 2025-12-11T16:08:00Z
