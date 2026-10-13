package com.lifedrop.service;

import com.lifedrop.dto.request.DonorProfileRequest;
import com.lifedrop.dto.response.DonorResponse;
import com.lifedrop.dto.response.EligibilityResponse;
import com.lifedrop.entity.Donor;
import com.lifedrop.entity.User;
import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.DonorBadge;
import com.lifedrop.enums.Role;
import com.lifedrop.exception.ResourceNotFoundException;
import com.lifedrop.repository.DonorRepository;
import com.lifedrop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DonorService {

    private final DonorRepository donorRepository;
    private final UserRepository userRepository;
    private final BloodCompatibilityService compatibilityService;
    private final DonorEligibilityService eligibilityService;

    public DonorService(DonorRepository donorRepository,
                        UserRepository userRepository,
                        BloodCompatibilityService compatibilityService,
                        DonorEligibilityService eligibilityService) {
        this.donorRepository = donorRepository;
        this.userRepository = userRepository;
        this.compatibilityService = compatibilityService;
        this.eligibilityService = eligibilityService;
    }

    @Transactional(readOnly = true)
    public List<DonorResponse> getAllDonors() {
        return donorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DonorResponse getDonorById(Long id) {
        Donor donor = donorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + id));
        return mapToResponse(donor);
    }

    @Transactional(readOnly = true)
    public DonorResponse getDonorByUserId(Long userId) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for user id: " + userId));
        return mapToResponse(donor);
    }

    @Transactional
    public DonorResponse createOrUpdateProfile(Long userId, DonorProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.DONOR && user.getRole() != Role.ADMIN) {
            user.setRole(Role.DONOR);
            userRepository.save(user);
        }

        Donor donor = donorRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Donor d = new Donor();
                    d.setUser(user);
                    return d;
                });

        donor.setBloodGroup(request.getBloodGroup());
        donor.setAge(request.getAge());
        donor.setGender(request.getGender());
        donor.setCity(request.getCity());
        donor.setAddress(request.getAddress());
        if (request.getAvailabilityStatus() != null) {
            donor.setAvailabilityStatus(request.getAvailabilityStatus());
        }
        if (request.getLastDonationDate() != null) {
            donor.setLastDonationDate(request.getLastDonationDate());
        }

        boolean isEligible = eligibilityService.isEligible(donor);
        donor.setEligible(isEligible);

        Donor saved = donorRepository.save(donor);
        return mapToResponse(saved);
    }

    @Transactional
    public DonorResponse updateDonor(Long id, DonorProfileRequest request) {
        Donor donor = donorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + id));

        if (request.getBloodGroup() != null) donor.setBloodGroup(request.getBloodGroup());
        if (request.getAge() != null) donor.setAge(request.getAge());
        if (request.getGender() != null) donor.setGender(request.getGender());
        if (request.getCity() != null) donor.setCity(request.getCity());
        if (request.getAddress() != null) donor.setAddress(request.getAddress());
        if (request.getAvailabilityStatus() != null) donor.setAvailabilityStatus(request.getAvailabilityStatus());
        if (request.getLastDonationDate() != null) donor.setLastDonationDate(request.getLastDonationDate());

        donor.setEligible(eligibilityService.isEligible(donor));
        Donor saved = donorRepository.save(donor);
        return mapToResponse(saved);
    }

    @Transactional
    public DonorResponse updateAvailability(Long id, boolean available) {
        Donor donor = donorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + id));

        donor.setAvailabilityStatus(available);
        donor.setEligible(eligibilityService.isEligible(donor));

        Donor saved = donorRepository.save(donor);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteDonor(Long id) {
        if (!donorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Donor not found with id: " + id);
        }
        donorRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<DonorResponse> searchDonors(String query, BloodGroup bloodGroup, String city,
                                           Boolean available, Boolean eligible) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanCity = (city != null && !city.trim().isEmpty()) ? city.trim() : null;

        return donorRepository.searchDonors(cleanQuery, bloodGroup, cleanCity, available, eligible)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DonorResponse> findCompatibleDonors(BloodGroup recipientBloodGroup, String city) {
        if (recipientBloodGroup == null) {
            return Collections.emptyList();
        }

        List<BloodGroup> compatibleDonorGroups = compatibilityService.getCompatibleDonorsFor(recipientBloodGroup);
        if (compatibleDonorGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<Donor> donors = donorRepository.findByBloodGroupInAndAvailabilityStatusTrue(compatibleDonorGroups);

        return donors.stream()
                .filter(eligibilityService::isEligible)
                .filter(d -> city == null || city.trim().isEmpty() || d.getCity().equalsIgnoreCase(city.trim()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EligibilityResponse getEligibility(Long donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + donorId));
        return eligibilityService.checkEligibility(donor);
    }

    public DonorResponse mapToResponse(Donor donor) {
        DonorResponse response = new DonorResponse();
        response.setId(donor.getId());
        if (donor.getUser() != null) {
            response.setUserId(donor.getUser().getId());
            response.setName(donor.getUser().getName());
            response.setEmail(donor.getUser().getEmail());
            response.setPhone(donor.getUser().getPhone());
        }
        response.setBloodGroup(donor.getBloodGroup());
        response.setAge(donor.getAge());
        response.setGender(donor.getGender());
        response.setCity(donor.getCity());
        response.setAddress(donor.getAddress());
        response.setLastDonationDate(donor.getLastDonationDate());
        response.setTotalDonations(donor.getTotalDonations());
        response.setEligible(donor.getEligible());
        response.setAvailabilityStatus(donor.getAvailabilityStatus());
        response.setRating(donor.getRating());
        response.setStars(donor.getStars());

        DonorBadge calculatedBadge = DonorBadge.calculateBadge(donor.getTotalDonations() != null ? donor.getTotalDonations() : 0);
        response.setBadge(calculatedBadge);
        response.setBadgeTitle(calculatedBadge.getTitle());
        response.setBadgeIcon(calculatedBadge.getIcon());
        response.setCreatedAt(donor.getCreatedAt());
        return response;
    }
}
