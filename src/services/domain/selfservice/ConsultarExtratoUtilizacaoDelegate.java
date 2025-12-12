package br.com.austa.experiencia.service.domain.selfservice;

import br.com.austa.experiencia.service.SelfServiceService;
import br.com.austa.experiencia.model.dto.ExtratoUtilizacaoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * Delegate responsável por consultar extrato de utilização de serviços médicos.
 *
 * Referenciado em: SUB-004_AutoAtendimento_Inteligente.bpmn
 * Activity ID: Activity_ConsultarExtratoUtilizacao
 *
 * Variáveis de entrada:
 * - beneficiarioId (String): ID único do beneficiário
 * - dataInicio (LocalDate): Data inicial do período
 * - dataFim (LocalDate): Data final do período
 * - tipoServico (String, opcional): Filtro por tipo (CONSULTA, EXAME, INTERNACAO, etc)
 * - agruparPor (String, default: "DATA"): Agrupamento (DATA, TIPO, PRESTADOR)
 *
 * Variáveis de saída:
 * - extratoUtilizacao (List<Map>): Lista de utilizações no período
 * - totalUtilizacoes (Integer): Quantidade total de utilizações
 * - valorTotal (BigDecimal): Valor total das utilizações
 * - extratoEncontrado (Boolean): Flag de sucesso da consulta
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("consultarExtratoUtilizacaoDelegate")
@RequiredArgsConstructor
public class ConsultarExtratoUtilizacaoDelegate implements JavaDelegate {

    private final SelfServiceService selfServiceService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando consulta de extrato de utilização - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            LocalDate dataInicio = (LocalDate) execution.getVariable("dataInicio");
            LocalDate dataFim = (LocalDate) execution.getVariable("dataFim");
            String tipoServico = (String) execution.getVariable("tipoServico");
            String agruparPor = execution.getVariable("agruparPor") != null
                ? (String) execution.getVariable("agruparPor") : "DATA";

            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, dataInicio, dataFim);

            // 3. Consultar extrato de utilização
            ExtratoUtilizacaoDTO extrato = selfServiceService.consultarExtratoUtilizacao(
                beneficiarioId, dataInicio, dataFim, tipoServico, agruparPor);

            // 4. Definir variáveis de saída
            execution.setVariable("extratoUtilizacao", extrato.getUtilizacoes());
            execution.setVariable("totalUtilizacoes", extrato.getTotalUtilizacoes());
            execution.setVariable("valorTotal", extrato.getValorTotal());
            execution.setVariable("extratoEncontrado", true);

            log.info("Extrato de utilização consultado - BeneficiarioId: {}, Período: {} a {}, Total: {}",
                     beneficiarioId, dataInicio, dataFim, extrato.getTotalUtilizacoes());

        } catch (Exception e) {
            log.error("Erro ao consultar extrato de utilização: {}", e.getMessage(), e);
            execution.setVariable("extratoEncontrado", false);
            execution.setVariable("erroConsulta", e.getMessage());
            throw new RuntimeException("Falha ao consultar extrato de utilização", e);
        }
    }

    private void validateInputs(String beneficiarioId, LocalDate dataInicio, LocalDate dataFim) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (dataInicio == null) {
            throw new IllegalArgumentException("dataInicio é obrigatório");
        }
        if (dataFim == null) {
            throw new IllegalArgumentException("dataFim é obrigatório");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("dataInicio não pode ser posterior a dataFim");
        }
        if (dataInicio.isBefore(LocalDate.now().minusYears(2))) {
            throw new IllegalArgumentException("dataInicio não pode ser anterior a 2 anos");
        }
    }
}
