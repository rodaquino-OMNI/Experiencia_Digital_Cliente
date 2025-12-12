package com.healthplan.services.navigator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

/**
 * Care Navigator Service - Intelligent navigator assignment and coordination.
 *
 * Matches beneficiaries with optimal care navigators based on
 * specialization, language, workload, and performance metrics.
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (Navigation Layer)
 */
@Slf4j
@Service
public class CareNavigatorService {

    @Autowired
    private NavigatorRepository navigatorRepository;

    @Autowired
    private WorkloadBalancer workloadBalancer;

    /**
     * Assigns optimal care navigator to beneficiary.
     *
     * Uses intelligent matching algorithm considering:
     * - Navigator specialization vs beneficiary needs
     * - Language compatibility
     * - Current workload
     * - Performance metrics
     * - Geographic proximity
     *
     * @param beneficiaryId Beneficiary unique identifier
     * @param profileData Beneficiary profile for matching
     * @return Assigned navigator information
     * @throws NavigatorAssignmentException if assignment fails
     */
    public NavigatorAssignment assignNavigator(String beneficiaryId, BeneficiaryProfile profileData) {
        log.info("Assigning navigator for beneficiary: {}", beneficiaryId);

        try {
            // Get available navigators
            List<Navigator> availableNavigators = navigatorRepository
                .findActiveNavigators();

            if (availableNavigators.isEmpty()) {
                throw new NavigatorAssignmentException("No navigators available");
            }

            // Score and rank navigators
            List<NavigatorMatch> matches = scoreNavigators(availableNavigators, profileData);

            // Select best match
            NavigatorMatch bestMatch = matches.get(0);
            Navigator selectedNavigator = bestMatch.getNavigator();

            // Create assignment
            NavigatorAssignment assignment = NavigatorAssignment.builder()
                .beneficiaryId(beneficiaryId)
                .navigatorId(selectedNavigator.getId())
                .navigatorName(selectedNavigator.getName())
                .navigatorPhone(selectedNavigator.getPhone())
                .navigatorEmail(selectedNavigator.getEmail())
                .specializations(selectedNavigator.getSpecializations())
                .assignedAt(LocalDateTime.now())
                .matchScore(bestMatch.getScore())
                .matchReasons(bestMatch.getReasons())
                .build();

            // Update workload
            workloadBalancer.incrementWorkload(selectedNavigator.getId());

            log.info("Navigator assigned successfully. Navigator: {}, Score: {}",
                selectedNavigator.getName(), bestMatch.getScore());

            return assignment;

        } catch (Exception e) {
            log.error("Error assigning navigator for beneficiary: {}", beneficiaryId, e);
            throw new NavigatorAssignmentException("Navigator assignment failed", e);
        }
    }

    /**
     * Reassigns navigator when needed (escalation, unavailability, etc).
     *
     * @param beneficiaryId Beneficiary identifier
     * @param currentNavigatorId Current navigator
     * @param reason Reassignment reason
     * @return New navigator assignment
     */
    public NavigatorAssignment reassignNavigator(
        String beneficiaryId,
        String currentNavigatorId,
        String reason
    ) {
        log.info("Reassigning navigator for beneficiary: {}. Reason: {}",
            beneficiaryId, reason);

        try {
            // Get beneficiary profile
            BeneficiaryProfile profile = getBeneficiaryProfile(beneficiaryId);

            // Get available navigators (excluding current)
            List<Navigator> navigators = navigatorRepository
                .findActiveNavigatorsExcluding(currentNavigatorId);

            // Score and select
            List<NavigatorMatch> matches = scoreNavigators(navigators, profile);
            NavigatorMatch bestMatch = matches.get(0);

            // Create new assignment
            NavigatorAssignment newAssignment = assignNavigator(beneficiaryId, profile);
            newAssignment.setReassignmentReason(reason);
            newAssignment.setPreviousNavigatorId(currentNavigatorId);

            // Update workloads
            workloadBalancer.decrementWorkload(currentNavigatorId);
            workloadBalancer.incrementWorkload(newAssignment.getNavigatorId());

            log.info("Navigator reassigned successfully. New: {}",
                newAssignment.getNavigatorName());

            return newAssignment;

        } catch (Exception e) {
            log.error("Error reassigning navigator for beneficiary: {}",
                beneficiaryId, e);
            throw new NavigatorAssignmentException("Navigator reassignment failed", e);
        }
    }

    /**
     * Gets current workload distribution across navigators.
     *
     * @return Workload statistics
     */
    public WorkloadDistribution getWorkloadDistribution() {
        log.info("Retrieving navigator workload distribution");

        try {
            List<Navigator> navigators = navigatorRepository.findAllNavigators();

            Map<String, Integer> distribution = new HashMap<>();
            int totalCases = 0;

            for (Navigator nav : navigators) {
                int workload = workloadBalancer.getCurrentWorkload(nav.getId());
                distribution.put(nav.getName(), workload);
                totalCases += workload;
            }

            return WorkloadDistribution.builder()
                .distribution(distribution)
                .totalCases(totalCases)
                .averageCasesPerNavigator(navigators.isEmpty() ? 0 :
                    (double) totalCases / navigators.size())
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error retrieving workload distribution", e);
            throw new NavigatorAssignmentException("Failed to get workload distribution", e);
        }
    }

