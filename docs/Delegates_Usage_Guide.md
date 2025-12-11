# Guia de Uso - Java Delegates

Este documento explica como utilizar os Java Delegates implementados para o projeto de Experiência Digital do Cliente.

## Delegates Disponíveis

### 1. TasyBeneficiarioService

**Propósito**: Integração com o sistema Tasy ERP para operações de beneficiários.

**Bean Name**: `tasyBeneficiarioService`

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/delegates/TasyBeneficiarioService.java`

#### Operações Suportadas

##### Criar Beneficiário
```xml
<serviceTask id="ServiceTask_CriarBeneficiario"
             name="Criar Beneficiário no Tasy"
             camunda:delegateExpression="${tasyBeneficiarioService}">
  <extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="tasyOperacao">criar</camunda:inputParameter>
      <camunda:inputParameter name="dadosCadastrais">
        ${execution.getVariable('dadosCadastrais')}
      </camunda:inputParameter>
      <camunda:inputParameter name="contratoId">${contratoId}</camunda:inputParameter>
    </camunda:inputOutput>
    <camunda:executionListener event="end"
                              delegateExpression="${errorLoggerService}"/>
  </extensionElements>
</serviceTask>
```

**Input Variables**:
- `tasyOperacao`: "criar"
- `dadosCadastrais` (Map): Dados do beneficiário
- `contratoId` (String): ID do contrato

**Output Variables**:
- `beneficiarioTasyId` (String): ID gerado no Tasy
- `tasyErro` (Boolean): true se houver erro

##### Buscar Beneficiário
```xml
<serviceTask id="ServiceTask_BuscarBeneficiario"
             name="Buscar Beneficiário"
             camunda:delegateExpression="${tasyBeneficiarioService}">
  <extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="tasyOperacao">buscar</camunda:inputParameter>
      <camunda:inputParameter name="cpf">${cpf}</camunda:inputParameter>
    </camunda:inputOutput>
  </extensionElements>
</serviceTask>
```

**Input Variables**:
- `tasyOperacao`: "buscar"
- `beneficiarioId` OU `cpf` OU `telefone` (String)

**Output Variables**:
- `beneficiarioEncontrado` (Boolean)
- `beneficiarioTasyId` (String)
- `dadosBeneficiario` (Map)

#### Configuração

Adicione no `application.yml`:
```yaml
tasy:
  api:
    base-url: http://tasy-api:8080
    timeout: 5000
```

#### Retry Configuration

```xml
<serviceTask id="ServiceTask_BuscarBeneficiario"
             name="Buscar Beneficiário"
             camunda:delegateExpression="${tasyBeneficiarioService}"
             camunda:asyncBefore="true">
  <extensionElements>
    <camunda:failedJobRetryTimeCycle>R3/PT1M</camunda:failedJobRetryTimeCycle>
  </extensionElements>
</serviceTask>
```

Isso configura:
- 3 tentativas (R3)
- Intervalo de 1 minuto entre tentativas (PT1M)

---

### 2. KafkaPublisherService

**Propósito**: Publicação de eventos no Apache Kafka para observabilidade e integração.

**Bean Name**: `kafkaPublisherService`

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/delegates/KafkaPublisherService.java`

#### Uso Básico

```xml
<serviceTask id="ServiceTask_PublicarEvento"
             name="Publicar Evento Kafka"
             camunda:delegateExpression="${kafkaPublisherService}">
  <extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="eventoTipo">BeneficiarioPerfilCompleto</camunda:inputParameter>
      <camunda:inputParameter name="topico">austa.jornada</camunda:inputParameter>
    </camunda:inputOutput>
  </extensionElements>
</serviceTask>
```

**Input Variables**:
- `eventoTipo` (String): Tipo do evento (ex: "BeneficiarioPerfilCompleto")
- `topico` (String, opcional): Tópico Kafka (se não fornecido, é determinado automaticamente)
- `beneficiarioId` (String): Usado como chave da mensagem

**Output Variables**:
- `eventoPublicado` (Boolean)
- `eventoId` (String): UUID do evento
- `kafkaErro` (Boolean): true se houver erro

#### Eventos Suportados

