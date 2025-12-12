# PROMPT TÉCNICO: Implementação DMN, Service Layer e Testes de Integração

## Visão Geral do Projeto

Este documento especifica as três fases finais de implementação do projeto **Experiência Digital do Cliente - AUSTA**:

| Fase | Swarm | Entregáveis | Estimativa |
|------|-------|-------------|------------|
| **FASE 1** | DMN Implementation | 11 arquivos de Decision Tables | 2-3 dias |
| **FASE 2** | Backend Service | 71 Delegate Beans Java | 5-7 dias |
| **FASE 3** | Integration Test | Suíte completa de testes E2E | 3-4 dias |

**Caminho base do projeto:** 
```
/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/
```

---

## Documentos de Referência Obrigatória

Antes de iniciar qualquer implementação, leia e siga rigorosamente:

```
docs/architecture/adr/ADR-001-build-tool-selection.md
docs/architecture/adr/ADR-002-module-organization.md
docs/architecture/adr/ADR-003-integration-patterns.md
docs/architecture/00_ARCHITECTURE_OVERVIEW.md
docs/architecture/01_PROJECT_STRUCTURE.md
docs/architecture/02_DEPENDENCY_SPECIFICATION.md
docs/architecture/03_DATABASE_SCHEMA.md
docs/architecture/04_KAFKA_ARCHITECTURE.md
docs/architecture/05_DEPLOYMENT_STRATEGY.md
docs/architecture/06_INTEGRATION_ARCHITECTURE.md
```

**Arquivos BPMN de referência (já implementados):**
```
src/bpmn/PROC-ORC-001_Orquestracao_Cuidado_Experiencia.bpmn
src/bpmn/SUB-001_Onboarding_Inteligente.bpmn
src/bpmn/SUB-002_Motor_Proativo.bpmn
src/bpmn/SUB-003_Recepcao_Classificacao.bpmn
src/bpmn/SUB-004_Self_Service.bpmn
src/bpmn/SUB-005_Agentes_IA.bpmn
src/bpmn/SUB-006_Autorizacao_Inteligente.bpmn
src/bpmn/SUB-007_Navegacao_Cuidado.bpmn
src/bpmn/SUB-008_Gestao_Cronicos.bpmn
src/bpmn/SUB-009_Gestao_Reclamacoes.bpmn
src/bpmn/SUB-010_Follow_Up_Feedback.bpmn
```

---

# FASE 1: DMN IMPLEMENTATION SWARM

## Objetivo

Criar **11 arquivos DMN** (Decision Model and Notation) que implementam as regras de negócio referenciadas nos arquivos BPMN através de `<bpmn:businessRuleTask>`.

## Localização dos Arquivos

```
src/main/resources/dmn/
├── DMN-001_Estratificacao_Risco.dmn
├── DMN-002_Deteccao_CPT.dmn
├── DMN-003_Classificacao_Urgencia.dmn
├── DMN-004_Roteamento_Demanda.dmn
├── DMN-005_Regras_Autorizacao.dmn
├── DMN-006_Protocolo_Clinico.dmn
├── DMN-007_Identificacao_Gatilhos.dmn
├── DMN-008_Elegibilidade_Programa.dmn
├── DMN-009_Prioridade_Atendimento.dmn
├── DMN-010_Classificacao_Reclamacao.dmn
└── DMN-011_Calculo_NPS.dmn
```

---

## Especificação Detalhada de Cada DMN

### DMN-001: Estratificação de Risco

**Referenciado em:** SUB-001_Onboarding_Inteligente.bpmn
**Decision ID:** `DMN_EstratificacaoRisco`
**Hit Policy:** `COLLECT` (Sum)

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `idade` | integer | Idade do beneficiário em anos |
| `imc` | double | Índice de Massa Corporal |
| `qtdComorbidades` | integer | Quantidade de condições crônicas |
| `historicoFamiliar` | boolean | Possui histórico familiar de doenças graves |
| `tabagista` | boolean | É fumante ativo |
| `etilista` | boolean | Consumo regular de álcool |
| `sedentario` | boolean | Não pratica exercícios regularmente |
| `scoreComportamental` | integer | Score de engajamento no screening (0-100) |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `scoreRisco` | integer | Score de risco calculado (0-100) |
| `classificacaoRisco` | string | BAIXO, MODERADO, ALTO, COMPLEXO |
| `frequenciaContato` | string | MENSAL, QUINZENAL, SEMANAL, DIARIO |

#### Decision Table

```
| idade | imc | qtdComorbidades | historicoFamiliar | tabagista | sedentario | scoreRisco | classificacaoRisco |
|-------|-----|-----------------|-------------------|-----------|------------|------------|-------------------|
| < 40  | < 25 | 0 | false | false | false | 10 | BAIXO |
| < 40  | < 25 | 0 | false | false | true | 15 | BAIXO |
| < 40  | < 25 | 0 | true | - | - | 20 | BAIXO |
| < 40  | [25..30) | [1..2] | - | false | - | 25 | MODERADO |
| < 40  | >= 30 | [1..2] | - | - | - | 35 | MODERADO |
| [40..60) | < 25 | 0 | false | false | - | 20 | BAIXO |
| [40..60) | < 25 | [1..2] | - | - | - | 35 | MODERADO |
| [40..60) | [25..30) | [1..3] | - | - | - | 45 | MODERADO |
| [40..60) | >= 30 | >= 2 | - | - | - | 55 | ALTO |
| [40..60) | - | >= 3 | true | true | - | 70 | ALTO |
| >= 60 | < 25 | [0..1] | false | false | - | 30 | MODERADO |
| >= 60 | < 25 | [2..3] | - | - | - | 50 | ALTO |
| >= 60 | >= 25 | >= 2 | - | - | - | 65 | ALTO |
| >= 60 | - | >= 4 | - | - | - | 80 | COMPLEXO |
| - | >= 35 | >= 3 | true | true | true | 85 | COMPLEXO |
| - | - | >= 5 | - | - | - | 90 | COMPLEXO |
```

#### Regra de Frequência de Contato (Tabela Auxiliar)

```
| classificacaoRisco | frequenciaContato |
|--------------------|-------------------|
| BAIXO | MENSAL |
| MODERADO | QUINZENAL |
| ALTO | SEMANAL |
| COMPLEXO | DIARIO |
```

---

### DMN-002: Detecção de CPT (Cobertura Parcial Temporária)

**Referenciado em:** SUB-001_Onboarding_Inteligente.bpmn
**Decision ID:** `DMN_DeteccaoCPT`
**Hit Policy:** `FIRST`

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `condicoesDetectadas` | list<string> | Lista de condições identificadas no screening |
| `exameAlteradoRecente` | boolean | Exame alterado nos últimos 6 meses |
| `medicamentoUsoRegular` | boolean | Uso regular de medicamento controlado |
| `procedimentoRecente` | boolean | Procedimento/cirurgia nos últimos 12 meses |
| `internacaoRecente` | boolean | Internação nos últimos 24 meses |
| `scoreComportamental` | integer | Score de consistência nas respostas |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `statusCPT` | string | NENHUM, SUSPEITA, CONFIRMADO |
| `condicoesSuspeitas` | list<string> | Condições que requerem investigação |
| `nivelConfianca` | integer | Confiança da detecção (0-100) |
| `acaoRequerida` | string | LIBERAR, INVESTIGAR, APLICAR_CPT |

#### Decision Table

```
| condicoesDetectadas | exameAlteradoRecente | medicamentoUsoRegular | scoreComportamental | statusCPT | nivelConfianca | acaoRequerida |
|---------------------|----------------------|-----------------------|---------------------|-----------|----------------|---------------|
| empty | false | false | >= 80 | NENHUM | 95 | LIBERAR |
| not empty | false | false | >= 80 | SUSPEITA | 40 | INVESTIGAR |
| not empty | true | false | >= 60 | SUSPEITA | 60 | INVESTIGAR |
| not empty | false | true | >= 60 | SUSPEITA | 70 | INVESTIGAR |
| not empty | true | true | >= 50 | CONFIRMADO | 80 | APLICAR_CPT |
| not empty | - | - | < 50 | SUSPEITA | 50 | INVESTIGAR |
| contains "DIABETES" | true | true | - | CONFIRMADO | 90 | APLICAR_CPT |
| contains "HIPERTENSAO" | true | true | - | CONFIRMADO | 85 | APLICAR_CPT |
| contains "CARDIOPATIA" | - | true | - | CONFIRMADO | 90 | APLICAR_CPT |
| contains "CANCER" | - | - | - | CONFIRMADO | 95 | APLICAR_CPT |
```

