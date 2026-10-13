package com.lifedrop.entity;

import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.RequestStatus;
import com.lifedrop.enums.RequestUrgency;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_requests", indexes = {
    @Index(name = "idx_requests_blood_group", columnList = "blood_group"),
    @Index(name = "idx_requests_status", columnList = "status"),
    @Index(name = "idx_requests_urgency", columnList = "urgency"),
    @Index(name = "idx_requests_hospital", columnList = "hospital_id")
})
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(name = "hospital_name", nullable = false, length = 150)
    private String hospitalName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 20)
    private BloodGroup bloodGroup;

    @NotNull
    @Min(1)
    @Column(name = "required_units", nullable = false)
    private Integer requiredUnits = 1;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 20)
    private RequestUrgency urgency = RequestUrgency.HIGH;

    @Column(name = "hospital_location", length = 150)
    private String hospitalLocation;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "notes", length = 500)
    private String notes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BloodRequest() {
    }

    public BloodRequest(User requester, Hospital hospital, String hospitalName, BloodGroup bloodGroup,
                        Integer requiredUnits, String hospitalLocation, RequestUrgency urgency,
                        LocalDate requiredDate, String contactPhone) {
        this.requester = requester;
        this.hospital = hospital;
        this.hospitalName = hospitalName != null ? hospitalName : (hospital != null ? hospital.getHospitalName() : requester.getName());
        this.bloodGroup = bloodGroup;
        this.requiredUnits = requiredUnits != null ? requiredUnits : 1;
        this.hospitalLocation = hospitalLocation;
        this.urgency = urgency != null ? urgency : RequestUrgency.HIGH;
        this.requiredDate = requiredDate != null ? requiredDate : LocalDate.now().plusDays(2);
        this.contactPhone = contactPhone != null ? contactPhone : requester.getPhone();
        this.status = RequestStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = RequestStatus.ACTIVE;
        if (this.urgency == null) this.urgency = RequestUrgency.HIGH;
        if (this.requiredUnits == null) this.requiredUnits = 1;
        if (this.hospitalName == null && this.hospital != null) {
            this.hospitalName = this.hospital.getHospitalName();
        } else if (this.hospitalName == null && this.requester != null) {
            this.hospitalName = this.requester.getName();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
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

    public RequestUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(RequestUrgency urgency) {
        this.urgency = urgency;
    }

    public String getHospitalLocation() {
        return hospitalLocation;
    }

    public void setHospitalLocation(String hospitalLocation) {
        this.hospitalLocation = hospitalLocation;
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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
