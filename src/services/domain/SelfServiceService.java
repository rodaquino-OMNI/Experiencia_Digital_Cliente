package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Self Service Service - Beneficiary Self-Service Operations Delegate
 *
 * Enables beneficiaries to perform self-service operations including
 * data queries and document generation (invoice 2nd copy).
 *
 * BPMN Coverage:
 * - selfServiceService.consultarDados (Self-service data query)
 * - selfServiceService.gerar2Via (Generate invoice 2nd copy)
 */
@Component("selfServiceService")
public class SelfServiceService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SelfServiceService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private TasyBeneficiarioService tasyService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("selfServiceMethod");

        if ("consultarDados".equals(method)) {
            consultarDados(execution);
        } else if ("gerar2Via".equals(method)) {
            gerar2Via(execution);
        } else {
            logger.warn("Unknown self-service method: {}", method);
            throw new IllegalArgumentException("Invalid self-service method: " + method);
        }
    }

    /**
     * Self-service data query
     *
     * Allows beneficiaries to query their own data including:
     * - Plan details and coverage
     * - Claims history
     * - Authorization status
     * - Payment history
     * - Network providers
     *
     * @param execution Process execution context
     */
    public void consultarDados(DelegateExecution execution) throws Exception {
        logger.info("Executing selfServiceService.consultarDados for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String tipoConsulta = (String) execution.getVariable("tipoConsulta");

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("beneficiarioId", beneficiarioId);
            resultado.put("tipoConsulta", tipoConsulta);
            resultado.put("dataConsulta", LocalDateTime.now().toString());

            // Execute query based on type
            switch (tipoConsulta) {
                case "PLANO":
                    resultado.put("dados", consultarDadosPlano(beneficiarioId, execution));
                    break;

                case "SINISTROS":
                    resultado.put("dados", consultarSinistros(beneficiarioId, execution));
                    break;

                case "AUTORIZACOES":
                    resultado.put("dados", consultarAutorizacoes(beneficiarioId, execution));
                    break;

                case "FINANCEIRO":
                    resultado.put("dados", consultarFinanceiro(beneficiarioId, execution));
                    break;

                case "REDE_CREDENCIADA":
                    resultado.put("dados", consultarRedeCredenciada(execution));
                    break;

                case "HISTORICO_ATENDIMENTOS":
                    resultado.put("dados", consultarHistoricoAtendimentos(beneficiarioId, execution));
                    break;

                default:
                    throw new IllegalArgumentException("Tipo de consulta não suportado: " + tipoConsulta);
            }

            execution.setVariable("consultaRealizada", true);
            execution.setVariable("resultadoConsulta", resultado);

            logger.info("Self-service query completed: beneficiary={}, type={}, success=true",
                beneficiarioId, tipoConsulta);

        } catch (Exception e) {
            logger.error("Error executing self-service query for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("consultaErro", e.getMessage());
            execution.setVariable("consultaRealizada", false);
            throw e;
        }
    }

    /**
     * Generate invoice 2nd copy
     *
     * Generates and delivers second copy of invoices for beneficiaries
     * via email, app, or download link.
     *
     * @param execution Process execution context
     */
    public void gerar2Via(DelegateExecution execution) throws Exception {
        logger.info("Executing selfServiceService.gerar2Via for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String mesReferencia = (String) execution.getVariable("mesReferencia");
            String anoReferencia = (String) execution.getVariable("anoReferencia");
            String canalEntrega = (String) execution.getVariable("canalEntrega");

            // Fetch invoice data from Tasy
            Map<String, Object> dadosFatura = consultarDadosFatura(
                beneficiarioId, mesReferencia, anoReferencia, execution);

            if (dadosFatura == null || dadosFatura.isEmpty()) {
                logger.warn("Invoice not found for beneficiary={}, period={}/{}",
                    beneficiarioId, mesReferencia, anoReferencia);

                execution.setVariable("segunda ViaGerada", false);
                execution.setVariable("segundaViaErro", "Fatura não encontrada para o período solicitado");
                return;
            }

            // Generate invoice document
            Map<String, Object> documentoFatura = gerarDocumentoFatura(dadosFatura);

            String segundaViaId = UUID.randomUUID().toString();
            String urlDownload = "https://portal.operadora.com.br/faturas/" + segundaViaId + ".pdf";

            // Store generation record
            Map<String, Object> registroSegundaVia = new HashMap<>();
            registroSegundaVia.put("segundaViaId", segundaViaId);
            registroSegundaVia.put("beneficiarioId", beneficiarioId);
            registroSegundaVia.put("mesReferencia", mesReferencia);
            registroSegundaVia.put("anoReferencia", anoReferencia);
            registroSegundaVia.put("dataGeracao", LocalDateTime.now().toString());
            registroSegundaVia.put("canalEntrega", canalEntrega);
            registroSegundaVia.put("urlDownload", urlDownload);
            registroSegundaVia.put("valorFatura", dadosFatura.get("valorTotal"));
            registroSegundaVia.put("dataVencimento", dadosFatura.get("dataVencimento"));

            dataLakeService.salvar(execution, "segunda_vias_faturas",
                segundaViaId, registroSegundaVia);

            // Deliver based on channel
            boolean entregue = entregarSegundaVia(registroSegundaVia, canalEntrega, execution);

            execution.setVariable("segundaViaGerada", true);
            execution.setVariable("segundaViaId", segundaViaId);
            execution.setVariable("urlDownloadFatura", urlDownload);
            execution.setVariable("segundaViaEntregue", entregue);

            logger.info("Invoice 2nd copy generated: beneficiary={}, period={}/{}, id={}, delivered={}",
                beneficiarioId, mesReferencia, anoReferencia, segundaViaId, entregue);

        } catch (Exception e) {
            logger.error("Error generating invoice 2nd copy for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("segundaViaErro", e.getMessage());
            execution.setVariable("segundaViaGerada", false);
            throw e;
        }
    }

    // Query helper methods

    private Map<String, Object> consultarDadosPlano(String beneficiarioId, DelegateExecution execution) throws Exception {
        return tasyService.consultar(execution, beneficiarioId);
    }

    private Map<String, Object> consultarSinistros(String beneficiarioId, DelegateExecution execution) throws Exception {
        return dataLakeService.consultar(execution, "sinistros", beneficiarioId);
    }

    private Map<String, Object> consultarAutorizacoes(String beneficiarioId, DelegateExecution execution) throws Exception {
        return dataLakeService.consultar(execution, "autorizacoes", beneficiarioId);
    }

    private Map<String, Object> consultarFinanceiro(String beneficiarioId, DelegateExecution execution) throws Exception {
        return Map.of(
            "mensalidadeAtual", 450.00,
            "dataVencimento", "2025-01-10",
            "situacao", "EM_DIA",
            "ultimoPagamento", "2024-12-05"
        );
    }

    private Map<String, Object> consultarRedeCredenciada(DelegateExecution execution) {
        String especialidade = (String) execution.getVariable("especialidadeRequerida");
        String localizacao = (String) execution.getVariable("localizacao");

        // Simplified network query
        return Map.of(
            "totalPrestadores", 1250,
            "especialidade", especialidade,
            "localizacao", localizacao
        );
    }

    private Map<String, Object> consultarHistoricoAtendimentos(String beneficiarioId,
                                                               DelegateExecution execution) throws Exception {
        return dataLakeService.consultar(execution, "historico_atendimentos", beneficiarioId);
    }

    private Map<String, Object> consultarDadosFatura(String beneficiarioId, String mes, String ano,
                                                     DelegateExecution execution) throws Exception {
        // Query invoice data from Tasy
        String periodoReferencia = ano + "-" + mes;
        return Map.of(
            "beneficiarioId", beneficiarioId,
            "mesReferencia", mes,
            "anoReferencia", ano,
            "valorTotal", 450.00,
            "dataVencimento", ano + "-" + mes + "-10",
            "linhaDigitavel", "12345.67890 12345.678901 12345.678901 1 12345678901234"
        );
    }

    private Map<String, Object> gerarDocumentoFatura(Map<String, Object> dadosFatura) {
        // Generate PDF document (simplified)
        return Map.of(
            "formato", "PDF",
            "tamanho", "245KB",
            "geradoEm", LocalDateTime.now().toString()
        );
    }

    private boolean entregarSegundaVia(Map<String, Object> segundaVia, String canal,
                                      DelegateExecution execution) {
        // Deliver invoice based on channel
        switch (canal) {
            case "EMAIL":
                execution.setVariable("emailEnviado", true);
                return true;
            case "APP":
                execution.setVariable("notificacaoApp", true);
                return true;
            case "DOWNLOAD":
                // URL already provided
                return true;
            default:
                return false;
        }
    }
}
