package com.lifedrop.dto.request;

import jakarta.validation.constraints.NotNull;

public class DonationDispatchDto {

    @NotNull(message = "Blood Request ID is required")
    private Long requestId;

    @NotNull(message = "Donor ID is required")
    private Long donorId;

    private String notes;

    public DonationDispatchDto() {
    }

    public DonationDispatchDto(Long requestId, Long donorId, String notes) {
        this.requestId = requestId;
        this.donorId = donorId;
        this.notes = notes;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getDonorId() {
        return donorId;
    }

    public void setDonorId(Long donorId) {
        this.donorId = donorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
