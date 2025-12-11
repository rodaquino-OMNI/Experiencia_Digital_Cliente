# Integration Architecture - External Systems

## Document Control
- **Version**: 1.0
- **Author**: System Architecture Designer
- **Date**: 2025-12-11
- **Status**: Draft

## Overview
This document defines all external system integrations for the Camunda 7 BPM platform, including integration patterns, API specifications, authentication mechanisms, and error handling strategies.

## Integration Landscape

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CAMUNDA BPM PLATFORM                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                   Integration Layer                              │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │  │
│  │  │   Tasy   │  │ WhatsApp │  │ IBM RPA  │  │ML/AI Eng.│        │  │
│  │  │Connector │  │ Business │  │Connector │  │  Client  │        │  │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘        │  │
│  └───────┼─────────────┼─────────────┼─────────────┼───────────────┘  │
│          │             │             │             │                   │
└──────────┼─────────────┼─────────────┼─────────────┼───────────────────┘
           │             │             │             │
           ▼             ▼             ▼             ▼
    ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
    │  Tasy    │  │ WhatsApp │  │ IBM RPA  │  │  ML/AI   │
    │   ERP    │  │ Business │  │  Studio  │  │  Engine  │
    │          │  │   API    │  │          │  │          │
    └──────────┘  └──────────┘  └──────────┘  └──────────┘
```

## 1. Tasy ERP Integration

### 1.1 Overview
**System**: Philips Tasy EMR/ERP
**Purpose**: Master data source for beneficiaries, authorizations, and medical records
**Integration Pattern**: REST API + Database Views (read-only)
**Authentication**: OAuth 2.0 Client Credentials

### 1.2 API Endpoints

#### Get Beneficiary Data
```
GET /api/v1/beneficiaries/{externalId}
Authorization: Bearer {access_token}
```

**Request Example**:
```bash
curl -X GET "https://tasy.austa.com.br/api/v1/beneficiaries/TASY-12345" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response Example**:
```json
{
  "externalId": "TASY-12345",
  "cpf": "12345678901",
  "fullName": "João Silva",
  "dateOfBirth": "1985-03-15",
  "gender": "M",
  "email": "joao@example.com",
  "phone": "+5511999999999",
  "contract": {
    "contractNumber": "CNT-2024-001",
    "planCode": "PLANO-BRONZE",
    "status": "ACTIVE",
    "admissionDate": "2024-01-01"
  },
  "address": {
    "street": "Rua das Flores",
    "number": "123",
    "city": "São Paulo",
    "state": "SP",
    "zipCode": "01234-567"
  }
}
```

#### Create Authorization Request
```
POST /api/v1/authorizations
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Example**:
```json
{
  "beneficiaryExternalId": "TASY-12345",
  "requestType": "EXAM",
  "procedureCode": "40101010",
  "procedureName": "Ecocardiograma com Doppler",
  "icd10Code": "I50.0",
  "clinicalIndication": "Investigação de insuficiência cardíaca",
  "requestingPhysician": {
    "physicianId": "PHYS-001",
    "name": "Dr. João Carvalho",
    "crm": "12345-SP"
  },
  "providerId": "PROV-100",
  "urgency": "ROUTINE"
}
```

**Response Example**:
```json
{
  "authorizationId": "AUTH-2024-12345",
  "status": "PENDING_REVIEW",
  "createdAt": "2024-12-11T10:00:00Z",
  "estimatedDecisionTime": "2024-12-11T12:00:00Z"
}
```

### 1.3 Java Integration Service

```java
@Service
@Slf4j
public class TasyIntegrationService {

    @Value("${tasy.api.base-url}")
    private String tasyBaseUrl;

    @Value("${tasy.oauth.client-id}")
    private String clientId;

    @Value("${tasy.oauth.client-secret}")
    private String clientSecret;

    @Value("${tasy.oauth.token-url}")
    private String tokenUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // OAuth2 Token Cache
    private String cachedAccessToken;
    private Instant tokenExpiryTime;

