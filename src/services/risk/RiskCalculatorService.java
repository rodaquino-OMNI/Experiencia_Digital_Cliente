package com.healthplan.services.risk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.LocalDate;
import java.time.Period;

/**
 * Risk Calculator Service - Clinical and financial risk scoring.
 *
 * Calculates risk scores based on demographics, medical history,
 * chronic conditions, and behavioral patterns.
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (Onboarding Intelligence)
 */
@Slf4j
@Service
public class RiskCalculatorService {

    // Risk weight constants
    private static final Map<String, Double> CHRONIC_CONDITION_WEIGHTS = Map.of(
        "DIABETES", 2.5,
        "HYPERTENSION", 2.0,
        "HEART_DISEASE", 3.0,
        "COPD", 2.8,
        "CANCER", 3.5,
        "KIDNEY_DISEASE", 3.2,
        "OBESITY", 1.8
    );

    private static final Map<String, Double> AGE_RISK_MULTIPLIERS = Map.of(
        "0-17", 0.8,
        "18-40", 1.0,
        "41-60", 1.5,
        "61-75", 2.2,
        "76+", 3.0
    );

    /**
     * Calculates comprehensive risk score for beneficiary.
     *
     * @param beneficiaryData Beneficiary profile data
     * @return Risk assessment result (0-100 scale)
     * @throws RiskCalculationException if calculation fails
     */
    public RiskScore calculateRiskScore(BeneficiaryProfile beneficiaryData) {
        log.info("Calculating risk score for beneficiary: {}", beneficiaryData.getId());

        try {
            // Component scores
            double demographicRisk = calculateDemographicRisk(beneficiaryData);
            double clinicalRisk = calculateClinicalRisk(beneficiaryData);
            double behavioralRisk = calculateBehavioralRisk(beneficiaryData);
            double historicalRisk = calculateHistoricalRisk(beneficiaryData);

            // Weighted combination
            double totalScore = (
                demographicRisk * 0.25 +
                clinicalRisk * 0.40 +
                behavioralRisk * 0.20 +
                historicalRisk * 0.15
            );

            // Normalize to 0-100 scale
            double normalizedScore = Math.min(100, Math.max(0, totalScore));

            RiskScore result = RiskScore.builder()
                .beneficiaryId(beneficiaryData.getId())
                .totalScore(normalizedScore)
                .riskLevel(determineRiskLevel(normalizedScore))
                .demographicComponent(demographicRisk)
                .clinicalComponent(clinicalRisk)
                .behavioralComponent(behavioralRisk)
                .historicalComponent(historicalRisk)
                .factors(identifyRiskFactors(beneficiaryData))
                .calculatedAt(java.time.LocalDateTime.now())
                .build();

            log.info("Risk score calculated: {} (Level: {})",
                normalizedScore, result.getRiskLevel());

            return result;

        } catch (Exception e) {
            log.error("Error calculating risk score for beneficiary: {}",
                beneficiaryData.getId(), e);
            throw new RiskCalculationException("Risk calculation failed", e);
        }
    }

    /**
     * Calculates demographic risk component.
     * Age, gender, geographic location factors.
     *
     * @param profile Beneficiary profile
     * @return Demographic risk score (0-100)
     */
    private double calculateDemographicRisk(BeneficiaryProfile profile) {
        double ageRisk = calculateAgeRisk(profile.getBirthDate());
        double genderRisk = calculateGenderRisk(profile.getGender());
        double locationRisk = calculateLocationRisk(profile.getZipCode());

        return (ageRisk * 0.6 + genderRisk * 0.2 + locationRisk * 0.2);
    }

    /**
     * Calculates clinical risk component.
     * Chronic conditions, medications, recent diagnoses.
     *
     * @param profile Beneficiary profile
     * @return Clinical risk score (0-100)
     */
    private double calculateClinicalRisk(BeneficiaryProfile profile) {
        double chronicConditionScore = 0.0;

        // Sum weighted chronic conditions
        for (String condition : profile.getChronicConditions()) {
            chronicConditionScore += CHRONIC_CONDITION_WEIGHTS.getOrDefault(
                condition.toUpperCase(), 1.0
            );
        }

        // Medication count indicator
        double medicationScore = Math.min(30, profile.getMedicationCount() * 3);

        // Recent hospitalizations
        double hospitalizationScore = profile.getHospitalizationsLastYear() * 15;

        // BMI risk
        double bmiScore = calculateBmiRisk(profile.getBmi());

        return Math.min(100,
            chronicConditionScore * 10 +
            medicationScore +
            hospitalizationScore +
            bmiScore
        );
    }

