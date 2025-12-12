package com.experiencia.services.domain.followup;

import com.experiencia.clients.EmailClient;
import com.experiencia.clients.NotificacaoClient;
import com.experiencia.models.Cliente;
import com.experiencia.models.RespostaNps;
import com.experiencia.models.PlanoRecuperacao;
import com.experiencia.models.Ticket;
import com.experiencia.repositories.ClienteRepository;
import com.experiencia.repositories.RespostaNpsRepository;
import com.experiencia.repositories.PlanoRecuperacaoRepository;
import com.experiencia.repositories.TicketRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Delegate: Acionar Recuperação de Detratores
 *
 * Responsabilidades:
 * - Criar plano de recuperação personalizado
 * - Abrir ticket prioritário para time de sucesso
 * - Notificar gerente de conta
 * - Agendar follow-up proativo
 * - Acionar compensações quando aplicável
 */
@Component("acionarRecuperacaoDetratoresDelegate")
public class AcionarRecuperacaoDetratoresDelegate implements JavaDelegate {

    @Autowired
    private RespostaNpsRepository respostaNpsRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PlanoRecuperacaoRepository planoRecuperacaoRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private NotificacaoClient notificacaoClient;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long respostaId = (Long) execution.getVariable("respostaId");
        String urgencia = (String) execution.getVariable("urgencia");

        // 1. Recuperar dados
        RespostaNps resposta = respostaNpsRepository.findById(respostaId)
            .orElseThrow(() -> new RuntimeException("Resposta NPS não encontrada: " + respostaId));

        Cliente cliente = clienteRepository.findById(resposta.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // 2. Avaliar severidade
        String severidade = avaliarSeveridade(resposta, cliente, urgencia);

        // 3. Criar plano de recuperação
        PlanoRecuperacao plano = criarPlanoRecuperacao(resposta, cliente, severidade);
        planoRecuperacaoRepository.save(plano);

        // 4. Abrir ticket prioritário
        Ticket ticket = abrirTicketRecuperacao(resposta, cliente, plano, severidade);
        ticketRepository.save(ticket);

        // 5. Notificar stakeholders
        notificarStakeholders(cliente, resposta, ticket, severidade);

        // 6. Executar ações imediatas baseadas em severidade
        executarAcoesImediatas(cliente, plano, severidade);

        // 7. Setar variáveis de processo
        execution.setVariable("planoRecuperacaoId", plano.getId());
        execution.setVariable("ticketId", ticket.getId());
        execution.setVariable("severidade", severidade);
        execution.setVariable("recuperacaoIniciada", true);
    }

    private String avaliarSeveridade(RespostaNps resposta, Cliente cliente, String urgencia) {
        // Fatores de severidade:
        // 1. Score NPS
        // 2. Urgência detectada na análise de sentimento
        // 3. Valor do cliente (LTV, segmento)
        // 4. Histórico recente

        int pontosSeveridade = 0;

        // Score muito baixo (0-3)
        if (resposta.getScore() <= 3) {
            pontosSeveridade += 3;
        } else if (resposta.getScore() <= 6) {
            pontosSeveridade += 2;
        }

        // Urgência detectada
        if ("ALTA".equals(urgencia)) {
            pontosSeveridade += 3;
        } else if ("MEDIA".equals(urgencia)) {
            pontosSeveridade += 1;
        }

        // Cliente de alto valor
        if ("PREMIUM".equals(cliente.getSegmento()) || "ENTERPRISE".equals(cliente.getSegmento())) {
            pontosSeveridade += 2;
        }

        // Tendência piorando
        if ("PIORANDO".equals(cliente.getTendenciaNps())) {
            pontosSeveridade += 1;
        }

        // Classificar severidade
        if (pontosSeveridade >= 7) {
            return "CRITICA";
        } else if (pontosSeveridade >= 4) {
            return "ALTA";
        } else {
            return "MEDIA";
        }
    }