| Evento | Tópico | Descrição |
|--------|--------|-----------|
| `BeneficiarioPerfilCompleto` | `austa.jornada.onboarding` | Perfil completo após onboarding |
| `OnboardingConcluido` | `austa.jornada.onboarding` | Onboarding finalizado |
| `AutorizacaoProcessada` | `austa.autorizacao` | Autorização processada |
| `JornadaCompleta` | `austa.jornada` | Jornada completa do beneficiário |
| `AlertaAltoRisco` | `austa.alertas` | Alerta de alto risco |
| `AcoesProativasExecutadas` | `austa.proatividade` | Ações proativas executadas |

#### Configuração

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
```

---

### 3. TemplateService

**Propósito**: Preparação de templates de comunicação personalizados.

**Bean Name**: `templateService`

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/delegates/TemplateService.java`

#### Uso Básico

```xml
<serviceTask id="ServiceTask_PrepararTemplate"
             name="Preparar Template de Boas-vindas"
             camunda:delegateExpression="${templateService}">
  <extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="templateId">boas_vindas_v2</camunda:inputParameter>
    </camunda:inputOutput>
  </extensionElements>
</serviceTask>
```

**Input Variables**:
- `templateId` (String, opcional): ID do template (se não fornecido, é determinado pelo contexto)
- `dadosCadastrais` (Map): Dados do beneficiário
- Variáveis específicas do template

**Output Variables**:
- `mensagemPersonalizada` (String): Mensagem pronta para envio
- `templateUtilizado` (String): ID do template usado
- `templatePreparado` (Boolean)

#### Templates Disponíveis

| Template ID | Uso | Variáveis |
|-------------|-----|-----------|
| `boas_vindas_v2` | Mensagem inicial de boas-vindas | nome, plano |
| `lembrete_onboarding_v1` | Lembrete para completar onboarding | nome, modulosCompletos |
| `resumo_onboarding_v2` | Resumo após onboarding completo | nome, classificacaoRisco |
| `checkup_pendente_v1` | Lembrete de check-up | nome, mesesUltimoCheckup, clinica |
| `medicamento_acabando_v1` | Alerta de medicamento acabando | nome, nomeMedicamento, diasRestantes |
| `exame_alterado_v1` | Alerta de exame alterado | nome, tipoExame, especialidade |

---

### 4. RiscoCalculatorService

**Propósito**: Cálculos de scores de risco e análises preditivas.

**Bean Name**: `riscoCalculatorService`

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/delegates/RiscoCalculatorService.java`

#### Operações Suportadas

##### Cálculo Completo de Risco
```xml
<serviceTask id="ServiceTask_CalcularRisco"
             name="Calcular Score de Risco"
             camunda:delegateExpression="${riscoCalculatorService}">
  <extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="tipoCalculo">completo</camunda:inputParameter>
    </camunda:inputOutput>
  </extensionElements>
</serviceTask>
```

**Input Variables**:
- `tipoCalculo`: "completo", "internacao", "comportamental" ou "bmi"
- `idade`, `peso`, `altura`, `qtdComorbidades`
- `respostasScreening` (Map)
- `historicoUtilizacoes` (List)

**Output Variables**:
- `scoreRisco` (Integer, 0-100)
- `scoreComportamental` (Integer, 0-100)
- `scorePredicaoInternacao` (Integer, 0-100)
- `riscoInternacaoCategoria` (String)
- `bmi` (Double)
- `categoriaBMI` (String)
- `fatoresRisco` (List<String>)

---

### 5. DataLakeService

**Propósito**: Persistência de dados analíticos no Data Lake.

**Bean Name**: `dataLakeService`

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/delegates/DataLakeService.java`

#### Operações Suportadas

##### Registrar Perfil Completo
```xml
<serviceTask id="ServiceTask_RegistrarPerfil"
             name="Registrar Perfil no Data Lake"
             camunda:delegateExpression="${dataLakeService}">
  <extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="dataLakeOperacao">registrar_perfil</camunda:inputParameter>
    </camunda:inputOutput>
  </extensionElements>
</serviceTask>
```

**Input Variables**:
- `dataLakeOperacao`: "registrar_perfil", "registrar_jornada", "registrar_interacao" ou "consolidar_metricas"
- Variáveis específicas da operação

**Output Variables**:
- `perfilRegistradoId` (String)
- `jornadaRegistradaId` (String)
- `interacaoRegistradaId` (String)
- `dataLakeSucesso` (Boolean)

#### Configuração

```yaml
datalake:
  api:
    base-url: http://datalake-api:8080
```

