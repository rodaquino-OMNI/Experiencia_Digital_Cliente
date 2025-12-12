package br.com.austa.experiencia.service.domain.recepcao;

import br.com.austa.experiencia.service.integration.CrmService;
import br.com.austa.experiencia.model.dto.Perfil360DTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por carregar perfil 360° do beneficiário.
 * Consolida dados de múltiplas fontes (Tasy, Salesforce, histórico, etc).
 *
 * Referenciado em: SUB-003_Recepcao_360.bpmn
 * Activity ID: Activity_CarregarPerfil360
 *
 * Variáveis de entrada:
 * - beneficiarioId (String, opcional): ID do beneficiário
 * - cpf (String, opcional): CPF para identificação
 * - cartaoNumero (String, opcional): Número da carteirinha
 * - incluirHistorico (Boolean, default: true): Incluir histórico de interações
 * - incluirAutorizacoes (Boolean, default: true): Incluir autorizações recentes
 *
 * Variáveis de saída:
 * - perfil360 (Map): Perfil completo 360° do beneficiário
 * - beneficiarioId (String): ID do beneficiário
 * - perfilEncontrado (Boolean): Flag de sucesso
 * - ultimaInteracao (LocalDateTime): Data/hora da última interação
 * - scoreSaude (Integer): Score de saúde (0-100)
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("carregarPerfil360Delegate")
@RequiredArgsConstructor
public class CarregarPerfil360Delegate implements JavaDelegate {

    private final CrmService crmService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando carregamento de perfil 360° - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String cpf = (String) execution.getVariable("cpf");
            String cartaoNumero = (String) execution.getVariable("cartaoNumero");
            Boolean incluirHistorico = execution.getVariable("incluirHistorico") != null
                ? (Boolean) execution.getVariable("incluirHistorico") : true;
            Boolean incluirAutorizacoes = execution.getVariable("incluirAutorizacoes") != null
                ? (Boolean) execution.getVariable("incluirAutorizacoes") : true;

            // 2. Validar dados obrigatórios (pelo menos um identificador)
            validateInputs(beneficiarioId, cpf, cartaoNumero);

            // 3. Carregar perfil 360° (múltiplas fontes de dados)
            Perfil360DTO perfil = crmService.carregarPerfil360(
                beneficiarioId, cpf, cartaoNumero, incluirHistorico, incluirAutorizacoes);

            // 4. Definir variáveis de saída
            execution.setVariable("perfil360", perfil.getDadosCompletos());
            execution.setVariable("beneficiarioId", perfil.getBeneficiarioId());
            execution.setVariable("perfilEncontrado", true);
            execution.setVariable("ultimaInteracao", perfil.getUltimaInteracao());
            execution.setVariable("scoreSaude", perfil.getScoreSaude());

            log.info("Perfil 360° carregado - BeneficiarioId: {}, Score: {}, Última interação: {}",
                     perfil.getBeneficiarioId(), perfil.getScoreSaude(), perfil.getUltimaInteracao());

        } catch (Exception e) {
            log.error("Erro ao carregar perfil 360°: {}", e.getMessage(), e);
            execution.setVariable("perfilEncontrado", false);
            execution.setVariable("erroPerfil", e.getMessage());
            throw new RuntimeException("Falha ao carregar perfil 360°", e);
        }
    }

    private void validateInputs(String beneficiarioId, String cpf, String cartaoNumero) {
        if ((beneficiarioId == null || beneficiarioId.isBlank()) &&
            (cpf == null || cpf.isBlank()) &&
            (cartaoNumero == null || cartaoNumero.isBlank())) {
            throw new IllegalArgumentException(
                "Pelo menos um identificador é obrigatório: beneficiarioId, cpf ou cartaoNumero");
        }
    }
}
