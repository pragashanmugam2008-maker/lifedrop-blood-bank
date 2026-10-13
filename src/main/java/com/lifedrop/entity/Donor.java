package com.lifedrop.entity;

import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.DonorBadge;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donors", indexes = {
    @Index(name = "idx_donor_blood_group", columnList = "blood_group"),
    @Index(name = "idx_donor_city", columnList = "city"),
    @Index(name = "idx_donor_available", columnList = "availability_status")
})
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 20)
    private BloodGroup bloodGroup;

    @Min(18)
    @Max(65)
    @Column(nullable = false)
    private Integer age;

    @Column(length = 20)
    private String gender;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(name = "total_donations", nullable = false)
    private Integer totalDonations = 0;

    @Column(name = "eligible", nullable = false)
    private Boolean eligible = true;

    @Column(name = "availability_status", nullable = false)
    private Boolean availabilityStatus = true;

    @Column(nullable = false)
    private Double rating = 5.0;

    @Column(nullable = false)
    private Integer stars = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DonorBadge badge = DonorBadge.NEW_DONOR;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Donor() {
    }

    public Donor(User user, BloodGroup bloodGroup, Integer age, String gender, String city, String address) {
        this.user = user;
        this.bloodGroup = bloodGroup;
        this.age = age;
        this.gender = gender;
        this.city = city;
        this.address = address;
        this.totalDonations = 0;
        this.eligible = true;
        this.availabilityStatus = true;
        this.rating = 5.0;
        this.stars = 0;
        this.badge = DonorBadge.NEW_DONOR;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.totalDonations == null) this.totalDonations = 0;
        if (this.eligible == null) this.eligible = true;
        if (this.availabilityStatus == null) this.availabilityStatus = true;
        if (this.rating == null) this.rating = 5.0;
        if (this.stars == null) this.stars = 0;
        if (this.badge == null) this.badge = DonorBadge.calculateBadge(this.totalDonations);
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

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public Integer getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(Integer totalDonations) {
        this.totalDonations = totalDonations;
        this.badge = DonorBadge.calculateBadge(totalDonations != null ? totalDonations : 0);
    }

    public Boolean getEligible() {
        return eligible;
    }

    public void setEligible(Boolean eligible) {
        this.eligible = eligible;
    }

    public Boolean getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(Boolean availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getStars() {
        return stars;
    }

    public void setStars(Integer stars) {
        this.stars = stars;
    }

    public DonorBadge getBadge() {
        return badge;
    }

    public void setBadge(DonorBadge badge) {
        this.badge = badge;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
