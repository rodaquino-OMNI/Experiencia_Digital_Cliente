package br.com.austa.experiencia.service.domain.recepcao;

import br.com.austa.experiencia.service.integration.NlpService;
import br.com.austa.experiencia.model.dto.IntencaoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por processar mensagem do usuário via NLP (AWS Comprehend).
 * Identifica intenção, entidades e sentimento.
 *
 * Referenciado em: SUB-003_Recepcao_360.bpmn
 * Activity ID: Activity_ProcessarNlp
 *
 * Variáveis de entrada:
 * - mensagemUsuario (String): Mensagem/texto do usuário
 * - idioma (String, default: "pt-BR"): Idioma da mensagem
 * - contextoAnterior (Map, opcional): Contexto de interações anteriores
 *
 * Variáveis de saída:
 * - intencao (String): Intenção identificada (CONSULTA, AUTORIZACAO, RECLAMACAO, etc)
 * - confiancaIntencao (Double): Nível de confiança (0.0 a 1.0)
 * - entidades (List<Map>): Entidades extraídas (datas, números, nomes, etc)
 * - sentimento (String): Sentimento identificado (POSITIVO, NEUTRO, NEGATIVO)
 * - topicos (List<String>): Tópicos identificados na mensagem
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("processarNlpDelegate")
@RequiredArgsConstructor
public class ProcessarNlpDelegate implements JavaDelegate {

    private final NlpService nlpService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando processamento NLP - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String mensagemUsuario = (String) execution.getVariable("mensagemUsuario");
            String idioma = execution.getVariable("idioma") != null
                ? (String) execution.getVariable("idioma") : "pt-BR";

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> contextoAnterior =
                (java.util.Map<String, Object>) execution.getVariable("contextoAnterior");

            // 2. Validar dados obrigatórios
            validateInputs(mensagemUsuario);

            // 3. Processar NLP (AWS Comprehend + modelo customizado)
            IntencaoDTO intencao = nlpService.processarMensagem(
                mensagemUsuario, idioma, contextoAnterior);

            // 4. Definir variáveis de saída
            execution.setVariable("intencao", intencao.getIntencao());
            execution.setVariable("confiancaIntencao", intencao.getConfianca());
            execution.setVariable("entidades", intencao.getEntidades());
            execution.setVariable("sentimento", intencao.getSentimento());
            execution.setVariable("topicos", intencao.getTopicos());

            log.info("NLP processado - Intenção: {} (confiança: {}), Sentimento: {}, Entidades: {}",
                     intencao.getIntencao(), intencao.getConfianca(),
                     intencao.getSentimento(), intencao.getEntidades().size());

        } catch (Exception e) {
            log.error("Erro ao processar NLP: {}", e.getMessage(), e);
            execution.setVariable("intencao", "DESCONHECIDA");
            execution.setVariable("confiancaIntencao", 0.0);
            execution.setVariable("sentimento", "NEUTRO");
            throw new RuntimeException("Falha ao processar NLP", e);
        }
    }

    private void validateInputs(String mensagemUsuario) {
        if (mensagemUsuario == null || mensagemUsuario.isBlank()) {
            throw new IllegalArgumentException("mensagemUsuario é obrigatório");
        }
        if (mensagemUsuario.length() > 5000) {
            throw new IllegalArgumentException("mensagemUsuario não pode exceder 5000 caracteres");
        }
    }
}
