package br.com.austa.experiencia.service.domain.navegacao;

import com.healthplan.services.network.NetworkDirectoryService;
import com.healthplan.models.ScheduledAppointment;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agendar Consulta Rede Delegate
 *
 * Agenda consulta com prestador na rede preferencial.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - providerId (String): ID do prestador selecionado
 * - slotId (String): ID do horário selecionado
 * - appointmentType (String): Tipo de consulta
 *
 * OUTPUT:
 * - appointmentId (String): ID da consulta agendada
 * - confirmationCode (String): Código de confirmação
 * - appointmentDateTime (DateTime): Data/hora da consulta
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-007 Navegação)
 */
@Slf4j
@Component("agendarConsultaRedeDelegate")
public class AgendarConsultaRedeDelegate implements JavaDelegate {

    @Autowired
    private NetworkDirectoryService networkService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        String providerId = (String) execution.getVariable("providerId");
        String slotId = (String) execution.getVariable("slotId");
        String appointmentType = (String) execution.getVariable("appointmentType");

        log.info("Agendando consulta - Beneficiário: {}, Prestador: {}",
            beneficiaryId, providerId);

        try {
            // Agendar consulta
            ScheduledAppointment appointment = networkService.scheduleAppointment(
                beneficiaryId,
                providerId,
                slotId,
                appointmentType
            );

            // Armazenar agendamento
            execution.setVariable("appointmentId", appointment.getAppointmentId());
            execution.setVariable("confirmationCode", appointment.getConfirmationCode());
            execution.setVariable("appointmentDateTime", appointment.getScheduledTime());
            execution.setVariable("appointmentDuration", appointment.getDuration());
            execution.setVariable("appointmentLocation", appointment.getLocation());
            execution.setVariable("appointmentStatus", appointment.getStatus());
            execution.setVariable("providerName", appointment.getProviderName());

            log.info("Consulta agendada - ID: {}, Confirmação: {}, Data: {}",
                appointment.getAppointmentId(),
                appointment.getConfirmationCode(),
                appointment.getScheduledTime());

        } catch (Exception e) {
            log.error("Erro ao agendar consulta - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("schedulingError", e.getMessage());
            throw e;
        }
    }
}
