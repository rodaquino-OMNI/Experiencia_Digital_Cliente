package br.com.austa.experiencia.service.domain.autorizacao;

import br.com.austa.experiencia.service.CptService;
import br.com.austa.experiencia.model.dto.CptDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por verificar bloqueios CPT (Cadastro de Prestador Temporariamente suspenso).
 * Valida se prestador ou beneficiário possuem restrições ativas.
 *
 * Referenciado em: SUB-006_Autorizacao_Hibrida.bpmn
 * Activity ID: Activity_VerificarCpt
 *
 * Variáveis de entrada:
 * - beneficiarioId (String): ID do beneficiário
 * - prestadorId (String): ID do prestador solicitante
 * - procedimentoId (String): Código do procedimento (TUSS)
 *
 * Variáveis de saída:
 * - cptBloqueado (Boolean): Se há bloqueio CPT ativo
 * - tipoBloqueio (String): Tipo do bloqueio (BENEFICIARIO, PRESTADOR, PROCEDIMENTO)
 * - motivoBloqueio (String): Motivo do bloqueio
 * - dataInicioBloqueio (LocalDate): Data de início do bloqueio
 * - dataFimBloqueio (LocalDate): Data de fim previsto (se aplicável)
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("verificarCptDelegate")
@RequiredArgsConstructor
public class VerificarCptDelegate implements JavaDelegate {

    private final CptService cptService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando verificação de bloqueios CPT - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String prestadorId = (String) execution.getVariable("prestadorId");
            String procedimentoId = (String) execution.getVariable("procedimentoId");

            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, prestadorId, procedimentoId);

            // 3. Verificar bloqueios CPT
            CptDTO cptStatus = cptService.verificarBloqueios(
                beneficiarioId, prestadorId, procedimentoId);

            // 4. Definir variáveis de saída
            execution.setVariable("cptBloqueado", cptStatus.isBloqueado());
            execution.setVariable("tipoBloqueio", cptStatus.getTipoBloqueio());
            execution.setVariable("motivoBloqueio", cptStatus.getMotivoBloqueio());
            execution.setVariable("dataInicioBloqueio", cptStatus.getDataInicioBloqueio());
            execution.setVariable("dataFimBloqueio", cptStatus.getDataFimBloqueio());

            if (cptStatus.isBloqueado()) {
                log.warn("CPT BLOQUEADO - Tipo: {}, Motivo: {}, BeneficiarioId: {}, PrestadorId: {}",
                         cptStatus.getTipoBloqueio(), cptStatus.getMotivoBloqueio(),
                         beneficiarioId, prestadorId);
            } else {
                log.info("CPT sem bloqueios - BeneficiarioId: {}, PrestadorId: {}",
                         beneficiarioId, prestadorId);
            }

        } catch (Exception e) {
            log.error("Erro ao verificar bloqueios CPT: {}", e.getMessage(), e);
            execution.setVariable("cptBloqueado", false);
            throw new RuntimeException("Falha ao verificar bloqueios CPT", e);
        }
    }

    private void validateInputs(String beneficiarioId, String prestadorId, String procedimentoId) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (prestadorId == null || prestadorId.isBlank()) {
            throw new IllegalArgumentException("prestadorId é obrigatório");
        }
        if (procedimentoId == null || procedimentoId.isBlank()) {
            throw new IllegalArgumentException("procedimentoId é obrigatório");
        }
    }
}
