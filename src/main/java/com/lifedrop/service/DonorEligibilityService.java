package com.lifedrop.service;

import com.lifedrop.dto.response.EligibilityResponse;
import com.lifedrop.entity.Donor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class DonorEligibilityService {

    private final int minimumDaysBetweenDonations;
    private final int minimumAge;
    private final int maximumAge;

    public DonorEligibilityService(
            @Value("${lifedrop.donation.minimum-days-between-donations:90}") int minimumDaysBetweenDonations,
            @Value("${lifedrop.donor.minimum-age:18}") int minimumAge,
            @Value("${lifedrop.donor.maximum-age:65}") int maximumAge) {
        this.minimumDaysBetweenDonations = minimumDaysBetweenDonations;
        this.minimumAge = minimumAge;
        this.maximumAge = maximumAge;
    }

    /**
     * Calculates donor eligibility based on project rules.
     * Note: These are application project rules and not formal medical advice.
     */
    public EligibilityResponse checkEligibility(Donor donor) {
        if (donor == null) {
            return new EligibilityResponse(false, "Donor record not found", null, 0);
        }

        // 1. Age verification
        if (donor.getAge() == null || donor.getAge() < minimumAge || donor.getAge() > maximumAge) {
            return new EligibilityResponse(
                    false,
                    String.format("Donor age (%d) is outside the eligible range (%d-%d years)",
                            donor.getAge() != null ? donor.getAge() : 0, minimumAge, maximumAge),
                    null,
                    0
            );
        }

        // 2. Availability verification
        if (donor.getAvailabilityStatus() == null || !donor.getAvailabilityStatus()) {
            return new EligibilityResponse(
                    false,
                    "Donor is currently marked as unavailable for donations",
                    null,
                    0
            );
        }

        // 3. Donation interval check (minimum days between donations)
        LocalDate lastDonation = donor.getLastDonationDate();
        if (lastDonation != null) {
            LocalDate nextEligible = lastDonation.plusDays(minimumDaysBetweenDonations);
            LocalDate today = LocalDate.now();

            if (today.isBefore(nextEligible)) {
                long daysRemaining = ChronoUnit.DAYS.between(today, nextEligible);
                return new EligibilityResponse(
                        false,
                        String.format("Minimum donation interval of %d days has not passed since last donation (%s)",
                                minimumDaysBetweenDonations, lastDonation),
                        nextEligible,
                        daysRemaining
                );
            }
        }

        // 4. Eligible
        return new EligibilityResponse(
                true,
                "Donor is eligible to donate blood",
                null,
                0
        );
    }

    public boolean isEligible(Donor donor) {
        return checkEligibility(donor).isEligible();
    }
}
