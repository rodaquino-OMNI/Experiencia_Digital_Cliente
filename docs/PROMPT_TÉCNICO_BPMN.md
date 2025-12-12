# Operadora Digital do Futuro

> **Versão:** 2.0  
> **Data:** Dezembro 2024  
> **Compatibilidade:** Camunda Platform 7.x  
> **Organização:** AUSTA Saúde

---

## SUMÁRIO

1. [Contexto e Papel do Agente](#1-contexto-e-papel-do-agente)
2. [Objetivo da Tarefa](#2-objetivo-da-tarefa)
3. [Arquitetura de Processos](#3-arquitetura-de-processos)
4. [Processo Orquestrador Principal](#4-processo-orquestrador-principal)
5. [Matriz de Interdependências](#5-matriz-de-interdependências)
6. [Variáveis de Processo](#6-variáveis-de-processo)
7. [Especificações Técnicas BPMN/Camunda 7](#7-especificações-técnicas-bpmncamunda-7)
8. [Detalhamento dos 10 Subprocessos](#8-detalhamento-dos-10-subprocessos)
9. [Catálogo de Erros e Exceções](#9-catálogo-de-erros-e-exceções)
10. [Correlação de Mensagens](#10-correlação-de-mensagens)
11. [Especificações de Integração](#11-especificações-de-integração)
12. [Eventos e Mensageria (Kafka)](#12-eventos-e-mensageria-kafka)
13. [Templates de Comunicação](#13-templates-de-comunicação)
14. [Métricas e Observabilidade](#14-métricas-e-observabilidade)
15. [Compliance e LGPD](#15-compliance-e-lgpd)
16. [Requisitos de Saída](#16-requisitos-de-saída)
17. [Validações e Checklist](#17-validações-e-checklist)

---

## 1. CONTEXTO E PAPEL DO AGENTE

### 1.1 Persona do Agente

Você é um **Arquiteto de Processos Sênior** com as seguintes especializações:

| Domínio | Expertise |
|---------|-----------|
| **Saúde Suplementar** | Regulação ANS, TISS, RN 465, RN 566, ciclo de receita, sinistralidade |
| **BPMN 2.0** | Modelagem avançada, padrões de workflow, anti-patterns |
| **Camunda Platform 7** | Engine configuration, job executor, history, external tasks |
| **Automação Inteligente** | RPA (IBM), IA/ML, NLP, Computer Vision, integração de sistemas |
| **Coordenação do Cuidado** | Care management, disease management, population health |
| **Customer Experience** | Jornada omnichannel, NPS, CES, design de serviços |

### 1.2 Capacidades Requeridas

O agente deve ser capaz de:

1. **Analisar** requisitos de negócio e traduzi-los em modelos BPMN executáveis
2. **Projetar** arquiteturas de processo escaláveis e manuteníveis
3. **Especificar** todos os elementos técnicos necessários para execução no Camunda 7
4. **Validar** consistência, completude e aderência a padrões
5. **Documentar** de forma clara e técnica para implementação

### 1.3 Princípios de Design

Ao modelar os processos, siga estes princípios:

| Princípio | Descrição |
|-----------|-----------|
| **Proatividade** | "Se o beneficiário precisou nos acionar, falhamos em antecipar sua necessidade" |
| **Resolução na Origem** | Resolver na camada mais simples possível (self-service > IA > humano) |
| **Contexto Completo** | Nunca pedir ao beneficiário informação que já temos |
| **Ownership** | Todo caso complexo tem um responsável claro até resolução |
| **Melhoria Contínua** | Feedback loops para refinar modelos e processos |

---

## 2. OBJETIVO DA TAREFA

### 2.1 Escopo

Modele um **ecossistema completo de processos BPMN** para uma operadora de plano de saúde 100% digital, composto por:

- **1 Processo Orquestrador Principal** - coordena toda a jornada do beneficiário
- **10 Subprocessos Especializados** - cada um responsável por uma etapa crítica
- **Processos de Suporte** - tratamento de erros, compensações, auditoria

### 2.2 Cobertura Funcional

O modelo deve cobrir **100% da jornada do beneficiário**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        JORNADA DO BENEFICIÁRIO                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ENTRADA          MONITORAMENTO       INTERAÇÃO        RESOLUÇÃO            │
│  ────────         ─────────────       ─────────        ─────────            │
│  • Onboarding     • Proatividade      • Recepção       • Self-Service       │
│  • Screening      • Gatilhos          • Classificação  • Agentes IA         │
│  • Estratificação • Predição          • Roteamento     • Navegação          │
│  • Plano Cuidados • Nudges            • Contexto       • Autorização        │
│                                                                              │
│  GESTÃO ESPECIAL           EXCEÇÕES              ENCERRAMENTO               │
│  ──────────────            ────────              ────────────               │
│  • Crônicos                • Reclamações         • Follow-up                │
│  • Alto Risco              • NIPs                • NPS                      │
│  • Transição Cuidado       • Ouvidoria           • Desfechos                │
│                                                  • Feedback ML              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Critérios de Sucesso

| Critério | Descrição | Validação |
|----------|-----------|-----------|
| **Completude** | Todas as etapas críticas modeladas | Checklist de cobertura |
| **Executabilidade** | XML válido importável no Camunda Modeler | Teste de importação |
| **Consistência** | Sem atividades soltas, loops infinitos ou deadlocks | Análise de fluxo |
| **Rastreabilidade** | Toda decisão/ação tem audit trail | Verificação de logs |
| **Testabilidade** | Processos podem ser testados unitariamente | Cenários de teste |

---

## 3. ARQUITETURA DE PROCESSOS

### 3.1 Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     PROCESSO ORQUESTRADOR PRINCIPAL                          │
│                 PROC-ORC-001_Orquestracao_Cuidado_Experiencia                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐            │
│    │ SUB-001  │    │ SUB-002  │    │ SUB-003  │    │ SUB-004  │            │
│    │Onboarding│───►│ Motor    │    │ Recepção │───►│  Self-   │            │
│    │          │    │ Proativo │    │Classific.│    │ Service  │            │
│    └──────────┘    └────┬─────┘    └────┬─────┘    └──────────┘            │
│                         │               │                                    │
│                         │               │          ┌──────────┐             │
│                         │               ├─────────►│ SUB-005  │             │
│                         │               │          │Agentes IA│             │
│                         │               │          └────┬─────┘             │
│                         │               │               │                    │
│    ┌──────────┐         │               │          ┌────▼─────┐             │
│    │ SUB-008  │◄────────┤               │          │ SUB-006  │             │
│    │ Crônicos │         │               │          │Autorização│            │
│    └──────────┘         │               │          └────┬─────┘             │
│                         │               │               │                    │
│                         │          ┌────▼─────┐        │                    │
│                         └─────────►│ SUB-007  │◄───────┘                    │
│                                    │Navegação │                              │
│                                    └────┬─────┘                              │
│    ┌──────────┐                         │          ┌──────────┐             │
│    │ SUB-009  │◄────────────────────────┼─────────►│ SUB-010  │             │
│    │Reclamaçõe│                         │          │Follow-up │             │
│    └──────────┘                         │          └──────────┘             │
│                                         │                                    │
└─────────────────────────────────────────┴────────────────────────────────────┘
```

### 3.2 Catálogo de Processos

| ID | Nome | Tipo | Trigger Principal | Owner |
|----|------|------|-------------------|-------|
| `PROC-ORC-001` | Orquestração do Cuidado e Experiência | Orquestrador | Múltiplos | Gerente de Experiência |
| `SUB-001` | Onboarding Inteligente e Screening | Subprocesso | Message: Nova Adesão | Coord. Onboarding |
| `SUB-002` | Motor Proativo de Antecipação | Subprocesso | Timer: Diário 06:00 | Coord. Proatividade |
| `SUB-003` | Recepção e Classificação Omnichannel | Subprocesso | Message: Interação Recebida | Coord. Atendimento |
| `SUB-004` | Self-Service e Autoatendimento | Subprocesso | Signal: Roteado Self-Service | Coord. Digital |
| `SUB-005` | Atendimento por Agentes IA | Subprocesso | Signal: Roteado Agente IA | Coord. IA |
| `SUB-006` | Autorização Inteligente | Subprocesso | Message: Solicitação Autorização | Coord. Regulação |
| `SUB-007` | Navegação e Coordenação do Cuidado | Subprocesso | Signal: Caso para Navegação | Gerente Navegação |
| `SUB-008` | Gestão de Crônicos e Alto Risco | Subprocesso | Signal: Elegível Programa | Coord. Crônicos |
| `SUB-009` | Gestão de Reclamações e Escalações | Subprocesso | Message: Reclamação Recebida | Coord. Ouvidoria |
| `SUB-010` | Follow-up, Feedback e Encerramento | Subprocesso | Signal: Ciclo Finalizado | Coord. Qualidade |

### 3.3 Estados do Beneficiário

O orquestrador gerencia o **ciclo de vida do beneficiário** através de estados:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    MÁQUINA DE ESTADOS DO BENEFICIÁRIO                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │  NOVO    │───►│ONBOARDING│───►│  ATIVO   │───►│MONITORADO│              │
│  │          │    │          │    │          │    │          │              │
│  └──────────┘    └──────────┘    └────┬─────┘    └────┬─────┘              │
│                                       │               │                      │
│                                       ▼               ▼                      │
│                                  ┌──────────┐    ┌──────────┐               │
│                                  │EM_ATENDI │    │EM_PROGRAMA│              │
│                                  │  MENTO   │    │ _CRONICO  │              │
│                                  └────┬─────┘    └────┬─────┘               │
│                                       │               │                      │
│                                       ▼               ▼                      │
│                                  ┌──────────┐    ┌──────────┐               │
│                                  │EM_JORNADA│    │EM_GESTAO │               │
│                                  │_CUIDADO  │    │_INTENSIVA│               │
│                                  └────┬─────┘    └──────────┘               │
│                                       │                                      │
│       ┌──────────┐                    ▼                                      │
│       │INATIVO/  │◄──────────────┌──────────┐                               │
│       │CANCELADO │               │ENCERRADO │                               │
│       └──────────┘               └──────────┘                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

| Estado | Descrição | Processos Ativos |
|--------|-----------|------------------|
| `NOVO` | Adesão confirmada, aguardando onboarding | - |
| `ONBOARDING` | Em processo de screening e estratificação | SUB-001 |
| `ATIVO` | Onboarding concluído, perfil completo | SUB-002 (background) |
| `MONITORADO` | Sob monitoramento proativo contínuo | SUB-002 |
| `EM_ATENDIMENTO` | Com demanda em processamento | SUB-003, SUB-004/005/006 |
| `EM_JORNADA_CUIDADO` | Sob acompanhamento de navegador | SUB-007 |
| `EM_PROGRAMA_CRONICO` | Inscrito em programa de gestão | SUB-008 |
| `EM_GESTAO_INTENSIVA` | Alto risco, acompanhamento intensivo | SUB-007 + SUB-008 |
| `ENCERRADO` | Ciclo de cuidado concluído | SUB-010 |
| `INATIVO` | Contrato cancelado/suspenso | - |

---

## 4. PROCESSO ORQUESTRADOR PRINCIPAL

### 4.1 Identificação

| Atributo | Valor |
|----------|-------|
| **ID** | `PROC-ORC-001` |
| **Nome** | Orquestração do Cuidado e Experiência do Beneficiário |
| **Versão** | 2.0 |
| **Executável** | Sim |
| **History TTL** | 365 dias |

### 4.2 Objetivo

Coordenar toda a jornada do beneficiário desde a adesão até o encerramento, garantindo:

1. **Visibilidade end-to-end** de todas as interações e estados
2. **Orquestração inteligente** de subprocessos baseada em contexto
3. **Gestão de múltiplos processos simultâneos** (ex: beneficiário pode estar em SUB-002 + SUB-008)
4. **Tratamento centralizado** de eventos globais (óbito, cancelamento, fraude)
5. **Rastreabilidade completa** para auditoria e compliance

### 4.3 Fluxo Detalhado do Orquestrador

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ POOL: Orquestrador de Jornada do Beneficiário                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ══════════════════════════════════════════════════════════════════════     │
│  LANE: Gestão de Ciclo de Vida                                              │
│  ══════════════════════════════════════════════════════════════════════     │
│                                                                              │
│  ┌─────────┐                                                                 │
│  │ START   │ (Message: "BeneficiarioAdicionado")                            │
│  │ Event   │                                                                 │
│  └────┬────┘                                                                 │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────┐                                    │
│  │ [Service Task]                      │                                    │
│  │ Inicializar Contexto do Beneficiário│                                    │
│  │ • Criar registro de jornada         │                                    │
│  │ • Definir estado inicial: NOVO      │                                    │
│  │ • Carregar dados contratuais        │                                    │
│  └────────────────┬────────────────────┘                                    │
│                   │                                                          │
│                   ▼                                                          │
│  ┌─────────────────────────────────────┐                                    │
│  │ [Call Activity]                     │                                    │
│  │ Executar SUB-001: Onboarding        │                                    │
│  │ • asyncBefore: true                 │                                    │
│  │ • Aguarda conclusão                 │                                    │
│  └────────────────┬────────────────────┘                                    │
│                   │ ◄─── [Boundary Error: ONBOARDING_FAILED]                │
│                   │            │                                             │
│                   │            ▼                                             │
│                   │      [Subprocess] Tratamento Falha Onboarding           │
│                   │            │                                             │
│                   │            └──► END (Error)                              │
│                   │                                                          │
│                   ▼                                                          │
│  ┌─────────────────────────────────────┐                                    │
│  │ [Service Task]                      │                                    │
│  │ Atualizar Estado: ATIVO             │                                    │
│  │ • Registrar perfil de risco         │                                    │
│  │ • Ativar monitoramento proativo     │                                    │
│  └────────────────┬────────────────────┘                                    │
│                   │                                                          │
│                   ▼                                                          │
│  ┌─────────────────────────────────────┐                                    │
│  │ [Parallel Gateway] Fork             │                                    │
│  │ Ativar processos paralelos          │                                    │
│  └───┬─────────────────────────────┬───┘                                    │
│      │                             │                                         │
│      ▼                             ▼                                         │
│  ┌───────────────────┐    ┌───────────────────────────────────┐             │
│  │ [Signal Throw]    │    │ [Event Subprocess] (non-interrupt)│             │
│  │ Ativar SUB-002    │    │ Listener de Interações            │             │
│  │ Motor Proativo    │    │                                   │             │
│  └───────────────────┘    │  ┌─────────┐                      │             │
│                           │  │ START   │ (Message: Interação) │             │
│                           │  └────┬────┘                      │             │
│                           │       │                           │             │
│                           │       ▼                           │             │
│                           │  ┌─────────────────────┐          │             │
│                           │  │ [Call Activity]     │          │             │
│                           │  │ SUB-003: Recepção   │          │             │
│                           │  └─────────────────────┘          │             │
│                           │                                   │             │
│                           └───────────────────────────────────┘             │
│      │                                                                       │
│      ▼                                                                       │
│  ════════════════════════════════════════════════════════════════════       │
│  LANE: Gestão de Programas Especiais                                        │
│  ════════════════════════════════════════════════════════════════════       │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐        │
│  │ [Event Subprocess] (non-interrupting)                           │        │
│  │ Elegibilidade para Programa de Crônicos                         │        │
│  │                                                                  │        │
│  │  ┌─────────┐                                                    │        │
│  │  │ START   │ (Signal: "ElegivelProgramaCronico")                │        │
│  │  └────┬────┘                                                    │        │
│  │       │                                                         │        │
│  │       ▼                                                         │        │
│  │  ┌─────────────────────────────────────┐                        │        │
│  │  │ [Exclusive Gateway]                 │                        │        │
│  │  │ "Já está em programa?"              │                        │        │
│  │  └───────┬─────────────────┬───────────┘                        │        │
│  │          │                 │                                    │        │
│  │          │ NÃO             │ SIM                                │        │
│  │          ▼                 ▼                                    │        │
│  │  ┌───────────────┐   ┌───────────────┐                          │        │
│  │  │[Call Activity]│   │[Service Task] │                          │        │
│  │  │SUB-008 Crônico│   │Atualizar prog.│                          │        │
│  │  └───────────────┘   └───────────────┘                          │        │
│  │                                                                  │        │
│  └─────────────────────────────────────────────────────────────────┘        │
│                                                                              │
│  ════════════════════════════════════════════════════════════════════       │
│  LANE: Tratamento de Eventos Globais                                        │
│  ════════════════════════════════════════════════════════════════════       │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐        │
│  │ [Event Subprocess] (interrupting)                               │        │
│  │ Cancelamento de Contrato                                        │        │
│  │                                                                  │        │
│  │  ┌─────────┐                                                    │        │
│  │  │ START   │ (Message: "ContratoCancelado")                     │        │
│  │  └────┬────┘                                                    │        │
│  │       │                                                         │        │
│  │       ▼                                                         │        │
│  │  ┌─────────────────────────────────────┐                        │        │
│  │  │ [Service Task]                      │                        │        │
│  │  │ Encerrar Todos Processos Ativos     │                        │        │
│  │  │ • Cancelar SUB-002                  │                        │        │
│  │  │ • Cancelar SUB-007 se ativo         │                        │        │
│  │  │ • Cancelar SUB-008 se ativo         │                        │        │
│  │  └────────────────┬────────────────────┘                        │        │
│  │                   │                                             │        │
│  │                   ▼                                             │        │
│  │  ┌─────────────────────────────────────┐                        │        │
│  │  │ [Service Task]                      │                        │        │
│  │  │ Atualizar Estado: INATIVO           │                        │        │
│  │  └────────────────┬────────────────────┘                        │        │
│  │                   │                                             │        │
│  │                   ▼                                             │        │
│  │  ┌─────────┐                                                    │        │
│  │  │   END   │ (Terminate)                                        │        │
│  │  └─────────┘                                                    │        │
│  │                                                                  │        │
│  └─────────────────────────────────────────────────────────────────┘        │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐        │
│  │ [Event Subprocess] (interrupting)                               │        │
│  │ Óbito do Beneficiário                                           │        │
│  │                                                                  │        │
│  │  ┌─────────┐                                                    │        │
│  │  │ START   │ (Message: "ObitoBeneficiario")                     │        │
│  │  └────┬────┘                                                    │        │
│  │       │                                                         │        │
│  │       ▼                                                         │        │
│  │  ┌─────────────────────────────────────┐                        │        │
│  │  │ [Parallel Gateway] Fork             │                        │        │
│  │  └───┬─────────────────────────────┬───┘                        │        │
│  │      │                             │                            │        │
│  │      ▼                             ▼                            │        │
│  │  ┌───────────────┐    ┌───────────────────────┐                 │        │
│  │  │[Service Task] │    │[User Task]            │                 │        │
│  │  │Encerrar procs │    │Contato com família    │                 │        │
│  │  └───────────────┘    │(se apropriado)        │                 │        │
│  │                       └───────────────────────┘                 │        │
│  │      │                             │                            │        │
│  │      ▼                             ▼                            │        │
│  │  ┌─────────────────────────────────────┐                        │        │
│  │  │ [Parallel Gateway] Join             │                        │        │
│  │  └────────────────┬────────────────────┘                        │        │
│  │                   │                                             │        │
│  │                   ▼                                             │        │
│  │  ┌─────────┐                                                    │        │
│  │  │   END   │ (Terminate)                                        │        │
│  │  └─────────┘                                                    │        │
│  │                                                                  │        │
│  └─────────────────────────────────────────────────────────────────┘        │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐        │
│  │ [Event Subprocess] (interrupting)                               │        │
│  │ Detecção de Fraude                                              │        │
│  │                                                                  │        │
│  │  ┌─────────┐                                                    │        │
│  │  │ START   │ (Signal: "FraudeDetectada")                        │        │
│  │  └────┬────┘                                                    │        │
│  │       │                                                         │        │
│  │       ▼                                                         │        │
│  │  ┌─────────────────────────────────────┐                        │        │
│  │  │ [Service Task]                      │                        │        │
│  │  │ Suspender Todos Processos           │                        │        │
│  │  └────────────────┬────────────────────┘                        │        │
│  │                   │                                             │        │
│  │                   ▼                                             │        │
│  │  ┌─────────────────────────────────────┐                        │        │
│  │  │ [User Task]                         │                        │        │
│  │  │ Análise de Fraude (Compliance)      │                        │        │
│  │  │ candidateGroups: "compliance"       │                        │        │
│  │  └────────────────┬────────────────────┘                        │        │
│  │                   │                                             │        │
│  │                   ▼                                             │        │
│  │  ┌─────────────────────────────────────┐                        │        │
│  │  │ [Exclusive Gateway]                 │                        │        │
│  │  │ "Fraude confirmada?"                │                        │        │
│  │  └───────┬─────────────────┬───────────┘                        │        │
│  │          │ SIM             │ NÃO                                │        │
│  │          ▼                 ▼                                    │        │
│  │  ┌───────────────┐   ┌───────────────┐                          │        │
│  │  │END (Terminate)│   │Reativar procs │                          │        │
│  │  │+ Notif. Legal │   └───────────────┘                          │        │
│  │  └───────────────┘                                              │        │
│  │                                                                  │        │
│  └─────────────────────────────────────────────────────────────────┘        │
│                                                                              │
│  ════════════════════════════════════════════════════════════════════       │
│  LANE: Monitoramento Contínuo (Processo Principal)                          │
│  ════════════════════════════════════════════════════════════════════       │
│                                                                              │
│  ┌─────────────────────────────────────┐                                    │
│  │ [Receive Task]                      │                                    │
│  │ Aguardar Evento de Encerramento     │                                    │
│  │ • Message: "JornadaEncerrada"       │                                    │
│  │ • Correlation: beneficiarioId       │                                    │
│  └────────────────┬────────────────────┘                                    │
│                   │                                                          │
│                   ▼                                                          │
│  ┌─────────────────────────────────────┐                                    │
│  │ [Service Task]                      │                                    │
│  │ Consolidar Métricas de Jornada      │                                    │
│  │ • Tempo total                       │                                    │
│  │ • Touchpoints                       │                                    │
│  │ • Desfechos                         │                                    │
│  │ • Custo total                       │                                    │
│  └────────────────┬────────────────────┘                                    │
│                   │                                                          │
│                   ▼                                                          │
│  ┌─────────────────────────────────────┐                                    │
│  │ [Service Task]                      │                                    │
│  │ Alimentar Data Lake                 │                                    │
│  │ • Publicar evento: JornadaCompleta  │                                    │
│  └────────────────┬────────────────────┘                                    │
│                   │                                                          │
│                   ▼                                                          │
│  ┌─────────┐                                                                 │
│  │   END   │                                                                 │
│  └─────────┘                                                                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.4 Configurações Camunda do Orquestrador

```xml
<bpmn:process id="PROC-ORC-001" 
              name="Orquestração do Cuidado e Experiência do Beneficiário" 
              isExecutable="true"
              camunda:historyTimeToLive="365"
              camunda:versionTag="2.0">
  
  <!-- Job Executor Configuration -->
  <bpmn:extensionElements>
    <camunda:properties>
      <camunda:property name="jobRetryTimeCycle" value="R3/PT5M"/>
    </camunda:properties>
  </bpmn:extensionElements>
  
</bpmn:process>
```

---

## 5. MATRIZ DE INTERDEPENDÊNCIAS

### 5.1 Chamadas Entre Processos

| Processo Origem | Processo Destino | Tipo de Chamada | Condição | Dados Passados |
|-----------------|------------------|-----------------|----------|----------------|
| `PROC-ORC-001` | `SUB-001` | Call Activity (sync) | Sempre no início | beneficiarioId, contratoId |
| `PROC-ORC-001` | `SUB-002` | Signal (async) | Após onboarding OK | beneficiarioId |
| `PROC-ORC-001` | `SUB-003` | Call Activity (event subprocess) | Interação recebida | beneficiarioId, interacaoId |
| `PROC-ORC-001` | `SUB-008` | Call Activity (event subprocess) | Elegível programa | beneficiarioId, condicaoCronica |
| `SUB-002` | `SUB-007` | Signal | Risco internação >70% | beneficiarioId, alertaId |
| `SUB-002` | `SUB-008` | Signal | Gatilho crônico | beneficiarioId, gatilhoId |
| `SUB-003` | `SUB-004` | Call Activity | Roteamento self-service | demandaContext |
| `SUB-003` | `SUB-005` | Call Activity | Roteamento agente IA | demandaContext |
| `SUB-003` | `SUB-007` | Call Activity | Roteamento navegador | demandaContext |
| `SUB-003` | `SUB-009` | Call Activity | Detectada reclamação | demandaContext |
| `SUB-004` | `SUB-005` | Signal | Escalação | demandaContext |
| `SUB-005` | `SUB-006` | Call Activity | Autorização necessária | autorizacaoRequest |
| `SUB-005` | `SUB-007` | Signal | Escalação navegador | demandaContext |
| `SUB-006` | `SUB-007` | Signal | Autorização complexa | autorizacaoId |
| `SUB-007` | `SUB-006` | Call Activity | Etapa requer autorização | autorizacaoRequest |
| `SUB-007` | `SUB-010` | Call Activity | Jornada concluída | jornadaId |
| `SUB-008` | `SUB-007` | Signal | Descompensação crítica | alertaId |
| `SUB-009` | `SUB-007` | Call Activity | Resolução requer navegação | reclamacaoId |
| `SUB-010` | `SUB-009` | Call Activity | Detrator identificado | feedbackId |
| `SUB-010` | `SUB-002` | Message | Atualizar modelo ML | feedbackData |

### 5.2 Diagrama de Dependências

```
                                    ┌─────────┐
                                    │PROC-ORC │
                                    │  -001   │
                                    └────┬────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    │                    │                    │
                    ▼                    ▼                    ▼
              ┌─────────┐          ┌─────────┐          ┌─────────┐
              │ SUB-001 │────────►│ SUB-002 │          │ SUB-003 │
              │Onboarding│  signal │ Proativo│          │Recepção │
              └─────────┘          └────┬────┘          └────┬────┘
                                        │                    │
                         ┌──────────────┼──────────────┬─────┴─────┬───────────┐
                         │              │              │           │           │
                         ▼              ▼              ▼           ▼           ▼
                   ┌─────────┐   ┌─────────┐   ┌─────────┐  ┌─────────┐  ┌─────────┐
                   │ SUB-007 │◄──│ SUB-008 │   │ SUB-004 │  │ SUB-005 │  │ SUB-009 │
                   │Navegação│   │ Crônicos│   │Self-Serv│  │Agente IA│  │Reclamaç │
                   └────┬────┘   └─────────┘   └────┬────┘  └────┬────┘  └─────────┘
                        │                           │            │
                        │                           │            │
                        │              ┌────────────┴────────────┘
                        │              │
                        ▼              ▼
                   ┌─────────┐   ┌─────────┐
                   │ SUB-010 │   │ SUB-006 │
                   │Follow-up│   │Autoriz. │
                   └─────────┘   └─────────┘
```

### 5.3 Regras de Concorrência

| Cenário | Processos Simultâneos | Regra |
|---------|----------------------|-------|
| Beneficiário ativo normal | SUB-002 (background) | Sempre ativo enquanto contrato ativo |
| Beneficiário em atendimento | SUB-002 + SUB-003 + (SUB-004 ou SUB-005) | Permitido, não conflitam |
| Beneficiário em jornada de cuidado | SUB-002 + SUB-007 | Permitido, SUB-007 tem prioridade em comunicações |
| Beneficiário crônico em atendimento | SUB-002 + SUB-008 + SUB-003 | Permitido, contexto compartilhado |
| Múltiplas demandas simultâneas | SUB-003 (múltiplas instâncias) | Cada demanda é instância separada |
| Reclamação durante jornada | SUB-007 + SUB-009 | SUB-009 tem prioridade, pode pausar SUB-007 |

---

## 6. VARIÁVEIS DE PROCESSO

### 6.1 Variáveis Globais (Escopo: Orquestrador)

| Variável | Tipo | Obrigatória | Descrição | Exemplo |
|----------|------|-------------|-----------|---------|
| `beneficiarioId` | String | Sim | ID único do beneficiário (UUID) | "550e8400-e29b-41d4-a716-446655440000" |
| `contratoId` | String | Sim | ID do contrato | "CTR-2024-001234" |
| `cpf` | String | Sim | CPF do beneficiário | "12345678901" |
| `telefone` | String | Sim | Telefone principal (WhatsApp) | "5517999998888" |
| `email` | String | Não | Email do beneficiário | "joao@email.com" |
| `tipoPlano` | String | Sim | Código do plano contratado | "ENF-APARTAMENTO-SP" |
| `empresaId` | String | Não | ID da empresa (se PJ) | "EMP-001234" |
| `dataAdesao` | Date | Sim | Data de início da vigência | "2024-01-15" |
| `estadoBeneficiario` | Enum | Sim | Estado atual do ciclo de vida | "ATIVO", "EM_JORNADA_CUIDADO" |

### 6.2 Variáveis de Perfil de Saúde

| Variável | Tipo | Obrigatória | Descrição | Exemplo |
|----------|------|-------------|-----------|---------|
| `perfilSaudeCompleto` | Boolean | Sim | Screening concluído? | true |
| `scoreRisco` | Integer | Sim | Score de risco (0-100) | 73 |
| `classificacaoRisco` | Enum | Sim | Nível de risco | "ALTO" |
| `condicoesCronicas` | Array[String] | Não | Lista de condições | ["DIABETES_TIPO_2", "HIPERTENSAO"] |
| `statusCPT` | Enum | Sim | Status de CPT | "SEM_CPT", "CPT_DETECTADO" |
| `cptCondicoes` | Array[Object] | Não | Condições com CPT | [{condicao, dataDeteccao, periodo}] |
| `imc` | Decimal | Não | Índice de Massa Corporal | 28.5 |
| `tabagista` | Boolean | Não | É fumante? | false |
| `etilista` | Enum | Não | Consumo de álcool | "SOCIAL", "FREQUENTE", "NAO" |
| `praticaExercicio` | Enum | Não | Frequência de exercício | "3X_SEMANA" |
| `historicoFamiliar` | Object | Não | Doenças na família | {diabetes: true, cancer: false} |

### 6.3 Variáveis de Interação/Demanda

| Variável | Tipo | Escopo | Descrição | Exemplo |
|----------|------|--------|-----------|---------|
| `interacaoId` | String | Instância | ID único da interação | "INT-2024-123456" |
| `canalOrigem` | Enum | Instância | Canal de entrada | "WHATSAPP", "APP", "TELEFONE" |
| `mensagemOriginal` | String | Instância | Texto da mensagem | "Preciso agendar uma consulta" |
| `intencaoDetectada` | String | Instância | Intenção classificada por NLP | "AGENDAMENTO_CONSULTA" |
| `entidadesExtraidas` | Object | Instância | Entidades do NLP | {especialidade: "cardiologia"} |
| `nivelUrgencia` | Enum | Instância | Urgência classificada | "ROTINA", "URGENTE", "EMERGENCIA" |
| `complexidade` | Enum | Instância | Complexidade estimada | "BAIXA", "MEDIA", "ALTA" |
| `camadaDestino` | Enum | Instância | Para onde rotear | "SELF_SERVICE", "AGENTE_IA", "NAVEGADOR" |
| `filaDestino` | String | Instância | Fila específica | "fila_agendamento" |
| `prioridade` | Integer | Instância | Prioridade na fila (1-10) | 7 |
| `contextoEnriquecido` | Object | Instância | Contexto 360° do beneficiário | {ultimasInteracoes, autorizacoesPendentes, ...} |
| `demandaResolvida` | Boolean | Instância | Demanda foi resolvida? | true |
| `motivoEscalacao` | String | Instância | Por que escalou? | "Complexidade alta" |

### 6.4 Variáveis de Autorização

| Variável | Tipo | Escopo | Descrição | Exemplo |
|----------|------|--------|-----------|---------|
| `autorizacaoId` | String | Instância | ID da autorização | "AUT-2024-789012" |
| `guiaTISS` | Object | Instância | Dados da guia TISS | {numeroGuia, tipoGuia, ...} |
| `procedimentos` | Array[Object] | Instância | Lista de procedimentos | [{codigo, descricao, qtd}] |
| `prestadorId` | String | Instância | ID do prestador | "PREST-001234" |
| `tipoAutorizacao` | Enum | Instância | Tipo de autorização | "CONSULTA", "SADT", "INTERNACAO" |
| `statusAutorizacao` | Enum | Instância | Status atual | "PENDENTE", "APROVADA", "NEGADA" |
| `requerAuditoria` | Boolean | Instância | Precisa auditor médico? | true |
| `justificativaNegativa` | String | Instância | Motivo da negativa | "Procedimento não coberto" |
| `numeroAutorizacao` | String | Instância | Número gerado | "123456789012345" |
| `validadeAutorizacao` | Date | Instância | Data limite | "2024-03-15" |

### 6.5 Variáveis de Navegação/Jornada

| Variável | Tipo | Escopo | Descrição | Exemplo |
|----------|------|--------|-----------|---------|
| `jornadaId` | String | Instância | ID da jornada de cuidado | "JOR-2024-345678" |
| `navegadorId` | String | Instância | ID do navegador atribuído | "NAV-001" |
| `planoCuidados` | Object | Instância | Plano de cuidados ativo | {etapas: [...], metas: [...]} |
| `etapaAtual` | Integer | Instância | Índice da etapa atual | 2 |
| `statusJornada` | Enum | Instância | Status da jornada | "EM_ANDAMENTO", "CONCLUIDA", "ABANDONADA" |
| `proximaAcao` | Object | Instância | Próxima ação planejada | {tipo, data, descricao} |
| `agendamentosAtivos` | Array[Object] | Instância | Agendamentos futuros | [{data, prestador, tipo}] |

### 6.6 Variáveis de Programa de Crônicos

| Variável | Tipo | Escopo | Descrição | Exemplo |
|----------|------|--------|-----------|---------|
| `programaId` | String | Instância | ID do programa | "PROG-DM2-001234" |
| `tipoPrograma` | Enum | Instância | Tipo de programa | "DIABETES", "HIPERTENSAO", "DPOC" |
| `dataInscricao` | Date | Instância | Data de inscrição | "2024-02-01" |
| `metasTerapeuticas` | Object | Instância | Metas definidas | {hba1c: "<7%", pa: "<140/90"} |
| `ultimosMarcadores` | Object | Instância | Últimos valores | {hba1c: "7.2%", data: "2024-02-15"} |
| `taxaAdesao` | Decimal | Instância | % de adesão ao programa | 0.85 |
| `frequenciaContato` | Enum | Instância | Frequência de contato | "SEMANAL", "QUINZENAL" |

### 6.7 Mapeamento de Variáveis em Call Activities

```xml
<!-- Exemplo: Chamada do SUB-003 pelo Orquestrador -->
<bpmn:callActivity id="CallActivity_SUB003" name="Recepção e Classificação" calledElement="SUB-003">
  <bpmn:extensionElements>
    <camunda:in source="beneficiarioId" target="beneficiarioId"/>
    <camunda:in source="interacaoId" target="interacaoId"/>
    <camunda:in source="canalOrigem" target="canalOrigem"/>
    <camunda:in source="mensagemOriginal" target="mensagemOriginal"/>
    <camunda:in source="contextoEnriquecido" target="contextoEnriquecido"/>
    <camunda:out source="demandaResolvida" target="demandaResolvida"/>
    <camunda:out source="camadaDestino" target="camadaDestino"/>
    <camunda:out source="resultadoAtendimento" target="resultadoAtendimento"/>
  </bpmn:extensionElements>
</bpmn:callActivity>
```


---

## 7. ESPECIFICAÇÕES TÉCNICAS BPMN/CAMUNDA 7

### 7.1 Pools e Lanes Padronizados

#### Pool Principal: Operadora de Saúde Digital

```
Pool: "Operadora de Saúde Digital"
├── Lane: Beneficiário
│   └── Atividades do beneficiário (receber mensagens, responder, tomar decisões)
│
├── Lane: Canal Digital
│   └── WhatsApp Business API, App Mobile, Portal Web, Voice AI
│
├── Lane: Central de Experiência
│   └── Atendentes humanos nível 1, gestão de filas
│
├── Lane: Agentes IA
│   └── GPT-4 fine-tuned, BERT, agentes especializados
│
├── Lane: Coordenação do Cuidado
│   └── Navegadores, gestão de casos
│
├── Lane: Enfermeiras Navegadoras
│   └── Acompanhamento clínico, educação em saúde
│
├── Lane: Auditoria Médica
│   └── Médicos auditores, protocolos clínicos
│
├── Lane: Back-office
│   └── Financeiro, regulação, jurídico
│
└── Lane: Sistemas
    └── Tasy ERP, Camunda, RPA, Data Lake
```

#### Pool Externo: Rede de Prestadores

```
Pool: "Rede de Prestadores"
├── Lane: Hospitais
├── Lane: Clínicas e Consultórios
├── Lane: Laboratórios e Diagnóstico
└── Lane: Profissionais de Saúde
```

#### Pool Externo: Entidades Externas

```
Pool: "Entidades Externas"
├── Lane: ANS (Agência Nacional de Saúde)
├── Lane: Órgãos de Defesa do Consumidor
└── Lane: Parceiros Tecnológicos
```

### 7.2 Catálogo de Eventos

#### Start Events

| ID | Tipo | Nome | Uso | Message/Signal/Timer |
|----|------|------|-----|---------------------|
| `StartEvent_None` | None | Início Padrão | Início manual ou teste | - |
| `StartEvent_Msg_NovaAdesao` | Message | Nova Adesão Confirmada | Trigger de onboarding | `Msg_NovaAdesao` |
| `StartEvent_Msg_Interacao` | Message | Interação Recebida | Entrada omnichannel | `Msg_InteracaoRecebida` |
| `StartEvent_Msg_Autorizacao` | Message | Solicitação Autorização | Pedido TISS | `Msg_SolicitacaoAutorizacao` |
| `StartEvent_Msg_Reclamacao` | Message | Reclamação Recebida | Entrada de reclamação | `Msg_ReclamacaoRecebida` |
| `StartEvent_Timer_Diario` | Timer | Execução Diária | Batch proativo | `R/P1D` (cron: 0 6 * * *) |
| `StartEvent_Timer_Semanal` | Timer | Execução Semanal | NPS periódico | `R/P7D` |
| `StartEvent_Signal_Elegivel` | Signal | Elegível Programa | Trigger programa crônicos | `Signal_ElegivelPrograma` |
| `StartEvent_Signal_Roteado` | Signal | Roteado para Camada | Pós-classificação | `Signal_RoteadoXXX` |

#### Intermediate Events

| ID | Tipo | Nome | Uso | Configuração |
|----|------|------|-----|--------------|
| `IntCatch_Timer_SLA` | Timer (Catch) | Aguardar SLA | Timeout de resposta | Variável: `${slaMinutos}` |
| `IntCatch_Timer_24h` | Timer (Catch) | Aguardar 24 horas | Follow-up pós-consulta | `PT24H` |
| `IntCatch_Timer_Vespera` | Timer (Catch) | Véspera Agendamento | Lembrete | `${dataAgendamento - P1D}` |
| `IntCatch_Msg_Resposta` | Message (Catch) | Aguardar Resposta | Resposta do beneficiário | Correlation: `beneficiarioId` |
| `IntCatch_Msg_Resultado` | Message (Catch) | Resultado Disponível | Resultado de exame | Correlation: `exameId` |
| `IntThrow_Msg_Notificacao` | Message (Throw) | Enviar Notificação | Disparo de mensagem | Target: Canal Digital |
| `IntThrow_Signal_Escalar` | Signal (Throw) | Escalar Demanda | Escalação entre camadas | `Signal_Escalacao` |
| `IntThrow_Signal_Alerta` | Signal (Throw) | Alerta Alto Risco | Notificar navegador | `Signal_AlertaAltoRisco` |

#### Boundary Events

| ID | Tipo | Attachable To | Interrupting | Configuração |
|----|------|---------------|--------------|--------------|
| `Boundary_Timer_Timeout` | Timer | Receive Task | Sim | Variável: `${timeoutMinutos}` |
| `Boundary_Timer_SLA` | Timer | User Task | Não | Variável: `${slaMinutos}` |
| `Boundary_Error_Integracao` | Error | Service Task | Sim | Error Code: `ERR_INTEGRACAO` |
| `Boundary_Error_Validacao` | Error | Service Task | Sim | Error Code: `ERR_VALIDACAO` |
| `Boundary_Msg_Cancelamento` | Message | Call Activity | Sim | `Msg_CancelarProcesso` |
| `Boundary_Signal_Prioridade` | Signal | Subprocess | Não | `Signal_AumentarPrioridade` |

#### End Events

| ID | Tipo | Nome | Uso |
|----|------|------|-----|
| `EndEvent_None` | None | Fim Normal | Conclusão padrão |
| `EndEvent_Msg_Notificacao` | Message | Fim com Notificação | Conclusão + mensagem |
| `EndEvent_Terminate` | Terminate | Cancelar Processo | Interrupção forçada |
| `EndEvent_Error` | Error | Fim com Erro | Erro não recuperável |
| `EndEvent_Escalation` | Escalation | Escalar para Pai | Notificar processo pai |

### 7.3 Catálogo de Tarefas

#### User Tasks

| ID | Nome | Candidate Groups | Form Key | SLA |
|----|------|------------------|----------|-----|
| `UserTask_AnalisarCaso` | Analisar Caso Complexo | navegadores | `form_analise_caso` | 4h |
| `UserTask_AuditoriaMedica` | Análise Médica | auditores_medicos | `form_auditoria` | 24h |
| `UserTask_TratarReclamacao` | Tratar Reclamação | ouvidoria | `form_reclamacao` | 72h |
| `UserTask_ContatoTelefonico` | Realizar Contato Telefônico | atendimento | `form_contato` | 2h |
| `UserTask_ValidarCPT` | Validar CPT | auditores_medicos | `form_cpt` | 48h |
| `UserTask_AprovarCompensacao` | Aprovar Compensação | gestores | `form_compensacao` | 24h |

#### Service Tasks

| ID | Nome | Implementation | Delegate Expression | Retry |
|----|------|----------------|--------------------|----|
| `ServiceTask_BuscarBeneficiario` | Buscar Beneficiário | Delegate | `${tasyBeneficiarioService}` | R3/PT1M |
| `ServiceTask_ClassificarIntencao` | Classificar Intenção (NLP) | External | topic: `nlp-classificacao` | R3/PT30S |
| `ServiceTask_GerarAutorizacao` | Gerar Autorização | Delegate | `${autorizacaoService}` | R3/PT1M |
| `ServiceTask_EnviarWhatsApp` | Enviar WhatsApp | External | topic: `whatsapp-sender` | R5/PT10S |
| `ServiceTask_ProcessarOCR` | Processar Documento OCR | External | topic: `ocr-processor` | R3/PT30S |
| `ServiceTask_CalcularRisco` | Calcular Score de Risco | Delegate | `${riscoCalculatorService}` | R3/PT1M |
| `ServiceTask_PublicarEvento` | Publicar Evento Kafka | Delegate | `${kafkaPublisherService}` | R5/PT5S |

#### Business Rule Tasks

| ID | Nome | Decision Ref | Result Variable |
|----|------|--------------|-----------------|
| `BRTask_ClassificarUrgencia` | Classificar Urgência | `DMN_ClassificarUrgencia` | `nivelUrgencia` |
| `BRTask_DefinirRoteamento` | Definir Roteamento | `DMN_DefinirRoteamento` | `camadaDestino` |
| `BRTask_RegrasAutorizacao` | Regras de Autorização | `DMN_RegrasAutorizacao` | `decisaoAutorizacao` |
| `BRTask_ProtocoloClinico` | Protocolo Clínico | `DMN_ProtocoloClinico` | `atendeProtocolo` |
| `BRTask_EstrategificarRisco` | Estratificar Risco | `DMN_EstratificacaoRisco` | `classificacaoRisco` |
| `BRTask_IdentificarGatilhos` | Identificar Gatilhos | `DMN_GatilhosProativos` | `gatilhosAtivados` |

#### Send/Receive Tasks

| ID | Tipo | Nome | Message | Correlation |
|----|------|------|---------|-------------|
| `SendTask_BoasVindas` | Send | Enviar Boas-vindas | `Msg_BoasVindas` | - |
| `SendTask_Lembrete` | Send | Enviar Lembrete | `Msg_Lembrete` | - |
| `ReceiveTask_RespostaBenef` | Receive | Aguardar Resposta | `Msg_RespostaBeneficiario` | `beneficiarioId` |
| `ReceiveTask_DocUpload` | Receive | Aguardar Upload Doc | `Msg_DocumentoUpload` | `beneficiarioId` |

### 7.4 Gateways

| Tipo | Símbolo | Quando Usar | Exemplo |
|------|---------|-------------|---------|
| **Exclusive (XOR)** | ◇ | Uma única saída baseada em condição | "Risco Alto ou Baixo?" |
| **Inclusive (OR)** | ◇ (círculo interno) | Múltiplas saídas possíveis | "Quais alertas enviar?" |
| **Parallel (AND)** | ◇ (+ interno) | Todas as saídas executam | "Validar E notificar" |
| **Event-Based** | ◇ (pentágono interno) | Aguardar primeiro evento | "Resposta OU timeout?" |
| **Complex** | ◇ (* interno) | Lógica N de M | "2 de 3 aprovações" |

### 7.5 Artefatos e Dados

#### Data Objects

| ID | Nome | Tipo | Descrição |
|----|------|------|-----------|
| `DataObject_FormularioScreening` | Formulário de Screening | JSON | Respostas do screening |
| `DataObject_GuiaTISS` | Guia TISS | XML | Guia de autorização |
| `DataObject_DossieBeneficiario` | Dossiê do Beneficiário | JSON | Contexto 360° |
| `DataObject_PlanoCuidados` | Plano de Cuidados | JSON | Plano individualizado |
| `DataObject_RelatorioNPS` | Relatório NPS | JSON | Resultados de pesquisa |

#### Data Stores

| ID | Nome | Tipo | Sistema |
|----|------|------|---------|
| `DataStore_Tasy` | Tasy ERP | Database | PostgreSQL |
| `DataStore_CRM` | CRM | Database | Salesforce/HubSpot |
| `DataStore_DataLake` | Data Lake | Object Storage | Delta Lake |
| `DataStore_Redis` | Cache | Key-Value | Redis |
| `DataStore_ElasticSearch` | Busca | Search Engine | ElasticSearch |

---

## 8. DETALHAMENTO DOS 10 SUBPROCESSOS

### SUB-001: Onboarding Inteligente e Screening de Saúde

#### Ficha Técnica

| Atributo | Valor |
|----------|-------|
| **ID** | `SUB-001_Onboarding_Screening` |
| **Nome** | Onboarding Inteligente e Screening de Saúde |
| **Versão** | 2.0 |
| **Owner** | Coordenador de Onboarding |
| **SLA Global** | 7 dias para conclusão completa |

#### Triggers

| Tipo | Nome | Origem | Dados Recebidos |
|------|------|--------|-----------------|
| Message | Nova Adesão Confirmada | Sistema de Vendas | beneficiarioId, contratoId, dadosCadastrais |

#### Entradas

| Dado | Tipo | Obrigatório | Fonte |
|------|------|-------------|-------|
| Dados cadastrais | Object | Sim | Sistema de Vendas |
| Tipo de plano | String | Sim | Contrato |
| Dados empresa (se PJ) | Object | Não | Contrato |
| Documentos enviados | Array[File] | Não | Upload beneficiário |

#### Saídas

| Dado | Tipo | Destino | Uso |
|------|------|---------|-----|
| Perfil de saúde completo | Object | Data Lake | Estratificação |
| Score de risco (0-100) | Integer | Variável processo | Roteamento |
| Classificação de risco | Enum | Variável processo | Programas |
| Status CPT | Enum | Tasy | Cobertura |
| Plano de cuidados inicial | Object | Data Lake | Navegação |

#### Fluxo Detalhado

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ SUB-001: Onboarding Inteligente e Screening de Saúde                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐                                                            │
│  │   START     │ (Message: "NovaAdesaoConfirmada")                          │
│  │   Event     │ Correlation: beneficiarioId                                │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Criar Registro no Tasy                               │    │
│  │ • delegateExpression: ${tasyBeneficiarioService.criar}              │    │
│  │ • Input: dadosCadastrais                                            │    │
│  │ • Output: beneficiarioTasyId                                        │    │
│  │ • Retry: R3/PT1M                                                    │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               │ ◄─── [Boundary Error: ERR_TASY_INDISPONIVEL]│
│                               │            │                                 │
│                               │            ▼                                 │
│                               │      [Subprocess] Retry com Backoff         │
│                               │            │                                 │
│                               │            └──► [Error End] se falhar 3x    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Preparar Template de Boas-vindas                     │    │
│  │ • delegateExpression: ${templateService.prepararBoasVindas}         │    │
│  │ • Input: nome, plano, empresa                                       │    │
│  │ • Output: mensagemPersonalizada                                     │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Send Task] Enviar Boas-vindas via WhatsApp                         │    │
│  │ • Message: Msg_BoasVindas                                           │    │
│  │ • Implementation: External Task (topic: whatsapp-sender)            │    │
│  │ • Template: HSM aprovado "boas_vindas_v2"                           │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Receive Task] Aguardar Aceite do Beneficiário                      │    │
│  │ • Message: Msg_AceiteBeneficiario                                   │    │
│  │ • Correlation: beneficiarioId + telefone                            │    │
│  │                                                                      │    │
│  │ ◄─── [Boundary Timer: 24h] (non-interrupting)                       │    │
│  │            │                                                         │    │
│  │            ▼                                                         │    │
│  │      ┌─────────────────────────────────────────────────────────┐    │    │
│  │      │ [Send Task] Reenviar Convite (1ª tentativa)             │    │    │
│  │      │ • Template: "lembrete_onboarding_v1"                    │    │    │
│  │      └──────────────────────┬──────────────────────────────────┘    │    │
│  │                             │                                       │    │
│  │                             ▼                                       │    │
│  │      ┌─────────────────────────────────────────────────────────┐    │    │
│  │      │ [Receive Task] Aguardar Aceite (2ª tentativa)           │    │    │
│  │      │                                                          │    │    │
│  │      │ ◄─── [Boundary Timer: 48h] (non-interrupting)           │    │    │
│  │      │            │                                             │    │    │
│  │      │            ▼                                             │    │    │
│  │      │      ┌───────────────────────────────────────────────┐  │    │    │
│  │      │      │ [User Task] Contato Telefônico                │  │    │    │
│  │      │      │ • candidateGroups: "atendimento_ativo"        │  │    │    │
│  │      │      │ • SLA: 4h                                     │  │    │    │
│  │      │      │ • formKey: "form_contato_onboarding"          │  │    │    │
│  │      │      └───────────────────────────────────────────────┘  │    │    │
│  │      │                                                          │    │    │
│  │      │ ◄─── [Boundary Timer: 7 dias] (interrupting)            │    │    │
│  │      │            │                                             │    │    │
│  │      │            ▼                                             │    │    │
│  │      │      [Service Task] Marcar como "Onboarding Incompleto" │    │    │
│  │      │            │                                             │    │    │
│  │      │            └──► [End Event] Onboarding Não Concluído    │    │    │
│  │      │                                                          │    │    │
│  │      └──────────────────────────────────────────────────────────┘    │    │
│  │                                                                      │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Subprocess] Screening Conversacional Gamificado                    │    │
│  │ • Multi-instance: Sequential (5 módulos)                           │    │
│  │                                                                      │    │
│  │  ┌────────────────────────────────────────────────────────────┐     │    │
│  │  │ Para cada módulo (1 a 5):                                  │     │    │
│  │  │                                                             │     │    │
│  │  │  ┌─────────────────────────────────────────────────────┐   │     │    │
│  │  │  │ [Service Task] Enviar Perguntas do Módulo           │   │     │    │
│  │  │  │ • Módulo 1: Dados Biométricos (peso, altura)        │   │     │    │
│  │  │  │ • Módulo 2: Hábitos de Vida                         │   │     │    │
│  │  │  │ • Módulo 3: Histórico Médico                        │   │     │    │
│  │  │  │ • Módulo 4: Medicamentos em Uso                     │   │     │    │
│  │  │  │ • Módulo 5: Histórico Familiar                      │   │     │    │
│  │  │  └────────────────────────┬────────────────────────────┘   │     │    │
│  │  │                           │                                 │     │    │
│  │  │                           ▼                                 │     │    │
│  │  │  ┌─────────────────────────────────────────────────────┐   │     │    │
│  │  │  │ [Receive Task] Capturar Respostas                   │   │     │    │
│  │  │  │ • Timeout: 30 min por módulo                        │   │     │    │
│  │  │  │ • Permite respostas parciais                        │   │     │    │
│  │  │  └────────────────────────┬────────────────────────────┘   │     │    │
│  │  │                           │                                 │     │    │
│  │  │                           ▼                                 │     │    │
│  │  │  ┌─────────────────────────────────────────────────────┐   │     │    │
│  │  │  │ [Service Task] Validar e Persistir Respostas        │   │     │    │
│  │  │  │ • Validação de formato e range                      │   │     │    │
│  │  │  │ • Cálculo de BMI se módulo 1                        │   │     │    │
│  │  │  │ • Gamificação: pontos por módulo completo           │   │     │    │
│  │  │  └─────────────────────────────────────────────────────┘   │     │    │
│  │  │                                                             │     │    │
│  │  └────────────────────────────────────────────────────────────┘     │    │
│  │                                                                      │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Parallel Gateway] Fork - Análises Simultâneas                      │    │
│  └───┬─────────────────────────────────────────────┬───────────────────┘    │
│      │                                             │                         │
│      ▼                                             ▼                         │
│  ┌───────────────────────────────┐    ┌───────────────────────────────────┐ │
│  │ [Service Task]                │    │ [Subprocess] Processamento de     │ │
│  │ Analisar Respostas com NLP    │    │ Documentos                        │ │
│  │ • External: nlp-health-analyzer│   │                                   │ │
│  │ • Input: respostasScreening   │    │  ┌─────────────────────────────┐  │ │
│  │ • Output: condicoesDetectadas │    │  │ [Receive Task] Aguardar    │  │ │
│  │           sintomasRelatados   │    │  │ Upload de Documentos       │  │ │
│  │           flagsAlerta         │    │  │ • Timeout: 7 dias          │  │ │
│  └───────────────────────────────┘    │  └──────────────┬──────────────┘  │ │
│      │                                │                 │                  │ │
│      │                                │                 ▼                  │ │
│      │                                │  ┌─────────────────────────────┐  │ │
│      │                                │  │ [Service Task] Processar   │  │ │
│      │                                │  │ com OCR + Computer Vision  │  │ │
│      │                                │  │ • External: ocr-processor  │  │ │
│      │                                │  │ • Extrai: exames, laudos   │  │ │
│      │                                │  └─────────────────────────────┘  │ │
│      │                                │                                   │ │
│      │                                └───────────────────────────────────┘ │
│      │                                             │                         │
│      ▼                                             ▼                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Parallel Gateway] Join                                             │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Business Rule Task] Estratificação de Risco                        │    │
│  │ • DMN: DMN_EstratificacaoRisco                                      │    │
│  │ • Inputs: bmi, idade, comorbidades, historicoFamiliar, habitos      │    │
│  │ • Outputs: scoreRisco (0-100), classificacaoRisco (BAIXO/MOD/ALTO/COMPL)│
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Business Rule Task] Detecção de CPT                                │    │
│  │ • DMN: DMN_DeteccaoCPT                                              │    │
│  │ • Inputs: condicoesDetectadas, examesProcessados, scoreComportamental│   │
│  │ • Outputs: statusCPT, condicoesSuspeitas[], nivelConfianca          │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "CPT Detectado com Alta Confiança?"             │    │
│  └───────┬─────────────────────────────────────────┬───────────────────┘    │
│          │                                         │                         │
│          │ SIM (confiança > 80%)                   │ NÃO                     │
│          ▼                                         │                         │
│  ┌───────────────────────────────────┐             │                         │
│  │ [Subprocess] Investigação de CPT  │             │                         │
│  │                                    │             │                         │
│  │  ┌────────────────────────────┐   │             │                         │
│  │  │ [User Task] Validação      │   │             │                         │
│  │  │ Médica de CPT              │   │             │                         │
│  │  │ • candidateGroups:         │   │             │                         │
│  │  │   auditores_medicos        │   │             │                         │
│  │  │ • SLA: 48h                 │   │             │                         │
│  │  └──────────────┬─────────────┘   │             │                         │
│  │                 │                  │             │                         │
│  │                 ▼                  │             │                         │
│  │  ┌────────────────────────────┐   │             │                         │
│  │  │ [Exclusive Gateway]        │   │             │                         │
│  │  │ "CPT Confirmado?"          │   │             │                         │
│  │  └─────┬──────────────┬───────┘   │             │                         │
│  │        │ SIM          │ NÃO       │             │                         │
│  │        ▼              ▼           │             │                         │
│  │  ┌───────────┐  ┌───────────┐     │             │                         │
│  │  │ [Service] │  │ [Service] │     │             │                         │
│  │  │ Aplicar   │  │ Liberar   │     │             │                         │
│  │  │ CPT       │  │ Cobertura │     │             │                         │
│  │  │ Contrato  │  │ Total     │     │             │                         │
│  │  └───────────┘  └───────────┘     │             │                         │
│  │                                    │             │                         │
│  └───────────────────────────────────┘             │                         │
│          │                                         │                         │
│          └─────────────────────────────────────────┘                         │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Criar Plano de Cuidados Inicial                      │    │
│  │ • delegateExpression: ${planoCuidadosService.criar}                 │    │
│  │ • Baseado em: classificacaoRisco, condicoesCronicas                 │    │
│  │ • Define: frequenciaContato, protocolos, metas                      │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Registrar Perfil Completo no Data Lake               │    │
│  │ • delegateExpression: ${dataLakeService.registrarPerfil}            │    │
│  │ • Publica evento: BeneficiarioPerfilCompleto                        │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Send Task] Enviar Resumo de Boas-vindas + Próximos Passos          │    │
│  │ • Template: "resumo_onboarding_v2"                                  │    │
│  │ • Conteúdo personalizado por classificação de risco                 │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Elegível para Programa de Crônicos?"           │    │
│  └───────┬─────────────────────────────────────────┬───────────────────┘    │
│          │ SIM                                     │ NÃO                     │
│          ▼                                         │                         │
│  ┌───────────────────────────────────┐             │                         │
│  │ [Signal Throw]                    │             │                         │
│  │ Signal: "ElegivelProgramaCronico" │             │                         │
│  │ • beneficiarioId                  │             │                         │
│  │ • condicoesCronicas               │             │                         │
│  └───────────────────────────────────┘             │                         │
│          │                                         │                         │
│          └─────────────────────────────────────────┘                         │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Signal Throw] "OnboardingConcluido"                                │    │
│  │ • Ativa SUB-002 (Motor Proativo)                                    │    │
│  │ • Atualiza estado: ATIVO                                            │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────┐                                                            │
│  │    END      │ (Message: "OnboardingCompletoBeneficiario")               │
│  │   Event     │                                                            │
│  └─────────────┘                                                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Regras de Negócio (DMN)

**DMN_EstratificacaoRisco:**

| Input | Tipo | Descrição |
|-------|------|-----------|
| `idade` | Integer | Idade em anos |
| `bmi` | Decimal | Índice de Massa Corporal |
| `qtdComorbidades` | Integer | Quantidade de condições crônicas |
| `historicoFamiliarPositivo` | Boolean | Histórico familiar de risco |
| `tabagista` | Boolean | É fumante |
| `sedentario` | Boolean | Não pratica exercícios |
| `scoreComportamental` | Integer | Score baseado no padrão de respostas |

| Output | Tipo | Valores |
|--------|------|---------|
| `scoreRisco` | Integer | 0-100 |
| `classificacaoRisco` | String | BAIXO (0-25), MODERADO (26-50), ALTO (51-75), COMPLEXO (76-100) |

**Tabela de Decisão (Hit Policy: Collect Sum):**

| idade | bmi | qtdComorbidades | historicoFamiliar | tabagista | sedentario | scoreRisco (parcial) |
|-------|-----|-----------------|-------------------|-----------|------------|---------------------|
| <30 | - | - | - | - | - | 0 |
| 30-50 | - | - | - | - | - | +10 |
| 51-65 | - | - | - | - | - | +20 |
| >65 | - | - | - | - | - | +30 |
| - | <25 | - | - | - | - | 0 |
| - | 25-30 | - | - | - | - | +10 |
| - | >30 | - | - | - | - | +20 |
| - | - | 0 | - | - | - | 0 |
| - | - | 1-2 | - | - | - | +15 |
| - | - | >=3 | - | - | - | +30 |
| - | - | - | true | - | - | +10 |
| - | - | - | - | true | - | +15 |
| - | - | - | - | - | true | +5 |

**DMN_DeteccaoCPT:**

| Input | Tipo |
|-------|------|
| `condicoesDetectadas` | List<String> |
| `exameAlteradoRecente` | Boolean |
| `medicamentoUsoRegular` | Boolean |
| `scoreComportamental` | Integer |
| `tempoDesdeUltimoExame` | Integer (dias) |

| Output | Tipo | Valores |
|--------|------|---------|
| `statusCPT` | String | SEM_CPT, SUSPEITA_BAIXA, SUSPEITA_ALTA, CPT_PROVAVEL |
| `condicoesSuspeitas` | List<String> | Lista de CIDs suspeitos |
| `nivelConfianca` | Integer | 0-100 |

#### SLAs

| Etapa | SLA | Escalação |
|-------|-----|-----------|
| Início do screening após adesão | 24h | Alerta coordenador |
| Conclusão do screening | 7 dias | Contato telefônico |
| Validação de CPT | 48h | Alerta gerente médico |
| Onboarding completo | 10 dias | Relatório executivo |

#### Métricas

| Métrica | Descrição | Meta |
|---------|-----------|------|
| `taxa_conclusao_onboarding` | % que completam onboarding | >85% |
| `tempo_medio_onboarding` | Tempo médio de conclusão | <5 dias |
| `taxa_screening_completo` | % com 5 módulos respondidos | >70% |
| `taxa_upload_documentos` | % que enviam documentos | >40% |
| `taxa_deteccao_cpt` | % com CPT detectado | Benchmark |


### SUB-002: Motor Proativo de Antecipação

#### Ficha Técnica

| Atributo | Valor |
|----------|-------|
| **ID** | `SUB-002_Motor_Proativo` |
| **Nome** | Motor Proativo de Antecipação |
| **Versão** | 2.0 |
| **Owner** | Coordenador de Proatividade |
| **Execução** | Batch diário 06:00 + Eventos em tempo real |

#### Triggers

| Tipo | Nome | Frequência | Dados |
|------|------|------------|-------|
| Timer | Execução Diária | Cron: 0 6 * * * | Toda base ativa |
| Signal | Onboarding Concluído | Evento | beneficiarioId |
| Message | Evento de Saúde | Tempo real | beneficiarioId, eventoTipo, dados |

#### Catálogo de Gatilhos Proativos

| ID | Gatilho | Condição | Ação | Prioridade |
|----|---------|----------|------|------------|
| `GAT-001` | Check-up Pendente | Último check-up > 12 meses | Lembrete + oferta agendamento | Média |
| `GAT-002` | Medicamento Acabando | Dias restantes < 7 | Lembrete + renovação receita | Alta |
| `GAT-003` | Exame Alterado sem Retorno | Resultado alterado + sem consulta agendada | Alerta + agendamento | Alta |
| `GAT-004` | Risco de Internação | Score predição > 70% | Alerta navegador + contato proativo | Crítica |
| `GAT-005` | Baixa Adesão Tratamento | Taxa adesão < 60% | Nudge comportamental | Média |
| `GAT-006` | Campanha Sazonal | Elegível + não participou | Convite campanha (vacinação, etc.) | Baixa |
| `GAT-007` | Aniversário de Adesão | Data = aniversário | Mensagem + oferta check-up | Baixa |
| `GAT-008` | Gap in Care | Condição crônica + sem consulta > 90 dias | Alerta + agendamento | Alta |
| `GAT-009` | Pós-Alta Hospitalar | Alta < 30 dias + sem follow-up | Contato navegador | Alta |
| `GAT-010` | Gestante sem Pré-natal | Gestante + sem consulta agendada | Alerta urgente | Crítica |
| `GAT-011` | Insatisfação Detectada | Sentimento negativo em interações recentes | Contato recuperação | Alta |
| `GAT-012` | Vencimento Carência | Carência vence em 7 dias | Informar cobertura liberada | Média |

#### Fluxo Detalhado

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ SUB-002: Motor Proativo de Antecipação                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ══════════════════════════════════════════════════════════════════════     │
│  FLUXO 1: EXECUÇÃO BATCH DIÁRIA                                             │
│  ══════════════════════════════════════════════════════════════════════     │
│                                                                              │
│  ┌─────────────┐                                                            │
│  │   START     │ (Timer: "0 6 * * *" - Todo dia às 06:00)                   │
│  │   Event     │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Carregar Base de Beneficiários Ativos                │    │
│  │ • Query: estado IN ('ATIVO', 'MONITORADO', 'EM_PROGRAMA_CRONICO')   │    │
│  │ • Paginação: 1000 por batch                                         │    │
│  │ • Output: listaBeneficiarios                                        │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Subprocess] Multi-Instance Paralelo                                │    │
│  │ • Collection: ${listaBeneficiarios}                                 │    │
│  │ • Element Variable: beneficiario                                    │    │
│  │ • Parallel: true (max 50 concurrent)                                │    │
│  │                                                                      │    │
│  │  ┌────────────────────────────────────────────────────────────────┐ │    │
│  │  │ Para cada beneficiário:                                        │ │    │
│  │  │                                                                 │ │    │
│  │  │  ┌─────────────────────────────────────────────────────────┐  │ │    │
│  │  │  │ [Service Task] Coletar Dados Atualizados                │  │ │    │
│  │  │  │ • Histórico de utilizações (últimos 90 dias)            │  │ │    │
│  │  │  │ • Medicamentos ativos                                    │  │ │    │
│  │  │  │ • Exames recentes                                        │  │ │    │
│  │  │  │ • Agendamentos futuros                                   │  │ │    │
│  │  │  │ • Dados de wearables (se disponível)                     │  │ │    │
│  │  │  │ • Histórico de interações                                │  │ │    │
│  │  │  └──────────────────────────┬──────────────────────────────┘  │ │    │
│  │  │                              │                                 │ │    │
│  │  │                              ▼                                 │ │    │
│  │  │  ┌─────────────────────────────────────────────────────────┐  │ │    │
│  │  │  │ [Business Rule Task] Identificar Gatilhos               │  │ │    │
│  │  │  │ • DMN: DMN_GatilhosProativos                            │  │ │    │
│  │  │  │ • Output: gatilhosAtivados[]                            │  │ │    │
│  │  │  └──────────────────────────┬──────────────────────────────┘  │ │    │
│  │  │                              │                                 │ │    │
│  │  │                              ▼                                 │ │    │
│  │  │  ┌─────────────────────────────────────────────────────────┐  │ │    │
│  │  │  │ [Exclusive Gateway] "Há gatilhos ativados?"             │  │ │    │
│  │  │  └───────┬───────────────────────────────┬─────────────────┘  │ │    │
│  │  │          │ SIM                           │ NÃO                 │ │    │
│  │  │          ▼                               ▼                     │ │    │
│  │  │  ┌───────────────────────────┐   ┌───────────────────────┐    │ │    │
│  │  │  │ [Inclusive Gateway]       │   │ [Service Task]        │    │ │    │
│  │  │  │ Fork por tipo de gatilho  │   │ Registrar "Sem Ação"  │    │ │    │
│  │  │  └───┬───┬───┬───┬───┬───┬───┘   └───────────────────────┘    │ │    │
│  │  │      │   │   │   │   │   │                                     │ │    │
│  │  │      │   │   │   │   │   └──► GAT-012: Carência               │ │    │
│  │  │      │   │   │   │   └──────► GAT-010/011: Críticos           │ │    │
│  │  │      │   │   │   └──────────► GAT-006/007: Campanhas          │ │    │
│  │  │      │   │   └──────────────► GAT-005: Adesão                 │ │    │
│  │  │      │   └──────────────────► GAT-003/004/008/009: Alertas    │ │    │
│  │  │      └──────────────────────► GAT-001/002: Lembretes          │ │    │
│  │  │                                                                │ │    │
│  │  │      [Para cada gatilho ativado, executa Call Activity        │ │    │
│  │  │       correspondente - detalhado abaixo]                       │ │    │
│  │  │                                                                │ │    │
│  │  │  ┌─────────────────────────────────────────────────────────┐  │ │    │
│  │  │  │ [Inclusive Gateway] Join                                │  │ │    │
│  │  │  └──────────────────────────┬──────────────────────────────┘  │ │    │
│  │  │                              │                                 │ │    │
│  │  │                              ▼                                 │ │    │
│  │  │  ┌─────────────────────────────────────────────────────────┐  │ │    │
│  │  │  │ [Service Task] Registrar Ações Executadas               │  │ │    │
│  │  │  │ • Publicar evento: AcoesProativasExecutadas             │  │ │    │
│  │  │  └─────────────────────────────────────────────────────────┘  │ │    │
│  │  │                                                                │ │    │
│  │  └────────────────────────────────────────────────────────────────┘ │    │
│  │                                                                      │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Consolidar Métricas de Proatividade                  │    │
│  │ • Total de beneficiários processados                                │    │
│  │ • Gatilhos ativados por tipo                                        │    │
│  │ • Ações executadas por tipo                                         │    │
│  │ • Taxa de sucesso de envio                                          │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Atualizar Dashboard Executivo                        │    │
│  │ • Publicar métricas para Power BI                                   │    │
│  │ • Alertar gestores se taxa de proatividade < meta                   │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────┐                                                            │
│  │    END      │                                                            │
│  └─────────────┘                                                            │
│                                                                              │
│  ══════════════════════════════════════════════════════════════════════     │
│  FLUXO 2: EVENTO DE PREDIÇÃO DE INTERNAÇÃO (Tempo Real)                     │
│  ══════════════════════════════════════════════════════════════════════     │
│                                                                              │
│  ┌─────────────┐                                                            │
│  │   START     │ (Message: "PreditorInternacaoAlerta")                      │
│  │   Event     │ Correlation: beneficiarioId                                │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Enriquecer Contexto do Alerta                        │    │
│  │ • Carregar perfil completo                                          │    │
│  │ • Histórico recente de utilizações                                  │    │
│  │ • Condições crônicas ativas                                         │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Score de Risco de Internação?"                 │    │
│  └───────┬──────────────────────┬──────────────────────┬───────────────┘    │
│          │ >90% (Crítico)       │ 70-90% (Alto)        │ <70% (Moderado)    │
│          ▼                      ▼                      ▼                     │
│  ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────┐        │
│  │ [Parallel Gateway]│  │ [User Task]       │  │ [Send Task]       │        │
│  │ Fork              │  │ Alerta para       │  │ Nudge preventivo  │        │
│  └─┬──────────────┬──┘  │ Navegador         │  │ via WhatsApp      │        │
│    │              │     │ • SLA: 2h         │  └───────────────────┘        │
│    ▼              ▼     └───────────────────┘                                │
│ ┌────────┐  ┌──────────┐                                                    │
│ │Alerta  │  │[Send Task│                                                    │
│ │Navegad.│  │Contato   │                                                    │
│ │+ Médico│  │Imediato  │                                                    │
│ └────────┘  └──────────┘                                                    │
│    │              │                                                          │
│    ▼              ▼                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Signal Throw] "AlertaAltoRisco"                                    │    │
│  │ • Ativa SUB-007 com flag de urgência                                │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────┐                                                            │
│  │    END      │                                                            │
│  └─────────────┘                                                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Subprocessos de Gatilho (Call Activities)

**CallActivity_GAT001_Checkup:**
```
Input: beneficiarioId, diasDesdeUltimoCheckup
Fluxo:
1. [Service Task] Buscar agenda disponível rede preferencial
2. [Send Task] WhatsApp: "João, seu check-up anual está pendente. Posso agendar para você?"
3. [Receive Task] Aguardar resposta (timeout: 48h)
4. [Exclusive Gateway] Resposta?
   - SIM → [Service Task] Agendar + [Send Task] Confirmar
   - NÃO → [Service Task] Registrar recusa
   - TIMEOUT → [Service Task] Agendar recontato em 30 dias
Output: acaoExecutada, agendamentoId (se aplicável)
```

**CallActivity_GAT004_RiscoInternacao:**
```
Input: beneficiarioId, scoreRisco, fatoresRisco
Fluxo:
1. [Service Task] Preparar dossiê de alto risco
2. [User Task] Navegador analisa e define plano de ação (SLA: 2h)
3. [Send Task] Contato proativo via canal preferido
4. [Receive Task] Capturar resposta do beneficiário
5. [Exclusive Gateway] Necessita intervenção?
   - SIM → [Signal Throw] Ativar SUB-007 com urgência
   - NÃO → [Service Task] Agendar follow-up em 7 dias
Output: acaoExecutada, jornadaId (se criada)
```

---

### SUB-003: Recepção e Classificação Omnichannel

#### Ficha Técnica

| Atributo | Valor |
|----------|-------|
| **ID** | `SUB-003_Recepcao_Classificacao` |
| **Nome** | Recepção e Classificação Omnichannel |
| **Versão** | 2.0 |
| **Owner** | Coordenador de Atendimento |
| **SLA Global** | Primeira resposta em <3 minutos |

#### Triggers

| Tipo | Nome | Canal | Correlation |
|------|------|-------|-------------|
| Message | Interação WhatsApp | WhatsApp Business API | telefone |
| Message | Interação App | App Mobile | beneficiarioId |
| Message | Interação Portal | Portal Web | beneficiarioId |
| Message | Interação Telefone | Voice AI / URA | telefone |
| Message | Interação Email | Email Corporativo | email |

#### Fluxo Detalhado

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ SUB-003: Recepção e Classificação Omnichannel                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐                                                            │
│  │   START     │ (Message: "InteracaoRecebida")                             │
│  │   Event     │ Correlation: telefone OR beneficiarioId OR email           │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Identificar Canal de Origem                          │    │
│  │ • Detecta: WHATSAPP, APP, PORTAL, TELEFONE, EMAIL                   │    │
│  │ • Extrai metadados do canal                                         │    │
│  │ • Registra timestamp de entrada                                     │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Parallel Gateway] Fork - Processamento Paralelo                    │    │
│  └───┬─────────────────────────────────────────────────────────────┬───┘    │
│      │                                                             │        │
│      ▼                                                             ▼        │
│  ┌───────────────────────────────────┐    ┌───────────────────────────────┐│
│  │ [Service Task]                    │    │ [Service Task]                ││
│  │ Extrair Identificadores           │    │ Processar Conteúdo (NLP)      ││
│  │ • Telefone → beneficiário         │    │ • External: nlp-classifier    ││
│  │ • Token → sessão                  │    │ • Detecta intenção            ││
│  │ • CPF → validação                 │    │ • Extrai entidades            ││
│  │                                   │    │ • Análise de sentimento       ││
│  └───────────────────────────────────┘    └───────────────────────────────┘│
│      │                                                             │        │
│      ▼                                                             ▼        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Parallel Gateway] Join                                             │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Buscar Beneficiário no Tasy                          │    │
│  │ • Query por telefone, CPF ou beneficiarioId                         │    │
│  │ • Retry: R3/PT1M                                                    │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Beneficiário Encontrado?"                      │    │
│  └───────┬─────────────────────────────────────────────────────┬───────┘    │
│          │ NÃO                                                 │ SIM        │
│          ▼                                                     │            │
│  ┌───────────────────────────────────────┐                     │            │
│  │ [Subprocess] Identificação Assistida  │                     │            │
│  │                                        │                     │            │
│  │  ┌──────────────────────────────────┐ │                     │            │
│  │  │ [Send Task] Solicitar CPF        │ │                     │            │
│  │  │ "Para te ajudar, informe seu CPF │ │                     │            │
│  │  │  ou número da carteirinha"       │ │                     │            │
│  │  └───────────────┬──────────────────┘ │                     │            │
│  │                  │                     │                     │            │
│  │                  ▼                     │                     │            │
│  │  ┌──────────────────────────────────┐ │                     │            │
│  │  │ [Receive Task] Aguardar Resposta │ │                     │            │
│  │  │ • Timeout: 5 min                 │ │                     │            │
│  │  │                                   │ │                     │            │
│  │  │ ◄─── [Boundary Timer: 5min]      │ │                     │            │
│  │  │            │                      │ │                     │            │
│  │  │            └──► END (Timeout)     │ │                     │            │
│  │  └───────────────┬──────────────────┘ │                     │            │
│  │                  │                     │                     │            │
│  │                  ▼                     │                     │            │
│  │  ┌──────────────────────────────────┐ │                     │            │
│  │  │ [Service Task] Validar CPF       │ │                     │            │
│  │  └───────────────┬──────────────────┘ │                     │            │
│  │                  │                     │                     │            │
│  │                  ▼                     │                     │            │
│  │  ┌──────────────────────────────────┐ │                     │            │
│  │  │ [Exclusive Gateway] "Válido?"    │ │                     │            │
│  │  └─────┬─────────────────┬──────────┘ │                     │            │
│  │        │ SIM             │ NÃO        │                     │            │
│  │        │                 ▼            │                     │            │
│  │        │  ┌────────────────────────┐  │                     │            │
│  │        │  │ [Send Task]            │  │                     │            │
│  │        │  │ "Não encontramos seu   │  │                     │            │
│  │        │  │  cadastro. Verifique   │  │                     │            │
│  │        │  │  os dados."            │  │                     │            │
│  │        │  └──────────┬─────────────┘  │                     │            │
│  │        │             │                │                     │            │
│  │        │             └──► END         │                     │            │
│  │        │                              │                     │            │
│  │        └──► (Continua fluxo principal)│                     │            │
│  │                                        │                     │            │
│  └───────────────────────────────────────┘                     │            │
│          │                                                     │            │
│          └─────────────────────────────────────────────────────┘            │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Carregar Perfil 360° do Beneficiário                 │    │
│  │                                                                      │    │
│  │ Dados carregados:                                                    │    │
│  │ • Dados cadastrais completos                                        │    │
│  │ • Classificação de risco atual                                      │    │
│  │ • Histórico de interações (últimos 30 dias)                         │    │
│  │ • Condições clínicas ativas                                         │    │
│  │ • Autorizações pendentes                                            │    │
│  │ • Consultas/exames agendados                                        │    │
│  │ • Jornadas de cuidado em andamento                                  │    │
│  │ • Reclamações abertas                                               │    │
│  │ • Preferências de comunicação                                       │    │
│  │ • Score de satisfação (NPS histórico)                               │    │
│  │                                                                      │    │
│  │ Output: contextoEnriquecido (Object)                                │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Verificar Contexto de Conversa Anterior              │    │
│  │ • Se última interação < 30 min: continuar contexto                  │    │
│  │ • Carregar histórico de mensagens da sessão                         │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Business Rule Task] Classificar Urgência                           │    │
│  │ • DMN: DMN_ClassificarUrgencia                                      │    │
│  │                                                                      │    │
│  │ Inputs:                                                              │    │
│  │ • intencaoDetectada                                                 │    │
│  │ • entidadesExtraidas (sintomas, datas, etc.)                        │    │
│  │ • sentimentoMensagem                                                │    │
│  │ • classificacaoRiscoBeneficiario                                    │    │
│  │ • idadeBeneficiario                                                 │    │
│  │ • comorbidadesAtivas                                                │    │
│  │ • historicoEmergencias                                              │    │
│  │                                                                      │    │
│  │ Outputs:                                                             │    │
│  │ • nivelUrgencia: EMERGENCIA | URGENTE | ROTINA | PREVENTIVO         │    │
│  │ • slaMinutos: Integer                                               │    │
│  │ • flagsAlerta: Array (ex: ["RISCO_CLINICO", "INSATISFACAO"])        │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Business Rule Task] Definir Roteamento                             │    │
│  │ • DMN: DMN_DefinirRoteamento                                        │    │
│  │                                                                      │    │
│  │ Inputs:                                                              │    │
│  │ • tipodemanda (da intenção)                                         │    │
│  │ • nivelUrgencia                                                     │    │
│  │ • complexidadeEstimada                                              │    │
│  │ • classificacaoRiscoBeneficiario                                    │    │
│  │ • capacidadeAtualCamadas (consulta tempo real)                      │    │
│  │ • preferenciaCanal                                                  │    │
│  │                                                                      │    │
│  │ Outputs:                                                             │    │
│  │ • camadaDestino: SELF_SERVICE | AGENTE_IA | NAVEGADOR | ESPECIALISTA│    │
│  │ • filaDestino: String                                               │    │
│  │ • prioridade: 1-10                                                  │    │
│  │ • motivoRoteamento: String                                          │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Nível de Urgência?"                            │    │
│  └───┬─────────────────┬─────────────────┬─────────────────────────────┘    │
│      │ EMERGENCIA      │ Outros          │                                  │
│      ▼                 │                 │                                  │
│  ┌───────────────────┐ │                 │                                  │
│  │ [Parallel Gateway]│ │                 │                                  │
│  │ Fork Emergência   │ │                 │                                  │
│  └─┬──────────────┬──┘ │                 │                                  │
│    │              │    │                 │                                  │
│    ▼              ▼    │                 │                                  │
│ ┌────────────┐ ┌────────────┐           │                                  │
│ │[Send Task] │ │[Signal     │           │                                  │
│ │"Procure PS │ │Throw]      │           │                                  │
│ │IMEDIATO"   │ │Protocolo   │           │                                  │
│ │+ Endereço  │ │Emergência  │           │                                  │
│ └────────────┘ └────────────┘           │                                  │
│    │              │                      │                                  │
│    ▼              ▼                      │                                  │
│  ┌─────────────────┐                     │                                  │
│  │ [Call Activity] │                     │                                  │
│  │ SUB-007 com     │                     │                                  │
│  │ flag URGENTE    │                     │                                  │
│  └────────┬────────┘                     │                                  │
│           │                              │                                  │
│           └──────────────────────────────┤                                  │
│                                          │                                  │
│                                          ▼                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Qual Camada Destino?"                          │    │
│  └───┬──────────────┬──────────────┬──────────────┬────────────────────┘    │
│      │              │              │              │                          │
│      │SELF_SERVICE  │AGENTE_IA     │NAVEGADOR     │ESPECIALISTA              │
│      ▼              ▼              ▼              ▼                          │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────────────────┐          │
│  │ [Call   │   │ [Call   │   │ [Call   │   │ [User Task]         │          │
│  │Activity]│   │Activity]│   │Activity]│   │ Encaminhar para     │          │
│  │ SUB-004 │   │ SUB-005 │   │ SUB-007 │   │ Back-office         │          │
│  └────┬────┘   └────┬────┘   └────┬────┘   │ • candidateGroups:  │          │
│       │             │             │        │   backoffice        │          │
│       │             │             │        └──────────┬──────────┘          │
│       │             │             │                   │                      │
│       └─────────────┴─────────────┴───────────────────┘                      │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Demanda Resolvida?"                            │    │
│  └───────┬─────────────────────────────────────────────────────┬───────┘    │
│          │ SIM                                                 │ NÃO        │
│          ▼                                                     ▼            │
│  ┌───────────────────────────────────┐   ┌─────────────────────────────────┐│
│  │ [Call Activity] SUB-010           │   │ [Exclusive Gateway]             ││
│  │ Follow-up                         │   │ "Escalar?"                      ││
│  └───────────────────────────────────┘   └───────┬─────────────────┬───────┘│
│          │                                       │ SIM             │ NÃO    │
│          │                                       ▼                 ▼        │
│          │                              ┌───────────────┐  ┌───────────────┐│
│          │                              │[Service Task] │  │[Send Task]    ││
│          │                              │Transferir com │  │Informar próx. ││
│          │                              │contexto       │  │passos         ││
│          │                              └───────┬───────┘  └───────┬───────┘│
│          │                                      │                  │        │
│          │                                      ▼                  │        │
│          │                              ┌───────────────┐          │        │
│          │                              │[Call Activity]│          │        │
│          │                              │Próxima Camada │          │        │
│          │                              └───────────────┘          │        │
│          │                                      │                  │        │
│          └──────────────────────────────────────┴──────────────────┘        │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Registrar Interação Completa no CRM                  │    │
│  │ • Timestamp início/fim                                              │    │
│  │ • Canal                                                             │    │
│  │ • Classificação                                                     │    │
│  │ • Roteamento                                                        │    │
│  │ • Resultado                                                         │    │
│  │ • Publicar evento: InteracaoProcessada                              │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────┐                                                            │
│  │    END      │                                                            │
│  └─────────────┘                                                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### SUB-004: Self-Service e Autoatendimento

[Detalhamento similar ao padrão anterior - mantido conforme v1.0 com adições de métricas e tratamento de erros]

---

### SUB-005: Atendimento por Agentes IA

[Detalhamento similar ao padrão anterior - mantido conforme v1.0 com adições de métricas e tratamento de erros]

---

### SUB-006: Autorização Inteligente de Procedimentos

#### Ficha Técnica

| Atributo | Valor |
|----------|-------|
| **ID** | `SUB-006_Autorizacao_Inteligente` |
| **Nome** | Autorização Inteligente de Procedimentos |
| **Versão** | 2.0 |
| **Owner** | Coordenador de Regulação |
| **Meta de Automação** | 85% autorizações automáticas |

#### Tipos de Autorização

| Tipo | Código | SLA | Automação |
|------|--------|-----|-----------|
| Consulta Eletiva | AUT-CONS | 5 min | 95% |
| SADT Simples | AUT-SADT-S | 5 min | 90% |
| SADT Complexo | AUT-SADT-C | 4h | 60% |
| Internação Eletiva | AUT-INT-E | 24h | 40% |
| Internação Urgência | AUT-INT-U | 2h | 50% |
| Cirurgia Eletiva | AUT-CIR-E | 24h | 30% |
| Home Care | AUT-HOME | 48h | 20% |
| OPME | AUT-OPME | 48h | 10% |

#### Fluxo Detalhado

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ SUB-006: Autorização Inteligente de Procedimentos                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐                                                            │
│  │   START     │ (Message: "SolicitacaoAutorizacao")                        │
│  │   Event     │ Correlation: guiaNumero                                    │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Receber e Validar Guia TISS                          │    │
│  │ • Validar XML/estrutura TISS                                        │    │
│  │ • Extrair procedimentos, beneficiário, prestador                    │    │
│  │ • Registrar recebimento                                             │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               │ ◄─── [Boundary Error: TISS_INVALIDO]        │
│                               │            │                                 │
│                               │            ▼                                 │
│                               │      [Send Task] Notificar erro ao prestador │
│                               │            │                                 │
│                               │            └──► END (Error)                  │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Parallel Gateway] Fork - Validações Simultâneas                    │    │
│  └───┬────────┬────────┬────────┬────────┬─────────────────────────────┘    │
│      │        │        │        │        │                                  │
│      ▼        ▼        ▼        ▼        ▼                                  │
│  ┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐                         │
│  │Validar ││Validar ││Validar ││Validar ││Validar │                         │
│  │Elegibil││Credenc.││Cobertu.││Carênc. ││CPT     │                         │
│  │Benefic.││Prestad.││Plano   ││        ││        │                         │
│  └───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘                         │
│      │        │        │        │        │                                  │
│      ▼        ▼        ▼        ▼        ▼                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Parallel Gateway] Join                                             │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Todas Validações OK?"                          │    │
│  └───────┬─────────────────────────────────────────────────────┬───────┘    │
│          │ NÃO                                                 │ SIM        │
│          ▼                                                     │            │
│  ┌───────────────────────────────────────────────┐             │            │
│  │ [Subprocess] Tratamento de Pendências          │             │            │
│  │                                                │             │            │
│  │  ┌──────────────────────────────────────────┐ │             │            │
│  │  │ [Service Task] Identificar Pendência     │ │             │            │
│  │  │ • ELEGIBILIDADE: Contrato inativo        │ │             │            │
│  │  │ • CREDENCIAMENTO: Prestador não credenc. │ │             │            │
│  │  │ • COBERTURA: Procedimento não coberto    │ │             │            │
│  │  │ • CARENCIA: Período não cumprido         │ │             │            │
│  │  │ • CPT: Condição pré-existente            │ │             │            │
│  │  └─────────────────────┬────────────────────┘ │             │            │
│  │                        │                      │             │            │
│  │                        ▼                      │             │            │
│  │  ┌──────────────────────────────────────────┐ │             │            │
│  │  │ [Exclusive Gateway] "Tipo de Pendência?" │ │             │            │
│  │  └────┬───────────────────────────┬─────────┘ │             │            │
│  │       │ SANÁVEL                   │ DEFINITIVA│             │            │
│  │       ▼                           ▼           │             │            │
│  │  ┌────────────────┐      ┌────────────────┐   │             │            │
│  │  │ [Send Task]    │      │ [Service Task] │   │             │            │
│  │  │ Notificar prest│      │ Negar          │   │             │            │
│  │  │ sobre pendência│      │ Autorização    │   │             │            │
│  │  └───────┬────────┘      └───────┬────────┘   │             │            │
│  │          │                       │            │             │            │
│  │          ▼                       │            │             │            │
│  │  ┌────────────────────────┐      │            │             │            │
│  │  │ [Receive Task]         │      │            │             │            │
│  │  │ Aguardar Correção      │      │            │             │            │
│  │  │ • Timeout: 48h         │      │            │             │            │
│  │  │                        │      │            │             │            │
│  │  │ ◄─── [Boundary Timer]  │      │            │             │            │
│  │  │            │           │      │            │             │            │
│  │  │            └──► Expirar│      │            │             │            │
│  │  └───────┬────────────────┘      │            │             │            │
│  │          │                       │            │             │            │
│  │          │ (Reprocessar validações)           │             │            │
│  │          │                       │            │             │            │
│  └──────────┴───────────────────────┴────────────┘             │            │
│                               │                                │            │
│                               │ (Se negar)                     │            │
│                               ▼                                │            │
│  ┌───────────────────────────────────────────────┐             │            │
│  │ [Send Task] Notificar Negativa + Recurso      │             │            │
│  │ • Justificativa detalhada                     │             │            │
│  │ • Orientação para recurso                     │             │            │
│  │ • Prazo: 30 dias para recurso                 │             │            │
│  └───────────────────────────────────────────────┘             │            │
│                               │                                │            │
│                               └──► END (Negada)                │            │
│                                                                │            │
│                               ◄────────────────────────────────┘            │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Classificar Tipo de Procedimento                     │    │
│  │ • AMBULATORIAL | SADT | HOSPITALAR | CIRURGICO | HOME_CARE | OPME   │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Business Rule Task] Regras de Autorização por Tipo                 │    │
│  │ • DMN: DMN_RegrasAutorizacao                                        │    │
│  │                                                                      │    │
│  │ Outputs:                                                             │    │
│  │ • podeAutorizarAutomaticamente: Boolean                             │    │
│  │ • tipoAnaliseRequerida: NENHUMA | PROTOCOLO | AUDITORIA_MEDICA      │    │
│  │ • motivoAnalise: String                                             │    │
│  │ • limiteValorAutomatico: Decimal                                    │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Pode Autorizar Automaticamente?"               │    │
│  └───────┬─────────────────────────────────────────────────────┬───────┘    │
│          │ SIM                                                 │ NÃO        │
│          ▼                                                     ▼            │
│  ┌───────────────────────────────────┐   ┌─────────────────────────────────┐│
│  │ [Service Task] Aprovar            │   │ [Subprocess] Análise Técnica   ││
│  │ Automaticamente                   │   │                                 ││
│  │ • Gerar número autorização        │   │  (Detalhado abaixo)             ││
│  │ • Registrar no Tasy               │   │                                 ││
│  │ • Definir validade                │   │                                 ││
│  └───────────────────────────────────┘   └─────────────────────────────────┘│
│          │                                         │                         │
│          └─────────────────────────────────────────┘                         │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Parallel Gateway] Fork - Notificações                              │    │
│  └───┬─────────────────────────────────────────────────────────────┬───┘    │
│      │                                                             │        │
│      ▼                                                             ▼        │
│  ┌───────────────────────────────────┐    ┌───────────────────────────────┐│
│  │ [Send Task] Notificar Prestador   │    │ [Send Task] Notificar         ││
│  │ • Via integração TISS             │    │ Beneficiário                  ││
│  │ • Email backup                    │    │ • Via WhatsApp                ││
│  │ • Número + validade               │    │ • "Sua autorização foi        ││
│  └───────────────────────────────────┘    │   aprovada: #123456"          ││
│      │                                    └───────────────────────────────┘│
│      │                                                             │        │
│      ▼                                                             ▼        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Parallel Gateway] Join                                             │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Atualizar Métricas de Autorização                    │    │
│  │ • Taxa de automação                                                 │    │
│  │ • Tempo médio por tipo                                              │    │
│  │ • Taxa de aprovação/negativa                                        │    │
│  │ • Publicar evento: AutorizacaoProcessada                            │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────┐                                                            │
│  │    END      │                                                            │
│  └─────────────┘                                                            │
│                                                                              │
│  ══════════════════════════════════════════════════════════════════════     │
│  SUBPROCESS: Análise Técnica                                                │
│  ══════════════════════════════════════════════════════════════════════     │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Preparar Dossiê para Auditoria                       │    │
│  │ • Histórico do beneficiário                                         │    │
│  │ • Utilizações anteriores do procedimento                            │    │
│  │ • Documentação clínica anexa                                        │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Business Rule Task] Identificar Protocolo Clínico                  │    │
│  │ • DMN: DMN_ProtocoloClinico                                         │    │
│  │ • Baseado em: CID, procedimento, indicação                          │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Tipo de Análise?"                              │    │
│  └───────┬─────────────────────────────────────────────────────┬───────┘    │
│          │ PROTOCOLO_AUTOMATICO                                │ AUDITORIA  │
│          ▼                                                     ▼            │
│  ┌───────────────────────────────────┐   ┌─────────────────────────────────┐│
│  │ [Service Task] Aplicar Protocolo  │   │ [User Task] Análise pelo       ││
│  │ • Verificar critérios             │   │ Médico Auditor                 ││
│  │ • Validar exames prévios          │   │ • candidateGroups:             ││
│  │ • Checar indicação clínica        │   │   auditores_medicos            ││
│  └───────────────┬───────────────────┘   │ • formKey: form_auditoria      ││
│                  │                       │ • SLA: 24h                     ││
│                  ▼                       │                                 ││
│  ┌───────────────────────────────────┐   │ ◄─── [Boundary Timer: 24h]     ││
│  │ [Exclusive Gateway]               │   │            │                    ││
│  │ "Atende Protocolo?"               │   │            ▼                    ││
│  └─────┬─────────────────┬───────────┘   │      [Signal Throw]             ││
│        │ SIM             │ NÃO           │      Alerta SLA                 ││
│        ▼                 ▼               │                                 ││
│  ┌───────────┐   ┌───────────────────┐   └───────────────┬─────────────────┘│
│  │ Aprovar   │   │ [Exclusive Gatew] │                   │                  │
│  │           │   │ "Escalar?"        │                   ▼                  │
│  └───────────┘   └─────┬─────────┬───┘   ┌───────────────────────────────┐ │
│                        │ SIM     │ NÃO   │ [Exclusive Gateway]           │ │
│                        ▼         ▼       │ "Decisão do Auditor?"         │ │
│                  ┌──────────┐ ┌───────┐  └───────┬───────────┬───────────┘ │
│                  │Auditoria │ │Negar  │          │ APROVAR   │ NEGAR       │
│                  │Médica    │ │       │          ▼           ▼             │
│                  └──────────┘ └───────┘   ┌──────────┐  ┌──────────┐       │
│                                           │ Aprovar  │  │ Negar    │       │
│                                           └──────────┘  └──────────┘       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```


---

## 9. CATÁLOGO DE ERROS E EXCEÇÕES

### 9.1 Códigos de Erro Padronizados

| Código | Nome | Descrição | Recuperável | Tratamento |
|--------|------|-----------|-------------|------------|
| `ERR-001` | BENEFICIARIO_NAO_ENCONTRADO | CPF/ID não localizado na base | Sim | Fluxo de identificação assistida |
| `ERR-002` | CONTRATO_INATIVO | Contrato cancelado, suspenso ou inadimplente | Não | Informar e encerrar |
| `ERR-003` | TIMEOUT_RESPOSTA_BENEFICIARIO | Beneficiário não respondeu no prazo | Sim | Retry ou escalação |
| `ERR-004` | INTEGRACAO_TASY_INDISPONIVEL | Falha de comunicação com Tasy ERP | Sim | Retry com backoff exponencial |
| `ERR-005` | INTEGRACAO_WHATSAPP_FALHA | Falha no envio de mensagem WhatsApp | Sim | Retry + fallback para SMS |
| `ERR-006` | NLP_CLASSIFICACAO_FALHA | Erro no serviço de NLP | Sim | Fallback para regras simples |
| `ERR-007` | AUTORIZACAO_PROCEDIMENTO_NAO_COBERTO | Procedimento fora da cobertura | Não | Notificar negativa |
| `ERR-008` | AUTORIZACAO_CARENCIA_NAO_CUMPRIDA | Carência ainda em vigor | Não | Informar prazo restante |
| `ERR-009` | AUTORIZACAO_CPT_BLOQUEIO | Procedimento bloqueado por CPT | Parcial | Verificar período CPT |
| `ERR-010` | PRESTADOR_NAO_CREDENCIADO | Prestador não pertence à rede | Não | Sugerir alternativas |
| `ERR-011` | AGENDAMENTO_SEM_DISPONIBILIDADE | Sem horários disponíveis | Sim | Buscar alternativas |
| `ERR-012` | DOCUMENTO_OCR_ILEGIVEL | OCR não conseguiu processar | Sim | Solicitar reenvio |
| `ERR-013` | TISS_XML_INVALIDO | Guia TISS com estrutura inválida | Sim | Notificar prestador |
| `ERR-014` | LIMITE_TENTATIVAS_EXCEDIDO | Máximo de retries atingido | Não | Escalação manual |
| `ERR-015` | FRAUDE_DETECTADA | Padrão suspeito identificado | Não | Bloquear e escalar compliance |
| `ERR-016` | SLA_VIOLADO | Tempo máximo de SLA excedido | N/A | Alerta + escalação |
| `ERR-017` | DADOS_INCOMPLETOS | Informações obrigatórias faltando | Sim | Solicitar complemento |
| `ERR-018` | SESSAO_EXPIRADA | Sessão de conversa expirou | Sim | Reiniciar contexto |
| `ERR-019` | KAFKA_PUBLISH_FALHA | Erro ao publicar evento | Sim | Retry + dead letter queue |
| `ERR-020` | MODELO_ML_INDISPONIVEL | Serviço de ML offline | Sim | Fallback para regras |

### 9.2 Estratégias de Retry

| Tipo de Erro | Estratégia | Configuração | Max Tentativas |
|--------------|------------|--------------|----------------|
| Integração Tasy | Exponential Backoff | PT1M, PT5M, PT15M | 3 |
| WhatsApp API | Linear Retry | PT10S entre tentativas | 5 |
| NLP Service | Immediate Retry | PT5S | 3 |
| Kafka Publish | Exponential | PT1S, PT5S, PT30S | 5 |
| OCR Service | Linear | PT30S | 3 |

### 9.3 Configuração de Error Handlers

```xml
<!-- Exemplo de Error Boundary Event -->
<bpmn:boundaryEvent id="BoundaryError_Tasy" attachedToRef="ServiceTask_BuscarTasy">
  <bpmn:errorEventDefinition errorRef="Error_TasyIndisponivel"/>
</bpmn:boundaryEvent>

<bpmn:error id="Error_TasyIndisponivel" 
            name="Tasy Indisponível" 
            errorCode="ERR-004"/>

<!-- Subprocess de Tratamento -->
<bpmn:subProcess id="SubProcess_ErrorHandler" triggeredByEvent="true">
  <bpmn:startEvent id="StartEvent_Error">
    <bpmn:errorEventDefinition errorRef="Error_TasyIndisponivel"/>
  </bpmn:startEvent>
  
  <bpmn:serviceTask id="Task_LogError" 
                    name="Registrar Erro"
                    camunda:delegateExpression="${errorLoggerService}"/>
  
  <bpmn:serviceTask id="Task_NotifyOps" 
                    name="Alertar Operações"
                    camunda:delegateExpression="${opsAlertService}"/>
  
  <bpmn:exclusiveGateway id="Gateway_Retry" name="Tentar novamente?"/>
  
  <!-- Lógica de retry ou escalação -->
</bpmn:subProcess>
```

### 9.4 Dead Letter Queue (DLQ)

| Fila Original | DLQ | Retenção | Ação |
|---------------|-----|----------|------|
| `whatsapp-sender` | `whatsapp-sender-dlq` | 7 dias | Reprocessamento manual |
| `nlp-classifier` | `nlp-classifier-dlq` | 3 dias | Análise e reprocessamento |
| `autorizacao-processor` | `autorizacao-dlq` | 30 dias | Auditoria obrigatória |
| `evento-beneficiario` | `evento-dlq` | 14 dias | Análise de consistência |

---

## 10. CORRELAÇÃO DE MENSAGENS

### 10.1 Correlation Keys

```xml
<!-- Definição de Correlation Keys -->
<bpmn:correlationKey name="BeneficiarioCorrelation">
  <bpmn:correlationPropertyRef>prop_beneficiarioId</bpmn:correlationPropertyRef>
</bpmn:correlationKey>

<bpmn:correlationKey name="InteracaoCorrelation">
  <bpmn:correlationPropertyRef>prop_beneficiarioId</bpmn:correlationPropertyRef>
  <bpmn:correlationPropertyRef>prop_telefone</bpmn:correlationPropertyRef>
</bpmn:correlationKey>

<bpmn:correlationKey name="AutorizacaoCorrelation">
  <bpmn:correlationPropertyRef>prop_guiaNumero</bpmn:correlationPropertyRef>
</bpmn:correlationKey>

<!-- Correlation Properties -->
<bpmn:correlationProperty id="prop_beneficiarioId" name="Beneficiário ID">
  <bpmn:correlationPropertyRetrievalExpression messageRef="Msg_RespostaBeneficiario">
    <bpmn:formalExpression>${beneficiarioId}</bpmn:formalExpression>
  </bpmn:correlationPropertyRetrievalExpression>
</bpmn:correlationProperty>

<bpmn:correlationProperty id="prop_telefone" name="Telefone">
  <bpmn:correlationPropertyRetrievalExpression messageRef="Msg_WhatsAppRecebido">
    <bpmn:formalExpression>${telefone}</bpmn:formalExpression>
  </bpmn:correlationPropertyRetrievalExpression>
</bpmn:correlationProperty>
```

### 10.2 Tabela de Correlações

| Mensagem | Correlation Key | Campos | Uso |
|----------|-----------------|--------|-----|
| `Msg_RespostaBeneficiario` | BeneficiarioCorrelation | beneficiarioId | Correlacionar resposta com instância aguardando |
| `Msg_WhatsAppRecebido` | InteracaoCorrelation | telefone | Identificar conversa ativa |
| `Msg_ResultadoExame` | BeneficiarioCorrelation | beneficiarioId, exameId | Correlacionar resultado com jornada |
| `Msg_RespostaPrestador` | AutorizacaoCorrelation | guiaNumero | Correlacionar resposta de autorização |
| `Msg_DocumentoUpload` | BeneficiarioCorrelation | beneficiarioId | Correlacionar upload com onboarding |
| `Msg_ConfirmacaoAgendamento` | BeneficiarioCorrelation | beneficiarioId, agendamentoId | Confirmar agendamento |

### 10.3 Exemplo de Receive Task com Correlação

```xml
<bpmn:receiveTask id="ReceiveTask_AguardarResposta" 
                  name="Aguardar Resposta do Beneficiário"
                  messageRef="Msg_RespostaBeneficiario">
  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="correlationKey_beneficiarioId">
        ${beneficiarioId}
      </camunda:inputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
</bpmn:receiveTask>
```

---

## 11. ESPECIFICAÇÕES DE INTEGRAÇÃO

### 11.1 APIs Internas

| Sistema | Endpoint | Método | Descrição | Timeout |
|---------|----------|--------|-----------|---------|
| **Tasy ERP** | `/api/v1/beneficiario/{cpf}` | GET | Buscar beneficiário por CPF | 5s |
| **Tasy ERP** | `/api/v1/beneficiario` | POST | Criar/atualizar beneficiário | 10s |
| **Tasy ERP** | `/api/v1/autorizacao` | POST | Criar autorização | 10s |
| **Tasy ERP** | `/api/v1/autorizacao/{id}` | GET | Consultar autorização | 5s |
| **Tasy ERP** | `/api/v1/agendamento` | POST | Criar agendamento | 10s |
| **Tasy ERP** | `/api/v1/prestador/disponibilidade` | GET | Consultar disponibilidade | 5s |
| **CRM** | `/api/v1/interacao` | POST | Registrar interação | 5s |
| **CRM** | `/api/v1/caso` | POST | Abrir caso | 5s |
| **Data Lake** | `/api/v1/evento` | POST | Publicar evento | 3s |
| **Data Lake** | `/api/v1/perfil/{id}` | GET | Buscar perfil enriquecido | 5s |

### 11.2 APIs Externas

| Serviço | Endpoint | Método | Descrição | Rate Limit |
|---------|----------|--------|-----------|------------|
| **WhatsApp Business API** | `/v1/messages` | POST | Enviar mensagem | 80 msg/s |
| **WhatsApp Business API** | `/v1/messages/{id}` | GET | Status da mensagem | 100 req/s |
| **GPT-4 API** | `/v1/chat/completions` | POST | Classificação NLP | 10 req/s |
| **Azure Computer Vision** | `/vision/v3.2/ocr` | POST | Processar documento | 20 req/s |
| **Google Maps** | `/maps/api/geocode` | GET | Geolocalização | 50 req/s |

### 11.3 Contratos de API (Schemas)

**BeneficiarioDTO:**
```json
{
  "beneficiarioId": "string (UUID)",
  "cpf": "string (11 dígitos)",
  "nome": "string",
  "dataNascimento": "date (YYYY-MM-DD)",
  "telefone": "string (E.164)",
  "email": "string",
  "endereco": {
    "logradouro": "string",
    "numero": "string",
    "complemento": "string",
    "bairro": "string",
    "cidade": "string",
    "uf": "string (2 chars)",
    "cep": "string (8 dígitos)"
  },
  "contrato": {
    "contratoId": "string",
    "planoId": "string",
    "dataInicio": "date",
    "status": "ATIVO | SUSPENSO | CANCELADO"
  },
  "perfilSaude": {
    "classificacaoRisco": "BAIXO | MODERADO | ALTO | COMPLEXO",
    "scoreRisco": "integer (0-100)",
    "condicoesCronicas": ["string (CID)"],
    "statusCPT": "SEM_CPT | CPT_ATIVO | CPT_EXPIRADO"
  }
}
```

**AutorizacaoRequestDTO:**
```json
{
  "guiaTISS": {
    "numeroGuia": "string",
    "tipoGuia": "CONSULTA | SADT | SP_SADT | INTERNACAO",
    "dataEmissao": "datetime"
  },
  "beneficiarioId": "string",
  "prestadorId": "string",
  "procedimentos": [
    {
      "codigo": "string (TUSS)",
      "descricao": "string",
      "quantidade": "integer",
      "valorUnitario": "decimal"
    }
  ],
  "indicacaoClinica": {
    "cid": "string",
    "justificativa": "string"
  },
  "anexos": [
    {
      "tipo": "LAUDO | EXAME | RELATORIO",
      "arquivo": "string (base64)"
    }
  ]
}
```

**AutorizacaoResponseDTO:**
```json
{
  "autorizacaoId": "string",
  "numeroAutorizacao": "string (15 dígitos)",
  "status": "APROVADA | NEGADA | PENDENTE | EM_ANALISE",
  "dataAprovacao": "datetime",
  "validade": "date",
  "justificativa": "string",
  "procedimentosAutorizados": [
    {
      "codigo": "string",
      "quantidadeAutorizada": "integer",
      "valorAutorizado": "decimal"
    }
  ]
}
```

### 11.4 Configuração de Service Tasks

```xml
<!-- Delegate Expression (Java/Spring) -->
<bpmn:serviceTask id="ServiceTask_BuscarBeneficiario" 
                  name="Buscar Beneficiário no Tasy"
                  camunda:delegateExpression="${tasyBeneficiarioService}">
  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="cpf">${cpf}</camunda:inputParameter>
      <camunda:outputParameter name="beneficiario">${beneficiarioDTO}</camunda:outputParameter>
    </camunda:inputOutput>
    <camunda:failedJobRetryTimeCycle>R3/PT1M</camunda:failedJobRetryTimeCycle>
  </bpmn:extensionElements>
</bpmn:serviceTask>

<!-- External Task (Workers) -->
<bpmn:serviceTask id="ServiceTask_ClassificarNLP" 
                  name="Classificar Intenção (NLP)"
                  camunda:type="external"
                  camunda:topic="nlp-classificacao">
  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="texto">${mensagemOriginal}</camunda:inputParameter>
      <camunda:inputParameter name="contexto">${contextoEnriquecido}</camunda:inputParameter>
      <camunda:outputParameter name="intencao">${intencaoDetectada}</camunda:outputParameter>
      <camunda:outputParameter name="entidades">${entidadesExtraidas}</camunda:outputParameter>
      <camunda:outputParameter name="sentimento">${sentimentoMensagem}</camunda:outputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

---

## 12. EVENTOS E MENSAGERIA (KAFKA)

### 12.1 Tópicos Kafka

| Tópico | Produtor(es) | Consumidor(es) | Schema | Retenção |
|--------|--------------|----------------|--------|----------|
| `beneficiario.onboarding.completed` | SUB-001 | SUB-002, Analytics | OnboardingCompletedEvent | 30 dias |
| `beneficiario.risco.atualizado` | SUB-001, SUB-010 | SUB-002, SUB-008 | RiscoAtualizadoEvent | 30 dias |
| `interacao.recebida` | Canal Digital | SUB-003 | InteracaoRecebidaEvent | 7 dias |
| `interacao.processada` | SUB-003 | Analytics, CRM | InteracaoProcessadaEvent | 30 dias |
| `autorizacao.solicitada` | SUB-005, Prestadores | SUB-006 | AutorizacaoSolicitadaEvent | 90 dias |
| `autorizacao.processada` | SUB-006 | Analytics, Prestadores | AutorizacaoProcessadaEvent | 90 dias |
| `gatilho.proativo.identificado` | SUB-002 | Réguas Comunicação | GatilhoProativoEvent | 7 dias |
| `jornada.cuidado.iniciada` | SUB-007 | Analytics | JornadaIniciadaEvent | 90 dias |
| `jornada.cuidado.concluida` | SUB-007 | SUB-010, Analytics | JornadaConcluidaEvent | 90 dias |
| `feedback.coletado` | SUB-010 | Analytics, ML Training | FeedbackColetadoEvent | 365 dias |
| `alerta.alto.risco` | SUB-002 | SUB-007, Navegadores | AlertaAltoRiscoEvent | 30 dias |
| `reclamacao.registrada` | SUB-009 | Analytics, Ouvidoria | ReclamacaoRegistradaEvent | 365 dias |

### 12.2 Schemas de Eventos (Avro)

**OnboardingCompletedEvent:**
```json
{
  "type": "record",
  "name": "OnboardingCompletedEvent",
  "namespace": "br.com.austa.eventos",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "beneficiarioId", "type": "string"},
    {"name": "contratoId", "type": "string"},
    {"name": "scoreRisco", "type": "int"},
    {"name": "classificacaoRisco", "type": {"type": "enum", "name": "ClassificacaoRisco", "symbols": ["BAIXO", "MODERADO", "ALTO", "COMPLEXO"]}},
    {"name": "condicoesCronicas", "type": {"type": "array", "items": "string"}},
    {"name": "statusCPT", "type": {"type": "enum", "name": "StatusCPT", "symbols": ["SEM_CPT", "CPT_DETECTADO", "EM_INVESTIGACAO"]}},
    {"name": "tempoOnboardingHoras", "type": "float"},
    {"name": "modulosCompletados", "type": "int"},
    {"name": "documentosEnviados", "type": "int"}
  ]
}
```

**GatilhoProativoEvent:**
```json
{
  "type": "record",
  "name": "GatilhoProativoEvent",
  "namespace": "br.com.austa.eventos",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "beneficiarioId", "type": "string"},
    {"name": "tipoGatilho", "type": "string"},
    {"name": "prioridade", "type": {"type": "enum", "name": "Prioridade", "symbols": ["BAIXA", "MEDIA", "ALTA", "CRITICA"]}},
    {"name": "dadosGatilho", "type": {"type": "map", "values": "string"}},
    {"name": "acaoRecomendada", "type": "string"},
    {"name": "canalPreferido", "type": "string"}
  ]
}
```

### 12.3 Configuração do Kafka Producer (Camunda)

```java
@Component("kafkaPublisherService")
public class KafkaPublisherDelegate implements JavaDelegate {
    
    @Autowired
    private KafkaTemplate<String, GenericRecord> kafkaTemplate;
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String topico = (String) execution.getVariable("kafkaTopic");
        String eventId = UUID.randomUUID().toString();
        
        GenericRecord evento = buildEvent(execution, eventId);
        
        kafkaTemplate.send(topico, evento.get("beneficiarioId").toString(), evento)
            .addCallback(
                result -> log.info("Evento publicado: {} -> {}", eventId, topico),
                ex -> {
                    log.error("Falha ao publicar evento: {}", eventId, ex);
                    throw new BpmnError("ERR-019", "Falha ao publicar evento Kafka");
                }
            );
    }
}
```

---

## 13. TEMPLATES DE COMUNICAÇÃO

### 13.1 Templates WhatsApp (HSM Aprovados)

| ID | Nome | Categoria | Variáveis | Uso |
|----|------|-----------|-----------|-----|
| `boas_vindas_v2` | Boas-vindas Onboarding | UTILITY | {nome}, {plano} | Primeiro contato |
| `lembrete_onboarding_v1` | Lembrete Screening | UTILITY | {nome} | Reengajamento |
| `checkup_pendente_v1` | Lembrete Check-up | UTILITY | {nome}, {meses} | Gatilho preventivo |
| `medicamento_renovar_v1` | Renovação Medicamento | UTILITY | {nome}, {medicamento}, {dias} | Gatilho adesão |
| `autorizacao_aprovada_v1` | Autorização Aprovada | TRANSACTIONAL | {nome}, {numero}, {procedimento} | Notificação |
| `autorizacao_pendente_v1` | Autorização Pendente | TRANSACTIONAL | {nome}, {procedimento}, {motivo} | Notificação |
| `agendamento_confirmado_v1` | Confirmação Agendamento | TRANSACTIONAL | {nome}, {data}, {hora}, {prestador}, {endereco} | Confirmação |
| `lembrete_consulta_v1` | Lembrete Consulta | UTILITY | {nome}, {data}, {hora}, {prestador} | Véspera |
| `pos_consulta_v1` | Follow-up Consulta | UTILITY | {nome}, {prestador} | Pós-atendimento |
| `nps_v1` | Pesquisa NPS | UTILITY | {nome} | Satisfação |
| `alerta_saude_v1` | Alerta de Saúde | UTILITY | {nome}, {alerta} | Proativo crítico |

### 13.2 Conteúdo dos Templates

**boas_vindas_v2:**
```
Olá {nome}! 👋

Bem-vindo(a) à AUSTA Saúde! 

Sou a Ana, sua assistente virtual, e vou te acompanhar nessa jornada de cuidado com a sua saúde.

Para começarmos, preciso conhecer você melhor. Vamos fazer um breve questionário sobre sua saúde? Leva menos de 5 minutos! ⏱️

Seu plano: {plano}

Posso começar? Responda:
👍 SIM, vamos lá!
⏰ Agora não, me lembre depois
```

**autorizacao_aprovada_v1:**
```
✅ Boa notícia, {nome}!

Sua autorização foi *aprovada*!

📋 *Detalhes:*
• Número: {numero}
• Procedimento: {procedimento}
• Validade: {validade}

Você já pode agendar diretamente com o prestador.

Precisa de ajuda para agendar? É só me chamar! 😊
```

**nps_v1:**
```
Olá {nome}! 😊

Queremos saber sua opinião sobre nosso atendimento.

Em uma escala de 0 a 10, o quanto você recomendaria a AUSTA para um amigo ou familiar?

Responda com um número de 0 a 10.

Sua opinião é muito importante para melhorarmos sempre! 💙
```

### 13.3 Personalização por Perfil

| Perfil | Adaptações |
|--------|------------|
| **Idoso (>65 anos)** | Fonte maior (se possível), linguagem simples, menos emojis, preferência por voz |
| **Gestante** | Tom acolhedor, foco em pré-natal, conteúdo específico |
| **Crônico** | Mensagens de encorajamento, lembretes de adesão, metas |
| **Alto Risco** | Urgência clara, contato telefônico como backup |
| **Jovem** | Mais informal, emojis, gamificação |
| **PJ/Executivo** | Tom profissional, direto, horários flexíveis |

### 13.4 Horários de Envio

| Tipo de Mensagem | Horário Permitido | Frequência Máxima |
|------------------|-------------------|-------------------|
| Transacional (autorização, confirmação) | 24h | Sem limite |
| Utility (lembretes, alertas) | 08:00 - 20:00 | 1/dia por tipo |
| Marketing (campanhas) | 09:00 - 18:00 | 1/semana |
| Emergência (saúde) | 24h | Sem limite |

---

## 14. MÉTRICAS E OBSERVABILIDADE

### 14.1 KPIs de Processo

| Processo | KPI | Fórmula | Meta | Frequência |
|----------|-----|---------|------|------------|
| SUB-001 | Taxa de Conclusão Onboarding | Concluídos / Iniciados | >85% | Diário |
| SUB-001 | Tempo Médio Onboarding | Média(dataFim - dataInício) | <5 dias | Diário |
| SUB-002 | Taxa de Proatividade | Contatos proativos / Base ativa | >30% | Diário |
| SUB-002 | Taxa de Engajamento Proativo | Respostas / Mensagens enviadas | >40% | Diário |
| SUB-003 | Tempo Primeira Resposta | Média(primeiraResposta - entrada) | <3 min | Tempo real |
| SUB-003 | Taxa de Roteamento Correto | Resolvidos na 1ª camada / Total | >80% | Diário |
| SUB-004 | Taxa de Resolução Self-Service | Resolvidos SS / Roteados SS | >95% | Diário |
| SUB-004 | Tempo Médio Resolução SS | Média(tempoResolução) | <60s | Tempo real |
| SUB-005 | Taxa de Resolução Agente IA | Resolvidos IA / Roteados IA | >85% | Diário |
| SUB-006 | Taxa de Automação Autorização | Auto-aprovadas / Total | >85% | Diário |
| SUB-006 | Tempo Médio Autorização | Por tipo de procedimento | Varia | Diário |
| SUB-007 | Taxa de Conclusão Jornada | Concluídas / Iniciadas | >80% | Semanal |
| SUB-007 | Taxa Direcionamento Rede Pref | Rede preferencial / Total | >70% | Semanal |
| SUB-008 | Taxa de Adesão Programa | Aderentes / Elegíveis | >60% | Mensal |
| SUB-008 | Taxa Atingimento Metas | Metas atingidas / Total metas | >70% | Mensal |
| SUB-009 | Tempo Médio Resolução Reclamação | Por origem | Varia | Semanal |
| SUB-009 | Taxa de Reincidência | Reclamações repetidas / Total | <10% | Mensal |
| SUB-010 | NPS Global | (Promotores - Detratores) / Respostas | >50 | Mensal |
| SUB-010 | Taxa de Resposta NPS | Respostas / Pesquisas enviadas | >30% | Mensal |

### 14.2 Pontos de Medição (Instrumentation)

```xml
<!-- Exemplo: Listener para métricas -->
<bpmn:serviceTask id="ServiceTask_Exemplo" name="Tarefa Exemplo">
  <bpmn:extensionElements>
    <camunda:executionListener event="start" 
                               delegateExpression="${metricsStartListener}"/>
    <camunda:executionListener event="end" 
                               delegateExpression="${metricsEndListener}"/>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

**MetricsListener (Java):**
```java
@Component("metricsStartListener")
public class MetricsStartListener implements ExecutionListener {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Override
    public void notify(DelegateExecution execution) {
        String processId = execution.getProcessDefinitionId();
        String activityId = execution.getCurrentActivityId();
        
        // Registrar início
        execution.setVariable("_metrics_startTime", System.currentTimeMillis());
        
        // Incrementar contador
        meterRegistry.counter("bpmn.activity.started", 
            "process", processId, 
            "activity", activityId)
            .increment();
    }
}

@Component("metricsEndListener")
public class MetricsEndListener implements ExecutionListener {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Override
    public void notify(DelegateExecution execution) {
        String processId = execution.getProcessDefinitionId();
        String activityId = execution.getCurrentActivityId();
        Long startTime = (Long) execution.getVariable("_metrics_startTime");
        
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Registrar duração
            meterRegistry.timer("bpmn.activity.duration",
                "process", processId,
                "activity", activityId)
                .record(Duration.ofMillis(duration));
        }
        
        // Incrementar contador de conclusão
        meterRegistry.counter("bpmn.activity.completed",
            "process", processId,
            "activity", activityId)
            .increment();
    }
}
```

### 14.3 Dashboards

| Dashboard | Público | Métricas Principais | Refresh |
|-----------|---------|---------------------|---------|
| **Operacional Tempo Real** | Central de Experiência | Fila atual, SLA, tempo resposta | 30s |
| **Gestão de Proatividade** | Coordenação | Taxa proatividade, gatilhos ativados, engajamento | 5 min |
| **Performance Autorização** | Regulação | Taxa automação, tempo médio, backlog | 5 min |
| **Jornadas de Cuidado** | Navegação | Jornadas ativas, conclusões, abandonos | 15 min |
| **Gestão de Crônicos** | Coord. Crônicos | Adesão, metas, alertas | 1h |
| **Satisfação (NPS)** | Diretoria | NPS por segmento, detratores, tendências | 1h |
| **Executivo** | C-Level | KPIs consolidados, sinistralidade, ROI | Diário |

### 14.4 Alertas

| Alerta | Condição | Severidade | Destinatário | Ação |
|--------|----------|------------|--------------|------|
| SLA Crítico | >90% do SLA consumido | Alta | Coordenador | Escalar tarefa |
| Fila Alta | Fila > 2x capacidade | Média | Gestor | Realocar recursos |
| Taxa Automação Baixa | <80% em 1h | Média | Tech Lead | Investigar |
| Integração Down | Falha >3 min | Crítica | Ops/Dev | Incident |
| NPS Detrator | Score <5 | Alta | Ouvidoria | Contato imediato |
| Risco Internação | Score >90% | Crítica | Navegador + Médico | Intervenção |
| Fraude Suspeita | Pattern match | Crítica | Compliance | Bloqueio |

---

## 15. COMPLIANCE E LGPD

### 15.1 Requisitos de Consentimento

| Tratamento | Base Legal | Consentimento | Coleta |
|------------|------------|---------------|--------|
| Dados cadastrais | Execução de contrato | Não necessário | Adesão |
| Dados de saúde (screening) | Consentimento explícito | Sim, específico | Onboarding |
| Comunicação proativa saúde | Consentimento | Sim | Onboarding |
| Comunicação marketing | Consentimento | Sim, separado | Pós-onboarding |
| Analytics e BI | Legítimo interesse | Não necessário | Automático |
| Treinamento de IA | Consentimento | Sim, específico | Onboarding |
| Compartilhamento com prestadores | Execução de contrato | Não necessário | Por demanda |

### 15.2 Fluxo de Coleta de Consentimento

```
[No SUB-001 - Onboarding]

┌─────────────────────────────────────────────────────────────────────────────┐
│ [Subprocess] Coleta de Consentimentos LGPD                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Send Task] Apresentar Política de Privacidade                      │    │
│  │ • Link para política completa                                       │    │
│  │ • Resumo dos principais pontos                                      │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Send Task] Solicitar Consentimentos Específicos                    │    │
│  │                                                                      │    │
│  │ 1. "Autorizo a AUSTA a coletar e processar meus dados de saúde      │    │
│  │    para personalizar meu cuidado" [Obrigatório para screening]      │    │
│  │                                                                      │    │
│  │ 2. "Autorizo receber comunicações proativas sobre minha saúde       │    │
│  │    via WhatsApp" [Obrigatório para proatividade]                    │    │
│  │                                                                      │    │
│  │ 3. "Autorizo o uso dos meus dados anonimizados para melhorar        │    │
│  │    os modelos de IA" [Opcional]                                     │    │
│  │                                                                      │    │
│  │ 4. "Desejo receber comunicações de marketing e campanhas"           │    │
│  │    [Opcional]                                                        │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Receive Task] Capturar Respostas                                   │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Service Task] Registrar Consentimentos                             │    │
│  │ • Data/hora do consentimento                                        │    │
│  │ • Versão da política                                                │    │
│  │ • IP/Device (se disponível)                                         │    │
│  │ • Detalhes de cada consentimento                                    │    │
│  └────────────────────────────┬────────────────────────────────────────┘    │
│                               │                                              │
│                               ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ [Exclusive Gateway] "Consentimentos obrigatórios OK?"               │    │
│  └───────┬─────────────────────────────────────────────────────┬───────┘    │
│          │ SIM                                                 │ NÃO        │
│          │                                                     ▼            │
│          │                               ┌─────────────────────────────────┐│
│          │                               │ [Send Task] Informar que        ││
│          │                               │ consentimento é necessário      ││
│          │                               │ para prosseguir                 ││
│          │                               └───────────────┬─────────────────┘│
│          │                                               │                  │
│          │                                               └──► Retry ou END  │
│          │                                                                  │
│          └──► Continuar onboarding                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 15.3 Direitos do Titular (LGPD)

| Direito | Processo | SLA | Implementação |
|---------|----------|-----|---------------|
| **Acesso** | Portal + WhatsApp | 15 dias | Export automático de dados |
| **Retificação** | Portal + Atendimento | 15 dias | Formulário + validação |
| **Eliminação** | Portal + Ouvidoria | 15 dias | Anonimização (não exclusão total por regulação saúde) |
| **Portabilidade** | Portal + Ouvidoria | 15 dias | Export em formato estruturado |
| **Revogação de Consentimento** | Portal + WhatsApp | Imediato | Desativa tratamentos opcionais |
| **Oposição** | Portal + Ouvidoria | 15 dias | Análise caso a caso |

### 15.4 Anonimização para Analytics

```sql
-- View anonimizada para treinamento de ML
CREATE VIEW vw_dados_anonimizados AS
SELECT 
    MD5(beneficiario_id) as id_hash,
    EXTRACT(YEAR FROM AGE(data_nascimento)) as faixa_etaria,
    CASE 
        WHEN EXTRACT(YEAR FROM AGE(data_nascimento)) < 30 THEN '18-29'
        WHEN EXTRACT(YEAR FROM AGE(data_nascimento)) < 50 THEN '30-49'
        WHEN EXTRACT(YEAR FROM AGE(data_nascimento)) < 65 THEN '50-64'
        ELSE '65+'
    END as grupo_etario,
    sexo,
    SUBSTRING(cep, 1, 3) || '0000' as regiao_cep,
    classificacao_risco,
    score_risco,
    array_agg(DISTINCT condicao_cronica) as condicoes,
    COUNT(DISTINCT utilizacao_id) as qtd_utilizacoes,
    SUM(valor_utilizacao) as valor_total
FROM beneficiarios b
LEFT JOIN utilizacoes u ON b.id = u.beneficiario_id
WHERE b.consentimento_analytics = true
GROUP BY 1,2,3,4,5,6,7;
```

---

## 16. REQUISITOS DE SAÍDA

### 16.1 Documentação Textual

Para cada processo/subprocesso, entregar:

**A) Ficha Técnica:**
```yaml
id: SUB-XXX
nome: Nome do Subprocesso
versao: 2.0
owner: Cargo do responsável
sla_global: Tempo máximo
triggers:
  - tipo: Message/Timer/Signal
    nome: Nome do trigger
    dados: [lista de dados recebidos]
entradas:
  - dado: Nome
    tipo: Tipo
    obrigatorio: sim/não
    fonte: Origem
saidas:
  - dado: Nome
    tipo: Tipo
    destino: Para onde vai
    uso: Como é usado
```

**B) Descrição do Fluxo:**
- Narrativa passo a passo
- Decisões e condições
- Exceções e tratamentos
- Pontos de automação

**C) Integrações:**
- Sistemas envolvidos
- APIs necessárias
- Eventos publicados/consumidos

### 16.2 Arquivos BPMN

Entregar arquivos `.bpmn` válidos contendo:

**Estrutura XML:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions 
    xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
    xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
    xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
    xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
    xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    id="Definitions_1"
    targetNamespace="http://austa.com.br/bpmn/operadora-digital"
    exporter="Camunda Modeler"
    exporterVersion="5.x">
```

**Elementos obrigatórios:**
- IDs únicos para todos os elementos
- Nomes descritivos
- Extensões Camunda configuradas
- BPMNDI com coordenadas completas

### 16.3 Tabelas DMN

Entregar arquivos `.dmn` para cada Business Rule Task:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
             xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/"
             xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/"
             xmlns:camunda="http://camunda.org/schema/1.0/dmn"
             id="Definitions_DMN"
             name="Regras de Negócio"
             namespace="http://austa.com.br/dmn">
             
  <decision id="DMN_ClassificarUrgencia" name="Classificar Urgência">
    <decisionTable id="DecisionTable_1" hitPolicy="FIRST">
      <!-- Inputs e Outputs -->
    </decisionTable>
  </decision>
</definitions>
```

---

## 17. VALIDAÇÕES E CHECKLIST

### 17.1 Checklist de Completude

- [ ] Processo orquestrador principal modelado
- [ ] Todos os 10 subprocessos detalhados
- [ ] Todas as lanes definidas em cada processo
- [ ] Message flows entre pools especificados
- [ ] Variáveis de processo documentadas
- [ ] Correlation keys definidas
- [ ] Catálogo de erros completo
- [ ] DMNs para todas as Business Rule Tasks
- [ ] Templates de comunicação listados
- [ ] Integrações especificadas
- [ ] Eventos Kafka documentados
- [ ] Métricas definidas

### 17.2 Checklist de Consistência BPMN

- [ ] Todo Start Event tem pelo menos um outgoing flow
- [ ] Todo End Event tem pelo menos um incoming flow
- [ ] Gateways têm flows de entrada e saída corretos
- [ ] Exclusive Gateways têm condições em todos os flows
- [ ] Parallel Gateways têm correspondente Join
- [ ] Nenhuma atividade "solta" (sem conexão)
- [ ] Loops têm condição de saída clara
- [ ] Boundary Events anexados corretamente
- [ ] Subprocesses têm Start e End Events internos

### 17.3 Checklist de Compatibilidade Camunda 7

- [ ] Apenas elementos suportados utilizados
- [ ] Extensões Camunda corretas (delegateExpression, type="external", etc.)
- [ ] formKey definido para User Tasks
- [ ] candidateGroups/candidateUsers configurados
- [ ] Retry configuration para Service Tasks
- [ ] historyTimeToLive definido
- [ ] asyncBefore/asyncAfter onde necessário
- [ ] inputOutput mappings em Call Activities

### 17.4 Checklist de Executabilidade

- [ ] Fluxo simulável do início ao fim
- [ ] Todas as decisões têm todos os caminhos mapeados
- [ ] Tratamento de exceções em pontos críticos
- [ ] Boundary timers para SLAs
- [ ] Dead letter queue para falhas
- [ ] Compensação para rollback se necessário

### 17.5 Validação de XML

```bash
# Comando para validar XML BPMN
camunda-bpmn-model-validator validate *.bpmn

# Importar no Camunda Modeler e verificar:
# - Sem warnings
# - Sem erros de parsing
# - Visualização correta de todos elementos
```

---

## ANEXO: EXEMPLO DE ESTRUTURA XML COMPLETA

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  id="Definitions_OperadoraDigital"
                  targetNamespace="http://austa.com.br/bpmn/operadora-digital"
                  exporter="Camunda Modeler"
                  exporterVersion="5.20.0">

  <!-- Messages -->
  <bpmn:message id="Msg_NovaAdesao" name="NovaAdesaoConfirmada"/>
  <bpmn:message id="Msg_Interacao" name="InteracaoRecebida"/>
  <bpmn:message id="Msg_Resposta" name="RespostaBeneficiario"/>
  
  <!-- Errors -->
  <bpmn:error id="Error_TasyIndisponivel" name="Tasy Indisponível" errorCode="ERR-004"/>
  <bpmn:error id="Error_Timeout" name="Timeout" errorCode="ERR-003"/>
  
  <!-- Signals -->
  <bpmn:signal id="Signal_OnboardingConcluido" name="OnboardingConcluido"/>
  <bpmn:signal id="Signal_AlertaAltoRisco" name="AlertaAltoRisco"/>

  <!-- Collaboration -->
  <bpmn:collaboration id="Collaboration_OperadoraDigital">
    <bpmn:participant id="Participant_Operadora" 
                      name="Operadora de Saúde Digital" 
                      processRef="PROC-ORC-001"/>
    <bpmn:participant id="Participant_Prestadores" 
                      name="Rede de Prestadores"/>
    <bpmn:messageFlow id="MsgFlow_1" 
                      sourceRef="SendTask_NotificarPrestador" 
                      targetRef="Participant_Prestadores"/>
  </bpmn:collaboration>

  <!-- Main Process -->
  <bpmn:process id="PROC-ORC-001" 
                name="Orquestração do Cuidado e Experiência" 
                isExecutable="true"
                camunda:historyTimeToLive="365"
                camunda:versionTag="2.0">
    
    <!-- Lane Set -->
    <bpmn:laneSet id="LaneSet_1">
      <bpmn:lane id="Lane_Beneficiario" name="Beneficiário">
        <bpmn:flowNodeRef>StartEvent_NovaAdesao</bpmn:flowNodeRef>
      </bpmn:lane>
      <bpmn:lane id="Lane_Sistemas" name="Sistemas">
        <bpmn:flowNodeRef>ServiceTask_InicializarContexto</bpmn:flowNodeRef>
        <bpmn:flowNodeRef>CallActivity_Onboarding</bpmn:flowNodeRef>
      </bpmn:lane>
    </bpmn:laneSet>

    <!-- Start Event -->
    <bpmn:startEvent id="StartEvent_NovaAdesao" name="Nova Adesão">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
      <bpmn:messageEventDefinition id="MsgEvtDef_1" messageRef="Msg_NovaAdesao"/>
    </bpmn:startEvent>

    <!-- Service Task -->
    <bpmn:serviceTask id="ServiceTask_InicializarContexto" 
                      name="Inicializar Contexto"
                      camunda:delegateExpression="${contextoInitService}">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- Call Activity -->
    <bpmn:callActivity id="CallActivity_Onboarding" 
                       name="Onboarding" 
                       calledElement="SUB-001">
      <bpmn:extensionElements>
        <camunda:in source="beneficiarioId" target="beneficiarioId"/>
        <camunda:out source="perfilSaude" target="perfilSaude"/>
      </bpmn:extensionElements>
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
    </bpmn:callActivity>

    <!-- End Event -->
    <bpmn:endEvent id="EndEvent_1" name="Jornada Encerrada">
      <bpmn:incoming>Flow_3</bpmn:incoming>
    </bpmn:endEvent>

    <!-- Sequence Flows -->
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_NovaAdesao" targetRef="ServiceTask_InicializarContexto"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="ServiceTask_InicializarContexto" targetRef="CallActivity_Onboarding"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="CallActivity_Onboarding" targetRef="EndEvent_1"/>

  </bpmn:process>

  <!-- BPMNDI -->
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Collaboration_OperadoraDigital">
      
      <bpmndi:BPMNShape id="Participant_Operadora_di" bpmnElement="Participant_Operadora" isHorizontal="true">
        <dc:Bounds x="160" y="80" width="1200" height="400"/>
        <bpmndi:BPMNLabel/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="Lane_Beneficiario_di" bpmnElement="Lane_Beneficiario" isHorizontal="true">
        <dc:Bounds x="190" y="80" width="1170" height="150"/>
        <bpmndi:BPMNLabel/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="Lane_Sistemas_di" bpmnElement="Lane_Sistemas" isHorizontal="true">
        <dc:Bounds x="190" y="230" width="1170" height="250"/>
        <bpmndi:BPMNLabel/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="StartEvent_NovaAdesao_di" bpmnElement="StartEvent_NovaAdesao">
        <dc:Bounds x="242" y="137" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="230" y="180" width="60" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="ServiceTask_InicializarContexto_di" bpmnElement="ServiceTask_InicializarContexto">
        <dc:Bounds x="350" y="300" width="100" height="80"/>
        <bpmndi:BPMNLabel/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="CallActivity_Onboarding_di" bpmnElement="CallActivity_Onboarding">
        <dc:Bounds x="520" y="300" width="100" height="80"/>
        <bpmndi:BPMNLabel/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="692" y="322" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="668" y="365" width="84" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="278" y="155"/>
        <di:waypoint x="314" y="155"/>
        <di:waypoint x="314" y="340"/>
        <di:waypoint x="350" y="340"/>
      </bpmndi:BPMNEdge>

      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="450" y="340"/>
        <di:waypoint x="520" y="340"/>
      </bpmndi:BPMNEdge>

      <bpmndi:BPMNEdge id="Flow_3_di" bpmnElement="Flow_3">
        <di:waypoint x="620" y="340"/>
        <di:waypoint x="692" y="340"/>
      </bpmndi:BPMNEdge>

    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>

</bpmn:definitions>
```

---

**FIM DO PROMPT TÉCNICO v2.0**

*Documento preparado para geração de processos BPMN executáveis no Camunda Platform 7*
*Operadora Digital do Futuro - AUSTA Saúde*
*Versão 2.0 - Dezembro 2024*
