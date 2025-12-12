package br.com.austa.experiencia.service.domain.proativo.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delegate responsible for collecting updated data from multiple sources for beneficiaries.
 *
 * <p>SUB-002 Motor Proativo - Step 2: Collect Updated Data</p>
 *
 * <p>Functionality:</p>
 * <ul>
 *   <li>Collects data from multiple sources (EHR, claims, lab results, pharmacy)</li>
 *   <li>Aggregates beneficiary health information</li>
 *   <li>Identifies data gaps and missing information</li>
 *   <li>Enriches beneficiary profiles with latest data</li>
 * </ul>
 *
 * <p>Input Variables:</p>
 * <ul>
 *   <li><b>beneficiariosAtivos</b> (List): List of beneficiary IDs to process</li>
 *   <li><b>dataSources</b> (List): Sources to collect data from</li>
 *   <li><b>dataTypes</b> (List): Types of data to collect</li>
 * </ul>
 *
 * <p>Output Variables:</p>
 * <ul>
 *   <li><b>dadosColetados</b> (Map): Collected data by beneficiary ID</li>
 *   <li><b>dataGaps</b> (List): Identified data gaps</li>
 *   <li><b>collectionTimestamp</b> (LocalDateTime): Collection timestamp</li>
 *   <li><b>successRate</b> (Double): Percentage of successful data collections</li>
 * </ul>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("coletarDadosAtualizadosDelegate")
public class ColetarDadosAtualizadosDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting data collection from multiple sources - Process: {}, Execution: {}",
                execution.getProcessInstanceId(), execution.getId());

        try {
            // Extract input parameters
            @SuppressWarnings("unchecked")
            List<String> beneficiariosAtivos = (List<String>) execution.getVariable("beneficiariosAtivos");

            @SuppressWarnings("unchecked")
            List<String> dataSources = (List<String>) execution.getVariable("dataSources");
            if (dataSources == null) {
                dataSources = List.of("EHR", "CLAIMS", "LAB", "PHARMACY");
            }

            @SuppressWarnings("unchecked")
            List<String> dataTypes = (List<String>) execution.getVariable("dataTypes");
            if (dataTypes == null) {
                dataTypes = List.of("VITALS", "MEDICATIONS", "DIAGNOSES", "PROCEDURES");
            }

            log.debug("Collecting data for {} beneficiaries from sources: {}, types: {}",
                    beneficiariosAtivos != null ? beneficiariosAtivos.size() : 0, dataSources, dataTypes);

            // Collect data from sources
            Map<String, Map<String, Object>> dadosColetados = collectDataFromSources(
                    beneficiariosAtivos, dataSources, dataTypes);

            // Identify data gaps
            List<String> dataGaps = identifyDataGaps(dadosColetados, dataTypes);

            // Calculate success rate
            int totalExpected = beneficiariosAtivos != null ? beneficiariosAtivos.size() * dataTypes.size() : 0;
            int totalCollected = dadosColetados.values().stream()
                    .mapToInt(data -> data.size())
                    .sum();
            double successRate = totalExpected > 0 ? (totalCollected * 100.0 / totalExpected) : 0.0;

            // Set output variables
            execution.setVariable("dadosColetados", dadosColetados);
            execution.setVariable("dataGaps", dataGaps);
            execution.setVariable("collectionTimestamp", LocalDateTime.now());
            execution.setVariable("successRate", successRate);

            log.info("Successfully collected data for {} beneficiaries with {:.2f}% success rate",
                    dadosColetados.size(), successRate);

        } catch (Exception e) {
            log.error("Error collecting updated data - Process: {}, Error: {}",
                    execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("errorMessage", "Failed to collect updated data: " + e.getMessage());
            execution.setVariable("hasError", true);

            throw new RuntimeException("Error in ColetarDadosAtualizadosDelegate", e);
        }
    }

    /**
     * Collects data from multiple sources for beneficiaries.
     */
    private Map<String, Map<String, Object>> collectDataFromSources(
            List<String> beneficiaries, List<String> sources, List<String> types) {

        Map<String, Map<String, Object>> collectedData = new HashMap<>();

        if (beneficiaries == null || beneficiaries.isEmpty()) {
            return collectedData;
        }

        for (String beneficiaryId : beneficiaries) {
            Map<String, Object> beneficiaryData = new HashMap<>();

            // Simulate data collection from each source
            for (String source : sources) {
                for (String type : types) {
                    // In production, this would call actual data source APIs
                    Object data = simulateDataCollection(beneficiaryId, source, type);
                    if (data != null) {
                        beneficiaryData.put(source + "_" + type, data);
                    }
                }
            }

            collectedData.put(beneficiaryId, beneficiaryData);
        }

        return collectedData;
    }

    /**
     * Simulates data collection from a source.
     */
    private Object simulateDataCollection(String beneficiaryId, String source, String type) {
        // Simulate 90% success rate
        if (Math.random() < 0.9) {
            return Map.of(
                "beneficiaryId", beneficiaryId,
                "source", source,
                "type", type,
                "timestamp", LocalDateTime.now(),
                "data", "Sample data for " + type
            );
        }
        return null;
    }

    /**
     * Identifies gaps in collected data.
     */
    private List<String> identifyDataGaps(Map<String, Map<String, Object>> collectedData, List<String> expectedTypes) {
        // In production, this would identify missing or incomplete data
        return List.of(); // Placeholder
    }
}