---

### 6. AutorizacaoService

**Propósito**: Processamento e gerenciamento de autorizações de procedimentos.

**Bean Name**: `autorizacaoService`

**Localização**: `/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/delegates/AutorizacaoService.java`

#### Operações Suportadas

##### Gerar Autorização
```xml
<serviceTask id="ServiceTask_GerarAutorizacao"
             name="Gerar Autorização"
             camunda:delegateExpression="${autorizacaoService}">
  <extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="autorizacaoOperacao">gerar</camunda:inputParameter>
    </camunda:inputOutput>
  </extensionElements>
</serviceTask>
```

**Input Variables**:
- `autorizacaoOperacao`: "gerar", "atualizar" ou "cancelar"
- `beneficiarioId` (String)
- `tipoProcedimento` (String)
- `valorProcedimento` (Double)
- `prestadorId` (String)
- `decisaoAutorizacao` (String)

**Output Variables**:
- `numeroAutorizacao` (String): Formato "AUT-YYYY-NNNNNNNN"
- `dataAutorizacao` (String)
- `validadeAutorizacao` (String)
- `autorizacaoSucesso` (Boolean)

---

## Tratamento de Erros

### Boundary Error Events

```xml
<serviceTask id="ServiceTask_BuscarBeneficiario"
             name="Buscar Beneficiário"
             camunda:delegateExpression="${tasyBeneficiarioService}"/>

<boundaryEvent id="BoundaryEvent_TasyIndisponivel"
               attachedToRef="ServiceTask_BuscarBeneficiario">
  <errorEventDefinition errorRef="Error_TasyIndisponivel"/>
</boundaryEvent>

<sequenceFlow id="Flow_TasyErro"
              sourceRef="BoundaryEvent_TasyIndisponivel"
              targetRef="SubProcess_TratarErroTasy"/>
```

### Error Definitions

```xml
<error id="Error_TasyIndisponivel"
       name="ERR_TASY_INDISPONIVEL"
       errorCode="ERR_TASY_INDISPONIVEL"/>

<error id="Error_KafkaPublish"
       name="ERR_KAFKA_PUBLISH"
       errorCode="ERR_KAFKA_PUBLISH"/>
```

---

## Configuração do Spring Boot

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Camunda -->
    <dependency>
        <groupId>org.camunda.bpm.springboot</groupId>
        <artifactId>camunda-bpm-spring-boot-starter</artifactId>
        <version>7.20.0</version>
    </dependency>

    <!-- Spring Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Jackson for JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### Application Configuration

```yaml
camunda:
  bpm:
    admin-user:
      id: admin
      password: admin
    filter:
      create: All tasks
    generic-properties:
      properties:
        enable-exceptions-after-unhandled-bpmn-error: true
        job-executor-acquire-by-priority: true

spring:
  application:
    name: experiencia-digital-cliente

  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3

tasy:
  api:
    base-url: http://tasy-api:8080
    timeout: 5000

datalake:
  api:
    base-url: http://datalake-api:8080
```

---

## Testes Unitários

### Exemplo de Teste com MockExecutionDelegate

```java
@ExtendWith(MockitoExtension.class)
public class TasyBeneficiarioServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private TasyBeneficiarioService service;

    @Test
    public void testCriarBeneficiario() throws Exception {
        // Given
        Map<String, Object> dadosCadastrais = new HashMap<>();
        dadosCadastrais.put("nome", "João Silva");

        when(execution.getVariable("tasyOperacao")).thenReturn("criar");
        when(execution.getVariable("dadosCadastrais")).thenReturn(dadosCadastrais);
        when(execution.getVariable("contratoId")).thenReturn("CNT-123");

        Map<String, Object> response = new HashMap<>();
        response.put("beneficiario_id", "BEN-456");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(response);

        // When
        service.execute(execution);

        // Then
        verify(execution).setVariable("beneficiarioTasyId", "BEN-456");
        verify(execution).setVariable("tasyErro", false);
    }
}
```

---

## Referências

- [Camunda Delegates Documentation](https://docs.camunda.org/manual/latest/user-guide/process-engine/delegation-code/)
- [Spring Boot Camunda Integration](https://docs.camunda.org/manual/latest/user-guide/spring-boot-integration/)
- Especificação Técnica: `PROMPT_TÉCNICO_BPMN`