---

### DMN-003: Classificação de Urgência

**Referenciado em:** SUB-003_Recepcao_Classificacao.bpmn
**Decision ID:** `DMN_ClassificacaoUrgencia`
**Hit Policy:** `FIRST`

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `intencaoDetectada` | string | Intenção classificada pelo NLP |
| `entidadesExtraidas` | list<string> | Entidades identificadas na mensagem |
| `sentimento` | string | POSITIVO, NEUTRO, NEGATIVO, CRITICO |
| `scoreRisco` | integer | Score de risco do beneficiário |
| `idade` | integer | Idade do beneficiário |
| `comorbidades` | list<string> | Condições crônicas ativas |
| `historicoUrgencias` | integer | Quantidade de urgências nos últimos 12 meses |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `nivelUrgencia` | string | EMERGENCIA, URGENTE, ROTINA, PREVENTIVO |
| `slaMinutos` | integer | SLA em minutos para primeira resposta |
| `flagsAlerta` | list<string> | Alertas especiais para o atendente |
| `prioridadeFila` | integer | Prioridade na fila (1-10, sendo 1 mais urgente) |

#### Decision Table

```
| intencaoDetectada | sentimento | scoreRisco | idade | nivelUrgencia | slaMinutos | prioridadeFila |
|-------------------|------------|------------|-------|---------------|------------|----------------|
| "EMERGENCIA_MEDICA" | - | - | - | EMERGENCIA | 0 | 1 |
| "DOR_TORACICA" | - | - | - | EMERGENCIA | 0 | 1 |
| "DISPNEIA_SEVERA" | - | - | - | EMERGENCIA | 0 | 1 |
| "SINTOMAS_AVC" | - | - | - | EMERGENCIA | 0 | 1 |
| "SINTOMAS_AGUDOS" | CRITICO | >= 70 | >= 60 | EMERGENCIA | 0 | 1 |
| "SINTOMAS_AGUDOS" | CRITICO | >= 70 | < 60 | URGENTE | 5 | 2 |
| "SINTOMAS_AGUDOS" | NEGATIVO | [50..70) | - | URGENTE | 10 | 3 |
| "SINTOMAS_AGUDOS" | - | < 50 | - | URGENTE | 15 | 4 |
| "RECLAMACAO" | CRITICO | - | - | URGENTE | 15 | 3 |
| "AUTORIZACAO" | - | >= 70 | - | URGENTE | 30 | 4 |
| "AUTORIZACAO" | - | < 70 | - | ROTINA | 60 | 6 |
| "AGENDAMENTO" | - | >= 70 | - | ROTINA | 30 | 5 |
| "AGENDAMENTO" | - | < 70 | - | ROTINA | 180 | 7 |
| "INFORMACAO" | - | - | - | ROTINA | 180 | 8 |
| "PREVENTIVO" | - | - | - | PREVENTIVO | 1440 | 9 |
| - | - | - | - | ROTINA | 180 | 7 |
```

---

### DMN-004: Roteamento de Demanda

**Referenciado em:** SUB-003_Recepcao_Classificacao.bpmn
**Decision ID:** `DMN_RoteamentoDemanda`
**Hit Policy:** `FIRST`

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `tipoDemanda` | string | Tipo da demanda identificada |
| `nivelUrgencia` | string | Nível de urgência calculado |
| `complexidade` | string | BAIXA, MEDIA, ALTA |
| `scoreRisco` | integer | Score de risco do beneficiário |
| `tentativasAnteriores` | integer | Tentativas de resolução anteriores |
| `canalOrigem` | string | Canal de origem da demanda |
| `preferenciaCanal` | string | Preferência de canal do beneficiário |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `camadaDestino` | string | SELF_SERVICE, AGENTE_IA, NAVEGADOR, ESPECIALISTA |
| `filaDestino` | string | Nome da fila específica |
| `prioridade` | integer | Prioridade na fila destino |
| `motivoRoteamento` | string | Justificativa do roteamento |
| `skillsRequeridos` | list<string> | Skills necessários do atendente |

#### Decision Table

```
| tipoDemanda | nivelUrgencia | complexidade | scoreRisco | camadaDestino | filaDestino | skillsRequeridos |
|-------------|---------------|--------------|------------|---------------|-------------|------------------|
| "CARTEIRINHA" | - | BAIXA | - | SELF_SERVICE | "autoatendimento" | [] |
| "BOLETO" | - | BAIXA | - | SELF_SERVICE | "autoatendimento" | [] |
| "STATUS_AUTORIZACAO" | - | BAIXA | - | SELF_SERVICE | "autoatendimento" | [] |
| "FAQ" | - | BAIXA | - | SELF_SERVICE | "autoatendimento" | [] |
| "TRIAGEM_SINTOMAS" | ROTINA | BAIXA | < 50 | AGENTE_IA | "triagem_ia" | ["nlp"] |
| "TRIAGEM_SINTOMAS" | ROTINA | MEDIA | [50..70) | AGENTE_IA | "triagem_ia" | ["nlp", "clinico"] |
| "TRIAGEM_SINTOMAS" | URGENTE | - | >= 70 | NAVEGADOR | "navegacao_urgente" | ["enfermagem", "urgencia"] |
| "AUTORIZACAO" | ROTINA | BAIXA | - | AGENTE_IA | "autorizacao_ia" | ["regulacao"] |
| "AUTORIZACAO" | ROTINA | MEDIA | - | NAVEGADOR | "autorizacao_humana" | ["regulacao", "auditoria"] |
| "AUTORIZACAO" | URGENTE | ALTA | - | ESPECIALISTA | "auditoria_medica" | ["medico", "auditoria"] |
| "AGENDAMENTO" | - | BAIXA | - | AGENTE_IA | "agendamento_ia" | ["agenda"] |
| "AGENDAMENTO" | - | MEDIA | >= 70 | NAVEGADOR | "navegacao_agenda" | ["enfermagem", "agenda"] |
| "RECLAMACAO" | - | ALTA | - | NAVEGADOR | "ouvidoria" | ["ouvidoria", "resolucao"] |
| "REEMBOLSO" | - | - | - | AGENTE_IA | "reembolso_ia" | ["financeiro"] |
| - | EMERGENCIA | - | - | NAVEGADOR | "emergencia" | ["enfermagem", "urgencia"] |
| - | - | ALTA | >= 70 | NAVEGADOR | "alto_risco" | ["enfermagem", "gestao_risco"] |
| - | - | - | - | AGENTE_IA | "geral_ia" | ["atendimento"] |
```

---

### DMN-005: Regras de Autorização

**Referenciado em:** SUB-006_Autorizacao_Inteligente.bpmn
**Decision ID:** `DMN_RegrasAutorizacao`
**Hit Policy:** `FIRST`

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `tipoProcedimento` | string | Tipo do procedimento solicitado |
| `codigoTUSS` | string | Código TUSS do procedimento |
| `valorSolicitado` | double | Valor do procedimento |
| `elegibilidadeOK` | boolean | Beneficiário elegível |
| `coberturaOK` | boolean | Procedimento coberto pelo plano |
| `carenciaOK` | boolean | Carência cumprida |
| `cptBloqueio` | boolean | CPT bloqueia o procedimento |
| `limiteAnualDisponivel` | double | Limite anual disponível |
| `historicoSolicitacoes` | integer | Solicitações similares nos últimos 30 dias |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `podeAutorizarAutomaticamente` | boolean | Pode autorizar sem análise humana |
| `tipoAnaliseRequerida` | string | NENHUMA, PROTOCOLO, AUDITORIA_MEDICA |
| `motivoAnalise` | string | Motivo da necessidade de análise |
| `limiteValorAutomatico` | double | Valor máximo para autorização automática |
| `prazoResposta` | string | SLA de resposta |

