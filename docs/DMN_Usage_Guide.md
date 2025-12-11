# Guia de Uso - Tabelas DMN

Este documento explica como utilizar as tabelas DMN (Decision Model and Notation) implementadas para o projeto de Experiência Digital do Cliente.

## Tabelas DMN Disponíveis

### 1. DMN_EstratificacaoRisco.dmn

**Propósito**: Calcular o score de risco de saúde do beneficiário durante o onboarding.

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/dmn/DMN_EstratificacaoRisco.dmn`

**Uso no BPMN**:
```xml
<businessRuleTask id="BRTask_EstrategificarRisco"
                  name="Estratificar Risco"
                  camunda:decisionRef="DMN_EstratificacaoRisco"
                  camunda:resultVariable="scoreRisco">
</businessRuleTask>
```

**Variáveis de Entrada**:
- `idade` (Integer): Idade em anos
- `bmi` (Double): Índice de Massa Corporal
- `qtdComorbidades` (Integer): Quantidade de comorbidades
- `historicoFamiliarPositivo` (Boolean): Histórico familiar de risco
- `tabagista` (Boolean): É fumante
- `sedentario` (Boolean): Não pratica exercícios

**Variáveis de Saída**:
- `scoreRisco` (Integer): Score de 0-100
- `classificacaoRisco` (String): BAIXO, MODERADO, ALTO ou COMPLEXO

**Exemplo de Execução**:
```java
Map<String, Object> variables = new HashMap<>();
variables.put("idade", 45);
variables.put("bmi", 28.5);
variables.put("qtdComorbidades", 2);
variables.put("historicoFamiliarPositivo", true);
variables.put("tabagista", false);
variables.put("sedentario", true);

// DMN retorna:
// scoreRisco = 55 (10 + 10 + 15 + 10 + 5 + 5)
// classificacaoRisco = "ALTO"
```

---

### 2. DMN_ClassificarUrgencia.dmn

**Propósito**: Classificar nível de urgência de interações recebidas.

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/dmn/DMN_ClassificarUrgencia.dmn`

**Uso no BPMN**:
```xml
<businessRuleTask id="BRTask_ClassificarUrgencia"
                  name="Classificar Urgência"
                  camunda:decisionRef="DMN_ClassificarUrgencia"
                  camunda:resultVariable="nivelUrgencia">
</businessRuleTask>
```

**Variáveis de Entrada**:
- `palavrasChaveUrgencia` (String): Palavras detectadas na mensagem
- `sentimentoNegativo` (Boolean): Sentimento negativo detectado
- `classificacaoRisco` (String): Classificação de risco do beneficiário
- `tentativasAnteriores` (Integer): Número de tentativas anteriores

**Variáveis de Saída**:
- `nivelUrgencia` (String): BAIXA, MEDIA, ALTA ou CRITICA
- `slaResposta` (Integer): SLA em minutos

**Exemplo de Execução**:
```java
Map<String, Object> variables = new HashMap<>();
variables.put("palavrasChaveUrgencia", "dor no peito");
variables.put("sentimentoNegativo", true);
variables.put("classificacaoRisco", "ALTO");
variables.put("tentativasAnteriores", 0);

// DMN retorna:
// nivelUrgencia = "CRITICA"
// slaResposta = 5 (minutos)
```

---

### 3. DMN_DefinirRoteamento.dmn

**Propósito**: Definir qual camada de atendimento deve processar a interação.

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/dmn/DMN_DefinirRoteamento.dmn`

**Uso no BPMN**:
```xml
<businessRuleTask id="BRTask_DefinirRoteamento"
                  name="Definir Roteamento"
                  camunda:decisionRef="DMN_DefinirRoteamento"
                  camunda:mapDecisionResult="singleEntry"
                  camunda:resultVariable="camadaDestino">
</businessRuleTask>
```

**Variáveis de Entrada**:
- `intencaoDetectada` (String): Intenção classificada pelo NLP
- `complexidadeInteracao` (String): BAIXA, MEDIA ou ALTA
- `nivelUrgencia` (String): BAIXA, MEDIA, ALTA ou CRITICA
- `sentimentoNegativo` (Boolean): Sentimento negativo
- `classificacaoRisco` (String): BAIXO, MODERADO, ALTO ou COMPLEXO

**Variáveis de Saída**:
- `camadaDestino` (String): SELF_SERVICE, AGENTE_IA, NAVEGACAO ou AUTORIZACAO
- `tipoSubprocesso` (String): SUB-004, SUB-005, SUB-006 ou SUB-007

**Exemplo de Execução**:
```java
Map<String, Object> variables = new HashMap<>();
variables.put("intencaoDetectada", "consultar_saldo");
variables.put("complexidadeInteracao", "BAIXA");
variables.put("nivelUrgencia", "BAIXA");
variables.put("sentimentoNegativo", false);
variables.put("classificacaoRisco", "BAIXO");

