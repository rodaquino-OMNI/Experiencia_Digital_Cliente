package br.com.austa.experiencia.service.domain.autorizacao;

import br.com.austa.experiencia.service.TissService;
import br.com.austa.experiencia.model.dto.GuiaTissDTO;
import br.com.austa.experiencia.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate responsável por receber e validar guia TISS (XML).
 * Valida conformidade com padrão TISS da ANS.
 *
 * Referenciado em: SUB-006_Autorizacao_Hibrida.bpmn
 * Activity ID: Activity_ReceberValidarGuiaTiss
 *
 * Variáveis de entrada:
 * - guiaTissXml (String): Conteúdo XML da guia TISS
 * - prestadorId (String): ID do prestador que enviou a guia
 * - tipoGuia (String): Tipo da guia (CONSULTA, SP/SADT, INTERNACAO, etc)
 *
 * Variáveis de saída:
 * - guiaValida (Boolean): Flag de validação da guia
 * - numeroGuia (String): Número da guia extraído do XML
 * - errosValidacao (List<String>): Lista de erros encontrados
 * - dadosGuia (Map): Dados estruturados extraídos da guia
 * - versaoTiss (String): Versão do padrão TISS identificada
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("receberValidarGuiaTissDelegate")
@RequiredArgsConstructor
public class ReceberValidarGuiaTissDelegate implements JavaDelegate {

    private final TissService tissService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando recebimento e validação de guia TISS - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String guiaTissXml = (String) execution.getVariable("guiaTissXml");
            String prestadorId = (String) execution.getVariable("prestadorId");
            String tipoGuia = (String) execution.getVariable("tipoGuia");

            // 2. Validar dados obrigatórios
            validateInputs(guiaTissXml, prestadorId, tipoGuia);

            // 3. Validar guia TISS (schema XSD, regras de negócio)
            GuiaTissDTO guiaValidada = tissService.validarGuiaTiss(guiaTissXml, tipoGuia);

            // 4. Definir variáveis de saída
            execution.setVariable("guiaValida", guiaValidada.isValida());
            execution.setVariable("numeroGuia", guiaValidada.getNumeroGuia());
            execution.setVariable("errosValidacao", guiaValidada.getErrosValidacao());
            execution.setVariable("dadosGuia", guiaValidada.getDadosEstruturados());
            execution.setVariable("versaoTiss", guiaValidada.getVersaoTiss());

            if (guiaValidada.isValida()) {
                log.info("Guia TISS válida - Número: {}, Tipo: {}, Versão: {}",
                         guiaValidada.getNumeroGuia(), tipoGuia, guiaValidada.getVersaoTiss());
            } else {
                log.warn("Guia TISS inválida - Número: {}, Erros: {}",
                         guiaValidada.getNumeroGuia(), guiaValidada.getErrosValidacao().size());
            }

        } catch (ValidationException e) {
            log.error("Erro de validação na guia TISS: {}", e.getMessage(), e);
            execution.setVariable("guiaValida", false);
            execution.setVariable("errosValidacao", java.util.List.of(e.getMessage()));
            // Não lança exceção - fluxo continua para tratamento de erro

        } catch (Exception e) {
            log.error("Erro inesperado ao validar guia TISS: {}", e.getMessage(), e);
            execution.setVariable("guiaValida", false);
            throw new RuntimeException("Falha ao processar guia TISS", e);
        }
    }

    private void validateInputs(String guiaTissXml, String prestadorId, String tipoGuia) {
        if (guiaTissXml == null || guiaTissXml.isBlank()) {
            throw new IllegalArgumentException("guiaTissXml é obrigatório");
        }
        if (prestadorId == null || prestadorId.isBlank()) {
            throw new IllegalArgumentException("prestadorId é obrigatório");
        }
        if (tipoGuia == null || tipoGuia.isBlank()) {
            throw new IllegalArgumentException("tipoGuia é obrigatório");
        }
        if (!guiaTissXml.trim().startsWith("<?xml") && !guiaTissXml.trim().startsWith("<")) {
            throw new IllegalArgumentException("guiaTissXml deve ser um XML válido");
        }
    }
}