#### Decision Table

```
| tipoProcedimento | elegibilidadeOK | coberturaOK | carenciaOK | cptBloqueio | valorSolicitado | podeAutorizarAutomaticamente | tipoAnaliseRequerida | prazoResposta |
|------------------|-----------------|-------------|------------|-------------|-----------------|------------------------------|----------------------|---------------|
| - | false | - | - | - | - | false | NENHUMA | "IMEDIATO" |
| - | - | false | - | - | - | false | NENHUMA | "IMEDIATO" |
| - | - | - | false | - | - | false | NENHUMA | "IMEDIATO" |
| - | - | - | - | true | - | false | AUDITORIA_MEDICA | "48H" |
| "CONSULTA_ELETIVA" | true | true | true | false | <= 500 | true | NENHUMA | "5MIN" |
| "SADT_SIMPLES" | true | true | true | false | <= 300 | true | NENHUMA | "5MIN" |
| "SADT_COMPLEXO" | true | true | true | false | <= 1000 | true | PROTOCOLO | "4H" |
| "SADT_COMPLEXO" | true | true | true | false | > 1000 | false | AUDITORIA_MEDICA | "24H" |
| "INTERNACAO_ELETIVA" | true | true | true | false | - | false | AUDITORIA_MEDICA | "24H" |
| "INTERNACAO_URGENCIA" | true | true | true | false | - | true | PROTOCOLO | "2H" |
| "CIRURGIA_ELETIVA" | true | true | true | false | - | false | AUDITORIA_MEDICA | "24H" |
| "HOME_CARE" | true | true | true | false | - | false | AUDITORIA_MEDICA | "48H" |
| "OPME" | true | true | true | false | - | false | AUDITORIA_MEDICA | "48H" |
```

---

### DMN-006: Protocolo Clínico

**Referenciado em:** SUB-006_Autorizacao_Inteligente.bpmn
**Decision ID:** `DMN_ProtocoloClinico`
**Hit Policy:** `FIRST`

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `codigoCID` | string | CID-10 principal |
| `codigoTUSS` | string | Código TUSS do procedimento |
| `idade` | integer | Idade do paciente |
| `sexo` | string | M ou F |
| `comorbidades` | list<string> | Comorbidades do paciente |
| `examesAnteriores` | list<string> | Exames já realizados |
| `tratamentosAnteriores` | list<string> | Tratamentos já realizados |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `protocoloAplicavel` | string | ID do protocolo aplicável |
| `atendeProtocolo` | boolean | Solicitação atende ao protocolo |
| `justificativaProtocolo` | string | Justificativa da análise |
| `examePrevioRequerido` | list<string> | Exames necessários antes |
| `quantidadeMaximaAutorizada` | integer | Quantidade máxima pelo protocolo |

#### Decision Table (Exemplo para alguns procedimentos)

```
| codigoCID | codigoTUSS | idade | sexo | atendeProtocolo | protocoloAplicavel | examePrevioRequerido |
|-----------|------------|-------|------|-----------------|--------------------|-----------------------|
| "E11.*" | "40302199" | - | - | true | "PROT_DIABETES_HBA1C" | [] |
| "E11.*" | "40316025" | - | - | true | "PROT_DIABETES_FUNDO" | ["40302199"] |
| "I10" | "40302040" | - | - | true | "PROT_HAS_CREATININA" | [] |
| "I10" | "40302067" | - | - | true | "PROT_HAS_POTASSIO" | [] |
| "Z12.3" | "40808050" | >= 40 | "F" | true | "PROT_MAMOGRAFIA_RASTREIO" | [] |
| "Z12.3" | "40808050" | < 40 | "F" | false | "PROT_MAMOGRAFIA_RASTREIO" | [] |
| "Z12.1" | "40808084" | >= 50 | - | true | "PROT_COLONOSCOPIA_RASTREIO" | [] |
| "Z12.1" | "40808084" | [40..50) | - | true | "PROT_COLONOSCOPIA_RISCO" | [] |
```

---

### DMN-007: Identificação de Gatilhos Proativos

**Referenciado em:** SUB-002_Motor_Proativo.bpmn
**Decision ID:** `DMN_IdentificacaoGatilhos`
**Hit Policy:** `COLLECT` (List)

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `diasSemCheckup` | integer | Dias desde último check-up |
| `diasMedicamentoAcabar` | integer | Dias até medicamento acabar |
| `temExameAlteradoSemRetorno` | boolean | Exame alterado sem consulta de retorno |
| `scorePreditivoInternacao` | integer | Score de predição de internação |
| `taxaAdesaoTratamento` | double | Taxa de adesão ao tratamento (0-1) |
| `diasSemConsulta` | integer | Dias desde última consulta |
| `diasPosAlta` | integer | Dias desde alta hospitalar |
| `gestanteSemPreNatal` | boolean | Gestante sem pré-natal em dia |
| `insatisfacaoDetectada` | boolean | Insatisfação detectada recentemente |
| `diasParaVencerCarencia` | integer | Dias para vencer carência |
| `mesAniversarioAdesao` | boolean | Mês de aniversário de adesão |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `gatilhosIdentificados` | list<string> | Lista de gatilhos ativados |
| `prioridade` | string | CRITICA, ALTA, MEDIA, BAIXA |
| `acaoRecomendada` | string | Ação a ser tomada |
| `canalPreferido` | string | Canal para comunicação |

#### Decision Table

```
| diasSemCheckup | gatilhosIdentificados | prioridade | acaoRecomendada |
|----------------|----------------------|------------|-----------------|
| >= 365 | ["GAT-001_CHECKUP_PENDENTE"] | MEDIA | "AGENDAR_CHECKUP" |

| diasMedicamentoAcabar | gatilhosIdentificados | prioridade | acaoRecomendada |
|-----------------------|----------------------|------------|-----------------|
| <= 7 | ["GAT-002_MEDICAMENTO_ACABANDO"] | ALTA | "RENOVAR_RECEITA" |

| temExameAlteradoSemRetorno | gatilhosIdentificados | prioridade | acaoRecomendada |
|---------------------------|----------------------|------------|-----------------|
| true | ["GAT-003_EXAME_SEM_RETORNO"] | ALTA | "AGENDAR_RETORNO" |

| scorePreditivoInternacao | gatilhosIdentificados | prioridade | acaoRecomendada |
|--------------------------|----------------------|------------|-----------------|
| >= 70 | ["GAT-004_RISCO_INTERNACAO"] | CRITICA | "CONTATO_NAVEGADOR" |

| taxaAdesaoTratamento | gatilhosIdentificados | prioridade | acaoRecomendada |
|----------------------|----------------------|------------|-----------------|
| < 0.6 | ["GAT-005_BAIXA_ADESAO"] | ALTA | "NUDGE_ADESAO" |

| diasSemConsulta | gatilhosIdentificados | prioridade | acaoRecomendada |
|-----------------|----------------------|------------|-----------------|
| >= 90 | ["GAT-008_GAP_IN_CARE"] | MEDIA | "CONTATO_PROATIVO" |

| diasPosAlta | gatilhosIdentificados | prioridade | acaoRecomendada |
|-------------|----------------------|------------|-----------------|
| <= 30 | ["GAT-009_POS_ALTA"] | ALTA | "FOLLOWUP_ALTA" |

| gestanteSemPreNatal | gatilhosIdentificados | prioridade | acaoRecomendada |
|---------------------|----------------------|------------|-----------------|
| true | ["GAT-010_GESTANTE_SEM_PRENATAL"] | CRITICA | "AGENDAR_PRENATAL" |

| insatisfacaoDetectada | gatilhosIdentificados | prioridade | acaoRecomendada |
|----------------------|----------------------|------------|-----------------|
| true | ["GAT-011_INSATISFACAO"] | ALTA | "CONTATO_OUVIDORIA" |

| diasParaVencerCarencia | gatilhosIdentificados | prioridade | acaoRecomendada |
|------------------------|----------------------|------------|-----------------|
| <= 7 | ["GAT-012_VENCIMENTO_CARENCIA"] | BAIXA | "NOTIFICAR_CARENCIA" |

| mesAniversarioAdesao | gatilhosIdentificados | prioridade | acaoRecomendada |
|----------------------|----------------------|------------|-----------------|
| true | ["GAT-007_ANIVERSARIO_ADESAO"] | BAIXA | "MENSAGEM_ANIVERSARIO" |
```

