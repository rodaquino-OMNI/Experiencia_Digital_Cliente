package br.com.austa.experiencia.service.domain.onboarding;

import com.healthplan.services.ocr.DocumentOcrService;
import com.healthplan.models.DocumentData;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Analisar Documentos OCR Delegate
 *
 * Analisa documentos enviados usando OCR para extração automática de dados.
 *
 * INPUT:
 * - uploadedDocuments (List<MultipartFile>): Documentos enviados
 * - documentTypes (List<String>): Tipos de cada documento
 *
 * OUTPUT:
 * - extractedData (Map): Dados extraídos dos documentos
 * - ocrConfidence (Double): Confiança da extração
 * - documentsValidated (Boolean): Documentos validados?
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-001 Onboarding)
 */
@Slf4j
@Component("analisarDocumentosOcrDelegate")
public class AnalisarDocumentosOcrDelegate implements JavaDelegate {

    @Autowired
    private DocumentOcrService ocrService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        @SuppressWarnings("unchecked")
        List<MultipartFile> documents = (List<MultipartFile>) execution.getVariable("uploadedDocuments");
        @SuppressWarnings("unchecked")
        List<String> documentTypes = (List<String>) execution.getVariable("documentTypes");

        log.info("Analisando {} documentos com OCR", documents.size());

        try {
            // Processar documentos em lote
            var batchResult = ocrService.batchAnalyze(documents, documentTypes);

            // Extrair dados principais
            Map<String, Object> extractedData = new java.util.HashMap<>();
            double totalConfidence = 0.0;

            for (DocumentData data : batchResult.getSuccessfulResults()) {
                extractedData.putAll(data.getExtractedFields());
                totalConfidence += data.getConfidenceScore();
            }

            double avgConfidence = batchResult.getSuccessCount() > 0 ?
                totalConfidence / batchResult.getSuccessCount() : 0.0;

            // Armazenar resultados
            execution.setVariable("extractedData", extractedData);
            execution.setVariable("ocrConfidence", avgConfidence);
            execution.setVariable("documentsValidated", avgConfidence >= 0.7);
            execution.setVariable("documentsProcessed", batchResult.getSuccessCount());
            execution.setVariable("documentsFailed", batchResult.getFailureCount());

            log.info("OCR concluído - {} sucesso, {} falhas, confiança: {:.2f}",
                batchResult.getSuccessCount(), batchResult.getFailureCount(), avgConfidence);

        } catch (Exception e) {
            log.error("Erro na análise OCR de documentos", e);
            execution.setVariable("ocrError", e.getMessage());
            execution.setVariable("documentsValidated", false);
            throw e;
        }
    }
}
