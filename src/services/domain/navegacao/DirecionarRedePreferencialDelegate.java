package br.com.austa.experiencia.service.domain.navegacao;

import com.healthplan.services.network.NetworkDirectoryService;
import com.healthplan.models.NetworkDirectionResult;
import com.healthplan.models.Location;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Direcionar Rede Preferencial Delegate
 *
 * Direciona beneficiário para prestadores na rede preferencial.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - requiredSpecialization (String): Especialização necessária
 * - preferredLocation (Object): Localização preferida
 *
 * OUTPUT:
 * - recommendedProviders (List): Prestadores recomendados
 * - networkId (String): ID da rede
 * - providersCount (Integer): Quantidade de prestadores encontrados
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-007 Navegação)
 */
@Slf4j
@Component("direcionarRedePreferencialDelegate")
public class DirecionarRedePreferencialDelegate implements JavaDelegate {

    @Autowired
    private NetworkDirectoryService networkService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        String specialization = (String) execution.getVariable("requiredSpecialization");
        Location location = (Location) execution.getVariable("preferredLocation");

        log.info("Direcionando para rede preferencial - Beneficiário: {}, Especialização: {}",
            beneficiaryId, specialization);

        try {
            // Direcionar para rede
            NetworkDirectionResult result = networkService.directToNetwork(
                beneficiaryId,
                specialization,
                location
            );

            // Armazenar resultados
            execution.setVariable("recommendedProviders", result.getRecommendedProviders());
            execution.setVariable("networkId", result.getNetworkId());
            execution.setVariable("networkName", result.getNetworkName());
            execution.setVariable("providersCount", result.getTotalProvidersFound());
            execution.setVariable("hasProviders", result.getTotalProvidersFound() > 0);

            log.info("Direcionamento concluído - {} prestadores encontrados na rede {}",
                result.getTotalProvidersFound(), result.getNetworkName());

        } catch (Exception e) {
            log.error("Erro ao direcionar para rede - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("networkDirectionError", e.getMessage());
            throw e;
        }
    }
}
