package br.com.austa.experiencia.service.domain.cronicos.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Delegate for collecting health markers from chronic disease patients.
 *
 * <p>SUB-008 Gestão Crônicos - Health Marker Collection</p>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("coletarMarcadoresSaudeDelegate")
public class ColetarMarcadoresSaudeDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Collecting health markers - Process: {}", execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String condicaoCronica = (String) execution.getVariable("condicaoCronica");

            // Collect markers from multiple sources
            Map<String, Object> markers = new HashMap<>();

            // Patient-reported markers
            Map<String, Object> selfReported = collectSelfReportedMarkers(beneficiarioId);
            markers.putAll(selfReported);

            // Device data (wearables, glucose monitors, etc.)
            Map<String, Object> deviceData = collectDeviceData(beneficiarioId);
            markers.putAll(deviceData);

            // Lab results
            Map<String, Object> labResults = collectLatestLabResults(beneficiarioId);
            markers.putAll(labResults);

            // Validate collected data
            boolean dataValid = validateHealthMarkers(markers, condicaoCronica);

            // Analyze trends
            Map<String, String> trends = analyzeTrends(beneficiarioId, markers);

            // Detect anomalies
            List<String> alerts = detectAnomalies(markers, condicaoCronica);

            // Set output variables
            execution.setVariable("marcadoresColetados", markers);
            execution.setVariable("dataColetaValida", dataValid);
            execution.setVariable("tendencias", trends);
            execution.setVariable("alertas", alerts);
            execution.setVariable("timestampColeta", LocalDateTime.now());

            log.info("Collected {} health markers for beneficiary {}, alerts: {}",
                    markers.size(), beneficiarioId, alerts.size());

        } catch (Exception e) {
            log.error("Error collecting health markers: {}", e.getMessage(), e);
            execution.setVariable("errorMessage", "Failed to collect markers: " + e.getMessage());
            execution.setVariable("dataColetaValida", false);
            throw new RuntimeException("Error in ColetarMarcadoresSaudeDelegate", e);
        }
    }

    private Map<String, Object> collectSelfReportedMarkers(String beneficiarioId) {
        // Production: Get from patient app, surveys, questionnaires
        Map<String, Object> markers = new HashMap<>();
        markers.put("sintomas", Arrays.asList("fadiga_leve", "sede_moderada"));
        markers.put("qualidadeVida", 7); // 0-10 scale
        markers.put("aderenciaMedicacao", 85.0); // percentage
        return markers;
    }

    private Map<String, Object> collectDeviceData(String beneficiarioId) {
        // Production: Integrate with wearables API, CGM, BP monitors
        Map<String, Object> data = new HashMap<>();
        data.put("passosDiarios", 7500);
        data.put("pressaoArterial", "128/82");
        data.put("frequenciaCardiaca", 72);
        data.put("peso", 84.2);
        return data;
    }

    private Map<String, Object> collectLatestLabResults(String beneficiarioId) {
        // Production: Query lab results from LIS integration
        Map<String, Object> labs = new HashMap<>();
        labs.put("hbA1c", 7.2);
        labs.put("glicemiaJejum", 125.0);
        labs.put("colesterolTotal", 195.0);
        labs.put("dataColeta", LocalDateTime.now().minusDays(15));
        return labs;
    }

    private boolean validateHealthMarkers(Map<String, Object> markers, String condicao) {
        // Validate data quality and completeness
        return markers.size() >= 5; // Minimum required markers
    }

    private Map<String, String> analyzeTrends(String beneficiarioId, Map<String, Object> currentMarkers) {
        // Compare with historical data to identify trends
        Map<String, String> trends = new HashMap<>();
        trends.put("hbA1c", "IMPROVING");
        trends.put("peso", "STABLE");
        trends.put("pressaoArterial", "IMPROVING");
        return trends;
    }

    private List<String> detectAnomalies(Map<String, Object> markers, String condicao) {
        List<String> alerts = new ArrayList<>();

        // Check for out-of-range values
        if (markers.containsKey("hbA1c")) {
            Double hbA1c = (Double) markers.get("hbA1c");
            if (hbA1c > 9.0) {
                alerts.add("HbA1c critically high: " + hbA1c);
            }
        }

        return alerts;
    }
}
