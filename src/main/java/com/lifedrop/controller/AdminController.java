package com.lifedrop.controller;

import com.lifedrop.dto.request.AvailabilityUpdateDto;
import com.lifedrop.dto.request.BloodRequestStatusDto;
import com.lifedrop.dto.request.DonationStatusDto;
import com.lifedrop.dto.request.InventoryUpdateDto;
import com.lifedrop.dto.response.*;
import com.lifedrop.entity.User;
import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.DonationStatus;
import com.lifedrop.enums.Role;
import com.lifedrop.exception.BadRequestException;
import com.lifedrop.exception.ResourceNotFoundException;
import com.lifedrop.repository.UserRepository;
import com.lifedrop.security.UserPrincipal;
import com.lifedrop.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Administrative management APIs (Requires ROLE_ADMIN)")
public class AdminController {

    private final StatisticsService statisticsService;
    private final UserRepository userRepository;
    private final DonorService donorService;
    private final HospitalService hospitalService;
    private final BloodRequestService bloodRequestService;
    private final DonationService donationService;
    private final InventoryService inventoryService;
    private final ReferralService referralService;

    public AdminController(StatisticsService statisticsService,
                           UserRepository userRepository,
                           DonorService donorService,
                           HospitalService hospitalService,
                           BloodRequestService bloodRequestService,
                           DonationService donationService,
                           InventoryService inventoryService,
                           ReferralService referralService) {
        this.statisticsService = statisticsService;
        this.userRepository = userRepository;
        this.donorService = donorService;
        this.hospitalService = hospitalService;
        this.bloodRequestService = bloodRequestService;
        this.donationService = donationService;
        this.inventoryService = inventoryService;
        this.referralService = referralService;
    }

    @GetMapping({"/stats", "/dashboard"})
    @Operation(summary = "Get full Admin Dashboard metrics")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getAdminStats() {
        AdminDashboardStatsResponse stats = statisticsService.getAdminStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    @Operation(summary = "Get list of all users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                        u.getRole(), null, null, null, null, u.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Change a user's role (ADMIN only)")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable Long id,
            @RequestParam Role role,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        if (adminPrincipal.getId().equals(id) && role != Role.ADMIN) {
            throw new BadRequestException("You cannot remove your own ADMIN role");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setRole(role);
        User saved = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("User role updated to " + role,
                new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getPhone(),
                        saved.getRole(), null, null, null, null, saved.getCreatedAt())));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user account (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        if (adminPrincipal.getId().equals(id)) {
            throw new BadRequestException("You cannot delete your own ADMIN account");
        }

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @GetMapping("/donors")
    @Operation(summary = "Admin manage all donors")
    public ResponseEntity<ApiResponse<List<DonorResponse>>> getAllDonors() {
        List<DonorResponse> donors = donorService.getAllDonors();
        return ResponseEntity.ok(ApiResponse.success(donors));
    }

    @PutMapping("/donors/{id}/availability")
    @Operation(summary = "Admin toggle donor availability")
    public ResponseEntity<ApiResponse<DonorResponse>> toggleDonorAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityUpdateDto dto) {

        DonorResponse response = donorService.updateAvailability(id, dto.getAvailable());
        return ResponseEntity.ok(ApiResponse.success("Donor availability updated", response));
    }

    @GetMapping("/hospitals")
    @Operation(summary = "Admin manage all registered hospitals")
    public ResponseEntity<ApiResponse<List<HospitalResponse>>> getAllHospitals() {
        List<HospitalResponse> hospitals = hospitalService.getAllHospitals();
        return ResponseEntity.ok(ApiResponse.success(hospitals));
    }

    @GetMapping("/requests")
    @Operation(summary = "Admin view all blood requests")
    public ResponseEntity<ApiResponse<List<BloodRequestResponse>>> getAllBloodRequests() {
        List<BloodRequestResponse> requests = bloodRequestService.getAllRequests(null, null, null);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @PutMapping("/requests/{id}/status")
    @Operation(summary = "Admin update/approve/fulfill blood request")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> updateRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody BloodRequestStatusDto dto) {

        BloodRequestResponse response = bloodRequestService.updateStatus(id, dto.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Request status updated to " + dto.getStatus(), response));
    }

    @GetMapping("/donations")
    @Operation(summary = "Admin view all donations")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> getAllDonations() {
        List<DonationResponse> donations = donationService.getAllDonations();
        return ResponseEntity.ok(ApiResponse.success(donations));
    }

    @PutMapping("/donations/{id}/verify")
    @Operation(summary = "Admin verify donation (updates blood inventory, marks request fulfilled, recalculates badge & triggers referral reward)")
    public ResponseEntity<ApiResponse<DonationResponse>> verifyDonation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        User adminUser = userRepository.findById(adminPrincipal.getId()).orElse(null);
        DonationResponse response = donationService.updateStatus(id, DonationStatus.COMPLETED, adminUser);
        return ResponseEntity.ok(ApiResponse.success("Donation verified successfully", response));
    }

    @PutMapping("/donations/{id}/status")
    @Operation(summary = "Admin update donation status")
    public ResponseEntity<ApiResponse<DonationResponse>> updateDonationStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            @Valid @RequestBody DonationStatusDto dto) {

        User adminUser = userRepository.findById(adminPrincipal.getId()).orElse(null);
        DonationResponse response = donationService.updateStatus(id, dto.getStatus(), adminUser);
        return ResponseEntity.ok(ApiResponse.success("Donation status updated to " + dto.getStatus(), response));
    }

    @GetMapping("/referrals")
    @Operation(summary = "Admin view all referral records")
    public ResponseEntity<ApiResponse<List<ReferralResponse>>> getAllReferrals() {
        List<ReferralResponse> referrals = referralService.getAllReferrals();
        return ResponseEntity.ok(ApiResponse.success(referrals));
    }

    @PutMapping("/inventory/{bloodGroup}")
    @Operation(summary = "Admin update blood stock units")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventoryStock(
            @PathVariable String bloodGroup,
            @Valid @RequestBody InventoryUpdateDto dto) {

        BloodGroup bg = BloodGroup.fromString(bloodGroup);
        InventoryResponse response = inventoryService.updateInventory(bg, dto);
        return ResponseEntity.ok(ApiResponse.success("Blood inventory updated for " + bg.getDisplayValue(), response));
    }
}
