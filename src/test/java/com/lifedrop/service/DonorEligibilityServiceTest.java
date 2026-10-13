package com.lifedrop.service;

import com.lifedrop.dto.response.EligibilityResponse;
import com.lifedrop.entity.Donor;
import com.lifedrop.enums.BloodGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DonorEligibilityServiceTest {

    private DonorEligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        eligibilityService = new DonorEligibilityService(90, 18, 65);
    }

    @Test
    @DisplayName("New donor with valid age and available status is eligible")
    void testNewDonorEligible() {
        Donor donor = new Donor();
        donor.setAge(25);
        donor.setAvailabilityStatus(true);
        donor.setLastDonationDate(null);
        donor.setBloodGroup(BloodGroup.O_POS);

        EligibilityResponse response = eligibilityService.checkEligibility(donor);
        assertTrue(response.isEligible());
        assertEquals(0, response.getDaysRemaining());
        assertNull(response.getNextEligibleDate());
    }

    @Test
    @DisplayName("Donor who donated 100 days ago (>90 days) is eligible")
    void testDonorPastIntervalEligible() {
        Donor donor = new Donor();
        donor.setAge(30);
        donor.setAvailabilityStatus(true);
        donor.setLastDonationDate(LocalDate.now().minusDays(100));

        EligibilityResponse response = eligibilityService.checkEligibility(donor);
        assertTrue(response.isEligible());
    }

    @Test
    @DisplayName("Donor who donated 30 days ago (<90 days) is ineligible with 60 days remaining")
    void testDonorRecentDonationIneligible() {
        Donor donor = new Donor();
        donor.setAge(30);
        donor.setAvailabilityStatus(true);
        donor.setLastDonationDate(LocalDate.now().minusDays(30));

        EligibilityResponse response = eligibilityService.checkEligibility(donor);
        assertFalse(response.isEligible());
        assertEquals(60, response.getDaysRemaining());
        assertNotNull(response.getNextEligibleDate());
    }

    @Test
    @DisplayName("Underage donor (<18) is ineligible")
    void testUnderageDonorIneligible() {
        Donor donor = new Donor();
        donor.setAge(17);
        donor.setAvailabilityStatus(true);

        EligibilityResponse response = eligibilityService.checkEligibility(donor);
        assertFalse(response.isEligible());
        assertTrue(response.getReason().contains("range"));
    }

    @Test
    @DisplayName("Overage donor (>65) is ineligible")
    void testOverageDonorIneligible() {
        Donor donor = new Donor();
        donor.setAge(68);
        donor.setAvailabilityStatus(true);

        EligibilityResponse response = eligibilityService.checkEligibility(donor);
        assertFalse(response.isEligible());
    }

    @Test
    @DisplayName("Unavailable donor is ineligible")
    void testUnavailableDonorIneligible() {
        Donor donor = new Donor();
        donor.setAge(28);
        donor.setAvailabilityStatus(false);

        EligibilityResponse response = eligibilityService.checkEligibility(donor);
        assertFalse(response.isEligible());
        assertTrue(response.getReason().contains("unavailable"));
    }
}
