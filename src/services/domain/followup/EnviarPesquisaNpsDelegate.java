package com.experiencia.services.domain.followup;

import com.experiencia.clients.EmailClient;
import com.experiencia.clients.SmsClient;
import com.experiencia.models.Cliente;
import com.experiencia.models.Interacao;
import com.experiencia.models.TemplateNps;
import com.experiencia.repositories.ClienteRepository;
import com.experiencia.repositories.InteracaoRepository;
import com.experiencia.repositories.TemplateNpsRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Delegate: Enviar Pesquisa NPS
 *
 * Responsabilidades:
 * - Recuperar dados do cliente
 * - Selecionar template apropriado baseado em segmento e preferências
 * - Gerar link único de pesquisa
 * - Enviar via canal preferencial
 * - Registrar envio
 */
@Component("enviarPesquisaNpsDelegate")
public class EnviarPesquisaNpsDelegate implements JavaDelegate {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private InteracaoRepository interacaoRepository;

    @Autowired
    private TemplateNpsRepository templateNpsRepository;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private SmsClient smsClient;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long clienteId = (Long) execution.getVariable("clienteId");
        Long interacaoId = (Long) execution.getVariable("interacaoId");

        // 1. Recuperar cliente e interação
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clienteId));

        Interacao interacao = interacaoRepository.findById(interacaoId)
            .orElseThrow(() -> new RuntimeException("Interação não encontrada: " + interacaoId));

        // 2. Selecionar template apropriado
        TemplateNps template = selecionarTemplate(cliente, interacao);

        // 3. Gerar link único de pesquisa
        String linkPesquisa = gerarLinkPesquisa(cliente, interacao);

        // 4. Personalizar mensagem
        String mensagem = personalizarMensagem(template, cliente, linkPesquisa);

        // 5. Enviar via canal preferencial
        boolean enviado = enviarPesquisa(cliente, mensagem, template);

        // 6. Registrar envio
        registrarEnvio(interacao, template, enviado);

        // 7. Setar variáveis de processo
        execution.setVariable("npsEnviado", enviado);
        execution.setVariable("dataEnvioNps", LocalDateTime.now());
        execution.setVariable("linkPesquisa", linkPesquisa);
    }

    private TemplateNps selecionarTemplate(Cliente cliente, Interacao interacao) {
        // Lógica de seleção:
        // 1. Baseado no segmento do cliente
        // 2. Tipo de interação
        // 3. Canal preferencial

        String segmento = cliente.getSegmento();
        String canal = cliente.getCanalPreferencial();
        String tipoInteracao = interacao.getTipo();

        Optional<TemplateNps> template = templateNpsRepository
            .findBySegmentoAndCanalAndTipoInteracao(segmento, canal, tipoInteracao);

        return template.orElseGet(() ->
            templateNpsRepository.findDefaultTemplate()
                .orElseThrow(() -> new RuntimeException("Nenhum template disponível"))
        );
    }

    private String gerarLinkPesquisa(Cliente cliente, Interacao interacao) {
        // Gerar token único
        String token = java.util.UUID.randomUUID().toString();

        // Armazenar token associado ao cliente e interação
        // (implementação simplificada - na prática, armazenar em cache ou BD)

        return String.format("https://pesquisa.empresa.com/nps/%s", token);
    }

    private String personalizarMensagem(TemplateNps template, Cliente cliente, String linkPesquisa) {
        String mensagem = template.getConteudo();

        // Substituir variáveis
        Map<String, String> variaveis = new HashMap<>();
        variaveis.put("{{nome}}", cliente.getNome());
        variaveis.put("{{primeiroNome}}", cliente.getPrimeiroNome());
        variaveis.put("{{link}}", linkPesquisa);

        for (Map.Entry<String, String> var : variaveis.entrySet()) {
            mensagem = mensagem.replace(var.getKey(), var.getValue());
        }

        return mensagem;
    }

    private boolean enviarPesquisa(Cliente cliente, String mensagem, TemplateNps template) {
        try {
            String canal = template.getCanal();

            switch (canal.toUpperCase()) {
                case "EMAIL":
                    return emailClient.enviar(
                        cliente.getEmail(),
                        template.getAssunto(),
                        mensagem
                    );

                case "SMS":
                    return smsClient.enviar(
                        cliente.getTelefone(),
                        mensagem
                    );

                case "WHATSAPP":
                    // Integração com WhatsApp Business API
                    return enviarWhatsApp(cliente.getTelefone(), mensagem);

                default:
                    // Fallback para email
                    return emailClient.enviar(
                        cliente.getEmail(),
                        template.getAssunto(),
                        mensagem
                    );
            }
        } catch (Exception e) {
            // Log erro
            System.err.println("Erro ao enviar pesquisa NPS: " + e.getMessage());
            return false;
        }
    }

    private boolean enviarWhatsApp(String telefone, String mensagem) {
        // Implementação simplificada
        // Na prática, integrar com WhatsApp Business API
        return true;
    }

    private void registrarEnvio(Interacao interacao, TemplateNps template, boolean enviado) {
        // Atualizar interação com dados do envio
        interacao.setNpsEnviado(enviado);
        interacao.setDataEnvioNps(LocalDateTime.now());
        interacao.setTemplateNpsId(template.getId());

        interacaoRepository.save(interacao);
    }
}
