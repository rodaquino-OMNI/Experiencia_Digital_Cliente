package br.com.austa.experiencia.service.domain.selfservice;

import br.com.austa.experiencia.service.SelfServiceService;
import br.com.austa.experiencia.model.dto.CarterinhaDigitalDTO;
import br.com.austa.experiencia.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por gerar carteirinha digital do beneficiário com QR Code.
 *
 * Referenciado em: SUB-004_AutoAtendimento_Inteligente.bpmn
 * Activity ID: Activity_GerarCarterinhaDigital
 *
 * Variáveis de entrada:
 * - beneficiarioId (String): ID único do beneficiário
 * - cartaoNumero (String): Número da carteirinha
 * - incluirQrCode (Boolean, default: true): Gerar QR Code
 * - formato (String, default: "PDF"): Formato de saída (PDF, PNG)
 *
 * Variáveis de saída:
 * - carterinhaUrl (String): URL da carteirinha gerada
 * - qrCodeData (String): Dados codificados no QR Code
 * - carterinhaGerada (Boolean): Flag de sucesso
 * - dataGeracao (LocalDateTime): Timestamp da geração
 * - downloadToken (String): Token temporário para download
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("gerarCarterinhaDigitalDelegate")
@RequiredArgsConstructor
public class GerarCarterinhaDigitalDelegate implements JavaDelegate {

    private final SelfServiceService selfServiceService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando geração de carteirinha digital - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String cartaoNumero = (String) execution.getVariable("cartaoNumero");
            Boolean incluirQrCode = execution.getVariable("incluirQrCode") != null
                ? (Boolean) execution.getVariable("incluirQrCode") : true;
            String formato = execution.getVariable("formato") != null
                ? (String) execution.getVariable("formato") : "PDF";

            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, cartaoNumero, formato);

            // 3. Gerar carteirinha digital
            CarterinhaDigitalDTO carteirinha = selfServiceService.gerarCarterinhaDigital(
                beneficiarioId, cartaoNumero, incluirQrCode, formato);

            // 4. Definir variáveis de saída
            execution.setVariable("carterinhaUrl", carteirinha.getUrl());
            execution.setVariable("qrCodeData", carteirinha.getQrCodeData());
            execution.setVariable("carterinhaGerada", true);
            execution.setVariable("dataGeracao", carteirinha.getDataGeracao());
            execution.setVariable("downloadToken", carteirinha.getDownloadToken());

            log.info("Carteirinha digital gerada com sucesso - BeneficiarioId: {}, Formato: {}",
                     beneficiarioId, formato);

        } catch (BusinessException e) {
            log.error("Erro de negócio ao gerar carteirinha: {}", e.getMessage(), e);
            execution.setVariable("carterinhaGerada", false);
            execution.setVariable("erroGeracao", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Erro inesperado ao gerar carteirinha: {}", e.getMessage(), e);
            execution.setVariable("carterinhaGerada", false);
            throw new RuntimeException("Falha ao gerar carteirinha digital", e);
        }
    }

    private void validateInputs(String beneficiarioId, String cartaoNumero, String formato) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (cartaoNumero == null || cartaoNumero.isBlank()) {
            throw new IllegalArgumentException("cartaoNumero é obrigatório");
        }
        if (!formato.matches("(?i)(PDF|PNG|JPG)")) {
            throw new IllegalArgumentException("formato deve ser PDF, PNG ou JPG");
        }
    }
}
