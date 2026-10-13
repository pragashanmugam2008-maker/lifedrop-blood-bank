package com.lifedrop.dto.request;

import com.lifedrop.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;

public class BloodRequestStatusDto {

    @NotNull(message = "Status is required")
    private RequestStatus status;

    public BloodRequestStatusDto() {
    }

    public BloodRequestStatusDto(RequestStatus status) {
        this.status = status;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
