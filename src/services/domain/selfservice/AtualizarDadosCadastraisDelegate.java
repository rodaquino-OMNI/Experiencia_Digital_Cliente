package br.com.austa.experiencia.service.domain.selfservice;

import br.com.austa.experiencia.service.DadosService;
import br.com.austa.experiencia.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Delegate responsável por atualizar dados cadastrais do beneficiário.
 *
 * Referenciado em: SUB-004_AutoAtendimento_Inteligente.bpmn
 * Activity ID: Activity_AtualizarDadosCadastrais
 *
 * Variáveis de entrada:
 * - beneficiarioId (String): ID único do beneficiário
 * - dadosAtualizados (Map): Mapa com campos a serem atualizados
 *   - telefone (String, opcional)
 *   - email (String, opcional)
 *   - endereco (Map, opcional): logradouro, numero, complemento, bairro, cidade, uf, cep
 * - validarDocumentos (Boolean, default: false): Requer documentos comprobatórios
 *
 * Variáveis de saída:
 * - dadosAtualizados (Boolean): Flag de sucesso da atualização
 * - camposAtualizados (List<String>): Lista de campos atualizados
 * - requerAprovacao (Boolean): Se a atualização requer aprovação manual
 * - timestampAtualizacao (LocalDateTime): Timestamp da atualização
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("atualizarDadosCadastraisDelegate")
@RequiredArgsConstructor
public class AtualizarDadosCadastraisDelegate implements JavaDelegate {

    private final DadosService dadosService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando atualização de dados cadastrais - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");

            @SuppressWarnings("unchecked")
            Map<String, Object> dadosAtualizados =
                (Map<String, Object>) execution.getVariable("dadosAtualizados");

            Boolean validarDocumentos = execution.getVariable("validarDocumentos") != null
                ? (Boolean) execution.getVariable("validarDocumentos") : false;

            // 2. Validar dados obrigatórios
            validateInputs(beneficiarioId, dadosAtualizados);

            // 3. Atualizar dados cadastrais
            var resultado = dadosService.atualizarDadosCadastrais(
                beneficiarioId, dadosAtualizados, validarDocumentos);

            // 4. Definir variáveis de saída
            execution.setVariable("dadosAtualizados", resultado.isSucesso());
            execution.setVariable("camposAtualizados", resultado.getCamposAtualizados());
            execution.setVariable("requerAprovacao", resultado.isRequerAprovacao());
            execution.setVariable("timestampAtualizacao", java.time.LocalDateTime.now());

            log.info("Dados cadastrais atualizados - BeneficiarioId: {}, Campos: {}, RequerAprovacao: {}",
                     beneficiarioId, resultado.getCamposAtualizados().size(), resultado.isRequerAprovacao());

        } catch (BusinessException e) {
            log.error("Erro de negócio ao atualizar dados: {}", e.getMessage(), e);
            execution.setVariable("dadosAtualizados", false);
            execution.setVariable("erroAtualizacao", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar dados: {}", e.getMessage(), e);
            execution.setVariable("dadosAtualizados", false);
            throw new RuntimeException("Falha ao atualizar dados cadastrais", e);
        }
    }

    private void validateInputs(String beneficiarioId, Map<String, Object> dadosAtualizados) {
        if (beneficiarioId == null || beneficiarioId.isBlank()) {
            throw new IllegalArgumentException("beneficiarioId é obrigatório");
        }
        if (dadosAtualizados == null || dadosAtualizados.isEmpty()) {
            throw new IllegalArgumentException("dadosAtualizados é obrigatório e não pode estar vazio");
        }
    }
}