// DMN retorna:
// camadaDestino = "SELF_SERVICE"
// tipoSubprocesso = "SUB-004"
```

---

### 4. DMN_RegrasAutorizacao.dmn

**Propósito**: Aplicar regras de negócio para autorização de procedimentos.

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/dmn/DMN_RegrasAutorizacao.dmn`

**Uso no BPMN**:
```xml
<businessRuleTask id="BRTask_RegrasAutorizacao"
                  name="Regras de Autorização"
                  camunda:decisionRef="DMN_RegrasAutorizacao"
                  camunda:mapDecisionResult="singleEntry"
                  camunda:resultVariable="decisaoAutorizacao">
</businessRuleTask>
```

**Variáveis de Entrada**:
- `tipoProcedimento` (String): Tipo do procedimento solicitado
- `diasDesdeAdesao` (Integer): Dias desde a adesão ao plano
- `valorProcedimento` (Double): Valor do procedimento
- `atendeProtocolo` (Boolean): Atende protocolo clínico
- `prestadorRede` (Boolean): Prestador é da rede credenciada

**Variáveis de Saída**:
- `decisaoAutorizacao` (String): APROVADO, NEGADO ou PENDENTE
- `motivoDecisao` (String): Motivo da decisão
- `requerAnaliseTecnica` (Boolean): Requer análise técnica

**Exemplo de Execução**:
```java
Map<String, Object> variables = new HashMap<>();
variables.put("tipoProcedimento", "CIRURGIA_ELETIVA");
variables.put("diasDesdeAdesao", 200);
variables.put("valorProcedimento", 15000.00);
variables.put("atendeProtocolo", true);
variables.put("prestadorRede", true);

// DMN retorna:
// decisaoAutorizacao = "APROVADO"
// motivoDecisao = "Cirurgia atende protocolo e fora de carência"
// requerAnaliseTecnica = true
```

---

### 5. DMN_GatilhosProativos.dmn

**Propósito**: Identificar gatilhos para ações proativas no motor de proatividade.

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/dmn/DMN_GatilhosProativos.dmn`

**Uso no BPMN**:
```xml
<businessRuleTask id="BRTask_IdentificarGatilhos"
                  name="Identificar Gatilhos"
                  camunda:decisionRef="DMN_GatilhosProativos"
                  camunda:mapDecisionResult="collectEntries"
                  camunda:resultVariable="gatilhosAtivados">
</businessRuleTask>
```

**Observação**: Esta DMN usa `hitPolicy="COLLECT"` para retornar múltiplos gatilhos ativados.

**Variáveis de Entrada**:
- `diasUltimoCheckup` (Integer): Dias desde último check-up
- `diasRestantesMedicamento` (Integer): Dias restantes de medicamento
- `exameAlteradoSemRetorno` (Boolean): Exame alterado sem retorno
- `scorePredicaoInternacao` (Integer): Score de predição de internação (0-100)
- `taxaAdesaoTratamento` (Integer): Taxa de adesão ao tratamento (%)
- `temCondicaoCronica` (Boolean): Tem condição crônica
- `diasSemConsulta` (Integer): Dias sem consulta
- `diasAposAlta` (Integer): Dias após alta hospitalar
- `gestante` (Boolean): É gestante
- `sentimentoNegativoRecente` (Boolean): Sentimento negativo recente
- `diasVencimentoCarencia` (Integer): Dias para vencimento da carência

**Variáveis de Saída** (Lista):
- `gatilhoId` (String): ID do gatilho (GAT-001, GAT-002, etc.)
- `prioridade` (String): BAIXA, MEDIA, ALTA ou CRITICA
- `acaoSugerida` (String): Descrição da ação sugerida

**Exemplo de Execução**:
```java
Map<String, Object> variables = new HashMap<>();
variables.put("diasUltimoCheckup", 400);
variables.put("diasRestantesMedicamento", 5);
variables.put("scorePredicaoInternacao", 75);