    public TasyIntegrationService(RestTemplateBuilder restTemplateBuilder,
                                   ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Get beneficiary data from Tasy ERP
     */
    public BeneficiaryDTO getBeneficiary(String externalId) {
        String accessToken = getAccessToken();
        String url = tasyBaseUrl + "/api/v1/beneficiaries/" + externalId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
            );

            return objectMapper.readValue(response.getBody(), BeneficiaryDTO.class);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BeneficiaryNotFoundException(
                    "Beneficiary not found: " + externalId
                );
            }
            throw new TasyIntegrationException(
                "Failed to fetch beneficiary from Tasy", e
            );
        } catch (Exception e) {
            log.error("Error communicating with Tasy API", e);
            throw new TasyIntegrationException(
                "Tasy API communication error", e
            );
        }
    }

    /**
     * Create authorization request in Tasy
     */
    public AuthorizationResponse createAuthorization(
            AuthorizationRequestDTO request) {

        String accessToken = getAccessToken();
        String url = tasyBaseUrl + "/api/v1/authorizations";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AuthorizationRequestDTO> httpRequest =
            new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AuthorizationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpRequest,
                AuthorizationResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error creating authorization in Tasy", e);
            throw new TasyIntegrationException(
                "Failed to create authorization in Tasy", e
            );
        }
    }

    /**
     * OAuth2 token management
     */
    private String getAccessToken() {
        // Return cached token if still valid
        if (cachedAccessToken != null &&
            tokenExpiryTime != null &&
            Instant.now().isBefore(tokenExpiryTime)) {
            return cachedAccessToken;
        }

        // Request new token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request =
            new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                TokenResponse.class
            );

            TokenResponse tokenResponse = response.getBody();
            cachedAccessToken = tokenResponse.getAccessToken();

            // Cache token for 90% of its lifetime
            int expiresIn = tokenResponse.getExpiresIn();
            tokenExpiryTime = Instant.now()
                .plusSeconds((long) (expiresIn * 0.9));

            return cachedAccessToken;

        } catch (Exception e) {
            log.error("Failed to obtain OAuth2 token from Tasy", e);
            throw new TasyAuthenticationException(
                "OAuth2 authentication failed", e
            );
        }
    }
}
```

### 1.4 Camunda Service Task Delegate

```java
@Component("tasyBeneficiaryFetcher")
public class TasyBeneficiaryFetcherDelegate implements JavaDelegate {

    @Autowired
    private TasyIntegrationService tasyService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String externalId = (String) execution.getVariable("externalId");

        try {
            BeneficiaryDTO beneficiary = tasyService.getBeneficiary(externalId);

            // Store beneficiary data in process variables
            execution.setVariable("beneficiaryId", beneficiary.getId());
            execution.setVariable("cpf", beneficiary.getCpf());
            execution.setVariable("fullName", beneficiary.getFullName());
            execution.setVariable("email", beneficiary.getEmail());
            execution.setVariable("phone", beneficiary.getPhone());

            // Store as JSON for complex objects
            String beneficiaryJson = new ObjectMapper()
                .writeValueAsString(beneficiary);
            execution.setVariable("beneficiaryData", beneficiaryJson);

        } catch (BeneficiaryNotFoundException e) {
            throw new BpmnError("BENEFICIARY_NOT_FOUND",
                "Beneficiary not found in Tasy: " + externalId);
        } catch (TasyIntegrationException e) {
            throw new BpmnError("TASY_INTEGRATION_ERROR",
                "Failed to communicate with Tasy ERP");
        }
    }
}
```

## 2. WhatsApp Business API Integration

### 2.1 Overview
**System**: WhatsApp Business API (Meta/Facebook)
**Purpose**: Primary communication channel with beneficiaries
**Integration Pattern**: REST API (Webhook for inbound)
**Authentication**: Bearer Token

### 2.2 API Configuration

```yaml
whatsapp:
  api:
    base-url: https://graph.facebook.com/v18.0
    phone-number-id: ${WHATSAPP_PHONE_NUMBER_ID}
    access-token: ${WHATSAPP_ACCESS_TOKEN}
    webhook-verify-token: ${WHATSAPP_WEBHOOK_VERIFY_TOKEN}