    private PlanoRecuperacao criarPlanoRecuperacao(RespostaNps resposta, Cliente cliente, String severidade) {
        PlanoRecuperacao plano = new PlanoRecuperacao();
        plano.setClienteId(cliente.getId());
        plano.setRespostaNpsId(resposta.getId());
        plano.setSeveridade(severidade);
        plano.setStatus("ATIVO");
        plano.setDataCriacao(LocalDateTime.now());

        // Definir ações baseadas em severidade
        switch (severidade) {
            case "CRITICA":
                plano.addAcao("CONTATO_GERENTE_IMEDIATO");
                plano.addAcao("OFERECER_COMPENSACAO");
                plano.addAcao("REUNIAO_PRESENCIAL");
                plano.addAcao("PLANO_ACAO_CUSTOMIZADO");
                plano.setPrazoResolucao(24); // horas
                break;

            case "ALTA":
                plano.addAcao("CONTATO_TELEFONICO_PRIORITARIO");
                plano.addAcao("AVALIAR_COMPENSACAO");
                plano.addAcao("FOLLOW_UP_SEMANAL");
                plano.setPrazoResolucao(72); // horas
                break;

            case "MEDIA":
                plano.addAcao("EMAIL_PERSONALIZADO");
                plano.addAcao("FOLLOW_UP_QUINZENAL");
                plano.setPrazoResolucao(168); // 1 semana
                break;
        }

        // Adicionar compensações se aplicável
        if (deveOferecerCompensacao(resposta, cliente)) {
            plano.setCompensacao(calcularCompensacao(cliente, severidade));
        }

        return plano;
    }

    private Ticket abrirTicketRecuperacao(RespostaNps resposta, Cliente cliente,
                                          PlanoRecuperacao plano, String severidade) {
        Ticket ticket = new Ticket();
        ticket.setClienteId(cliente.getId());
        ticket.setTipo("RECUPERACAO_DETRATOR");
        ticket.setSeveridade(severidade);
        ticket.setStatus("ABERTO");
        ticket.setDataAbertura(LocalDateTime.now());

        // Prioridade baseada em severidade
        switch (severidade) {
            case "CRITICA":
                ticket.setPrioridade("URGENTE");
                ticket.setSla(24);
                break;
            case "ALTA":
                ticket.setPrioridade("ALTA");
                ticket.setSla(72);
                break;
            default:
                ticket.setPrioridade("MEDIA");
                ticket.setSla(168);
        }

        // Descrição detalhada
        StringBuilder descricao = new StringBuilder();
        descricao.append("RECUPERAÇÃO DE DETRATOR NPS\n\n");
        descricao.append("Cliente: ").append(cliente.getNome()).append("\n");
        descricao.append("Score NPS: ").append(resposta.getScore()).append("\n");
        descricao.append("Severidade: ").append(severidade).append("\n\n");

        if (resposta.getFeedback() != null) {
            descricao.append("Feedback:\n").append(resposta.getFeedback()).append("\n\n");
        }

        descricao.append("Plano de Recuperação:\n");
        for (String acao : plano.getAcoes()) {
            descricao.append("- ").append(acao).append("\n");
        }

        ticket.setDescricao(descricao.toString());

        // Atribuir para equipe apropriada
        ticket.setEquipe(determinarEquipe(severidade, cliente));

        return ticket;
    }

    private void notificarStakeholders(Cliente cliente, RespostaNps resposta,
                                       Ticket ticket, String severidade) {
        // 1. Notificar gerente de conta (se cliente premium)
        if ("PREMIUM".equals(cliente.getSegmento()) || "ENTERPRISE".equals(cliente.getSegmento())) {
            String gerenteEmail = cliente.getGerenteContaEmail();
            if (gerenteEmail != null) {
                emailClient.enviar(
                    gerenteEmail,
                    "URGENTE: Cliente Detrator NPS - " + cliente.getNome(),
                    montarEmailGerente(cliente, resposta, ticket)
                );
            }
        }

        // 2. Notificar equipe de sucesso do cliente
        notificacaoClient.notificar(
            "EQUIPE_SUCESSO_CLIENTE",
            "Novo caso de recuperação: " + severidade,
            ticket.getId()
        );

        // 3. Se crítico, notificar diretoria
        if ("CRITICA".equals(severidade)) {
            notificacaoClient.notificar(
                "DIRETORIA_CX",
                "CRÍTICO: Detrator NPS score " + resposta.getScore(),
                ticket.getId()
            );
        }
    }

