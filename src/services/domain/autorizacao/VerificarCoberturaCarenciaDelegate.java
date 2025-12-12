package br.com.austa.experiencia.service.domain.autorizacao;

import br.com.austa.experiencia.service.CoberturaService;
import br.com.austa.experiencia.model.dto.CoberturaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por verificar cobertura do procedimento e carência.
 *
 * Referenciado em: SUB-006_Autorizacao_Hibrida.bpmn
 * Activity ID: Activity_VerificarCoberturaCarencia
 *
 * Variáveis de entrada:
 * - beneficiarioId (String): ID do beneficiário
 * - procedimentoId (String): Código do procedimento (TUSS)
 * - dataProcedimento (LocalDate): Data prevista do procedimento
 * - tipoAtendimento (String): Tipo (ELETIVO, URGENCIA, EMERGENCIA)
 *
 * Variáveis de saída:
 * - procedimentoCoberto (Boolean): Se o procedimento é coberto
 * - emCarencia (Boolean): Se está em período de carência
 * - diasCarenciaRestantes (Integer): Dias restantes de carência (se aplicável)
 * - motivoNaoCobertura (String): Motivo se não coberto
 * - coparticipacao (BigDecimal): Valor de coparticipação (se aplicável)
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("verificarCoberturaCarenciaDelegate")
@RequiredArgsConstructor
public class VerificarCoberturaCarenciaDelegate implements JavaDelegate {

    private final CoberturaService coberturaService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando verificação de cobertura e carência - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String procedimentoId = (String) execution.getVariable("procedimentoId");
            java.time.LocalDate dataProcedimento =
                (java.time.LocalDate) execution.getVariable("dataProcedimento");
            String tipoAtendimento = (String) execution.getVariable("tipoAtendimento");

            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, procedimentoId, dataProcedimento, tipoAtendimento);

            // 3. Verificar cobertura e carência
            CoberturaDTO cobertura = coberturaService.verificarCoberturaCarencia(
                beneficiarioId, procedimentoId, dataProcedimento, tipoAtendimento);

            // 4. Definir variáveis de saída
            execution.setVariable("procedimentoCoberto", cobertura.isCoberto());
            execution.setVariable("emCarencia", cobertura.isEmCarencia());
            execution.setVariable("diasCarenciaRestantes", cobertura.getDiasCarenciaRestantes());
            execution.setVariable("motivoNaoCobertura", cobertura.getMotivoNaoCobertura());
            execution.setVariable("coparticipacao", cobertura.getValorCoparticipacao());

            log.info("Cobertura verificada - BeneficiarioId: {}, Procedimento: {}, Coberto: {}, Carência: {}",
                     beneficiarioId, procedimentoId, cobertura.isCoberto(), cobertura.isEmCarencia());

        } catch (Exception e) {
            log.error("Erro ao verificar cobertura e carência: {}", e.getMessage(), e);
            execution.setVariable("procedimentoCoberto", false);
            execution.setVariable("motivoNaoCobertura", "Erro ao verificar cobertura");
            throw new RuntimeException("Falha ao verificar cobertura e carência", e);
        }
    }

    private void validateInputs(String beneficiarioId, String procedimentoId,
                                 java.time.LocalDate dataProcedimento, String tipoAtendimento) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (procedimentoId == null || procedimentoId.isBlank()) {
            throw new IllegalArgumentException("procedimentoId é obrigatório");
        }
        if (dataProcedimento == null) {
            throw new IllegalArgumentException("dataProcedimento é obrigatório");
        }
        if (tipoAtendimento == null || !tipoAtendimento.matches("(?i)(ELETIVO|URGENCIA|EMERGENCIA)")) {
            throw new IllegalArgumentException("tipoAtendimento deve ser ELETIVO, URGENCIA ou EMERGENCIA");
        }
    }
}