```

### 2.3 Send Message Service

```java
@Service
@Slf4j
public class WhatsAppService {

    @Value("${whatsapp.api.base-url}")
    private String baseUrl;

    @Value("${whatsapp.api.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.api.access-token}")
    private String accessToken;

    private final RestTemplate restTemplate;

    /**
     * Send template message to beneficiary
     */
    public WhatsAppMessageResponse sendTemplateMessage(
            String recipientPhone,
            String templateName,
            Map<String, String> parameters) {

        String url = String.format("%s/%s/messages", baseUrl, phoneNumberId);

        // Build request body
        WhatsAppMessageRequest request = WhatsAppMessageRequest.builder()
            .messagingProduct("whatsapp")
            .recipientType("individual")
            .to(recipientPhone)
            .type("template")
            .template(
                WhatsAppTemplate.builder()
                    .name(templateName)
                    .language(new WhatsAppLanguage("pt_BR"))
                    .components(buildComponents(parameters))
                    .build()
            )
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<WhatsAppMessageRequest> httpRequest =
            new HttpEntity<>(request, headers);

        try {
            ResponseEntity<WhatsAppMessageResponse> response =
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    WhatsAppMessageResponse.class
                );

            log.info("WhatsApp message sent successfully: {}",
                response.getBody().getMessageId());

            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("WhatsApp API error: {}", e.getResponseBodyAsString());
            throw new WhatsAppIntegrationException(
                "Failed to send WhatsApp message", e
            );
        }
    }

    /**
     * Send interactive button message
     */
    public WhatsAppMessageResponse sendButtonMessage(
            String recipientPhone,
            String bodyText,
            List<WhatsAppButton> buttons) {

        String url = String.format("%s/%s/messages", baseUrl, phoneNumberId);

        WhatsAppMessageRequest request = WhatsAppMessageRequest.builder()
            .messagingProduct("whatsapp")
            .recipientType("individual")
            .to(recipientPhone)
            .type("interactive")
            .interactive(
                WhatsAppInteractive.builder()
                    .type("button")
                    .body(new WhatsAppText(bodyText))
                    .action(
                        WhatsAppAction.builder()
                            .buttons(buttons)
                            .build()
                    )
                    .build()
            )
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<WhatsAppMessageRequest> httpRequest =
            new HttpEntity<>(request, headers);

        try {
            ResponseEntity<WhatsAppMessageResponse> response =
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    WhatsAppMessageResponse.class
                );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error sending button message", e);
            throw new WhatsAppIntegrationException(
                "Failed to send button message", e
            );
        }
    }

    private List<WhatsAppComponent> buildComponents(
            Map<String, String> parameters) {

        List<WhatsAppParameter> params = parameters.entrySet().stream()
            .map(entry -> WhatsAppParameter.builder()
                .type("text")
                .text(entry.getValue())
                .build())
            .collect(Collectors.toList());

        return List.of(
            WhatsAppComponent.builder()
                .type("body")
                .parameters(params)
                .build()
        );
    }
}
```

### 2.4 Webhook Receiver

```java
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@Slf4j
public class WhatsAppWebhookController {

