package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.CartaoDigitalDTO;
import br.com.austa.experiencia.utils.TestDataBuilder;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Integration tests for SUB-004: Self-Service workflow.
 *
 * Tests cover:
 * - Digital card generation and delivery
 * - Authorization status consultation
 * - Boleto generation and payment
 * - Cadastral data updates
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"selfservice.cartao.gerado", "selfservice.boleto.gerado"})
@DisplayName("SUB-004: Self-Service - Workflow Integration Tests")
class SelfServiceWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-004_SelfService";

    @Test
    @DisplayName("Deve gerar e entregar cartão digital via app")
    void deveGerarEntregarCartaoDigital() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("tipoCartao", "DIGITAL");
        variables.put("canalEntrega", "APP");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        CartaoDigitalDTO cartaoGerado = (CartaoDigitalDTO) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("cartaoDigital")
                .singleResult().getValue();

        assertThat(cartaoGerado).isNotNull();
        assertThat(cartaoGerado.getNumeroCartao()).isNotNull();
        assertThat(cartaoGerado.getQrCode()).isNotNull();
    }

    @Test
    @DisplayName("Deve consultar status de autorização em tempo real")
    void deveConsultarStatusAutorizacaoTempoReal() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("numeroAutorizacao", "AUTH-12345");
        variables.put("tipoConsulta", "STATUS_AUTORIZACAO");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String statusAutorizacao = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("statusAutorizacao")
                .singleResult().getValue();

        assertThat(statusAutorizacao).isIn("APROVADA", "PENDENTE", "NEGADA");
    }

    @Test
    @DisplayName("Deve gerar boleto com código de barras e QR code PIX")
    void deveGerarBoletoComCodigoBarrasQrPix() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("valorBoleto", 450.00);
        variables.put("dataVencimento", LocalDate.now().plusDays(10));
        variables.put("descricao", "Mensalidade Plano Saúde");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        Map<String, Object> boletoGerado = (Map<String, Object>)
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(instance.getId())
                        .variableName("boletoGerado")
                        .singleResult().getValue();

        assertThat(boletoGerado).containsKeys("codigoBarras", "qrCodePix", "linhaDigitavel");
    }

    @Test
    @DisplayName("Deve atualizar dados cadastrais do beneficiário")
    void deveAtualizarDadosCadastrais() {
        // Given
        Map<String, Object> dadosAtualizacao = new HashMap<>();
        dadosAtualizacao.put("telefone", "11987654321");
        dadosAtualizacao.put("email", "novo.email@example.com");
        dadosAtualizacao.put("endereco", "Rua Nova, 123");

        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("dadosAtualizacao", dadosAtualizacao);
        variables.put("tipoOperacao", "ATUALIZACAO_CADASTRAL");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        Boolean cadastroAtualizado = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("cadastroAtualizado")
                .singleResult().getValue();

        assertThat(cadastroAtualizado).isTrue();
    }

    @Test
    @DisplayName("Deve validar e rejeitar dados inválidos na atualização cadastral")
    void deveValidarRejeitarDadosInvalidos() {
        // Given
        Map<String, Object> dadosInvalidos = new HashMap<>();
        dadosInvalidos.put("telefone", "INVALID");
        dadosInvalidos.put("email", "email_invalido");

        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("dadosAtualizacao", dadosInvalidos);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        Boolean validacaoFalhou = (Boolean) historyService.createHistodicVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("validacaoFalhou")
                .singleResult().getValue();

        assertThat(validacaoFalhou).isTrue();
    }
}
