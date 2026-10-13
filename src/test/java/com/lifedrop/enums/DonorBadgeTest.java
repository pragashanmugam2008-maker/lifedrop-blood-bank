package com.lifedrop.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DonorBadgeTest {

    @Test
    @DisplayName("Badge calculation follows exact donation count tiers")
    void testBadgeCalculations() {
        assertEquals(DonorBadge.NEW_DONOR, DonorBadge.calculateBadge(0));
        assertEquals(DonorBadge.NEW_DONOR, DonorBadge.calculateBadge(1));
        assertEquals(DonorBadge.NEW_DONOR, DonorBadge.calculateBadge(2));

        assertEquals(DonorBadge.REGULAR_DONOR, DonorBadge.calculateBadge(3));
        assertEquals(DonorBadge.REGULAR_DONOR, DonorBadge.calculateBadge(4));
        assertEquals(DonorBadge.REGULAR_DONOR, DonorBadge.calculateBadge(5));

        assertEquals(DonorBadge.LIFE_SAVER, DonorBadge.calculateBadge(6));
        assertEquals(DonorBadge.LIFE_SAVER, DonorBadge.calculateBadge(8));
        assertEquals(DonorBadge.LIFE_SAVER, DonorBadge.calculateBadge(10));

        assertEquals(DonorBadge.HERO_DONOR, DonorBadge.calculateBadge(11));
        assertEquals(DonorBadge.HERO_DONOR, DonorBadge.calculateBadge(25));
    }
}
