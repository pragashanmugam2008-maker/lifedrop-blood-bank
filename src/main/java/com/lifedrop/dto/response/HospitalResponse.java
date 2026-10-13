package com.lifedrop.dto.response;

import java.time.LocalDateTime;

public class HospitalResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String hospitalName;
    private String licenseNumber;
    private String city;
    private String address;
    private String emergencyHelpline;
    private String contactPerson;
    private Boolean verified;
    private long activeRequestsCount;
    private long fulfilledRequestsCount;
    private LocalDateTime createdAt;

    public HospitalResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public long getActiveRequestsCount() {
        return activeRequestsCount;
    }

    public void setActiveRequestsCount(long activeRequestsCount) {
        this.activeRequestsCount = activeRequestsCount;
    }

    public long getFulfilledRequestsCount() {
        return fulfilledRequestsCount;
    }

    public void setFulfilledRequestsCount(long fulfilledRequestsCount) {
        this.fulfilledRequestsCount = fulfilledRequestsCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
