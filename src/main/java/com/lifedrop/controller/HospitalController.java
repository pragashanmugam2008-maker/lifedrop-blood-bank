package com.lifedrop.controller;

import com.lifedrop.dto.request.HospitalProfileRequest;
import com.lifedrop.dto.response.ApiResponse;
import com.lifedrop.dto.response.HospitalResponse;
import com.lifedrop.security.UserPrincipal;
import com.lifedrop.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@Tag(name = "Hospitals", description = "Hospital profiles, directory, and search APIs")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    @Operation(summary = "Get all registered hospitals or search by name / city")
    public ResponseEntity<ApiResponse<List<HospitalResponse>>> getAllHospitals(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city) {

        List<HospitalResponse> hospitals = hospitalService.searchHospitals(query, city);
        return ResponseEntity.ok(ApiResponse.success(hospitals));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hospital details by ID")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalById(@PathVariable Long id) {
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(ApiResponse.success(hospital));
    }

    @GetMapping("/my-profile")
    @Operation(summary = "Get current authenticated hospital profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<HospitalResponse>> getMyHospitalProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Please log in"));
        }

        HospitalResponse hospital = hospitalService.getHospitalByUserId(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(hospital));
    }

    @PostMapping
    @Operation(summary = "Create or update hospital profile for authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<HospitalResponse>> createOrUpdateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody HospitalProfileRequest request) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Please log in"));
        }

        HospitalResponse response = hospitalService.createOrUpdateProfile(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hospital profile saved successfully", response));
    }
}
