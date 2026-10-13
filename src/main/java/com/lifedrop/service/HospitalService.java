package com.lifedrop.service;

import com.lifedrop.dto.request.HospitalProfileRequest;
import com.lifedrop.dto.response.HospitalResponse;
import com.lifedrop.entity.Hospital;
import com.lifedrop.entity.User;
import com.lifedrop.enums.RequestStatus;
import com.lifedrop.enums.Role;
import com.lifedrop.exception.ResourceNotFoundException;
import com.lifedrop.repository.BloodRequestRepository;
import com.lifedrop.repository.HospitalRepository;
import com.lifedrop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final BloodRequestRepository bloodRequestRepository;

    public HospitalService(HospitalRepository hospitalRepository,
                           UserRepository userRepository,
                           BloodRequestRepository bloodRequestRepository) {
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.bloodRequestRepository = bloodRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<HospitalResponse> getAllHospitals() {
        return hospitalRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HospitalResponse getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found with id: " + id));
        return mapToResponse(hospital);
    }

    @Transactional(readOnly = true)
    public HospitalResponse getHospitalByUserId(Long userId) {
        Hospital hospital = hospitalRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital profile not found for user id: " + userId));
        return mapToResponse(hospital);
    }

    @Transactional
    public HospitalResponse createOrUpdateProfile(Long userId, HospitalProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.HOSPITAL && user.getRole() != Role.ADMIN) {
            user.setRole(Role.HOSPITAL);
            userRepository.save(user);
        }

        Hospital hospital = hospitalRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Hospital h = new Hospital();
                    h.setUser(user);
                    return h;
                });

        hospital.setHospitalName(request.getHospitalName());
        hospital.setLicenseNumber(request.getLicenseNumber());
        hospital.setCity(request.getCity());
        hospital.setAddress(request.getAddress());
        hospital.setEmergencyHelpline(request.getEmergencyHelpline());
        hospital.setContactPerson(request.getContactPerson());

        Hospital saved = hospitalRepository.save(hospital);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<HospitalResponse> searchHospitals(String query, String city) {
        if ((query != null && !query.trim().isEmpty()) || (city != null && !city.trim().isEmpty())) {
            String q = query != null ? query.trim() : "";
            String c = city != null ? city.trim() : "";
            return hospitalRepository.findByHospitalNameContainingIgnoreCaseOrCityContainingIgnoreCase(q, c)
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return getAllHospitals();
    }

    public HospitalResponse mapToResponse(Hospital hospital) {
        HospitalResponse response = new HospitalResponse();
        response.setId(hospital.getId());
        if (hospital.getUser() != null) {
            response.setUserId(hospital.getUser().getId());
            response.setName(hospital.getUser().getName());
            response.setEmail(hospital.getUser().getEmail());
            response.setPhone(hospital.getUser().getPhone());
        }
        response.setHospitalName(hospital.getHospitalName());
        response.setLicenseNumber(hospital.getLicenseNumber());
        response.setCity(hospital.getCity());
        response.setAddress(hospital.getAddress());
        response.setEmergencyHelpline(hospital.getEmergencyHelpline());
        response.setContactPerson(hospital.getContactPerson());
        response.setVerified(hospital.getVerified());
        response.setCreatedAt(hospital.getCreatedAt());

        if (hospital.getId() != null) {
            long active = bloodRequestRepository.findByHospitalIdOrderByCreatedAtDesc(hospital.getId())
                    .stream().filter(r -> r.getStatus() == RequestStatus.ACTIVE).count();
            long fulfilled = bloodRequestRepository.findByHospitalIdOrderByCreatedAtDesc(hospital.getId())
                    .stream().filter(r -> r.getStatus() == RequestStatus.FULFILLED).count();
            response.setActiveRequestsCount(active);
            response.setFulfilledRequestsCount(fulfilled);
        }

        return response;
    }
}
