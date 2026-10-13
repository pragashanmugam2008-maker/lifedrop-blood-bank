package com.lifedrop.dto.response;

import java.time.LocalDate;

public class EligibilityResponse {
    private boolean eligible;
    private String reason;
    private LocalDate nextEligibleDate;
    private long daysRemaining;

    public EligibilityResponse() {
    }

    public EligibilityResponse(boolean eligible, String reason, LocalDate nextEligibleDate, long daysRemaining) {
        this.eligible = eligible;
        this.reason = reason;
        this.nextEligibleDate = nextEligibleDate;
        this.daysRemaining = daysRemaining;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getNextEligibleDate() {
        return nextEligibleDate;
    }

    public void setNextEligibleDate(LocalDate nextEligibleDate) {
        this.nextEligibleDate = nextEligibleDate;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }
}
