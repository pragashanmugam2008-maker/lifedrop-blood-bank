package com.lifedrop.controller;

import com.lifedrop.dto.request.AvailabilityUpdateDto;
import com.lifedrop.dto.request.DonorProfileRequest;
import com.lifedrop.dto.response.ApiResponse;
import com.lifedrop.dto.response.DonorResponse;
import com.lifedrop.dto.response.EligibilityResponse;
import com.lifedrop.enums.BloodGroup;
import com.lifedrop.security.UserPrincipal;
import com.lifedrop.service.DonorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
@Tag(name = "Donors", description = "Donor discovery, profile management, compatibility, and eligibility")
public class DonorController {

    private final DonorService donorService;

    public DonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    @GetMapping
    @Operation(summary = "Search and filter donors by name/query, blood group, city, availability, and eligibility")
    public ResponseEntity<ApiResponse<List<DonorResponse>>> searchDonors(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Boolean eligible) {

        String searchQuery = query != null ? query : name;
        BloodGroup bg = (bloodGroup != null && !bloodGroup.trim().isEmpty()) ? BloodGroup.fromString(bloodGroup) : null;

        List<DonorResponse> donors = donorService.searchDonors(searchQuery, bg, city, available, eligible);
        return ResponseEntity.ok(ApiResponse.success(donors));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get donor profile by ID")
    public ResponseEntity<ApiResponse<DonorResponse>> getDonorById(@PathVariable Long id) {
        DonorResponse donor = donorService.getDonorById(id);
        return ResponseEntity.ok(ApiResponse.success(donor));
    }

    @GetMapping("/my-profile")
    @Operation(summary = "Get current authenticated donor profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<DonorResponse>> getMyDonorProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Please log in"));
        }
        DonorResponse donor = donorService.getDonorByUserId(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(donor));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get donor profile by User ID")
    public ResponseEntity<ApiResponse<DonorResponse>> getDonorByUserId(@PathVariable Long userId) {
        DonorResponse donor = donorService.getDonorByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(donor));
    }

    @GetMapping("/{id}/eligibility")
    @Operation(summary = "Check donor eligibility rules (Age 18-65, Availability, 90-day whole blood interval)")
    public ResponseEntity<ApiResponse<EligibilityResponse>> getDonorEligibility(@PathVariable Long id) {
        EligibilityResponse eligibility = donorService.getEligibility(id);
        return ResponseEntity.ok(ApiResponse.success(eligibility));
    }

    @GetMapping("/compatible")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Find available, eligible donors compatible with a recipient blood group (using canDonateTo rules - Hospital/Admin only)")
    public ResponseEntity<ApiResponse<List<DonorResponse>>> findCompatibleDonors(
            @RequestParam String bloodGroup,
            @RequestParam(required = false) String city) {

        BloodGroup recipientBg = BloodGroup.fromString(bloodGroup);
        List<DonorResponse> donors = donorService.findCompatibleDonors(recipientBg, city);
        return ResponseEntity.ok(ApiResponse.success(donors));
    }

    @PostMapping
    @Operation(summary = "Create or update donor profile for authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<DonorResponse>> createOrUpdateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DonorProfileRequest request) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Please log in"));
        }

        DonorResponse response = donorService.createOrUpdateProfile(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Donor profile saved successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update donor profile by ID")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<DonorResponse>> updateDonor(
            @PathVariable Long id,
            @Valid @RequestBody DonorProfileRequest request) {

        DonorResponse response = donorService.updateDonor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Donor profile updated successfully", response));
    }

    @PutMapping("/{id}/availability")
    @Operation(summary = "Toggle donor availability status")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<DonorResponse>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityUpdateDto dto) {

        DonorResponse response = donorService.updateAvailability(id, dto.getAvailable());
        return ResponseEntity.ok(ApiResponse.success("Donor availability updated", response));
    }
}
