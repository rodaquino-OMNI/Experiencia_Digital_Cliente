package br.com.austa.experiencia.service.domain.recepcao;

import br.com.austa.experiencia.service.CanalService;
import br.com.austa.experiencia.model.dto.CanalDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por identificar o canal de origem da interação.
 *
 * Referenciado em: SUB-003_Recepcao_360.bpmn
 * Activity ID: Activity_IdentificarCanalOrigem
 *
 * Variáveis de entrada:
 * - canalId (String): Identificador do canal (whatsapp, app, web, telefone, etc)
 * - dadosCanal (Map, opcional): Metadados do canal (IP, device, etc)
 * - sessionId (String, opcional): ID da sessão do usuário
 *
 * Variáveis de saída:
 * - canalTipo (String): Tipo do canal identificado
 * - canalNome (String): Nome descritivo do canal
 * - canalAtributos (Map): Atributos e capacidades do canal
 * - suportaMultimidia (Boolean): Se canal suporta mídia
 * - suportaRichContent (Boolean): Se canal suporta conteúdo rico
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("identificarCanalOrigemDelegate")
@RequiredArgsConstructor
public class IdentificarCanalOrigemDelegate implements JavaDelegate {

    private final CanalService canalService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando identificação de canal de origem - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String canalId = (String) execution.getVariable("canalId");

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> dadosCanal =
                (java.util.Map<String, Object>) execution.getVariable("dadosCanal");

            String sessionId = (String) execution.getVariable("sessionId");

            // 2. Validar dados obrigatórios
            validateInputs(canalId);

            // 3. Identificar e configurar canal
            CanalDTO canal = canalService.identificarCanal(canalId, dadosCanal, sessionId);

            // 4. Definir variáveis de saída
            execution.setVariable("canalTipo", canal.getTipo());
            execution.setVariable("canalNome", canal.getNome());
            execution.setVariable("canalAtributos", canal.getAtributos());
            execution.setVariable("suportaMultimidia", canal.isSuportaMultimidia());
            execution.setVariable("suportaRichContent", canal.isSuportaRichContent());

            log.info("Canal identificado - Tipo: {}, Nome: {}, Multimídia: {}",
                     canal.getTipo(), canal.getNome(), canal.isSuportaMultimidia());

        } catch (Exception e) {
            log.error("Erro ao identificar canal: {}", e.getMessage(), e);
            // Define valores padrão em caso de erro
            execution.setVariable("canalTipo", "UNKNOWN");
            execution.setVariable("canalNome", "Canal Desconhecido");
            execution.setVariable("suportaMultimidia", false);
            execution.setVariable("suportaRichContent", false);
            throw new RuntimeException("Falha ao identificar canal de origem", e);
        }
    }

    private void validateInputs(String canalId) {
        if (canalId == null || canalId.isBlank()) {
            throw new IllegalArgumentException("canalId é obrigatório");
        }
    }
}
