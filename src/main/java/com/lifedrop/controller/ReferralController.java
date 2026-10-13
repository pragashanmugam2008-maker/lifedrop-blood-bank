package com.lifedrop.controller;

import com.lifedrop.dto.response.ApiResponse;
import com.lifedrop.dto.response.ReferralResponse;
import com.lifedrop.security.UserPrincipal;
import com.lifedrop.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referrals")
@Tag(name = "Referrals", description = "Referral code generation, tracking, and star reward APIs")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate a new referral code for the authenticated user")
    public ResponseEntity<ApiResponse<ReferralResponse>> generateReferralCode(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        ReferralResponse response = referralService.generateReferralCode(userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Referral code generated successfully", response));
    }

    @GetMapping("/my-referrals")
    @Operation(summary = "Get list of referrals and rewards earned by the authenticated user")
    public ResponseEntity<ApiResponse<List<ReferralResponse>>> getMyReferrals(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        List<ReferralResponse> referrals = referralService.getReferralsByUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(referrals));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all referrals in the system (ADMIN only)")
    public ResponseEntity<ApiResponse<List<ReferralResponse>>> getAllReferrals() {
        List<ReferralResponse> referrals = referralService.getAllReferrals();
        return ResponseEntity.ok(ApiResponse.success(referrals));
    }
}