    /**
     * Calculates behavioral risk component.
     * Smoking, alcohol, exercise, diet patterns.
     *
     * @param profile Beneficiary profile
     * @return Behavioral risk score (0-100)
     */
    private double calculateBehavioralRisk(BeneficiaryProfile profile) {
        double risk = 0.0;

        // Smoking
        if (profile.isSmoker()) {
            risk += 30;
        } else if (profile.isFormerSmoker()) {
            risk += 10;
        }

        // Alcohol consumption
        if (profile.getAlcoholConsumption() != null) {
            switch (profile.getAlcoholConsumption()) {
                case "HEAVY" -> risk += 25;
                case "MODERATE" -> risk += 10;
                case "LIGHT" -> risk += 5;
            }
        }

        // Physical activity
        if (profile.getWeeklyExerciseHours() == null || profile.getWeeklyExerciseHours() < 2) {
            risk += 20;
        }

        // Diet quality
        if (profile.getDietQuality() != null) {
            switch (profile.getDietQuality()) {
                case "POOR" -> risk += 15;
                case "FAIR" -> risk += 8;
            }
        }

        return Math.min(100, risk);
    }

    /**
     * Calculates historical risk component.
     * Claims history, ER visits, preventive care compliance.
     *
     * @param profile Beneficiary profile
     * @return Historical risk score (0-100)
     */
    private double calculateHistoricalRisk(BeneficiaryProfile profile) {
        double risk = 0.0;

        // ER visit frequency
        risk += Math.min(40, profile.getErVisitsLastYear() * 10);

        // Claims frequency and cost
        risk += Math.min(30, profile.getClaimsLastYear() * 2);

        // Preventive care gap (inverse indicator)
        if (!profile.isPreventiveCareUpToDate()) {
            risk += 20;
        }

        // Appointment no-show rate
        if (profile.getNoShowRate() > 0.2) {
            risk += 10;
        }

        return Math.min(100, risk);
    }

    /**
     * Calculates age-based risk.
     *
     * @param birthDate Date of birth
     * @return Age risk score
     */
    private double calculateAgeRisk(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();

        String ageGroup = switch (age) {
            case int a when a <= 17 -> "0-17";
            case int a when a <= 40 -> "18-40";
            case int a when a <= 60 -> "41-60";
            case int a when a <= 75 -> "61-75";
            default -> "76+";
        };

        double multiplier = AGE_RISK_MULTIPLIERS.get(ageGroup);
        return multiplier * 20; // Base score of 20
    }

    /**
     * Calculates gender-based risk.
     *
     * @param gender Gender identifier
     * @return Gender risk score
     */
    private double calculateGenderRisk(String gender) {
        // Simplified - in reality would consider age-gender interactions
        return switch (gender.toUpperCase()) {
            case "FEMALE" -> 15; // Higher healthcare utilization
            case "MALE" -> 12;
            default -> 10;
        };
    }

    /**
     * Calculates location-based risk.
     *
     * @param zipCode Geographic identifier
     * @return Location risk score
     */
    private double calculateLocationRisk(String zipCode) {
        // Would integrate with geographic health data
        // For now, simplified
        return 10.0;
    }

    /**
     * Calculates BMI-based risk.
     *
     * @param bmi Body Mass Index
     * @return BMI risk score
     */
    private double calculateBmiRisk(Double bmi) {
        if (bmi == null) return 5; // Unknown

        if (bmi < 18.5) return 15; // Underweight
        if (bmi < 25) return 0; // Normal
        if (bmi < 30) return 10; // Overweight
        if (bmi < 35) return 20; // Obese Class I
        if (bmi < 40) return 30; // Obese Class II
        return 40; // Obese Class III
    }

    /**
     * Determines risk level category.
     *
     * @param score Calculated risk score
     * @return Risk level classification
     */
    private String determineRiskLevel(double score) {
        if (score >= 75) return "CRITICAL";
        if (score >= 50) return "HIGH";
        if (score >= 25) return "MEDIUM";
        return "LOW";
    }

    /**
     * Identifies specific risk factors.
     *
     * @param profile Beneficiary profile
     * @return List of identified risk factors
     */
    private List<RiskFactor> identifyRiskFactors(BeneficiaryProfile profile) {
        List<RiskFactor> factors = new ArrayList<>();

        // Age factor
        int age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
        if (age > 65) {
            factors.add(new RiskFactor("AGE", "Idade avançada (65+)", "HIGH"));
        }

        // Chronic conditions
        for (String condition : profile.getChronicConditions()) {
            factors.add(new RiskFactor(
                "CHRONIC_CONDITION",
                "Condição crônica: " + condition,
                "HIGH"
            ));
        }

        // Behavioral factors
        if (profile.isSmoker()) {
            factors.add(new RiskFactor("SMOKING", "Tabagismo ativo", "HIGH"));
        }

        if (profile.getBmi() != null && profile.getBmi() >= 30) {
            factors.add(new RiskFactor("OBESITY", "Obesidade", "MEDIUM"));
        }

        // Healthcare utilization
        if (profile.getErVisitsLastYear() >= 3) {
            factors.add(new RiskFactor(
                "HIGH_ER_USE",
                "Alto uso de emergência",
                "MEDIUM"
            ));
        }

        return factors;
    }
}