    private void executarAcoesImediatas(Cliente cliente, PlanoRecuperacao plano, String severidade) {
        // Ações automáticas baseadas em severidade

        if ("CRITICA".equals(severidade)) {
            // 1. Ligar imediatamente (criar task para call center)
            criarTaskLigacao(cliente, "IMEDIATA");

            // 2. Enviar email de desculpas personalizado
            enviarEmailDesculpas(cliente);

            // 3. Preparar proposta de compensação
            prepararCompensacao(cliente, plano);
        }

        // Agendar follow-ups
        agendarFollowUps(cliente, plano);
    }

    private boolean deveOferecerCompensacao(RespostaNps resposta, Cliente cliente) {
        // Critérios para oferecer compensação:
        // 1. Score muito baixo (0-4)
        // 2. Cliente de alto valor
        // 3. Problema relacionado a falha da empresa

        return resposta.getScore() <= 4 &&
               ("PREMIUM".equals(cliente.getSegmento()) || "ENTERPRISE".equals(cliente.getSegmento()));
    }

    private String calcularCompensacao(Cliente cliente, String severidade) {
        // Simplificado - na prática, usar regras de negócio mais complexas
        switch (severidade) {
            case "CRITICA":
                return "DESCONTO_30PCT_PROXIMA_COMPRA";
            case "ALTA":
                return "DESCONTO_15PCT_PROXIMA_COMPRA";
            default:
                return "FRETE_GRATIS_PROXIMA_COMPRA";
        }
    }

    private String determinarEquipe(String severidade, Cliente cliente) {
        if ("CRITICA".equals(severidade)) {
            return "SUCESSO_CLIENTE_PREMIUM";
        } else if ("PREMIUM".equals(cliente.getSegmento())) {
            return "SUCESSO_CLIENTE_VIP";
        } else {
            return "SUCESSO_CLIENTE_GERAL";
        }
    }

    private String montarEmailGerente(Cliente cliente, RespostaNps resposta, Ticket ticket) {
        return String.format(
            "Prezado(a) Gerente,\n\n" +
            "Informamos que o cliente %s (ID: %d) respondeu à pesquisa NPS com score %d (Detrator).\n\n" +
            "Feedback: %s\n\n" +
            "Um ticket prioritário foi aberto (#%d) e ações de recuperação foram iniciadas.\n\n" +
            "Por favor, entre em contato com o cliente o mais breve possível.\n\n" +
            "Atenciosamente,\n" +
            "Sistema de Experiência do Cliente",
            cliente.getNome(),
            cliente.getId(),
            resposta.getScore(),
            resposta.getFeedback() != null ? resposta.getFeedback() : "Não fornecido",
            ticket.getId()
        );
    }

    private void criarTaskLigacao(Cliente cliente, String prioridade) {
        // Criar task no sistema de call center
        System.out.println("Task de ligação criada para cliente: " + cliente.getId());
    }

    private void enviarEmailDesculpas(Cliente cliente) {
        // Email personalizado de desculpas
        emailClient.enviar(
            cliente.getEmail(),
            "Pedimos desculpas pela sua experiência",
            "Prezado(a) " + cliente.getNome() + ",\n\n" +
            "Agradecemos o seu feedback e pedimos sinceras desculpas pela experiência abaixo do esperado.\n\n" +
            "Nossa equipe entrará em contato em breve para resolver a situação.\n\n" +
            "Atenciosamente,\n" +
            "Equipe de Relacionamento"
        );
    }

    private void prepararCompensacao(Cliente cliente, PlanoRecuperacao plano) {
        // Preparar proposta de compensação
        System.out.println("Compensação preparada: " + plano.getCompensacao());
    }

    private void agendarFollowUps(Cliente cliente, PlanoRecuperacao plano) {
        // Agendar follow-ups automáticos
        System.out.println("Follow-ups agendados para cliente: " + cliente.getId());
    }
}
