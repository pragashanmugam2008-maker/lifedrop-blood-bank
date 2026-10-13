package com.lifedrop.controller;

import com.lifedrop.dto.request.DonationCreateDto;
import com.lifedrop.dto.request.DonationStatusDto;
import com.lifedrop.dto.response.ApiResponse;
import com.lifedrop.dto.response.DonationResponse;
import com.lifedrop.entity.User;
import com.lifedrop.repository.UserRepository;
import com.lifedrop.security.UserPrincipal;
import com.lifedrop.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@Tag(name = "Donations", description = "Donation recording, history, and verification APIs")
public class DonationController {

    private final DonationService donationService;
    private final UserRepository userRepository;

    public DonationController(DonationService donationService, UserRepository userRepository) {
        this.donationService = donationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Record a new blood donation")
    public ResponseEntity<ApiResponse<DonationResponse>> recordDonation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DonationCreateDto dto) {

        User verifiedBy = null;
        if (userPrincipal != null) {
            verifiedBy = userRepository.findById(userPrincipal.getId()).orElse(null);
        }

        DonationResponse response = donationService.recordDonation(dto, verifiedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Donation recorded and processed successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all donation records")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> getAllDonations() {
        List<DonationResponse> donations = donationService.getAllDonations();
        return ResponseEntity.ok(ApiResponse.success(donations));
    }

    @GetMapping("/donor/{donorId}")
    @Operation(summary = "Get donation history for a specific donor")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> getDonationsByDonor(@PathVariable Long donorId) {
        List<DonationResponse> donations = donationService.getDonationsByDonor(donorId);
        return ResponseEntity.ok(ApiResponse.success(donations));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update donation status (SCHEDULED, COMPLETED, CANCELLED)")
    public ResponseEntity<ApiResponse<DonationResponse>> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DonationStatusDto dto) {

        User verifiedBy = null;
        if (userPrincipal != null) {
            verifiedBy = userRepository.findById(userPrincipal.getId()).orElse(null);
        }

        DonationResponse response = donationService.updateStatus(id, dto.getStatus(), verifiedBy);
        return ResponseEntity.ok(ApiResponse.success("Donation status updated", response));
    }
}
