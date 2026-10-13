package com.lifedrop.dto.response;

public class AdminDashboardStatsResponse {
    private long totalUsers;
    private long totalDonors;
    private long totalHospitals;
    private long totalDonations;
    private long successfulDonations;
    private long activeRequests;
    private long emergencyRequests;
    private long totalInventoryUnits;
    private long livesHelped;
    private long referralRewards;

    public AdminDashboardStatsResponse() {
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalDonors() {
        return totalDonors;
    }

    public void setTotalDonors(long totalDonors) {
        this.totalDonors = totalDonors;
    }

    public long getTotalHospitals() {
        return totalHospitals;
    }

    public void setTotalHospitals(long totalHospitals) {
        this.totalHospitals = totalHospitals;
    }

    public long getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(long totalDonations) {
        this.totalDonations = totalDonations;
    }

    public long getSuccessfulDonations() {
        return successfulDonations;
    }

    public void setSuccessfulDonations(long successfulDonations) {
        this.successfulDonations = successfulDonations;
    }

    public long getActiveRequests() {
        return activeRequests;
    }

    public void setActiveRequests(long activeRequests) {
        this.activeRequests = activeRequests;
    }

    public long getEmergencyRequests() {
        return emergencyRequests;
    }

    public void setEmergencyRequests(long emergencyRequests) {
        this.emergencyRequests = emergencyRequests;
    }

    public long getTotalInventoryUnits() {
        return totalInventoryUnits;
    }

    public void setTotalInventoryUnits(long totalInventoryUnits) {
        this.totalInventoryUnits = totalInventoryUnits;
    }

    public long getLivesHelped() {
        return livesHelped;
    }

    public void setLivesHelped(long livesHelped) {
        this.livesHelped = livesHelped;
    }

    public long getReferralRewards() {
        return referralRewards;
    }

    public void setReferralRewards(long referralRewards) {
        this.referralRewards = referralRewards;
    }
}
