package com.lifedrop.service;

import com.lifedrop.enums.BloodGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BloodCompatibilityServiceTest {

    private BloodCompatibilityService compatibilityService;

    @BeforeEach
    void setUp() {
        compatibilityService = new BloodCompatibilityService();
    }

    @Test
    @DisplayName("O- is universal donor (can donate to all 8 blood groups)")
    void testUniversalDonorONeg() {
        for (BloodGroup recipient : BloodGroup.values()) {
            assertTrue(compatibilityService.canDonateTo(BloodGroup.O_NEG, recipient),
                    "O- must be able to donate to " + recipient.getDisplayValue());
        }
    }

    @Test
    @DisplayName("AB+ is universal recipient (can receive from all 8 blood groups, but donates ONLY to AB+)")
    void testUniversalRecipientABPos() {
        // Can receive from all
        List<BloodGroup> donorsForABPos = compatibilityService.getCompatibleDonorsFor(BloodGroup.AB_POS);
        assertEquals(8, donorsForABPos.size(), "AB+ should receive from all 8 blood groups");

        // Can donate ONLY to AB+
        assertTrue(compatibilityService.canDonateTo(BloodGroup.AB_POS, BloodGroup.AB_POS));
        for (BloodGroup recipient : BloodGroup.values()) {
            if (recipient != BloodGroup.AB_POS) {
                assertFalse(compatibilityService.canDonateTo(BloodGroup.AB_POS, recipient),
                        "AB+ should NOT be able to donate to " + recipient.getDisplayValue());
            }
        }
    }

    @Test
    @DisplayName("O+ can donate to O+, A+, B+, AB+")
    void testOPosCompatibility() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.O_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.A_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.B_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.AB_POS));

        assertFalse(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.O_NEG));
        assertFalse(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.A_NEG));
        assertFalse(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.B_NEG));
        assertFalse(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.AB_NEG));
    }

    @Test
    @DisplayName("A+ can donate to A+, AB+")
    void testAPosCompatibility() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.A_POS, BloodGroup.A_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.A_POS, BloodGroup.AB_POS));

        assertFalse(compatibilityService.canDonateTo(BloodGroup.A_POS, BloodGroup.B_POS));
        assertFalse(compatibilityService.canDonateTo(BloodGroup.A_POS, BloodGroup.O_POS));
    }

    @Test
    @DisplayName("B+ can donate to B+, AB+")
    void testBPosCompatibility() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.B_POS, BloodGroup.B_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.B_POS, BloodGroup.AB_POS));

        assertFalse(compatibilityService.canDonateTo(BloodGroup.B_POS, BloodGroup.A_POS));
        assertFalse(compatibilityService.canDonateTo(BloodGroup.B_POS, BloodGroup.O_POS));
    }

    @Test
    @DisplayName("A- can donate to A-, A+, AB-, AB+")
    void testANegCompatibility() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.A_NEG, BloodGroup.A_NEG));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.A_NEG, BloodGroup.A_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.A_NEG, BloodGroup.AB_NEG));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.A_NEG, BloodGroup.AB_POS));

        assertFalse(compatibilityService.canDonateTo(BloodGroup.A_NEG, BloodGroup.B_POS));
        assertFalse(compatibilityService.canDonateTo(BloodGroup.A_NEG, BloodGroup.O_POS));
    }

    @Test
    @DisplayName("B- can donate to B-, B+, AB-, AB+")
    void testBNegCompatibility() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.B_NEG, BloodGroup.B_NEG));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.B_NEG, BloodGroup.B_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.B_NEG, BloodGroup.AB_NEG));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.B_NEG, BloodGroup.AB_POS));

        assertFalse(compatibilityService.canDonateTo(BloodGroup.B_NEG, BloodGroup.A_POS));
        assertFalse(compatibilityService.canDonateTo(BloodGroup.B_NEG, BloodGroup.O_POS));
    }

    @Test
    @DisplayName("AB- can donate to AB-, AB+")
    void testABNegCompatibility() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.AB_NEG, BloodGroup.AB_NEG));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.AB_NEG, BloodGroup.AB_POS));

        assertFalse(compatibilityService.canDonateTo(BloodGroup.AB_NEG, BloodGroup.A_POS));
        assertFalse(compatibilityService.canDonateTo(BloodGroup.AB_NEG, BloodGroup.B_POS));
    }

    @Test
    @DisplayName("Validation Case 1: O+ donor -> B+ request -> compatible")
    void testValidationCase1_OPosToBPos_Compatible() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.B_POS),
                "O+ donor red blood cells MUST be compatible with B+ request");
        assertTrue(compatibilityService.getCompatibleRecipientsFor(BloodGroup.O_POS).contains(BloodGroup.B_POS));
        assertTrue(compatibilityService.getCompatibleDonorsFor(BloodGroup.B_POS).contains(BloodGroup.O_POS));
    }

    @Test
    @DisplayName("Validation Case 2: B+ donor -> O+ request -> incompatible")
    void testValidationCase2_BPosToOPos_Incompatible() {
        assertFalse(compatibilityService.canDonateTo(BloodGroup.B_POS, BloodGroup.O_POS),
                "B+ donor blood must NOT be compatible with O+ request");
        assertFalse(compatibilityService.getCompatibleRecipientsFor(BloodGroup.B_POS).contains(BloodGroup.O_POS));
        assertFalse(compatibilityService.getCompatibleDonorsFor(BloodGroup.O_POS).contains(BloodGroup.B_POS));
    }

    @Test
    @DisplayName("Validation Case 3: A+ donor -> AB+ request -> compatible")
    void testValidationCase3_APosToABPos_Compatible() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.A_POS, BloodGroup.AB_POS),
                "A+ donor blood MUST be compatible with AB+ request");
        assertTrue(compatibilityService.getCompatibleRecipientsFor(BloodGroup.A_POS).contains(BloodGroup.AB_POS));
        assertTrue(compatibilityService.getCompatibleDonorsFor(BloodGroup.AB_POS).contains(BloodGroup.A_POS));
    }

    @Test
    @DisplayName("Validation Case 4: AB+ donor -> A+ request -> incompatible")
    void testValidationCase4_ABPosToAPos_Incompatible() {
        assertFalse(compatibilityService.canDonateTo(BloodGroup.AB_POS, BloodGroup.A_POS),
                "AB+ donor blood must NOT be compatible with A+ request");
        assertFalse(compatibilityService.getCompatibleRecipientsFor(BloodGroup.AB_POS).contains(BloodGroup.A_POS));
        assertFalse(compatibilityService.getCompatibleDonorsFor(BloodGroup.A_POS).contains(BloodGroup.AB_POS));
    }

    @Test
    @DisplayName("Validation Case 5: O- donor -> AB+ request -> compatible")
    void testValidationCase5_ONegToABPos_Compatible() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_NEG, BloodGroup.AB_POS),
                "O- donor blood MUST be compatible with AB+ request");
        assertTrue(compatibilityService.getCompatibleRecipientsFor(BloodGroup.O_NEG).contains(BloodGroup.AB_POS));
        assertTrue(compatibilityService.getCompatibleDonorsFor(BloodGroup.AB_POS).contains(BloodGroup.O_NEG));
    }

    @Test
    @DisplayName("Validation Case 6: AB+ donor -> AB+ request -> compatible")
    void testValidationCase6_ABPosToABPos_Compatible() {
        assertTrue(compatibilityService.canDonateTo(BloodGroup.AB_POS, BloodGroup.AB_POS),
                "AB+ donor blood MUST be compatible with AB+ request");
        assertTrue(compatibilityService.getCompatibleRecipientsFor(BloodGroup.AB_POS).contains(BloodGroup.AB_POS));
        assertTrue(compatibilityService.getCompatibleDonorsFor(BloodGroup.AB_POS).contains(BloodGroup.AB_POS));
    }
}
