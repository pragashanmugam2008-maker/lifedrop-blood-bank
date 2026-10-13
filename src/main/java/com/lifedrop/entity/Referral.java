package com.lifedrop.entity;

import com.lifedrop.enums.ReferralStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "referrals", indexes = {
    @Index(name = "idx_referral_code", columnList = "referral_code"),
    @Index(name = "idx_referral_referrer", columnList = "referrer_id"),
    @Index(name = "idx_referral_referred", columnList = "referred_user_id")
})
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "referred_user_id")
    private User referredUser;

    @NotBlank
    @Column(name = "referral_code", nullable = false, length = 50)
    private String referralCode;

    @Column(name = "stars_earned", nullable = false)
    private Integer starsEarned = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferralStatus status = ReferralStatus.CREATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Referral() {
    }

    public Referral(User referrer, String referralCode) {
        this.referrer = referrer;
        this.referralCode = referralCode;
        this.starsEarned = 0;
        this.status = ReferralStatus.CREATED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.starsEarned == null) this.starsEarned = 0;
        if (this.status == null) this.status = ReferralStatus.CREATED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getReferrer() {
        return referrer;
    }

    public void setReferrer(User referrer) {
        this.referrer = referrer;
    }

    public User getReferredUser() {
        return referredUser;
    }

    public void setReferredUser(User referredUser) {
        this.referredUser = referredUser;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public Integer getStarsEarned() {
        return starsEarned;
    }

    public void setStarsEarned(Integer starsEarned) {
        this.starsEarned = starsEarned;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
