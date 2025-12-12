package br.com.austa.experiencia.services.domain.reclamacoes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Escala reclamação para ouvidoria em casos críticos.
 *
 * Variáveis de entrada:
 * - protocoloReclamacao (String)
 * - motivoEscalacao (String)
 * - criticidade (String)
 * - canalOrigem (String): Se ANS ou PROCON, prioridade máxima
 *
 * Variáveis de saída:
 * - protocoloOuvidoria (String)
 * - responsavelOuvidoria (String)
 * - prazoResposta (LocalDateTime)
 */
@Slf4j
@Component("escalarOuvidoriaDelegate")
@RequiredArgsConstructor
public class EscalarOuvidoriaDelegate implements JavaDelegate {

    private final OuvidoriaService ouvidoriaService;
    private final NotificationService notificationService;
    private final AnsService ansService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String protocolo = (String) execution.getVariable("protocoloReclamacao");
        String canalOrigem = (String) execution.getVariable("canalOrigem");
        String motivoEscalacao = (String) execution.getVariable("motivoEscalacao");

        log.warn("Escalando para ouvidoria - Protocolo: {}, Canal: {}",
                 protocolo, canalOrigem);

        // 1. Determinar prioridade
        PrioridadeOuvidoria prioridade = determinePrioridade(canalOrigem);

        // 2. Criar caso na ouvidoria
        CasoOuvidoria caso = ouvidoriaService.criarCaso(
            protocolo, motivoEscalacao, prioridade);

        // 3. Se origem é ANS, registrar NIP
        if ("ANS".equals(canalOrigem)) {
            String protocoloAns = ansService.registrarNip(protocolo, caso);
            execution.setVariable("protocoloAns", protocoloAns);
        }

        // 4. Notificar responsáveis
        notificationService.alertOuvidoria(caso);

        // 5. Calcular prazo conforme regulamentação
        LocalDateTime prazoResposta = calculatePrazo(canalOrigem, prioridade);

        // 6. Definir variáveis
        execution.setVariable("protocoloOuvidoria", caso.getProtocolo());
        execution.setVariable("responsavelOuvidoria", caso.getResponsavel());
        execution.setVariable("prazoResposta", prazoResposta);
        execution.setVariable("escaladoParaOuvidoria", true);
        execution.setVariable("prioridadeOuvidoria", prioridade.name());

        log.info("Caso escalado para ouvidoria - Protocolo: {}, Prazo: {}",
                 caso.getProtocolo(), prazoResposta);
    }

    private PrioridadeOuvidoria determinePrioridade(String canalOrigem) {
        return switch (canalOrigem) {
            case "ANS", "PROCON" -> PrioridadeOuvidoria.CRITICA;
            case "RECLAME_AQUI" -> PrioridadeOuvidoria.ALTA;
            default -> PrioridadeOuvidoria.NORMAL;
        };
    }

    private LocalDateTime calculatePrazo(String canalOrigem, PrioridadeOuvidoria prioridade) {
        return switch (canalOrigem) {
            case "ANS" -> LocalDateTime.now().plusDays(5);  // NIP: 5 dias úteis
            case "PROCON" -> LocalDateTime.now().plusDays(10); // PROCON: 10 dias
            default -> LocalDateTime.now().plusDays(
                prioridade == PrioridadeOuvidoria.CRITICA ? 1 :
                prioridade == PrioridadeOuvidoria.ALTA ? 3 : 7);
        };
    }
}
