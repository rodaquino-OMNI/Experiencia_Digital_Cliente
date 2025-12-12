package com.healthplan.services.network;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

/**
 * Network Directory Service - Preferred provider network management.
 *
 * Directs beneficiaries to optimal providers within preferred network
 * based on specialization, location, availability, and quality metrics.
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (Navigation Intelligence)
 */
@Slf4j
@Service
public class NetworkDirectoryService {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private QualityMetricsService qualityMetricsService;

    /**
     * Directs beneficiary to optimal provider in preferred network.
     *
     * @param beneficiaryId Beneficiary identifier
     * @param specialization Required medical specialization
     * @param location Geographic preference
     * @return Recommended providers ranked by fit
     * @throws NetworkDirectionException if direction fails
     */
    public NetworkDirectionResult directToNetwork(
        String beneficiaryId,
        String specialization,
        Location location
    ) {
        log.info("Directing beneficiary {} to network for {}",
            beneficiaryId, specialization);

        try {
            // Get beneficiary plan details
            PlanDetails planDetails = getPlanDetails(beneficiaryId);

            // Find network providers
            List<Provider> networkProviders = providerRepository
                .findByNetworkAndSpecialization(
                    planDetails.getNetworkId(),
                    specialization
                );

            if (networkProviders.isEmpty()) {
                log.warn("No network providers found for specialization: {}", specialization);
                return handleNoProvidersFound(specialization, location);
            }

            // Score and rank providers
            List<ProviderMatch> rankedProviders = rankProviders(
                networkProviders,
                location,
                planDetails
            );

            // Build result
            NetworkDirectionResult result = NetworkDirectionResult.builder()
                .beneficiaryId(beneficiaryId)
                .specialization(specialization)
                .networkId(planDetails.getNetworkId())
                .networkName(planDetails.getNetworkName())
                .recommendedProviders(rankedProviders.subList(0,
                    Math.min(10, rankedProviders.size())))
                .totalProvidersFound(networkProviders.size())
                .searchLocation(location)
                .timestamp(LocalDateTime.now())
                .build();

            log.info("Network direction complete. Found {} providers",
                result.getTotalProvidersFound());

            return result;

        } catch (Exception e) {
            log.error("Error directing to network for beneficiary: {}",
                beneficiaryId, e);
            throw new NetworkDirectionException("Network direction failed", e);
        }
    }

