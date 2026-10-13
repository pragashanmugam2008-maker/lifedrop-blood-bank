package com.lifedrop.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ReferralUseDto {

    @NotBlank(message = "Referral code is required")
    private String referralCode;

    public ReferralUseDto() {
    }

    public ReferralUseDto(String referralCode) {
        this.referralCode = referralCode;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }
}
