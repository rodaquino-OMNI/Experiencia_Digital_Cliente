package br.com.austa.experiencia.services.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Serviço Financeiro para compensações e créditos
 */
@Slf4j
@Service
public class FinanceiroService {

    /**
     * Aplica desconto na mensalidade do beneficiário
     */
    public String aplicarDesconto(String beneficiarioId, BigDecimal valor, int meses) {
        String codigo = "DESC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Desconto aplicado: {} para beneficiário: {} no valor de {} por {} meses",
                 codigo, beneficiarioId, valor, meses);

        // Em produção: integração com sistema financeiro
        return codigo;
    }

    /**
     * Adiciona crédito na conta do beneficiário
     */
    public String adicionarCredito(String beneficiarioId, BigDecimal valor) {
        String codigo = "CRED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Crédito adicionado: {} para beneficiário: {} no valor de {}",
                 codigo, beneficiarioId, valor);

        return codigo;
    }

    /**
     * Processa reembolso para o beneficiário
     */
    public String processarReembolso(String beneficiarioId, BigDecimal valor, String dadosBancarios) {
        String codigo = "REMB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Reembolso processado: {} para beneficiário: {} no valor de {}",
                 codigo, beneficiarioId, valor);

        // Em produção: integração com sistema de pagamentos
        return codigo;
    }
}