---

### DMN-008: Elegibilidade para Programa

**Referenciado em:** SUB-008_Gestao_Cronicos.bpmn
**Decision ID:** `DMN_ElegibilidadePrograma`
**Hit Policy:** `COLLECT` (List)

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `condicoesCronicas` | list<string> | Condições crônicas do beneficiário |
| `idade` | integer | Idade do beneficiário |
| `scoreRisco` | integer | Score de risco |
| `gestante` | boolean | É gestante |
| `internacaoRecente` | boolean | Internação nos últimos 90 dias |
| `temNavegadorAtribuido` | boolean | Já tem navegador |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `programasElegiveis` | list<string> | Programas para os quais é elegível |
| `programaPrioritario` | string | Programa de maior prioridade |
| `nivelIntensidade` | string | LEVE, MODERADO, INTENSIVO |

#### Decision Table

```
| condicoesCronicas | idade | scoreRisco | gestante | programasElegiveis | programaPrioritario |
|-------------------|-------|------------|----------|--------------------|--------------------|
| contains "DIABETES" | - | - | - | ["PROG_DIABETES"] | "PROG_DIABETES" |
| contains "HIPERTENSAO" | - | - | - | ["PROG_HIPERTENSAO"] | "PROG_HIPERTENSAO" |
| contains "DPOC" | - | - | - | ["PROG_DPOC"] | "PROG_DPOC" |
| contains "ICC" | - | - | - | ["PROG_ICC"] | "PROG_ICC" |
| contains "ASMA" | - | - | - | ["PROG_ASMA"] | "PROG_ASMA" |
| contains "OBESIDADE" | - | - | - | ["PROG_OBESIDADE"] | "PROG_OBESIDADE" |
| contains "DRC" | - | - | - | ["PROG_RENAL"] | "PROG_RENAL" |
| - | - | - | true | ["PROG_GESTANTE"] | "PROG_GESTANTE" |
| - | >= 65 | >= 50 | - | ["PROG_IDOSO"] | "PROG_IDOSO" |
| - | - | >= 70 | - | ["PROG_ALTO_RISCO"] | "PROG_ALTO_RISCO" |
```

---

### DMN-009: Prioridade de Atendimento

**Referenciado em:** SUB-007_Navegacao_Cuidado.bpmn
**Decision ID:** `DMN_PrioridadeAtendimento`
**Hit Policy:** `FIRST`

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `classificacaoRisco` | string | Classificação de risco do beneficiário |
| `tipoJornada` | string | Tipo da jornada de cuidado |
| `diasEmEspera` | integer | Dias aguardando atendimento |
| `tentativasContato` | integer | Tentativas de contato sem sucesso |
| `reclamacaoAberta` | boolean | Possui reclamação aberta |
| `vip` | boolean | Beneficiário VIP/especial |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `prioridadeFinal` | integer | Prioridade final (1-10) |
| `tempoMaximoEspera` | integer | Tempo máximo de espera em minutos |
| `navegadorPreferencial` | string | Tipo de navegador preferencial |
| `flagUrgente` | boolean | Flag de urgência |

#### Decision Table

```
| classificacaoRisco | tipoJornada | diasEmEspera | reclamacaoAberta | vip | prioridadeFinal | tempoMaximoEspera | flagUrgente |
|--------------------|-------------|--------------|------------------|-----|-----------------|-------------------|-------------|
| "COMPLEXO" | - | - | - | - | 1 | 30 | true |
| "ALTO" | "INTERNACAO" | - | - | - | 1 | 60 | true |
| "ALTO" | - | - | true | - | 2 | 60 | true |
| "ALTO" | - | - | - | true | 2 | 60 | true |
| "ALTO" | - | >= 3 | - | - | 2 | 60 | true |
| "ALTO" | - | - | - | - | 3 | 120 | false |
| "MODERADO" | - | - | true | - | 3 | 120 | false |
| "MODERADO" | - | - | - | true | 3 | 120 | false |
| "MODERADO" | - | >= 5 | - | - | 4 | 240 | false |
| "MODERADO" | - | - | - | - | 5 | 480 | false |
| "BAIXO" | - | - | true | - | 5 | 480 | false |
| "BAIXO" | - | - | - | - | 7 | 1440 | false |
```

---

### DMN-010: Classificação de Reclamação

**Referenciado em:** SUB-009_Gestao_Reclamacoes.bpmn
**Decision ID:** `DMN_ClassificacaoReclamacao`
**Hit Policy:** `FIRST`

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `canalOrigem` | string | Canal de origem da reclamação |
| `tipoReclamacao` | string | Tipo da reclamação |
| `sentimento` | string | Sentimento detectado |
| `mencionaOrgaoRegulador` | boolean | Menciona ANS, Procon, etc. |
| `reincidente` | boolean | Reclamação reincidente |
| `valorEnvolvido` | double | Valor financeiro envolvido |
| `tempoSemResposta` | integer | Tempo sem resposta em horas |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `criticidade` | string | BAIXA, MEDIA, ALTA, CRITICA |
| `slaHoras` | integer | SLA em horas |
| `responsavel` | string | Área responsável |
| `requerCompensacao` | boolean | Requer análise de compensação |
| `riscoCancelamento` | string | BAIXO, MEDIO, ALTO |

#### Decision Table

```
| canalOrigem | tipoReclamacao | mencionaOrgaoRegulador | reincidente | criticidade | slaHoras | responsavel | riscoCancelamento |
|-------------|----------------|------------------------|-------------|-------------|----------|-------------|-------------------|
| "ANS" | - | - | - | CRITICA | 24 | "OUVIDORIA" | ALTO |
| "PROCON" | - | - | - | CRITICA | 24 | "JURIDICO" | ALTO |
| - | - | true | - | CRITICA | 24 | "OUVIDORIA" | ALTO |
| - | "NEGATIVA_COBERTURA" | - | true | ALTA | 48 | "REGULACAO" | ALTO |
| - | "DEMORA_AUTORIZACAO" | - | true | ALTA | 48 | "REGULACAO" | MEDIO |
| - | "MAU_ATENDIMENTO" | - | - | MEDIA | 72 | "QUALIDADE" | MEDIO |
| - | "COBRANCA_INDEVIDA" | - | - | ALTA | 48 | "FINANCEIRO" | MEDIO |
| "RECLAME_AQUI" | - | - | - | ALTA | 24 | "OUVIDORIA" | ALTO |
| - | - | - | true | ALTA | 48 | "OUVIDORIA" | MEDIO |
| - | - | - | - | MEDIA | 72 | "ATENDIMENTO" | BAIXO |
```

---

### DMN-011: Cálculo de NPS

**Referenciado em:** SUB-010_Follow_Up_Feedback.bpmn
**Decision ID:** `DMN_CalculoNPS`
**Hit Policy:** `FIRST`

#### Inputs

| Input | Type | Description |
|-------|------|-------------|
| `notaNPS` | integer | Nota dada pelo beneficiário (0-10) |
| `comentario` | string | Comentário do beneficiário |
| `sentimentoComentario` | string | Sentimento do comentário |
| `jornadaAvaliada` | string | Tipo de jornada avaliada |
| `tempoResposta` | integer | Tempo de resposta em minutos |

#### Outputs

