package com.lifedrop.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum BloodGroup {
    O_POS("O+"),
    O_NEG("O-"),
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-");

    private final String displayValue;

    BloodGroup(String displayValue) {
        this.displayValue = displayValue;
    }

    @JsonValue
    public String getDisplayValue() {
        return displayValue;
    }

    @JsonCreator
    public static BloodGroup fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalized = text.trim().toUpperCase().replace(" ", "");
        for (BloodGroup bg : BloodGroup.values()) {
            if (bg.name().equalsIgnoreCase(normalized) ||
                bg.displayValue.equalsIgnoreCase(normalized) ||
                bg.name().replace("_POS", "+").replace("_NEG", "-").equalsIgnoreCase(normalized) ||
                (normalized.equals("O+") && bg == O_POS) ||
                (normalized.equals("O-") && bg == O_NEG) ||
                (normalized.equals("A+") && bg == A_POS) ||
                (normalized.equals("A-") && bg == A_NEG) ||
                (normalized.equals("B+") && bg == B_POS) ||
                (normalized.equals("B-") && bg == B_NEG) ||
                (normalized.equals("AB+") && bg == AB_POS) ||
                (normalized.equals("AB-") && bg == AB_NEG) ||
                (normalized.equals("OPOS") && bg == O_POS) ||
                (normalized.equals("ONEG") && bg == O_NEG) ||
                (normalized.equals("APOS") && bg == A_POS) ||
                (normalized.equals("ANEG") && bg == A_NEG) ||
                (normalized.equals("BPOS") && bg == B_POS) ||
                (normalized.equals("BNEG") && bg == B_NEG) ||
                (normalized.equals("ABPOS") && bg == AB_POS) ||
                (normalized.equals("ABNEG") && bg == AB_NEG)) {
                return bg;
            }
        }
        throw new IllegalArgumentException("Unknown blood group: " + text + ". Supported values: " +
                Arrays.toString(Arrays.stream(BloodGroup.values()).map(BloodGroup::getDisplayValue).toArray()));
    }

    @Override
    public String toString() {
        return displayValue;
    }
}
