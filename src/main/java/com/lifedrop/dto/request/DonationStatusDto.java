package com.lifedrop.dto.request;

import com.lifedrop.enums.DonationStatus;
import jakarta.validation.constraints.NotNull;

public class DonationStatusDto {

    @NotNull(message = "Status is required")
    private DonationStatus status;

    public DonationStatusDto() {
    }

    public DonationStatusDto(DonationStatus status) {
        this.status = status;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }
}
