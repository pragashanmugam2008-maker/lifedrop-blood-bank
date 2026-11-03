package com.lifedrop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifedrop.dto.request.AvailabilityUpdateDto;
import com.lifedrop.dto.request.BloodRequestStatusDto;
import com.lifedrop.dto.request.InventoryUpdateDto;
import com.lifedrop.dto.request.LoginRequest;
import com.lifedrop.dto.response.AuthResponse;
import com.lifedrop.entity.BloodRequest;
import com.lifedrop.entity.Donor;
import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.RequestStatus;
import com.lifedrop.repository.BloodRequestRepository;
import com.lifedrop.repository.DonorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPanelIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String donorToken;
    private String hospitalToken;

    @BeforeEach
    void setUp() {
        // Authenticate Admin
        LoginRequest adminLogin = new LoginRequest("admin@lifedrop.org", "admin123");
        AuthResponse adminAuth = authService.login(adminLogin);
        assertNotNull(adminAuth);
        adminToken = adminAuth.getToken();

        // Authenticate Donor
        LoginRequest donorLogin = new LoginRequest("rahul@lifedrop.org", "donor123");
        AuthResponse donorAuth = authService.login(donorLogin);
        assertNotNull(donorAuth);
        donorToken = donorAuth.getToken();

        // Authenticate Hospital
        LoginRequest hospitalLogin = new LoginRequest("apollo@lifedrop.org", "hospital123");
        AuthResponse hospitalAuth = authService.login(hospitalLogin);
        assertNotNull(hospitalAuth);
        hospitalToken = hospitalAuth.getToken();
    }

    @Test
    @DisplayName("Admin can securely access Dashboard Statistics with all required metrics")
    void testAdminCanAccessDashboardStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDonors").isNumber())
                .andExpect(jsonPath("$.data.totalRequests").isNumber())
                .andExpect(jsonPath("$.data.pendingRequests").isNumber())
                .andExpect(jsonPath("$.data.approvedRequests").isNumber())
                .andExpect(jsonPath("$.data.rejectedRequests").isNumber())
                .andExpect(jsonPath("$.data.totalInventoryUnits").isNumber());
    }

    @Test
    @DisplayName("Ordinary Donor is strictly FORBIDDEN (403) from accessing Admin Endpoints")
    void testOrdinaryDonorCannotAccessAdminEndpoints() throws Exception {
        // 1. Cannot view admin stats
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 2. Cannot view admin donors
        mockMvc.perform(get("/api/admin/donors")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 3. Cannot view admin requests
        mockMvc.perform(get("/api/admin/requests")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 4. Cannot update blood inventory
        InventoryUpdateDto updateDto = new InventoryUpdateDto();
        updateDto.setAvailableUnits(50);
        mockMvc.perform(put("/api/admin/inventory/O_POS")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Hospital is strictly FORBIDDEN (403) from accessing Admin Endpoints")
    void testOrdinaryHospitalCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + hospitalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/donors")
                        .header("Authorization", "Bearer " + hospitalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to Admin Endpoints is rejected")
    void testUnauthenticatedCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Admin can view all donors and toggle availability status")
    void testAdminCanManageDonors() throws Exception {
        // View all donors
        mockMvc.perform(get("/api/admin/donors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));

        // Toggle donor availability to false
        Donor donor = donorRepository.findAll().get(0);
        AvailabilityUpdateDto dtoFalse = new AvailabilityUpdateDto(false);

        mockMvc.perform(put("/api/admin/donors/" + donor.getId() + "/availability")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoFalse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availabilityStatus").value(false));

        // Toggle donor availability to true
        AvailabilityUpdateDto dtoTrue = new AvailabilityUpdateDto(true);

        mockMvc.perform(put("/api/admin/donors/" + donor.getId() + "/availability")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoTrue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availabilityStatus").value(true));
    }

    @Test
    @DisplayName("Admin can view all blood requests and update status (Approve / Reject)")
    void testAdminCanManageBloodRequests() throws Exception {
        // View all requests
        mockMvc.perform(get("/api/admin/requests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));

        BloodRequest request = bloodRequestRepository.findAll().get(0);

        // Approve request
        BloodRequestStatusDto approveDto = new BloodRequestStatusDto(RequestStatus.APPROVED);
        mockMvc.perform(put("/api/admin/requests/" + request.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Reject request
        BloodRequestStatusDto rejectDto = new BloodRequestStatusDto(RequestStatus.REJECTED);
        mockMvc.perform(put("/api/admin/requests/" + request.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Admin can update blood stock, and validation prevents negative quantities")
    void testAdminCanUpdateStockAndValidationPreventsNegative() throws Exception {
        // Valid stock update
        InventoryUpdateDto validDto = new InventoryUpdateDto();
        validDto.setAvailableUnits(42);
        validDto.setMinimumRequiredUnits(15);

        mockMvc.perform(put("/api/admin/inventory/A+")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bloodGroup").value("A+"))
                .andExpect(jsonPath("$.data.availableUnits").value(42));

        // Invalid negative stock update (must return 400 Bad Request)
        InventoryUpdateDto negativeDto = new InventoryUpdateDto();
        negativeDto.setAvailableUnits(-10);

        mockMvc.perform(put("/api/admin/inventory/A+")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeDto)))
                .andExpect(status().isBadRequest());
    }
}
