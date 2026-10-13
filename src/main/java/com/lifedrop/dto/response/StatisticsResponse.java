package com.lifedrop.dto.response;

public class StatisticsResponse {
    private long registeredDonors;
    private long successfulDonations;
    private long livesHelped;
    private int emergencyResponsePercentage;
    private long availableDonors;
    private long activeBloodRequests;
    private long totalAvailableUnits;
    private long totalStarsAwarded;

    public StatisticsResponse() {
    }

    public StatisticsResponse(long registeredDonors, long successfulDonations, long livesHelped,
                              int emergencyResponsePercentage, long availableDonors, long activeBloodRequests,
                              long totalAvailableUnits, long totalStarsAwarded) {
        this.registeredDonors = registeredDonors;
        this.successfulDonations = successfulDonations;
        this.livesHelped = livesHelped;
        this.emergencyResponsePercentage = emergencyResponsePercentage;
        this.availableDonors = availableDonors;
        this.activeBloodRequests = activeBloodRequests;
        this.totalAvailableUnits = totalAvailableUnits;
        this.totalStarsAwarded = totalStarsAwarded;
    }

    public long getRegisteredDonors() {
        return registeredDonors;
    }

    public void setRegisteredDonors(long registeredDonors) {
        this.registeredDonors = registeredDonors;
    }

    public long getSuccessfulDonations() {
        return successfulDonations;
    }

    public void setSuccessfulDonations(long successfulDonations) {
        this.successfulDonations = successfulDonations;
    }

    public long getLivesHelped() {
        return livesHelped;
    }

    public void setLivesHelped(long livesHelped) {
        this.livesHelped = livesHelped;
    }

    public int getEmergencyResponsePercentage() {
        return emergencyResponsePercentage;
    }

    public void setEmergencyResponsePercentage(int emergencyResponsePercentage) {
        this.emergencyResponsePercentage = emergencyResponsePercentage;
    }

    public long getAvailableDonors() {
        return availableDonors;
    }

    public void setAvailableDonors(long availableDonors) {
        this.availableDonors = availableDonors;
    }

    public long getActiveBloodRequests() {
        return activeBloodRequests;
    }

    public void setActiveBloodRequests(long activeBloodRequests) {
        this.activeBloodRequests = activeBloodRequests;
    }

    public long getTotalAvailableUnits() {
        return totalAvailableUnits;
    }

    public void setTotalAvailableUnits(long totalAvailableUnits) {
        this.totalAvailableUnits = totalAvailableUnits;
    }

    public long getTotalStarsAwarded() {
        return totalStarsAwarded;
    }

    public void setTotalStarsAwarded(long totalStarsAwarded) {
        this.totalStarsAwarded = totalStarsAwarded;
    }
}