| Output | Type | Description |
|--------|------|-------------|
| `categoriaNPS` | string | DETRATOR, NEUTRO, PROMOTOR |
| `acaoRequerida` | string | Ação a ser tomada |
| `prioridadeContato` | string | Prioridade para contato de follow-up |
| `departamentoNotificado` | string | Departamento a ser notificado |

#### Decision Table

```
| notaNPS | sentimentoComentario | categoriaNPS | acaoRequerida | prioridadeContato | departamentoNotificado |
|---------|---------------------|--------------|---------------|-------------------|------------------------|
| [0..6] | "CRITICO" | DETRATOR | "CONTATO_IMEDIATO" | CRITICA | "OUVIDORIA" |
| [0..6] | "NEGATIVO" | DETRATOR | "CONTATO_24H" | ALTA | "QUALIDADE" |
| [0..6] | - | DETRATOR | "CONTATO_48H" | MEDIA | "QUALIDADE" |
| [7..8] | "NEGATIVO" | NEUTRO | "ANALISAR_COMENTARIO" | BAIXA | "QUALIDADE" |
| [7..8] | - | NEUTRO | "MONITORAR" | BAIXA | null |
| [9..10] | "POSITIVO" | PROMOTOR | "AGRADECER" | BAIXA | "MARKETING" |
| [9..10] | - | PROMOTOR | "SOLICITAR_INDICACAO" | BAIXA | "COMERCIAL" |
```

---

## Estrutura XML Padrão para DMN

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
             xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/"
             xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/"
             xmlns:camunda="http://camunda.org/schema/1.0/dmn"
             id="Definitions_DMN001"
             name="Estratificação de Risco"
             namespace="http://camunda.org/schema/1.0/dmn">
  
  <decision id="DMN_EstratificacaoRisco" name="Estratificação de Risco">
    <decisionTable id="DecisionTable_1" hitPolicy="COLLECT" aggregation="SUM">
      <input id="Input_idade" label="Idade" camunda:inputVariable="idade">
        <inputExpression id="InputExpression_idade" typeRef="integer">
          <text>idade</text>
        </inputExpression>
      </input>
      <!-- ... mais inputs ... -->
      
      <output id="Output_scoreRisco" label="Score de Risco" name="scoreRisco" typeRef="integer" />
      <output id="Output_classificacao" label="Classificação" name="classificacaoRisco" typeRef="string" />
      
      <rule id="Rule_1">
        <inputEntry id="InputEntry_1"><text>&lt; 40</text></inputEntry>
        <!-- ... mais input entries ... -->
        <outputEntry id="OutputEntry_1"><text>10</text></outputEntry>
        <outputEntry id="OutputEntry_2"><text>"BAIXO"</text></outputEntry>
      </rule>
      <!-- ... mais rules ... -->
    </decisionTable>
  </decision>
  
