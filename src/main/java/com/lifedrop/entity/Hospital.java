package com.lifedrop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospitals", indexes = {
    @Index(name = "idx_hospital_user", columnList = "user_id", unique = true),
    @Index(name = "idx_hospital_city", columnList = "city")
})
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotBlank
    @Column(name = "hospital_name", nullable = false, length = 150)
    private String hospitalName;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @NotBlank
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "emergency_helpline", length = 30)
    private String emergencyHelpline;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "verified", nullable = false)
    private Boolean verified = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Hospital() {
    }

    public Hospital(User user, String hospitalName, String licenseNumber, String city, String address, String emergencyHelpline, String contactPerson) {
        this.user = user;
        this.hospitalName = hospitalName;
        this.licenseNumber = licenseNumber;
        this.city = city;
        this.address = address;
        this.emergencyHelpline = emergencyHelpline;
        this.contactPerson = contactPerson;
        this.verified = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.verified == null) this.verified = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyHelpline() {
        return emergencyHelpline;
    }

    public void setEmergencyHelpline(String emergencyHelpline) {
        this.emergencyHelpline = emergencyHelpline;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
