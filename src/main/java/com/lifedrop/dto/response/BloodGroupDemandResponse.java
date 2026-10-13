package com.lifedrop.dto.response;

import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.RequestUrgency;

public class BloodGroupDemandResponse {

    private BloodGroup bloodGroup;
    private String bloodGroupDisplay;
    private long activeRequestsCount;
    private long totalUnitsNeeded;
    private RequestUrgency highestUrgency;
    private String demandLevel;

    public BloodGroupDemandResponse() {
    }

    public BloodGroupDemandResponse(BloodGroup bloodGroup, long activeRequestsCount, long totalUnitsNeeded, RequestUrgency highestUrgency) {
        this.bloodGroup = bloodGroup;
        this.bloodGroupDisplay = bloodGroup != null ? bloodGroup.getDisplayValue() : "";
        this.activeRequestsCount = activeRequestsCount;
        this.totalUnitsNeeded = totalUnitsNeeded;
        this.highestUrgency = highestUrgency;
        if (highestUrgency == RequestUrgency.CRITICAL || totalUnitsNeeded >= 5) {
            this.demandLevel = "CRITICAL";
        } else if (highestUrgency == RequestUrgency.HIGH || totalUnitsNeeded >= 2) {
            this.demandLevel = "HIGH";
        } else {
            this.demandLevel = "MODERATE";
        }
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
        this.bloodGroupDisplay = bloodGroup != null ? bloodGroup.getDisplayValue() : "";
    }

    public String getBloodGroupDisplay() {
        return bloodGroupDisplay;
    }

    public void setBloodGroupDisplay(String bloodGroupDisplay) {
        this.bloodGroupDisplay = bloodGroupDisplay;
    }

    public long getActiveRequestsCount() {
        return activeRequestsCount;
    }

    public void setActiveRequestsCount(long activeRequestsCount) {
        this.activeRequestsCount = activeRequestsCount;
    }

    public long getTotalUnitsNeeded() {
        return totalUnitsNeeded;
    }

    public void setTotalUnitsNeeded(long totalUnitsNeeded) {
        this.totalUnitsNeeded = totalUnitsNeeded;
    }

    public RequestUrgency getHighestUrgency() {
        return highestUrgency;
    }

    public void setHighestUrgency(RequestUrgency highestUrgency) {
        this.highestUrgency = highestUrgency;
    }

    public long getUnitsNeeded() {
        return totalUnitsNeeded;
    }

    public long getActiveRequestCount() {
        return activeRequestsCount;
    }

    public String getUrgencyLevel() {
        return highestUrgency != null ? highestUrgency.name() : (demandLevel != null ? demandLevel : "HIGH");
    }
}
