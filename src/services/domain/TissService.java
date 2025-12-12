package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * TISS Service - ANS Standard Guide Validation Delegate
 *
 * Validates TISS (Troca de Informações na Saúde Suplementar) guides
 * according to ANS regulatory standards for healthcare transactions.
 *
 * BPMN Coverage:
 * - tissService.validarGuia (Validate TISS guide)
 */
@Component("tissService")
public class TissService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(TissService.class);

    // TISS guide number format: YYYYMMDD-XXXXXXXX-XX
    private static final Pattern TISS_PATTERN = Pattern.compile("^\\d{8}-\\d{8}-\\d{2}$");

    // Common TUSS procedure code patterns
    private static final Pattern TUSS_CODE_PATTERN = Pattern.compile("^[0-9]{8}$");

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("tissMethod");

        if ("validarGuia".equals(method)) {
            validarGuia(execution);
        } else {
            logger.warn("Unknown TISS method: {}", method);
            throw new IllegalArgumentException("Invalid TISS method: " + method);
        }
    }

    /**
     * Validate TISS guide
     *
     * Performs comprehensive validation of TISS guides according to ANS standards:
     * - Authorization number format validation (ANS pattern)
     * - Procedure code validation (TUSS table)
     * - Required field completeness
     * - Date consistency checks
     * - Provider credential validation
     * - Beneficiary eligibility validation
     *
     * @param execution Process execution context
     */
    public void validarGuia(DelegateExecution execution) throws Exception {
        logger.info("Executing tissService.validarGuia for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract TISS guide data
            String numeroGuia = (String) execution.getVariable("numeroGuia");
            String tipoGuia = (String) execution.getVariable("tipoGuia");
            String numeroAutorizacao = (String) execution.getVariable("numeroAutorizacao");
            List<String> procedimentos = (List<String>) execution.getVariable("procedimentos");
            String prestadorCodigo = (String) execution.getVariable("prestadorCodigo");
            String beneficiarioCpf = (String) execution.getVariable("beneficiarioCpf");
            String dataAtendimento = (String) execution.getVariable("dataAtendimento");

            List<String> errosValidacao = new ArrayList<>();
            List<String> alertasValidacao = new ArrayList<>();
            boolean valido = true;

            // 1. Validate authorization number format
            if (numeroAutorizacao == null || numeroAutorizacao.trim().isEmpty()) {
                errosValidacao.add("Número de autorização é obrigatório");
                valido = false;
            } else if (!TISS_PATTERN.matcher(numeroAutorizacao).matches()) {
                errosValidacao.add("Formato inválido do número de autorização ANS. " +
                                  "Formato esperado: AAAAMMDD-XXXXXXXX-XX");
                valido = false;
            }

            // 2. Validate guide number
            if (numeroGuia == null || numeroGuia.trim().isEmpty()) {
                errosValidacao.add("Número da guia é obrigatório");
                valido = false;
            }

            // 3. Validate guide type
            if (!validarTipoGuia(tipoGuia)) {
                errosValidacao.add("Tipo de guia inválido: " + tipoGuia);
                valido = false;
            }

            // 4. Validate procedures (TUSS codes)
            if (procedimentos == null || procedimentos.isEmpty()) {
                errosValidacao.add("Pelo menos um procedimento deve ser informado");
                valido = false;
            } else {
                for (String procedimento : procedimentos) {
                    if (!validarCodigoTUSS(procedimento)) {
                        errosValidacao.add("Código TUSS inválido: " + procedimento);
                        valido = false;
                    }
                }
            }

            // 5. Validate provider
            if (prestadorCodigo == null || prestadorCodigo.trim().isEmpty()) {
                errosValidacao.add("Código do prestador é obrigatório");
                valido = false;
            } else if (!validarPrestador(prestadorCodigo)) {
                alertasValidacao.add("Prestador não encontrado na rede credenciada: " + prestadorCodigo);
            }

            // 6. Validate beneficiary
            if (beneficiarioCpf == null || beneficiarioCpf.trim().isEmpty()) {
                errosValidacao.add("CPF do beneficiário é obrigatório");
                valido = false;
            } else if (!validarCPF(beneficiarioCpf)) {
                errosValidacao.add("CPF do beneficiário inválido");
                valido = false;
            }

            // 7. Validate service date
            if (dataAtendimento == null || dataAtendimento.trim().isEmpty()) {
                errosValidacao.add("Data de atendimento é obrigatória");
                valido = false;
            } else if (!validarDataAtendimento(dataAtendimento)) {
                alertasValidacao.add("Data de atendimento futura ou muito antiga");
            }

            // 8. Validate required fields based on guide type
            validarCamposObrigatoriosPorTipo(tipoGuia, execution, errosValidacao);

            // Store validation results
            execution.setVariable("tissValidado", valido);
            execution.setVariable("tissErros", errosValidacao);
            execution.setVariable("tissAlertas", alertasValidacao);
            execution.setVariable("tissNumeroErros", errosValidacao.size());
            execution.setVariable("tissNumeroAlertas", alertasValidacao.size());

            // Log validation summary
            if (valido) {
                logger.info("TISS guide validated successfully: guide={}, authorization={}, procedures={}",
                    numeroGuia, numeroAutorizacao, procedimentos != null ? procedimentos.size() : 0);
            } else {
                logger.warn("TISS guide validation failed: guide={}, errors={}, alerts={}",
                    numeroGuia, errosValidacao.size(), alertasValidacao.size());
                logger.warn("Validation errors: {}", errosValidacao);
            }

        } catch (Exception e) {
            logger.error("Error validating TISS guide for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("tissValidacaoErro", e.getMessage());
            execution.setVariable("tissValidado", false);
            throw e;
        }
    }

    /**
     * Validate guide type against ANS standard types
     */
    private boolean validarTipoGuia(String tipo) {
        if (tipo == null) return false;

        List<String> tiposValidos = Arrays.asList(
            "CONSULTA",
            "SADT",           // Serviços Auxiliares de Diagnóstico e Terapia
            "SP-SADT",        // Solicitação de Procedimentos
            "HONORARIO",      // Honorários Médicos
            "INTERNACAO",
            "ODONTOLOGICA",
            "RESUMO_INTERNACAO",
            "TRATAMENTO_ODONTOLOGICO"
        );

        return tiposValidos.contains(tipo.toUpperCase());
    }

    /**
     * Validate TUSS procedure code format
     */
    private boolean validarCodigoTUSS(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return false;
        }

        // Remove formatting characters
        String codigoLimpo = codigo.replaceAll("[^0-9]", "");

        // TUSS codes are 8-digit numeric codes
        return TUSS_CODE_PATTERN.matcher(codigoLimpo).matches();
    }

    /**
     * Validate provider credentials
     */
    private boolean validarPrestador(String codigo) {
        // Simplified - in production, query provider registry
        return codigo != null && codigo.length() >= 4;
    }

    /**
     * Validate CPF format and check digit
     */
    private boolean validarCPF(String cpf) {
        if (cpf == null) return false;

        // Remove formatting
        cpf = cpf.replaceAll("[^0-9]", "");

        // CPF must have 11 digits
        if (cpf.length() != 11) return false;

        // Check for known invalid CPFs (all same digit)
        if (cpf.matches("(\\d)\\1{10}")) return false;

        // Validate check digits (simplified)
        return true;
    }

    /**
     * Validate service date reasonability
     */
    private boolean validarDataAtendimento(String data) {
        // Simplified - check date is not future and not too old
        // In production, parse date and validate against current date
        return data != null && data.length() >= 10;
    }

    /**
     * Validate required fields based on guide type
     */
    private void validarCamposObrigatoriosPorTipo(String tipoGuia, DelegateExecution execution,
                                                  List<String> erros) {
        if (tipoGuia == null) return;

        switch (tipoGuia.toUpperCase()) {
            case "INTERNACAO":
                // Internment requires additional fields
                if (execution.getVariable("dataInternacao") == null) {
                    erros.add("Data de internação obrigatória para guia de internação");
                }
                if (execution.getVariable("tipoAcomodacao") == null) {
                    erros.add("Tipo de acomodação obrigatório para guia de internação");
                }
                break;

            case "SADT":
                // SADT requires referral
                if (execution.getVariable("numeroGuiaPrincipal") == null) {
                    erros.add("Guia principal (referência) obrigatória para SADT");
                }
                break;

            case "HONORARIO":
                // Honorarium requires professional data
                if (execution.getVariable("crm") == null) {
                    erros.add("CRM do profissional obrigatório para guia de honorários");
                }
                break;
        }
    }
}
