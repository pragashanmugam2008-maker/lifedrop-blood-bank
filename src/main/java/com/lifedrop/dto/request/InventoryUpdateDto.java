package com.lifedrop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class InventoryUpdateDto {

    @NotNull(message = "Available units is required")
    @Min(value = 0, message = "Available units cannot be negative")
    private Integer availableUnits;

    @Min(value = 0, message = "Reserved units cannot be negative")
    private Integer reservedUnits = 0;

    @Min(value = 0, message = "Minimum required units cannot be negative")
    private Integer minimumRequiredUnits = 15;

    public InventoryUpdateDto() {
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
}