</definitions>
```

---

## Checklist de Validação DMN

Para cada arquivo DMN:

- [ ] Decision ID corresponde ao `camunda:decisionRef` no BPMN
- [ ] Hit Policy adequada ao tipo de decisão
- [ ] Todos os inputs tipados corretamente
- [ ] Todos os outputs tipados corretamente
- [ ] Regras cobrem todos os cenários possíveis
- [ ] Regra default para casos não mapeados
- [ ] Arquivo abre corretamente no Camunda Modeler
- [ ] Deploy para Camunda Engine sem erros
- [ ] Testes unitários passam

---

# FASE 2: BACKEND SERVICE SWARM

## Objetivo

Implementar **71 Delegate Beans** Java/Spring que executam a lógica de negócio referenciada nos arquivos BPMN através de `camunda:delegateExpression="${...}"`.

## Estrutura de Pacotes

```
src/main/java/br/com/austa/experiencia/
├── delegate/
│   ├── onboarding/           # SUB-001 (8 delegates)
│   ├── proativo/             # SUB-002 (7 delegates)
│   ├── recepcao/             # SUB-003 (6 delegates)
│   ├── selfservice/          # SUB-004 (5 delegates)
│   ├── agenteia/             # SUB-005 (7 delegates)
│   ├── autorizacao/          # SUB-006 (9 delegates)
│   ├── navegacao/            # SUB-007 (8 delegates)
│   ├── cronicos/             # SUB-008 (6 delegates)
│   ├── reclamacoes/          # SUB-009 (7 delegates)
│   ├── followup/             # SUB-010 (5 delegates)
│   └── common/               # Delegates compartilhados (3 delegates)
├── service/
│   ├── integration/          # Serviços de integração
│   ├── domain/               # Serviços de domínio
│   └── external/             # Serviços externos (APIs)
├── repository/
├── model/
│   ├── entity/
│   └── dto/
├── config/
├── exception/
└── util/
```

---

## Catálogo Completo de Delegates

### SUB-001: Onboarding Inteligente (8 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 1 | `criarRegistroTasyDelegate` | `CriarRegistroTasyDelegate` | Criar registro do beneficiário no Tasy ERP |
| 2 | `enviarBoasVindasDelegate` | `EnviarBoasVindasDelegate` | Enviar mensagem de boas-vindas via WhatsApp |
| 3 | `processarRespostaScreeningDelegate` | `ProcessarRespostaScreeningDelegate` | Processar respostas do screening gamificado |
| 4 | `analisarDocumentosOcrDelegate` | `AnalisarDocumentosOcrDelegate` | Processar documentos via OCR/Computer Vision |
| 5 | `calcularScoreRiscoDelegate` | `CalcularScoreRiscoDelegate` | Calcular score de risco combinando fontes |
| 6 | `criarPlanoCuidadosDelegate` | `CriarPlanoCuidadosDelegate` | Criar plano de cuidados inicial |
| 7 | `registrarPerfilDataLakeDelegate` | `RegistrarPerfilDataLakeDelegate` | Registrar perfil no Data Lake |
| 8 | `notificarOnboardingConcluidoDelegate` | `NotificarOnboardingConcluidoDelegate` | Publicar evento de onboarding concluído |

### SUB-002: Motor Proativo (7 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 9 | `carregarBeneficiariosAtivosDelegate` | `CarregarBeneficiariosAtivosDelegate` | Carregar base de beneficiários para processamento |
| 10 | `coletarDadosAtualizadosDelegate` | `ColetarDadosAtualizadosDelegate` | Coletar dados de múltiplas fontes |
| 11 | `executarAcaoProativaDelegate` | `ExecutarAcaoProativaDelegate` | Executar ação proativa identificada |
| 12 | `enviarNudgePreventeDelegate` | `EnviarNudgePreventeDelegate` | Enviar nudge comportamental |
| 13 | `alertarNavegadorDelegate` | `AlertarNavegadorDelegate` | Alertar navegador sobre caso urgente |
| 14 | `registrarAcaoExecutadaDelegate` | `RegistrarAcaoExecutadaDelegate` | Registrar ação proativa no histórico |
| 15 | `atualizarDashboardProatividadeDelegate` | `AtualizarDashboardProatividadeDelegate` | Atualizar métricas de proatividade |

### SUB-003: Recepção e Classificação (6 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 16 | `identificarCanalOrigemDelegate` | `IdentificarCanalOrigemDelegate` | Identificar canal de origem da interação |
| 17 | `processarNlpDelegate` | `ProcessarNlpDelegate` | Classificar intenção via NLP |
| 18 | `buscarBeneficiarioTasyDelegate` | `BuscarBeneficiarioTasyDelegate` | Buscar dados do beneficiário no Tasy |
| 19 | `carregarPerfil360Delegate` | `CarregarPerfil360Delegate` | Carregar perfil 360° do beneficiário |
| 20 | `verificarContextoConversaDelegate` | `VerificarContextoConversaDelegate` | Verificar contexto de conversa anterior |
| 21 | `registrarInteracaoDelegate` | `RegistrarInteracaoDelegate` | Registrar interação no CRM |

### SUB-004: Self-Service (5 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 22 | `gerarCarterinhaDigitalDelegate` | `GerarCarterinhaDigitalDelegate` | Gerar carteirinha digital com QR Code |
| 23 | `consultarStatusAutorizacaoDelegate` | `ConsultarStatusAutorizacaoDelegate` | Consultar status de autorização |
| 24 | `gerarBoletoDelegate` | `GerarBoletoDelegate` | Gerar segunda via de boleto |
| 25 | `atualizarDadosCadastraisDelegate` | `AtualizarDadosCadastraisDelegate` | Atualizar dados cadastrais |
| 26 | `consultarExtratoUtilizacaoDelegate` | `ConsultarExtratoUtilizacaoDelegate` | Consultar extrato de utilização |

### SUB-005: Agentes IA (7 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 27 | `iniciarTriagemIaDelegate` | `IniciarTriagemIaDelegate` | Iniciar triagem com agente IA |
| 28 | `processarRespostaTriagemDelegate` | `ProcessarRespostaTriagemDelegate` | Processar resposta da triagem |
| 29 | `consultarProtocolosDelegate` | `ConsultarProtocolosDelegate` | Consultar protocolos clínicos |
| 30 | `gerarRecomendacaoIaDelegate` | `GerarRecomendacaoIaDelegate` | Gerar recomendação via IA |
| 31 | `verificarNecessidadeEscalacaoDelegate` | `VerificarNecessidadeEscalacaoDelegate` | Verificar se precisa escalar |
| 32 | `transferirComContextoDelegate` | `TransferirComContextoDelegate` | Transferir com contexto completo |
| 33 | `registrarAtendimentoIaDelegate` | `RegistrarAtendimentoIaDelegate` | Registrar atendimento IA |

### SUB-006: Autorização Inteligente (9 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 34 | `receberValidarGuiaTissDelegate` | `ReceberValidarGuiaTissDelegate` | Receber e validar guia TISS |
| 35 | `verificarElegibilidadeDelegate` | `VerificarElegibilidadeDelegate` | Verificar elegibilidade do beneficiário |
| 36 | `verificarCredenciamentoDelegate` | `VerificarCredenciamentoDelegate` | Verificar credenciamento do prestador |
| 37 | `verificarCoberturaCarenciaDelegate` | `VerificarCoberturaCarenciaDelegate` | Verificar cobertura e carência |
| 38 | `verificarCptDelegate` | `VerificarCptDelegate` | Verificar bloqueio por CPT |
| 39 | `aprovarAutomaticamenteDelegate` | `AprovarAutomaticamenteDelegate` | Aprovar autorização automaticamente |
| 40 | `prepararDossieAuditoriaDelegate` | `PrepararDossieAuditoriaDelegate` | Preparar dossiê para auditoria |
| 41 | `notificarPrestadorDelegate` | `NotificarPrestadorDelegate` | Notificar prestador sobre decisão |
| 42 | `notificarBeneficiarioAutorizacaoDelegate` | `NotificarBeneficiarioAutorizacaoDelegate` | Notificar beneficiário sobre autorização |

### SUB-007: Navegação do Cuidado (8 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 43 | `atribuirNavegadorDelegate` | `AtribuirNavegadorDelegate` | Atribuir navegador ao caso |
| 44 | `direcionarRedePreferencialDelegate` | `DirecionarRedePreferencialDelegate` | Direcionar para rede preferencial |
| 45 | `criarJornadaCuidadoDelegate` | `CriarJornadaCuidadoDelegate` | Criar jornada de cuidado |
| 46 | `agendarConsultaRedeDelegate` | `AgendarConsultaRedeDelegate` | Agendar consulta na rede |
| 47 | `monitorarEtapaJornadaDelegate` | `MonitorarEtapaJornadaDelegate` | Monitorar etapa da jornada |
| 48 | `comunicarStatusBeneficiarioDelegate` | `ComunicarStatusBeneficiarioDelegate` | Comunicar status ao beneficiário |
| 49 | `registrarDesfechoDelegate` | `RegistrarDesfechoDelegate` | Registrar desfecho clínico |
| 50 | `encerrarJornadaDelegate` | `EncerrarJornadaDelegate` | Encerrar jornada de cuidado |

### SUB-008: Gestão de Crônicos (6 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 51 | `inscreverProgramaCronicoDelegate` | `InscreverProgramaCronicoDelegate` | Inscrever beneficiário no programa |
| 52 | `definirMetasTerapeuticasDelegate` | `DefinirMetasTerapeuticasDelegate` | Definir metas terapêuticas |
| 53 | `enviarLembreteMedicacaoDelegate` | `EnviarLembreteMedicacaoDelegate` | Enviar lembrete de medicação |
| 54 | `coletarMarcadoresSaudeDelegate` | `ColetarMarcadoresSaudeDelegate` | Coletar marcadores de saúde |
| 55 | `avaliarProgressoDelegate` | `AvaliarProgressoDelegate` | Avaliar progresso no programa |
| 56 | `ajustarPlanoTratamentoDelegate` | `AjustarPlanoTratamentoDelegate` | Ajustar plano de tratamento |

### SUB-009: Gestão de Reclamações (7 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 57 | `registrarReclamacaoDelegate` | `RegistrarReclamacaoDelegate` | Registrar reclamação no sistema |
| 58 | `analisarCausaRaizDelegate` | `AnalisarCausaRaizDelegate` | Analisar causa raiz |
| 59 | `buscarSolucoesAnterioresDelegate` | `BuscarSolucoesAnterioresDelegate` | Buscar soluções de casos similares |
| 60 | `proporSolucaoDelegate` | `ProporSolucaoDelegate` | Propor solução ao beneficiário |
| 61 | `aplicarCompensacaoDelegate` | `AplicarCompensacaoDelegate` | Aplicar compensação se aprovada |
| 62 | `escalarOuvidoriaDelegate` | `EscalarOuvidoriaDelegate` | Escalar para ouvidoria |
| 63 | `registrarResolucaoDelegate` | `RegistrarResolucaoDelegate` | Registrar resolução da reclamação |

### SUB-010: Follow-up e Feedback (5 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 64 | `enviarPesquisaNpsDelegate` | `EnviarPesquisaNpsDelegate` | Enviar pesquisa NPS |
| 65 | `processarRespostaNpsDelegate` | `ProcessarRespostaNpsDelegate` | Processar resposta NPS |
| 66 | `analisarSentimentoDelegate` | `AnalisarSentimentoDelegate` | Analisar sentimento do comentário |
| 67 | `acionarRecuperacaoDetratoresDelegate` | `AcionarRecuperacaoDetratoresDelegate` | Acionar recuperação de detratores |
| 68 | `atualizarModelosPreditivosDelegate` | `AtualizarModelosPreditivosDelegate` | Atualizar modelos preditivos |

### Common (3 Delegates)

| # | Bean Name | Classe | Responsabilidade |
|---|-----------|--------|------------------|
| 69 | `publicarEventoKafkaDelegate` | `PublicarEventoKafkaDelegate` | Publicar evento no Kafka |
| 70 | `enviarWhatsappDelegate` | `EnviarWhatsappDelegate` | Enviar mensagem WhatsApp |
| 71 | `logAuditoriaDelegate` | `LogAuditoriaDelegate` | Registrar log de auditoria |

---

## Template de Implementação de Delegate

```java
package br.com.austa.experiencia.delegate.onboarding;

import br.com.austa.experiencia.service.integration.TasyService;
import br.com.austa.experiencia.model.dto.BeneficiarioDTO;
import br.com.austa.experiencia.exception.IntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por criar o registro do beneficiário no Tasy ERP.
 * 
 * Referenciado em: SUB-001_Onboarding_Inteligente.bpmn
 * Activity ID: Activity_CriarRegistroTasy
 * 
 * Variáveis de entrada:
 * - beneficiarioId (String): ID único do beneficiário
 * - cpf (String): CPF do beneficiário
 * - dadosCadastrais (Map): Dados cadastrais coletados
 * 
 * Variáveis de saída:
 * - codigoTasy (String): Código gerado no Tasy
 * - registroCriado (Boolean): Flag de sucesso
 * 
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("criarRegistroTasyDelegate")
@RequiredArgsConstructor
public class CriarRegistroTasyDelegate implements JavaDelegate {

