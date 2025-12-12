package br.com.austa.experiencia.service.domain.autorizacao;

import br.com.austa.experiencia.service.integration.TasyBeneficiarioService;
import br.com.austa.experiencia.model.dto.ElegibilidadeDTO;
import br.com.austa.experiencia.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por verificar elegibilidade do beneficiário.
 * Valida se o beneficiário está ativo e elegível para o procedimento.
 *
 * Referenciado em: SUB-006_Autorizacao_Hibrida.bpmn
 * Activity ID: Activity_VerificarElegibilidade
 *
 * Variáveis de entrada:
 * - cartaoNumero (String): Número da carteirinha do beneficiário
 * - cpf (String, opcional): CPF para validação adicional
 * - dataProcedimento (LocalDate): Data prevista do procedimento
 *
 * Variáveis de saída:
 * - beneficiarioElegivel (Boolean): Flag de elegibilidade
 * - motivoInelegibilidade (String): Motivo se não elegível
 * - statusPlano (String): Status do plano (ATIVO, SUSPENSO, CANCELADO)
 * - beneficiarioId (String): ID do beneficiário
 * - nomeBeneficiario (String): Nome do beneficiário
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("verificarElegibilidadeDelegate")
@RequiredArgsConstructor
public class VerificarElegibilidadeDelegate implements JavaDelegate {

    private final TasyBeneficiarioService tasyBeneficiarioService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando verificação de elegibilidade - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String cartaoNumero = (String) execution.getVariable("cartaoNumero");
            String cpf = (String) execution.getVariable("cpf");
            java.time.LocalDate dataProcedimento =
                (java.time.LocalDate) execution.getVariable("dataProcedimento");

            // 2. Validar dados obrigatórios
            validateInputs(cartaoNumero, dataProcedimento);

            // 3. Consultar elegibilidade no Tasy
            ElegibilidadeDTO elegibilidade = tasyBeneficiarioService.verificarElegibilidade(
                cartaoNumero, cpf, dataProcedimento);

            // 4. Definir variáveis de saída
            execution.setVariable("beneficiarioElegivel", elegibilidade.isElegivel());
            execution.setVariable("motivoInelegibilidade", elegibilidade.getMotivoInelegibilidade());
            execution.setVariable("statusPlano", elegibilidade.getStatusPlano());
            execution.setVariable("beneficiarioId", elegibilidade.getBeneficiarioId());
            execution.setVariable("nomeBeneficiario", elegibilidade.getNomeBeneficiario());

            if (elegibilidade.isElegivel()) {
                log.info("Beneficiário elegível - Cartão: {}, Nome: {}",
                         maskCartao(cartaoNumero), elegibilidade.getNomeBeneficiario());
            } else {
                log.warn("Beneficiário NÃO elegível - Cartão: {}, Motivo: {}",
                         maskCartao(cartaoNumero), elegibilidade.getMotivoInelegibilidade());
            }

        } catch (BusinessException e) {
            log.error("Erro de negócio ao verificar elegibilidade: {}", e.getMessage(), e);
            execution.setVariable("beneficiarioElegivel", false);
            execution.setVariable("motivoInelegibilidade", e.getMessage());
            // Não lança exceção - fluxo continua para negação

        } catch (Exception e) {
            log.error("Erro inesperado ao verificar elegibilidade: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao verificar elegibilidade", e);
        }
    }

    private void validateInputs(String cartaoNumero, java.time.LocalDate dataProcedimento) {
        if (cartaoNumero == null || cartaoNumero.isBlank()) {
            throw new IllegalArgumentException("cartaoNumero é obrigatório");
        }
        if (dataProcedimento == null) {
            throw new IllegalArgumentException("dataProcedimento é obrigatório");
        }
    }

    private String maskCartao(String cartao) {
        if (cartao == null || cartao.length() < 4) return "***";
        return "***" + cartao.substring(cartao.length() - 4);
    }
}