// DMN retorna (múltiplos gatilhos):
// [
//   {gatilhoId: "GAT-001", prioridade: "MEDIA", acaoSugerida: "Enviar lembrete de check-up..."},
//   {gatilhoId: "GAT-002", prioridade: "ALTA", acaoSugerida: "Lembrete renovação receita..."},
//   {gatilhoId: "GAT-004", prioridade: "CRITICA", acaoSugerida: "Alerta navegador + contato proativo..."}
// ]
```

---

## Importando DMN no Camunda Modeler

1. Abra o Camunda Modeler
2. Vá em File > Open File
3. Selecione o arquivo `.dmn` desejado
4. O diagrama DMN será exibido graficamente

## Deploy no Camunda Platform 7

### Via Camunda Modeler:
1. No Camunda Modeler, clique em "Deploy" (ícone de nuvem)
2. Configure o endpoint REST: `http://localhost:8080/engine-rest`
3. Defina o Deployment Name (ex: "DMN Estratificação Risco v1.0")
4. Clique em "Deploy"

### Via Spring Boot:
```java
@Configuration
public class CamundaConfiguration {

    @Bean
    public ProcessEnginePlugin deploymentPlugin() {
        return new ProcessEnginePlugin() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
                // Deploy será feito automaticamente
            }
        };
    }
}
```

Coloque os arquivos `.dmn` em: `src/main/resources/dmn/`

### Via REST API:
```bash
curl -X POST http://localhost:8080/engine-rest/deployment/create \
  -H "Content-Type: multipart/form-data" \
  -F "deployment-name=DMN Estratificacao Risco" \
  -F "enable-duplicate-filtering=true" \
  -F "deploy-changed-only=true" \
  -F "data=@DMN_EstratificacaoRisco.dmn"
```

---

## Testando DMN Tables

### Via REST API:
```bash
curl -X POST http://localhost:8080/engine-rest/decision-definition/key/DMN_EstratificacaoRisco/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "variables": {
      "idade": {"value": 45, "type": "Integer"},
      "bmi": {"value": 28.5, "type": "Double"},
      "qtdComorbidades": {"value": 2, "type": "Integer"},
      "historicoFamiliarPositivo": {"value": true, "type": "Boolean"},
      "tabagista": {"value": false, "type": "Boolean"},
      "sedentario": {"value": true, "type": "Boolean"}
    }
  }'
```

### Via Java:
```java
@Autowired
private DecisionService decisionService;

public void testarDMN() {
    VariableMap variables = Variables.createVariables()
        .putValue("idade", 45)
        .putValue("bmi", 28.5)
        .putValue("qtdComorbidades", 2)
        .putValue("historicoFamiliarPositivo", true)
        .putValue("tabagista", false)
        .putValue("sedentario", true);

    DmnDecisionResult result = decisionService
        .evaluateDecisionByKey("DMN_EstratificacaoRisco")
        .variables(variables)
        .evaluate();

    Integer scoreRisco = result.getSingleEntry();
    System.out.println("Score de Risco: " + scoreRisco);
}
```

---

## Troubleshooting

### Erro: "Decision with key 'DMN_XXX' cannot be found"
**Solução**: Verifique se o DMN foi deployado corretamente. Use `GET /engine-rest/decision-definition` para listar decisões deployadas.

### Erro: "Cannot evaluate decision"
**Solução**: Verifique se todas as variáveis de entrada foram fornecidas com os tipos corretos.

### Erro: "Hit policy violation"
**Solução**: Para DMN com `hitPolicy="FIRST"`, certifique-se que as regras estão ordenadas corretamente. Para `hitPolicy="COLLECT"`, use `mapDecisionResult="collectEntries"`.

---

## Manutenção e Versionamento

1. Sempre incremente a versão ao fazer mudanças significativas
2. Mantenha a compatibilidade com processos em execução
3. Documente mudanças no histórico de versões
4. Teste exaustivamente antes de deploy em produção

---

## Referências

- [Camunda DMN Documentation](https://docs.camunda.org/manual/latest/reference/dmn11/)
- [DMN 1.3 Specification](https://www.omg.org/spec/DMN/1.3/)
- Especificação Técnica: `PROMPT_TÉCNICO_BPMN`