    private final TasyService tasyService;
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando criação de registro no Tasy - ProcessInstance: {}", 
                 execution.getProcessInstanceId());
        
        try {
            // 1. Extrair variáveis de entrada
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String cpf = (String) execution.getVariable("cpf");
            @SuppressWarnings("unchecked")
            Map<String, Object> dadosCadastrais = 
                (Map<String, Object>) execution.getVariable("dadosCadastrais");
            
            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, cpf, dadosCadastrais);
            
            // 3. Construir DTO
            BeneficiarioDTO dto = buildBeneficiarioDTO(beneficiarioId, cpf, dadosCadastrais);
            
            // 4. Chamar serviço de integração
            String codigoTasy = tasyService.criarBeneficiario(dto);
            
            // 5. Definir variáveis de saída
            execution.setVariable("codigoTasy", codigoTasy);
            execution.setVariable("registroCriado", true);
            
            log.info("Registro criado com sucesso no Tasy - Código: {}, BeneficiarioId: {}", 
                     codigoTasy, beneficiarioId);
            
        } catch (IntegrationException e) {
            log.error("Erro de integração ao criar registro no Tasy: {}", e.getMessage(), e);
            execution.setVariable("registroCriado", false);
            execution.setVariable("erroIntegracao", e.getMessage());
            throw e; // Propaga para boundary error event
            
        } catch (Exception e) {
            log.error("Erro inesperado ao criar registro no Tasy: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao criar registro no Tasy", e);
        }
    }
    
    private void validateInputs(String beneficiarioId, String cpf, 
                                Map<String, Object> dadosCadastrais) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("cpf é obrigatório");
        }
        if (dadosCadastrais == null || dadosCadastrais.isEmpty()) {
            throw new IllegalArgumentException("dadosCadastrais é obrigatório");
        }
    }
    
    private BeneficiarioDTO buildBeneficiarioDTO(String beneficiarioId, String cpf,
                                                  Map<String, Object> dadosCadastrais) {
        return BeneficiarioDTO.builder()
            .id(beneficiarioId)
            .cpf(cpf)
            .nome((String) dadosCadastrais.get("nome"))
            .dataNascimento((LocalDate) dadosCadastrais.get("dataNascimento"))
            .telefone((String) dadosCadastrais.get("telefone"))
            .email((String) dadosCadastrais.get("email"))
            .endereco((String) dadosCadastrais.get("endereco"))
            .build();
    }
}
```

---

## Serviços de Integração Requeridos

### Integrações Internas

| Serviço | Classe | Endpoints |
|---------|--------|-----------|
| Tasy ERP | `TasyService` | CRUD Beneficiário, Autorização, Agendamento |
| CRM | `CrmService` | Interações, Casos, Histórico |
| Data Lake | `DataLakeService` | Perfis, Eventos, Analytics |

### Integrações Externas

| Serviço | Classe | API |
|---------|--------|-----|
| WhatsApp | `WhatsAppService` | WhatsApp Business API |
| NLP | `NlpService` | GPT-4 API / Azure OpenAI |
| OCR | `OcrService` | Azure Computer Vision |
| Kafka | `KafkaProducerService` | Apache Kafka |

---

## Regras de Implementação

### Padrões Obrigatórios

1. **Naming Convention:**
   - Bean: `camelCase` terminando em `Delegate`
   - Classe: `PascalCase` terminando em `Delegate`
   - Pacote: lowercase conforme subprocesso

2. **Logging:**
   - Usar `@Slf4j` do Lombok
   - Log de entrada com ProcessInstanceId
   - Log de saída com resultado
   - Log de erro com stack trace

3. **Transações:**
   - Delegates são transacionais por padrão
   - Usar `@Transactional(propagation = REQUIRES_NEW)` para isolamento

4. **Tratamento de Erros:**
   - Exceptions específicas para boundary error events
   - Sempre definir variável de erro para auditoria
   - Propagar exceptions críticas

5. **Variáveis:**
   - Documentar todas as variáveis de entrada e saída no Javadoc
   - Usar tipos primitivos quando possível
   - Serializar objetos complexos como JSON

---

## Checklist de Implementação por Delegate

Para cada delegate:

- [ ] Classe criada no pacote correto
- [ ] Bean name corresponde ao `camunda:delegateExpression` no BPMN
- [ ] Implementa `JavaDelegate`
- [ ] Anotado com `@Component("beanName")`
- [ ] Injeção de dependências via construtor (`@RequiredArgsConstructor`)
- [ ] Logging adequado (`@Slf4j`)
- [ ] Javadoc com variáveis de entrada e saída
- [ ] Validação de inputs
- [ ] Tratamento de exceções
- [ ] Variáveis de saída definidas
- [ ] Testes unitários criados
- [ ] Testes de integração criados

---

# FASE 3: INTEGRATION TEST SWARM

## Objetivo

Criar suíte completa de **testes de integração end-to-end** que validam os workflows BPMN com DMN e Delegates integrados.

## Estrutura de Testes

```
src/test/java/br/com/austa/experiencia/
├── integration/
│   ├── workflow/
│   │   ├── OrquestracaoWorkflowIT.java
│   │   ├── OnboardingWorkflowIT.java
│   │   ├── MotorProativoWorkflowIT.java
│   │   ├── RecepcaoClassificacaoWorkflowIT.java
│   │   ├── SelfServiceWorkflowIT.java
│   │   ├── AgentesIaWorkflowIT.java
│   │   ├── AutorizacaoWorkflowIT.java
│   │   ├── NavegacaoCuidadoWorkflowIT.java
│   │   ├── GestaoCronicosWorkflowIT.java
│   │   ├── GestaoReclamacoesWorkflowIT.java
│   │   └── FollowUpFeedbackWorkflowIT.java
│   ├── dmn/
│   │   ├── EstratificacaoRiscoDmnIT.java
│   │   ├── DeteccaoCptDmnIT.java
│   │   ├── ClassificacaoUrgenciaDmnIT.java
│   │   └── ... (11 arquivos)
│   └── delegate/
│       ├── OnboardingDelegatesIT.java
│       ├── ProativoDelegatesIT.java
│       └── ... (10 arquivos)
├── e2e/
│   ├── JornadaBeneficiarioE2EIT.java
│   ├── AutorizacaoE2EIT.java
│   ├── ReclamacaoE2EIT.java
│   └── CronicoE2EIT.java
└── support/
    ├── TestContainersConfig.java
    ├── CamundaTestConfig.java
    ├── MockServersConfig.java
    └── TestDataFactory.java
```

---

## Configuração de Ambiente de Testes

### TestContainers

```java
@TestConfiguration
public class TestContainersConfig {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("experiencia_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

### WireMock para APIs Externas

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
}
```

---

## Cenários de Teste por Workflow

### SUB-001: Onboarding Inteligente

```java
@SpringBootTest
@Testcontainers
@CamundaSpringBootTest
class OnboardingWorkflowIT {

    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private HistoryService historyService;

    @Test
    @DisplayName("Deve completar onboarding de beneficiário baixo risco sem CPT")
    void deveCompletarOnboardingBaixoRiscoSemCpt() {
        // Given
        Map<String, Object> variables = Map.of(
            "beneficiarioId", "BEN-001",
            "cpf", "12345678901",
            "nome", "João Silva",
            "idade", 30,
            "dadosScreening", criarDadosScreeningBaixoRisco()
        );

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "SUB-001_Onboarding_Inteligente", variables);

        // Then
        assertThat(instance).isEnded();
        assertThat(historyService.createHistoricVariableInstanceQuery()
            .processInstanceId(instance.getId())
            .variableName("classificacaoRisco")
            .singleResult().getValue()).isEqualTo("BAIXO");
        assertThat(historyService.createHistoricVariableInstanceQuery()
            .processInstanceId(instance.getId())
            .variableName("statusCPT")
            .singleResult().getValue()).isEqualTo("NENHUM");
    }

