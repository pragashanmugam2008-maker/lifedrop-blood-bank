package com.lifedrop.service;

import com.lifedrop.dto.request.LoginRequest;
import com.lifedrop.dto.request.RegisterRequest;
import com.lifedrop.dto.response.AuthResponse;
import com.lifedrop.enums.Role;
import com.lifedrop.enums.BloodGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthPerformanceTest {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Measure Donor Login Performance")
    void testDonorLoginPerformance() {
        LoginRequest request = new LoginRequest();
        request.setEmail("rahul@lifedrop.org");
        request.setPassword("donor123");

        long start = System.currentTimeMillis();
        AuthResponse response = authService.login(request);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== BENCHMARK: Donor Login took " + duration + " ms ===");
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("rahul@lifedrop.org", response.getEmail());
        assertEquals(Role.DONOR, response.getRole());
        assertTrue(duration < 2000, "Login must complete under 2000ms");
    }

    @Test
    @DisplayName("Measure Hospital Login Performance")
    void testHospitalLoginPerformance() {
        LoginRequest request = new LoginRequest();
        request.setEmail("apollo@lifedrop.org");
        request.setPassword("hospital123");

        long start = System.currentTimeMillis();
        AuthResponse response = authService.login(request);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== BENCHMARK: Hospital Login took " + duration + " ms ===");
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(Role.HOSPITAL, response.getRole());
        assertEquals("Apollo Speciality Hospital", response.getHospitalName());
        assertTrue(duration < 2000, "Hospital Login must complete under 2000ms");
    }

    @Test
    @DisplayName("Measure Registration Performance")
    void testRegisterPerformance() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Fast Donor");
        request.setEmail("fastdonor_" + System.currentTimeMillis() + "@lifedrop.org");
        request.setPassword("StrongPass123!");
        request.setPhone("+91 9123456789");
        request.setRole(Role.DONOR);
        request.setBloodGroup(BloodGroup.O_POS);
        request.setAge(26);
        request.setCity("Chennai");

        long start = System.currentTimeMillis();
        AuthResponse response = authService.register(request);
        long duration = System.currentTimeMillis() - start;

        System.out.println("=== BENCHMARK: Donor Registration took " + duration + " ms ===");
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getDonorId());
        assertTrue(duration < 2000, "Registration must complete under 2000ms");
    }
}
