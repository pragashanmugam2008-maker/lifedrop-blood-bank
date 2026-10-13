package com.lifedrop.dto.request;

import jakarta.validation.constraints.NotNull;

public class AvailabilityUpdateDto {

    @NotNull(message = "Availability status is required")
    private Boolean available;

    public AvailabilityUpdateDto() {
    }

    public AvailabilityUpdateDto(Boolean available) {
        this.available = available;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
