package com.lifedrop.dto.response;

import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.InventoryStatus;
import java.time.LocalDateTime;

public class InventoryResponse {
    private Long id;
    private BloodGroup bloodGroup;
    private Integer availableUnits;
    private Integer reservedUnits;
    private Integer minimumRequiredUnits;
    private InventoryStatus status;
    private String demandBadge;
    private LocalDateTime lastUpdated;

    public InventoryResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getAvailableUnits() {
        return availableUnits;
    }

    public void setAvailableUnits(Integer availableUnits) {
        this.availableUnits = availableUnits;
    }

    public Integer getReservedUnits() {
        return reservedUnits;
    }

    public void setReservedUnits(Integer reservedUnits) {
        this.reservedUnits = reservedUnits;
    }

    public Integer getMinimumRequiredUnits() {
        return minimumRequiredUnits;
    }

    public void setMinimumRequiredUnits(Integer minimumRequiredUnits) {
        this.minimumRequiredUnits = minimumRequiredUnits;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public String getDemandBadge() {
        return demandBadge;
    }

    public void setDemandBadge(String demandBadge) {
        this.demandBadge = demandBadge;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