    @Value("${whatsapp.api.webhook-verify-token}")
    private String verifyToken;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Webhook verification (GET)
     */
    @GetMapping
    public ResponseEntity<?> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        log.warn("WhatsApp webhook verification failed");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Receive inbound messages (POST)
     */
    @PostMapping
    public ResponseEntity<Void> receiveMessage(
            @RequestBody WhatsAppWebhookPayload payload) {

        log.info("Received WhatsApp webhook: {}", payload);

        try {
            // Process each entry
            payload.getEntry().forEach(entry -> {
                entry.getChanges().forEach(change -> {
                    WhatsAppValue value = change.getValue();

                    // Process messages
                    if (value.getMessages() != null) {
                        value.getMessages().forEach(message -> {
                            processInboundMessage(message, value);
                        });
                    }

                    // Process status updates
                    if (value.getStatuses() != null) {
                        value.getStatuses().forEach(this::processStatusUpdate);
                    }
                });
            });

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook", e);
            // Return 200 to prevent WhatsApp from retrying
            return ResponseEntity.ok().build();
        }
    }

    private void processInboundMessage(
            WhatsAppMessage message,
            WhatsAppValue value) {

        // Build interaction event
        InteractionReceivedEvent event = InteractionReceivedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventTimestamp(Instant.now())
            .eventType("InteractionReceived")
            .correlationId(message.getId())
            .payload(
                InteractionPayload.builder()
                    .interactionId(UUID.randomUUID().toString())
                    .channel("WHATSAPP")
                    .interactionType("INBOUND_MESSAGE")
                    .senderPhone(message.getFrom())
                    .content(extractMessageContent(message))
                    .receivedAt(Instant.now())
                    .build()
            )
            .build();

        // Publish to Kafka
        kafkaTemplate.send("operadora.interaction.received",
            message.getFrom(), event);

        log.info("Inbound WhatsApp message published to Kafka: {}",
            message.getId());
    }

    private void processStatusUpdate(WhatsAppStatus status) {
        // Handle delivery receipts, read receipts, etc.
        log.info("WhatsApp message status: {} - {}",
            status.getId(), status.getStatus());
    }

    private String extractMessageContent(WhatsAppMessage message) {
        return switch (message.getType()) {
            case "text" -> message.getText().getBody();
            case "button" -> message.getButton().getText();
            case "interactive" -> extractInteractiveResponse(message);
            default -> message.getType() + " message received";
        };
    }

    private String extractInteractiveResponse(WhatsAppMessage message) {
        if (message.getInteractive() != null) {
            WhatsAppInteractiveResponse interactive =
                message.getInteractive();

            return switch (interactive.getType()) {
                case "button_reply" ->
                    interactive.getButtonReply().getTitle();
                case "list_reply" ->
                    interactive.getListReply().getTitle();
                default -> "Interactive response";
            };
        }
        return "Interactive message";
    }
}
```

## 3. IBM RPA Integration

### 3.1 Overview
**System**: IBM Robotic Process Automation
**Purpose**: Automate repetitive tasks (scheduling, data entry, verification)
**Integration Pattern**: REST API + Event-driven
**Authentication**: API Key

### 3.2 RPA Service

```java
@Service
@Slf4j
public class RpaIntegrationService {

    @Value("${rpa.api.base-url}")
    private String rpaBaseUrl;

    @Value("${rpa.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    /**
     * Trigger RPA bot to schedule appointment
     */
    public RpaExecutionResponse scheduleAppointment(
            AppointmentSchedulingRequest request) {

        String url = rpaBaseUrl + "/api/v1/bots/appointment-scheduler/execute";

        RpaExecutionRequest rpaRequest = RpaExecutionRequest.builder()
            .botId("appointment-scheduler")
            .parameters(Map.of(
                "beneficiaryId", request.getBeneficiaryId(),
                "specialty", request.getSpecialty(),
                "providerId", request.getProviderId(),
                "preferredDate", request.getPreferredDate(),
                "preferredTime", request.getPreferredTime()
            ))
            .priority("MEDIUM")
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RpaExecutionRequest> httpRequest =
            new HttpEntity<>(rpaRequest, headers);

        try {
            ResponseEntity<RpaExecutionResponse> response =
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    RpaExecutionResponse.class
                );

            log.info("RPA bot execution started: {}",
                response.getBody().getExecutionId());

            return response.getBody();

        } catch (Exception e) {
            log.error("Error triggering RPA bot", e);
            throw new RpaIntegrationException(
                "Failed to trigger RPA bot execution", e
            );
        }
    }

