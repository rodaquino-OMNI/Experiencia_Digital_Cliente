package br.com.austa.experiencia.service.domain.selfservice;

import br.com.austa.experiencia.service.SelfServiceService;
import br.com.austa.experiencia.model.dto.BoletoDTO;
import br.com.austa.experiencia.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * Delegate responsável por gerar segunda via de boleto bancário.
 *
 * Referenciado em: SUB-004_AutoAtendimento_Inteligente.bpmn
 * Activity ID: Activity_GerarBoleto
 *
 * Variáveis de entrada:
 * - beneficiarioId (String): ID único do beneficiário
 * - competencia (String): Competência do boleto (formato: YYYY-MM)
 * - nossoNumero (String, opcional): Nosso número do boleto original
 * - enviarPorEmail (Boolean, default: true): Enviar boleto por email
 *
 * Variáveis de saída:
 * - boletoUrl (String): URL do PDF do boleto
 * - linhaDigitavel (String): Linha digitável do boleto
 * - codigoBarras (String): Código de barras do boleto
 * - dataVencimento (LocalDate): Data de vencimento
 * - valor (BigDecimal): Valor do boleto
 * - boletoGerado (Boolean): Flag de sucesso
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("gerarBoletoDelegate")
@RequiredArgsConstructor
public class GerarBoletoDelegate implements JavaDelegate {

    private final SelfServiceService selfServiceService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando geração de boleto - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String competencia = (String) execution.getVariable("competencia");
            String nossoNumero = (String) execution.getVariable("nossoNumero");
            Boolean enviarPorEmail = execution.getVariable("enviarPorEmail") != null
                ? (Boolean) execution.getVariable("enviarPorEmail") : true;

            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, competencia);

            // 3. Gerar boleto (segunda via ou novo)
            BoletoDTO boleto = selfServiceService.gerarSegundaViaBoleto(
                beneficiarioId, competencia, nossoNumero, enviarPorEmail);

            // 4. Definir variáveis de saída
            execution.setVariable("boletoUrl", boleto.getUrl());
            execution.setVariable("linhaDigitavel", boleto.getLinhaDigitavel());
            execution.setVariable("codigoBarras", boleto.getCodigoBarras());
            execution.setVariable("dataVencimento", boleto.getDataVencimento());
            execution.setVariable("valor", boleto.getValor());
            execution.setVariable("boletoGerado", true);

            log.info("Boleto gerado com sucesso - BeneficiarioId: {}, Competência: {}, Valor: {}",
                     beneficiarioId, competencia, boleto.getValor());

        } catch (BusinessException e) {
            log.error("Erro de negócio ao gerar boleto: {}", e.getMessage(), e);
            execution.setVariable("boletoGerado", false);
            execution.setVariable("erroGeracao", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Erro inesperado ao gerar boleto: {}", e.getMessage(), e);
            execution.setVariable("boletoGerado", false);
            throw new RuntimeException("Falha ao gerar boleto", e);
        }
    }

    private void validateInputs(String beneficiarioId, String competencia) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (competencia == null || !competencia.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("competencia deve estar no formato YYYY-MM");
        }

        // Validar competência não futura
        LocalDate compData = LocalDate.parse(competencia + "-01");
        if (compData.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("competencia não pode ser futura");
        }
    }
}
