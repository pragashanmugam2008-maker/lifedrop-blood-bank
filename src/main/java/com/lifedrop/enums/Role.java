package com.lifedrop.enums;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    DONOR,
    HOSPITAL,
    ADMIN;

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }

    public static Role fromString(String value) {
        if (value == null) return DONOR;
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DONOR;
        }
    }
}
