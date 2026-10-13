package com.lifedrop.dto.request;

import jakarta.validation.constraints.NotBlank;

public class HospitalProfileRequest {

    @NotBlank(message = "Hospital name is required")
    private String hospitalName;

    private String licenseNumber;

    @NotBlank(message = "City is required")
    private String city;

    private String address;

    private String emergencyHelpline;

    private String contactPerson;

    public HospitalProfileRequest() {
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
}
