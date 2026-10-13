package com.lifedrop.dto.request;

import com.lifedrop.enums.DonationStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class DonorRequestResponseDto {

    @NotNull(message = "Response status is required (ACCEPTED or REJECTED)")
    private DonationStatus status;

    private LocalDate scheduledDate;

    private String scheduledTime;

    private String notes;

    public DonorRequestResponseDto() {
    }

    public DonorRequestResponseDto(DonationStatus status, LocalDate scheduledDate, String notes) {
        this.status = status;
        this.scheduledDate = scheduledDate;
        this.notes = notes;
    }

    public DonorRequestResponseDto(DonationStatus status, LocalDate scheduledDate, String scheduledTime, String notes) {
        this.status = status;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.notes = notes;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
