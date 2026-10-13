package com.lifedrop.controller;

import com.lifedrop.dto.request.BloodRequestCreateDto;
import com.lifedrop.dto.request.BloodRequestStatusDto;
import com.lifedrop.dto.request.DonorRequestResponseDto;
import com.lifedrop.dto.response.ApiResponse;
import com.lifedrop.dto.response.BloodGroupDemandResponse;
import com.lifedrop.dto.response.BloodRequestResponse;
import com.lifedrop.dto.response.DonationResponse;
import com.lifedrop.entity.Donation;
import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.RequestStatus;
import com.lifedrop.enums.RequestUrgency;
import com.lifedrop.security.UserPrincipal;
import com.lifedrop.service.BloodRequestService;
import com.lifedrop.service.DonationService;
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
@RequestMapping("/api/blood-requests")
@Tag(name = "Blood Requests", description = "Hospital blood request creation, donor matching, direct dispatch, and donor response APIs")
public class BloodRequestController {

    private final BloodRequestService requestService;
    private final DonationService donationService;

    public BloodRequestController(BloodRequestService requestService, DonationService donationService) {
        this.requestService = requestService;
        this.donationService = donationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    @Operation(summary = "Create a blood request (Hospital / Admin)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> createRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BloodRequestCreateDto dto) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        BloodRequestResponse response = requestService.createRequest(userPrincipal.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hospital blood request submitted successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all blood requests (Hospital/Admin/Public view, filtered for donors)")
    public ResponseEntity<ApiResponse<List<BloodRequestResponse>>> getAllRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestUrgency urgency,
            @RequestParam(required = false) String bloodGroup) {

        // If donor is logged in, restrict view strictly to compatible incoming requests
        if (userPrincipal != null && userPrincipal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DONOR"))) {
            List<BloodRequestResponse> requests = requestService.getIncomingRequestsForDonor(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(requests));
        }

        BloodGroup bg = (bloodGroup != null && !bloodGroup.trim().isEmpty()) ? BloodGroup.fromString(bloodGroup) : null;
        List<BloodRequestResponse> requests = requestService.getAllRequests(status, urgency, bg);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping("/demanded-groups")
    @Operation(summary = "Get highly demanded blood groups dynamically calculated from active database requests")
    public ResponseEntity<ApiResponse<List<BloodGroupDemandResponse>>> getHighlyDemandedBloodGroups() {
        List<BloodGroupDemandResponse> demanded = requestService.getHighlyDemandedBloodGroups();
        return ResponseEntity.ok(ApiResponse.success(demanded));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get blood request details by ID")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> getRequestById(@PathVariable Long id) {
        BloodRequestResponse request = requestService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.success(request));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    @Operation(summary = "Get blood requests created by the authenticated hospital")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<BloodRequestResponse>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        List<BloodRequestResponse> requests = requestService.getRequestsByHospital(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN')")
    @Operation(summary = "Get incoming hospital blood requests compatible with the authenticated donor")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<BloodRequestResponse>>> getIncomingRequestsForDonor(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        List<BloodRequestResponse> requests = requestService.getIncomingRequestsForDonor(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @PostMapping("/{id}/send-to-donor/{donorId}")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    @Operation(summary = "Hospital sends a direct blood donation request to a matched compatible donor")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<DonationResponse>> sendRequestToDonor(
            @PathVariable Long id,
            @PathVariable Long donorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        Donation donation = requestService.sendRequestToDonor(id, donorId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Donation request sent to donor", donationService.mapToResponse(donation)));
    }

    @PutMapping("/{id}/respond")
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN')")
    @Operation(summary = "Donor responds [ACCEPT] or [REJECT] to an incoming hospital blood request")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<DonationResponse>> respondToRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DonorRequestResponseDto dto) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        Donation donation = requestService.respondToRequest(id, userPrincipal.getId(), dto);
        return ResponseEntity.ok(ApiResponse.success("Response recorded: " + dto.getStatus(), donationService.mapToResponse(donation)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    @Operation(summary = "Update blood request status (ACTIVE, FULFILLED, CANCELLED)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BloodRequestStatusDto dto) {

        BloodRequestResponse updated = requestService.updateStatus(id, dto.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Blood request status updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    @Operation(summary = "Cancel / delete blood request")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> deleteRequest(@PathVariable Long id) {
        requestService.deleteRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Blood request deleted successfully", null));
    }
}
