package com.lifedrop.entity;

import com.lifedrop.enums.DonationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations", indexes = {
    @Index(name = "idx_donations_donor", columnList = "donor_id"),
    @Index(name = "idx_donations_hospital", columnList = "hospital_id"),
    @Index(name = "idx_donations_status", columnList = "status"),
    @Index(name = "idx_donations_date", columnList = "donation_date")
})
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id")
    private BloodRequest request;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @NotNull
    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @NotNull
    @Min(1)
    @Column(name = "units", nullable = false)
    private Integer units = 1;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DonationStatus status = DonationStatus.COMPLETED;

    @Column(name = "notes", length = 255)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Donation() {
    }

    public Donation(Donor donor, BloodRequest request, Hospital hospital, LocalDate donationDate, Integer units, DonationStatus status, String notes) {
        this.donor = donor;
        this.request = request;
        this.hospital = hospital;
        this.donationDate = donationDate != null ? donationDate : LocalDate.now();
        this.units = units != null ? units : 1;
        this.status = status != null ? status : DonationStatus.COMPLETED;
        this.notes = notes;
    }

    public Donation(Donor donor, BloodRequest request, LocalDate donationDate, Integer units, DonationStatus status, String notes) {
        this(donor, request, request != null ? request.getHospital() : null, donationDate, units, status, notes);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.donationDate == null) this.donationDate = LocalDate.now();
        if (this.units == null) this.units = 1;
        if (this.status == null) this.status = DonationStatus.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Donor getDonor() {
        return donor;
    }

    public void setDonor(Donor donor) {
        this.donor = donor;
    }

    public BloodRequest getRequest() {
        return request;
    }

    public void setRequest(BloodRequest request) {
        this.request = request;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public Integer getUnits() {
        return units;
    }

    public void setUnits(Integer units) {
        this.units = units;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
