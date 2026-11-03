package com.lifedrop.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifedrop.enums.DonationStatus;
import com.lifedrop.enums.DonorResponseStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class DonorRequestResponseDto {

    @NotNull(message = "Response status is required (ACCEPTED or REJECTED)")
    private DonorResponseStatus status;

    private LocalDate scheduledDate;

    private String scheduledTime;

    private String notes;

    public DonorRequestResponseDto() {
    }

    public DonorRequestResponseDto(DonorResponseStatus status, LocalDate scheduledDate, String notes) {
        this.status = status;
        this.scheduledDate = scheduledDate;
        this.notes = notes;
    }

    public DonorRequestResponseDto(DonationStatus donationStatus, LocalDate scheduledDate, String notes) {
        this.status = donationStatus != null ? DonorResponseStatus.valueOf(donationStatus.name()) : null;
        this.scheduledDate = scheduledDate;
        this.notes = notes;
    }

    public DonorRequestResponseDto(DonorResponseStatus status, LocalDate scheduledDate, String scheduledTime, String notes) {
        this.status = status;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.notes = notes;
    }

    public DonorResponseStatus getStatus() {
        return status;
    }

    public void setStatus(DonorResponseStatus status) {
        this.status = status;
    }

    public void setStatus(DonationStatus donationStatus) {
        this.status = donationStatus != null ? DonorResponseStatus.valueOf(donationStatus.name()) : null;
    }

    @JsonSetter("status")
    public void setStatusFromString(String statusStr) {
        if (statusStr != null) {
            try {
                this.status = DonorResponseStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (Exception e) {
                this.status = null;
            }
        }
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
