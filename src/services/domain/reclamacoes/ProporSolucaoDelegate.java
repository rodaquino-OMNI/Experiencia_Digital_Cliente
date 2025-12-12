package br.com.austa.experiencia.services.domain.reclamacoes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Propõe solução ao beneficiário baseado na análise.
 *
 * Variáveis de entrada:
 * - beneficiarioId (String)
 * - protocoloReclamacao (String)
 * - solucaoRecomendada (SolucaoDTO)
 * - canalOrigem (String)
 *
 * Variáveis de saída:
 * - solucaoProposta (SolucaoDTO)
 * - mensagemProposta (String)
 * - aguardandoAceite (Boolean)
 */
@Slf4j
@Component("proporSolucaoDelegate")
@RequiredArgsConstructor
public class ProporSolucaoDelegate implements JavaDelegate {

    private final WhatsAppService whatsAppService;
    private final NotificationService notificationService;
    private final ReclamacaoService reclamacaoService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String protocolo = (String) execution.getVariable("protocoloReclamacao");
        String canalOrigem = (String) execution.getVariable("canalOrigem");
        SolucaoDTO solucaoRecomendada = (SolucaoDTO) execution.getVariable("solucaoRecomendada");

        // 1. Preparar mensagem de proposta
        String mensagem = buildMensagemProposta(protocolo, solucaoRecomendada);

        // 2. Enviar pelo canal de origem
        boolean enviado = false;
        switch (canalOrigem) {
            case "WHATSAPP":
                enviado = whatsAppService.sendMessage(beneficiarioId, mensagem);
                break;
            case "APP":
                enviado = notificationService.sendPush(beneficiarioId, mensagem);
                break;
            case "EMAIL":
                enviado = notificationService.sendEmail(beneficiarioId,
                    "Solução para sua reclamação - Protocolo " + protocolo, mensagem);
                break;
            default:
                enviado = notificationService.sendSms(beneficiarioId, mensagem);
        }

        // 3. Atualizar status da reclamação
        reclamacaoService.atualizarStatus(protocolo, "AGUARDANDO_ACEITE");

        // 4. Definir variáveis
        execution.setVariable("solucaoProposta", solucaoRecomendada);
        execution.setVariable("mensagemProposta", mensagem);
        execution.setVariable("aguardandoAceite", true);
        execution.setVariable("propostaEnviada", enviado);
        execution.setVariable("dataPropostaEnviada", LocalDateTime.now());

        log.info("Solução proposta enviada - Protocolo: {}, Canal: {}", protocolo, canalOrigem);
    }

    private String buildMensagemProposta(String protocolo, SolucaoDTO solucao) {
        return String.format("""
            Olá! Analisamos sua reclamação (Protocolo: %s).

            📋 Nossa proposta de solução:
            %s

            ✅ Para aceitar esta solução, responda SIM
            ❌ Para recusar e falar com um especialista, responda NAO

            Estamos à disposição!
            """, protocolo, solucao.getDescricao());
    }
}
