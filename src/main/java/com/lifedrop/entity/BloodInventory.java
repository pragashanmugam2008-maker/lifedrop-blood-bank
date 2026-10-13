package com.lifedrop.entity;

import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.InventoryStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_inventory", indexes = {
    @Index(name = "idx_inventory_blood_group", columnList = "blood_group", unique = true)
})
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, unique = true, length = 20)
    private BloodGroup bloodGroup;

    @NotNull
    @Min(0)
    @Column(name = "available_units", nullable = false)
    private Integer availableUnits = 0;

    @NotNull
    @Min(0)
    @Column(name = "reserved_units", nullable = false)
    private Integer reservedUnits = 0;

    @NotNull
    @Min(0)
    @Column(name = "minimum_required_units", nullable = false)
    private Integer minimumRequiredUnits = 15;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    public BloodInventory() {
    }

    public BloodInventory(BloodGroup bloodGroup, Integer availableUnits, Integer minimumRequiredUnits) {
        this.bloodGroup = bloodGroup;
        this.availableUnits = availableUnits != null ? availableUnits : 0;
        this.reservedUnits = 0;
        this.minimumRequiredUnits = minimumRequiredUnits != null ? minimumRequiredUnits : 15;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
        if (this.availableUnits == null) this.availableUnits = 0;
        if (this.reservedUnits == null) this.reservedUnits = 0;
        if (this.minimumRequiredUnits == null) this.minimumRequiredUnits = 15;
    }

    public InventoryStatus calculateStatus() {
        int avail = this.availableUnits != null ? this.availableUnits : 0;
        if (avail < 5) {
            return InventoryStatus.CRITICAL;
        } else if (avail <= 15) {
            return InventoryStatus.LOW;
        } else if (avail <= 30) {
            return InventoryStatus.NORMAL;
        } else {
            return InventoryStatus.GOOD;
        }
    }

    public String calculateDemandBadge() {
        int avail = this.availableUnits != null ? this.availableUnits : 0;
        if (avail < 5) {
            return "CRITICAL NEED";
        } else if (avail <= 15) {
            return "HIGH DEMAND";
        } else if (avail <= 30) {
            return "MODERATE";
        } else {
            return "WELL STOCKED";
        }
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