    /**
     * Finds available appointment slots with network providers.
     *
     * @param providerId Provider identifier
     * @param startDate Search start date
     * @param endDate Search end date
     * @return Available appointment slots
     */
    public List<AppointmentSlot> findAvailableSlots(
        String providerId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        log.info("Finding available slots for provider: {} from {} to {}",
            providerId, startDate, endDate);

        try {
            Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new NetworkDirectionException("Provider not found"));

            // Query provider's calendar
            List<AppointmentSlot> slots = provider.getCalendarService()
                .getAvailableSlots(startDate, endDate);

            log.info("Found {} available slots", slots.size());
            return slots;

        } catch (Exception e) {
            log.error("Error finding slots for provider: {}", providerId, e);
            throw new NetworkDirectionException("Slot search failed", e);
        }
    }

    /**
     * Schedules appointment with network provider.
     *
     * @param beneficiaryId Beneficiary identifier
     * @param providerId Provider identifier
     * @param slotId Selected time slot
     * @param appointmentType Type of appointment
     * @return Scheduled appointment details
     */
    public ScheduledAppointment scheduleAppointment(
        String beneficiaryId,
        String providerId,
        String slotId,
        String appointmentType
    ) {
        log.info("Scheduling appointment for beneficiary: {} with provider: {}",
            beneficiaryId, providerId);

        try {
            // Validate provider is in network
            validateProviderInNetwork(beneficiaryId, providerId);

            // Reserve slot
            AppointmentSlot slot = reserveSlot(providerId, slotId);

            // Create appointment
            ScheduledAppointment appointment = ScheduledAppointment.builder()
                .appointmentId(UUID.randomUUID().toString())
                .beneficiaryId(beneficiaryId)
                .providerId(providerId)
                .providerName(getProviderName(providerId))
                .appointmentType(appointmentType)
                .scheduledTime(slot.getStartTime())
                .duration(slot.getDuration())
                .location(slot.getLocation())
                .status("CONFIRMED")
                .confirmationCode(generateConfirmationCode())
                .createdAt(LocalDateTime.now())
                .build();

            // Send confirmations
            sendAppointmentConfirmations(appointment);

            log.info("Appointment scheduled successfully: {}",
                appointment.getAppointmentId());

            return appointment;

        } catch (Exception e) {
            log.error("Error scheduling appointment for beneficiary: {}",
                beneficiaryId, e);
            throw new NetworkDirectionException("Appointment scheduling failed", e);
        }
    }

    /**
     * Gets detailed provider information.
     *
     * @param providerId Provider identifier
     * @return Detailed provider profile
     */
    public ProviderProfile getProviderDetails(String providerId) {
        log.info("Retrieving provider details: {}", providerId);

        try {
            Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new NetworkDirectionException("Provider not found"));

            // Enrich with quality metrics
            QualityMetrics metrics = qualityMetricsService.getMetrics(providerId);

            return ProviderProfile.builder()
                .providerId(provider.getId())
                .name(provider.getName())
                .specializations(provider.getSpecializations())
                .credentials(provider.getCredentials())
                .languages(provider.getLanguages())
                .locations(provider.getLocations())
                .acceptingNewPatients(provider.isAcceptingNewPatients())
                .qualityMetrics(metrics)
                .patientReviews(provider.getReviews())
                .insurancesAccepted(provider.getInsurancesAccepted())
                .build();

        } catch (Exception e) {
            log.error("Error retrieving provider details: {}", providerId, e);
            throw new NetworkDirectionException("Provider details retrieval failed", e);
        }
    }

    /**
     * Searches providers by multiple criteria.
     *
     * @param criteria Search criteria
     * @return Matching providers
     */
    public List<ProviderMatch> searchProviders(ProviderSearchCriteria criteria) {
        log.info("Searching providers with criteria: {}", criteria);

        try {
            List<Provider> providers = providerRepository.findByCriteria(criteria);

            return rankProviders(
                providers,
                criteria.getLocation(),
                criteria.getPlanDetails()
            );

        } catch (Exception e) {
            log.error("Error searching providers", e);
            throw new NetworkDirectionException("Provider search failed", e);
        }
    }

    // Private helper methods

    private List<ProviderMatch> rankProviders(
        List<Provider> providers,
        Location location,
        PlanDetails planDetails
    ) {
        List<ProviderMatch> matches = new ArrayList<>();

        for (Provider provider : providers) {
            double score = 0.0;
            List<String> reasons = new ArrayList<>();

            // Distance (30%)
            double distanceScore = calculateDistanceScore(provider, location);
            score += distanceScore * 0.3;
            if (distanceScore > 0.8) {
                reasons.add("Localização próxima");
            }

            // Quality metrics (25%)
            double qualityScore = calculateQualityScore(provider);
            score += qualityScore * 0.25;
            if (qualityScore > 0.8) {
                reasons.add("Alta qualidade");
            }

            // Availability (20%)
            double availScore = calculateAvailabilityScore(provider);
            score += availScore * 0.2;
            if (availScore > 0.7) {
                reasons.add("Boa disponibilidade");
            }

            // Patient satisfaction (15%)
            double satisfactionScore = calculateSatisfactionScore(provider);
            score += satisfactionScore * 0.15;
            if (satisfactionScore > 0.8) {
                reasons.add("Alta satisfação dos pacientes");
            }

            // Cost efficiency (10%)
            double costScore = calculateCostScore(provider, planDetails);
            score += costScore * 0.1;
            if (costScore > 0.8) {
                reasons.add("Custo-benefício");
            }

            matches.add(ProviderMatch.builder()
                .provider(provider)
                .score(score)
                .distance(calculateDistance(provider.getLocation(), location))
                .nextAvailableSlot(getNextAvailableSlot(provider))
                .matchReasons(reasons)
                .build());
        }

        // Sort by score descending
        matches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return matches;
    }

    private double calculateDistanceScore(Provider provider, Location location) {
        double distance = calculateDistance(provider.getLocation(), location);

        // Score inversely proportional to distance
        if (distance <= 5) return 1.0;
        if (distance <= 10) return 0.8;
        if (distance <= 20) return 0.6;
        if (distance <= 50) return 0.4;
        return 0.2;
    }

    private double calculateQualityScore(Provider provider) {
        QualityMetrics metrics = qualityMetricsService.getMetrics(provider.getId());
        return metrics.getOverallScore();
    }

    private double calculateAvailabilityScore(Provider provider) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoWeeks = now.plusWeeks(2);

        List<AppointmentSlot> slots = findAvailableSlots(
            provider.getId(), now, twoWeeks
        );

        // More slots = better score
        if (slots.size() >= 20) return 1.0;
        if (slots.size() >= 10) return 0.8;
        if (slots.size() >= 5) return 0.6;
        if (slots.size() >= 2) return 0.4;
        return slots.isEmpty() ? 0.0 : 0.2;
    }

    private double calculateSatisfactionScore(Provider provider) {
        List<Review> reviews = provider.getReviews();
        if (reviews.isEmpty()) return 0.5;

        double avgRating = reviews.stream()
            .mapToDouble(Review::getRating)
            .average()
            .orElse(3.0);

        return avgRating / 5.0; // Normalize to 0-1
    }

    private double calculateCostScore(Provider provider, PlanDetails planDetails) {
        // Lower cost = better score
        double avgCost = provider.getAverageCostPerVisit();
        double maxCost = planDetails.getMaxAllowedCost();

        return 1.0 - (avgCost / maxCost);
    }

    private double calculateDistance(Location loc1, Location loc2) {
        // Haversine formula for distance calculation
        return 10.0; // Simplified
    }

    private LocalDateTime getNextAvailableSlot(Provider provider) {
        List<AppointmentSlot> slots = findAvailableSlots(
            provider.getId(),
            LocalDateTime.now(),
            LocalDateTime.now().plusMonths(1)
        );

        return slots.isEmpty() ? null : slots.get(0).getStartTime();
    }

    private PlanDetails getPlanDetails(String beneficiaryId) {
        // Retrieve plan details from repository
        return new PlanDetails();
    }

    private NetworkDirectionResult handleNoProvidersFound(
        String specialization,
        Location location
    ) {
        log.warn("No providers found. Returning empty result.");
        return NetworkDirectionResult.builder()
            .specialization(specialization)
            .totalProvidersFound(0)
            .recommendedProviders(List.of())
            .timestamp(LocalDateTime.now())
            .build();
    }

    private void validateProviderInNetwork(String beneficiaryId, String providerId) {
        // Validation logic
    }

    private AppointmentSlot reserveSlot(String providerId, String slotId) {
        // Slot reservation logic
        return new AppointmentSlot();
    }

    private String getProviderName(String providerId) {
        return providerRepository.findById(providerId)
            .map(Provider::getName)
            .orElse("Unknown Provider");
    }

    private String generateConfirmationCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void sendAppointmentConfirmations(ScheduledAppointment appointment) {
        // Send SMS/email confirmations
        log.info("Sending confirmations for appointment: {}",
            appointment.getAppointmentId());
    }
}
