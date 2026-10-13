package com.lifedrop.dto.request;

import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.RequestUrgency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class BloodRequestCreateDto {

    @NotBlank(message = "Hospital name is required")
    private String hospitalName;

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotNull(message = "Required units is required")
    @Min(value = 1, message = "Required units must be at least 1")
    private Integer requiredUnits = 1;

    @NotBlank(message = "Hospital location/city is required")
    private String hospitalLocation;

    private RequestUrgency urgency = RequestUrgency.HIGH;

    private LocalDate requiredDate;

    private String contactPhone;

    private String notes;

    public BloodRequestCreateDto() {
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getRequiredUnits() {
        return requiredUnits;
    }

    public void setRequiredUnits(Integer requiredUnits) {
        this.requiredUnits = requiredUnits;
    }

    public String getHospitalLocation() {
        return hospitalLocation;
    }

    public void setHospitalLocation(String hospitalLocation) {
        this.hospitalLocation = hospitalLocation;
    }

    public RequestUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(RequestUrgency urgency) {
        this.urgency = urgency;
    }

    public LocalDate getRequiredDate() {
        return requiredDate;
    }

    public void setRequiredDate(LocalDate requiredDate) {
        this.requiredDate = requiredDate;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