    @Test
    @DisplayName("Deve detectar CPT e encaminhar para validação médica")
    void deveDetectarCptEEncaminharValidacao() {
        // Given
        Map<String, Object> variables = Map.of(
            "beneficiarioId", "BEN-002",
            "cpf", "98765432101",
            "dadosScreening", criarDadosScreeningComCpt()
        );

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "SUB-001_Onboarding_Inteligente", variables);

        // Then - Processo deve estar aguardando validação médica
        assertThat(instance).isWaitingAt("UserTask_ValidacaoMedicaCpt");
    }

    @Test
    @DisplayName("Deve publicar evento Kafka ao concluir onboarding")
    void devePublicarEventoKafkaAoConcluir() {
        // Given
        Map<String, Object> variables = criarVariaveisOnboardingCompleto();

        // When
        runtimeService.startProcessInstanceByKey(
            "SUB-001_Onboarding_Inteligente", variables);

        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ConsumerRecord<String, String>> records = 
                kafkaConsumer.poll(Duration.ofSeconds(1));
            assertThat(records).anyMatch(r -> 
                r.topic().equals("beneficiario.onboarding.completed"));
        });
    }
}
```

### SUB-006: Autorização Inteligente

```java
@SpringBootTest
@Testcontainers
@CamundaSpringBootTest
class AutorizacaoWorkflowIT {

    @Test
    @DisplayName("Deve autorizar consulta eletiva automaticamente")
    void deveAutorizarConsultaEletivaAutomaticamente() {
        // Given
        Map<String, Object> variables = Map.of(
            "guiaTISS", criarGuiaTissConsultaEletiva(),
            "beneficiarioId", "BEN-001",
            "prestadorId", "PREST-001",
            "valorSolicitado", 350.00
        );

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "SUB-006_Autorizacao_Inteligente", variables);

        // Then
        assertThat(instance).isEnded();
        assertThat(getVariable(instance, "statusAutorizacao")).isEqualTo("APROVADA");
        assertThat(getVariable(instance, "tipoAutorizacao")).isEqualTo("AUTOMATICA");
        assertThat(getVariable(instance, "numeroAutorizacao")).isNotNull();
    }

    @Test
    @DisplayName("Deve escalar para auditoria médica em cirurgia eletiva")
    void deveEscalarParaAuditoriaMedicaEmCirurgia() {
        // Given
        Map<String, Object> variables = Map.of(
            "guiaTISS", criarGuiaTissCirurgiaEletiva(),
            "beneficiarioId", "BEN-001",
            "valorSolicitado", 15000.00
        );

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "SUB-006_Autorizacao_Inteligente", variables);

        // Then
        assertThat(instance).isWaitingAt("UserTask_AuditoriaMedica");
    }

    @Test
    @DisplayName("Deve negar autorização por carência não cumprida")
    void deveNegarPorCarenciaNaoCumprida() {
        // Given
        Map<String, Object> variables = Map.of(
            "guiaTISS", criarGuiaTissInternacao(),
            "beneficiarioId", "BEN-NOVO",  // Beneficiário recém cadastrado
            "dataAdesao", LocalDate.now().minusDays(30)
        );

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "SUB-006_Autorizacao_Inteligente", variables);

        // Then
        assertThat(instance).isEnded();
        assertThat(getVariable(instance, "statusAutorizacao")).isEqualTo("NEGADA");
        assertThat(getVariable(instance, "motivoNegativa")).contains("CARENCIA");
    }
}
```

---

## Testes End-to-End

### Jornada Completa do Beneficiário

```java
@SpringBootTest
@Testcontainers
@CamundaSpringBootTest
class JornadaBeneficiarioE2EIT {

    @Test
    @DisplayName("Jornada completa: Adesão → Onboarding → Interação → Autorização → Follow-up")
    void jornadaCompletaNovoBeneficiario() {
        // 1. ADESÃO - Trigger do orquestrador
        String beneficiarioId = UUID.randomUUID().toString();
        ProcessInstance orquestrador = runtimeService.startProcessInstanceByMessage(
            "Msg_NovaAdesaoConfirmada",
            Map.of("beneficiarioId", beneficiarioId, "cpf", "12345678901"));

        // 2. ONBOARDING - Deve iniciar automaticamente
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<ProcessInstance> onboardings = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("SUB-001_Onboarding_Inteligente")
                .variableValueEquals("beneficiarioId", beneficiarioId)
                .list();
            assertThat(onboardings).hasSize(1);
        });

        // Simular respostas do screening
        completarScreening(beneficiarioId);

        // 3. INTERAÇÃO - Simular mensagem WhatsApp
        runtimeService.correlateMessage(
            "Msg_InteracaoRecebida",
            Map.of("telefone", "11999999999"),
            Map.of(
                "mensagem", "Preciso agendar uma consulta com cardiologista",
                "canal", "WHATSAPP"
            ));

        // 4. AUTORIZAÇÃO - Deve ser processada
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<HistoricActivityInstance> autorizacoes = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(orquestrador.getId())
                .activityType("callActivity")
                .activityId("CallActivity_Autorizacao")
                .list();
            assertThat(autorizacoes).isNotEmpty();
        });

        // 5. FOLLOW-UP - Deve ser agendado
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<Job> timers = managementService.createJobQuery()
                .processInstanceId(orquestrador.getId())
                .timers()
                .list();
            assertThat(timers).anyMatch(j -> 
                j.getJobDefinition().getActivityId().contains("FollowUp"));
        });

        // Validações finais
        assertThat(orquestrador).isActive();
        assertThat(getVariable(orquestrador, "estadoBeneficiario")).isEqualTo("ATIVO");
    }
}
```

---

## Matriz de Cobertura de Testes

| Workflow | Cenários | Testes | Cobertura |
|----------|----------|--------|-----------|
| PROC-ORC-001 | 5 | Happy path, eventos, exceções, cancelamento, óbito | 100% |
| SUB-001 | 8 | Baixo/Médio/Alto risco, CPT, timeout, erros | 100% |
| SUB-002 | 6 | Gatilhos, batch, tempo real, alertas | 100% |
| SUB-003 | 7 | Canais, NLP, roteamento, emergência | 100% |
| SUB-004 | 5 | Self-service tasks | 100% |
| SUB-005 | 6 | Triagem, escalação, handoff | 100% |
| SUB-006 | 10 | Tipos de autorização, negativas, auditoria | 100% |
| SUB-007 | 7 | Navegação, jornadas, desfechos | 100% |
| SUB-008 | 5 | Programas, metas, adesão | 100% |
| SUB-009 | 6 | Reclamações, escalação, compensação | 100% |
| SUB-010 | 4 | NPS, detratores, feedback | 100% |
| **TOTAL** | **69** | | **100%** |

---

## Checklist Final de Validação

### Fase 1 - DMN
- [ ] 11 arquivos DMN criados
- [ ] Todos os Decision IDs correspondem aos BPMN
- [ ] Hit Policies corretas
- [ ] Regras cobrem todos os cenários
- [ ] Deploy no Camunda Engine sem erros
- [ ] Testes unitários passam

### Fase 2 - Delegates
- [ ] 71 delegates implementados
- [ ] Bean names correspondem aos BPMN
- [ ] Injeção de dependências correta
- [ ] Logging implementado
- [ ] Tratamento de erros adequado
- [ ] Testes unitários passam

### Fase 3 - Testes de Integração
- [ ] TestContainers configurado
- [ ] WireMock para APIs externas
- [ ] Todos os workflows testados
- [ ] Cenários E2E validados
- [ ] Cobertura >= 80%
- [ ] CI/CD pipeline verde

---

## Comandos de Execução

```bash
# Executar todos os testes
./mvnw test

# Executar apenas testes de integração
./mvnw verify -P integration-tests

# Executar testes com cobertura
./mvnw verify jacoco:report

# Executar testes específicos
./mvnw test -Dtest=OnboardingWorkflowIT

# Deploy dos processos
./mvnw camunda:deploy
```