    /**
     * Check RPA execution status
     */
    public RpaExecutionStatus getExecutionStatus(String executionId) {
        String url = rpaBaseUrl + "/api/v1/executions/" + executionId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<RpaExecutionStatus> response =
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    RpaExecutionStatus.class
                );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error checking RPA execution status", e);
            throw new RpaIntegrationException(
                "Failed to retrieve RPA execution status", e
            );
        }
    }
}
```

## 4. ML/AI Engine Integration

### 4.1 Overview
**System**: Custom ML/AI Engine (TensorFlow Serving / MLflow)
**Purpose**: Risk stratification, sentiment analysis, prediction models
**Integration Pattern**: REST API (synchronous for real-time, async for batch)
**Authentication**: JWT Bearer Token

### 4.2 ML Service

```java
@Service
@Slf4j
public class MlEngineService {

    @Value("${ml.api.base-url}")
    private String mlBaseUrl;

    @Value("${ml.api.token}")
    private String mlApiToken;

    private final RestTemplate restTemplate;

    /**
     * Calculate risk stratification
     */
    public RiskStratificationResult calculateRisk(
            RiskCalculationRequest request) {

        String url = mlBaseUrl + "/api/v1/models/risk-stratification/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mlApiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RiskCalculationRequest> httpRequest =
            new HttpEntity<>(request, headers);

        try {
            ResponseEntity<RiskStratificationResult> response =
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    RiskStratificationResult.class
                );

            log.info("Risk stratification calculated: {}",
                response.getBody().getRiskLevel());

            return response.getBody();

        } catch (Exception e) {
            log.error("Error calling ML risk stratification model", e);
            throw new MlIntegrationException(
                "ML risk calculation failed", e
            );
        }
    }

    /**
     * Analyze sentiment from text
     */
    public SentimentAnalysisResult analyzeSentiment(String text) {
        String url = mlBaseUrl + "/api/v1/models/sentiment-analysis/predict";

        SentimentRequest request = new SentimentRequest(text);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mlApiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SentimentRequest> httpRequest =
            new HttpEntity<>(request, headers);

        try {
            ResponseEntity<SentimentAnalysisResult> response =
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    SentimentAnalysisResult.class
                );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error analyzing sentiment", e);
            // Return neutral sentiment on error
            return new SentimentAnalysisResult("NEUTRAL", 0.5);
        }
    }
}
```

## Error Handling Strategy

### Retry Configuration
```java
@Configuration
public class IntegrationRetryConfig {

    @Bean
    public RetryTemplate tasyRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Fixed backoff: 2 seconds between retries
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Retry up to 3 times
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    public RetryTemplate whatsappRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Exponential backoff: 1s, 2s, 4s
        ExponentialBackOffPolicy backOffPolicy =
            new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Retry on specific exceptions
        Map<Class<? extends Throwable>, Boolean> exceptions = Map.of(
            HttpServerErrorException.class, true,
            ResourceAccessException.class, true,
            SocketTimeoutException.class, true
        );

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, exceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
```

## Circuit Breaker Pattern

```java
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .recordExceptions(
                HttpServerErrorException.class,
                ResourceAccessException.class
            )
            .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public CircuitBreaker tasyCircuitBreaker(
            CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("tasy");
    }

    @Bean
    public CircuitBreaker whatsappCircuitBreaker(
            CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("whatsapp");
    }
}
```

## Related Documents
- [02_DEPENDENCY_SPECIFICATION.md](./02_DEPENDENCY_SPECIFICATION.md)
- [04_KAFKA_ARCHITECTURE.md](./04_KAFKA_ARCHITECTURE.md)
- [ADR-003: Integration Pattern Selection](./adr/ADR-003-integration-patterns.md)
