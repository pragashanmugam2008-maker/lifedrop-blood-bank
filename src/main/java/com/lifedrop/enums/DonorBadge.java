package com.lifedrop.enums;

public enum DonorBadge {
    NEW_DONOR("New Donor", "🌱"),
    REGULAR_DONOR("Regular Donor", "⭐"),
    LIFE_SAVER("Life Saver", "🎖️"),
    HERO_DONOR("Hero Donor", "👑");

    private final String title;
    private final String icon;

    DonorBadge(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public static DonorBadge calculateBadge(int donationCount) {
        if (donationCount >= 11) {
            return HERO_DONOR;
        } else if (donationCount >= 6) {
            return LIFE_SAVER;
        } else if (donationCount >= 3) {
            return REGULAR_DONOR;
        } else {
            return NEW_DONOR;
        }
    }
}