    /**
     * Balances workload across navigators.
     * Redistributes cases from overloaded to underloaded navigators.
     *
     * @return Rebalancing result
     */
    public RebalancingResult rebalanceWorkload() {
        log.info("Starting workload rebalancing");

        try {
            List<Navigator> navigators = navigatorRepository.findActiveNavigators();

            // Calculate average workload
            int totalWorkload = navigators.stream()
                .mapToInt(n -> workloadBalancer.getCurrentWorkload(n.getId()))
                .sum();
            double avgWorkload = (double) totalWorkload / navigators.size();

            // Identify overloaded and underloaded
            List<Navigator> overloaded = navigators.stream()
                .filter(n -> workloadBalancer.getCurrentWorkload(n.getId()) > avgWorkload * 1.3)
                .toList();

            List<Navigator> underloaded = navigators.stream()
                .filter(n -> workloadBalancer.getCurrentWorkload(n.getId()) < avgWorkload * 0.7)
                .toList();

            int reassignedCases = 0;

            // Reassign cases
            for (Navigator overNav : overloaded) {
                if (underloaded.isEmpty()) break;

                int excessCases = (int) (workloadBalancer.getCurrentWorkload(overNav.getId()) - avgWorkload);

                // Get cases to reassign
                List<String> casesToReassign = getCasesForReassignment(
                    overNav.getId(), excessCases
                );

                // Distribute to underloaded navigators
                for (String caseId : casesToReassign) {
                    if (underloaded.isEmpty()) break;

                    Navigator underNav = underloaded.get(0);
                    reassignNavigator(caseId, overNav.getId(), "WORKLOAD_BALANCING");
                    reassignedCases++;

                    // Update underloaded list
                    if (workloadBalancer.getCurrentWorkload(underNav.getId()) >= avgWorkload) {
                        underloaded.remove(0);
                    }
                }
            }

            log.info("Workload rebalancing complete. Reassigned {} cases", reassignedCases);

            return RebalancingResult.builder()
                .reassignedCases(reassignedCases)
                .previousAverage(avgWorkload)
                .newAverage(calculateCurrentAverage())
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error rebalancing workload", e);
            throw new NavigatorAssignmentException("Workload rebalancing failed", e);
        }
    }

    // Private helper methods

    private List<NavigatorMatch> scoreNavigators(
        List<Navigator> navigators,
        BeneficiaryProfile profile
    ) {
        List<NavigatorMatch> matches = new ArrayList<>();

        for (Navigator nav : navigators) {
            double score = 0.0;
            List<String> reasons = new ArrayList<>();

            // Specialization match (40%)
            double specScore = calculateSpecializationScore(nav, profile);
            score += specScore * 0.4;
            if (specScore > 0.7) {
                reasons.add("Especialização compatível");
            }

            // Language match (20%)
            double langScore = calculateLanguageScore(nav, profile);
            score += langScore * 0.2;
            if (langScore == 1.0) {
                reasons.add("Idioma compatível");
            }

            // Workload (20%)
            double workloadScore = calculateWorkloadScore(nav);
            score += workloadScore * 0.2;
            if (workloadScore > 0.7) {
                reasons.add("Disponibilidade adequada");
            }

            // Performance (15%)
            double perfScore = calculatePerformanceScore(nav);
            score += perfScore * 0.15;
            if (perfScore > 0.8) {
                reasons.add("Alto desempenho");
            }

            // Geographic proximity (5%)
            double geoScore = calculateGeographicScore(nav, profile);
            score += geoScore * 0.05;

            matches.add(NavigatorMatch.builder()
                .navigator(nav)
                .score(score)
                .reasons(reasons)
                .build());
        }

        // Sort by score descending
        matches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return matches;
    }

    private double calculateSpecializationScore(Navigator nav, BeneficiaryProfile profile) {
        Set<String> navSpec = new HashSet<>(nav.getSpecializations());
        Set<String> reqSpec = new HashSet<>(profile.getRequiredSpecializations());

        if (reqSpec.isEmpty()) return 0.5; // No special requirements

        Set<String> intersection = new HashSet<>(navSpec);
        intersection.retainAll(reqSpec);

        return (double) intersection.size() / reqSpec.size();
    }

    private double calculateLanguageScore(Navigator nav, BeneficiaryProfile profile) {
        return nav.getLanguages().contains(profile.getPreferredLanguage()) ? 1.0 : 0.3;
    }

    private double calculateWorkloadScore(Navigator nav) {
        int current = workloadBalancer.getCurrentWorkload(nav.getId());
        int max = nav.getMaxCaseload();

        double utilization = (double) current / max;

        // Best score at 50-70% utilization
        if (utilization >= 0.5 && utilization <= 0.7) return 1.0;
        if (utilization < 0.5) return 0.8;
        if (utilization <= 0.85) return 0.6;
        return 0.3; // Overloaded
    }

    private double calculatePerformanceScore(Navigator nav) {
        // Based on satisfaction ratings, resolution rate, etc.
        return nav.getPerformanceMetrics().getOverallScore();
    }

    private double calculateGeographicScore(Navigator nav, BeneficiaryProfile profile) {
        // Geographic proximity scoring
        return 0.5; // Simplified
    }

    private BeneficiaryProfile getBeneficiaryProfile(String beneficiaryId) {
        // Retrieve profile from repository
        return new BeneficiaryProfile();
    }

    private List<String> getCasesForReassignment(String navigatorId, int count) {
        // Get least critical cases for reassignment
        return new ArrayList<>();
    }

    private double calculateCurrentAverage() {
        List<Navigator> navigators = navigatorRepository.findActiveNavigators();
        int total = navigators.stream()
            .mapToInt(n -> workloadBalancer.getCurrentWorkload(n.getId()))
            .sum();
        return navigators.isEmpty() ? 0 : (double) total / navigators.size();
    }
}
