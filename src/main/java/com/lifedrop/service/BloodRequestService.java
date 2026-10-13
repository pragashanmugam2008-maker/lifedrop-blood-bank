package com.lifedrop.service;

import com.lifedrop.dto.request.BloodRequestCreateDto;
import com.lifedrop.dto.request.DonorRequestResponseDto;
import com.lifedrop.dto.response.BloodGroupDemandResponse;
import com.lifedrop.dto.response.BloodRequestResponse;
import com.lifedrop.dto.response.DonationResponse;
import com.lifedrop.entity.*;
import com.lifedrop.enums.*;
import com.lifedrop.exception.BadRequestException;
import com.lifedrop.exception.ResourceNotFoundException;
import com.lifedrop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BloodRequestService {

    private final BloodRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final BloodCompatibilityService compatibilityService;
    private final DonorEligibilityService eligibilityService;

    public BloodRequestService(BloodRequestRepository requestRepository,
                               UserRepository userRepository,
                               HospitalRepository hospitalRepository,
                               DonorRepository donorRepository,
                               DonationRepository donationRepository,
                               BloodCompatibilityService compatibilityService,
                               DonorEligibilityService eligibilityService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.compatibilityService = compatibilityService;
        this.eligibilityService = eligibilityService;
    }

    @Transactional
    public BloodRequestResponse createRequest(Long requesterId, BloodRequestCreateDto dto) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Requester user not found with id: " + requesterId));

        Hospital hospital = hospitalRepository.findByUserId(requesterId).orElse(null);

        String hospitalName = dto.getHospitalName() != null && !dto.getHospitalName().trim().isEmpty() ?
                dto.getHospitalName().trim() : (hospital != null ? hospital.getHospitalName() : requester.getName());

        String location = dto.getHospitalLocation() != null && !dto.getHospitalLocation().trim().isEmpty() ?
                dto.getHospitalLocation().trim() : (hospital != null ? hospital.getCity() : "Chennai");

        BloodRequest request = new BloodRequest();
        request.setRequester(requester);
        request.setHospital(hospital);
        request.setHospitalName(hospitalName);
        request.setBloodGroup(dto.getBloodGroup());
        request.setRequiredUnits(dto.getRequiredUnits() != null ? dto.getRequiredUnits() : 1);
        request.setHospitalLocation(location);
        request.setUrgency(dto.getUrgency() != null ? dto.getUrgency() : RequestUrgency.HIGH);
        request.setRequiredDate(dto.getRequiredDate() != null ? dto.getRequiredDate() : LocalDate.now().plusDays(2));
        request.setContactPhone(dto.getContactPhone() != null && !dto.getContactPhone().trim().isEmpty() ?
                dto.getContactPhone() : requester.getPhone());
        request.setNotes(dto.getNotes());
        request.setStatus(RequestStatus.ACTIVE);

        BloodRequest saved = requestRepository.save(request);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getAllRequests(RequestStatus status, RequestUrgency urgency, BloodGroup bloodGroup) {
        List<BloodRequest> requests = requestRepository.findAll();

        return requests.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> urgency == null || r.getUrgency() == urgency)
                .filter(r -> bloodGroup == null || r.getBloodGroup() == bloodGroup)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BloodRequestResponse getRequestById(Long id) {
        BloodRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + id));
        return mapToResponse(request);
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getRequestsByRequester(Long requesterId) {
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getRequestsByHospital(Long hospitalUserId) {
        Optional<Hospital> optHospital = hospitalRepository.findByUserId(hospitalUserId);
        if (optHospital.isPresent()) {
            return requestRepository.findByHospitalIdOrderByCreatedAtDesc(optHospital.get().getId())
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return getRequestsByRequester(hospitalUserId);
    }

    /**
     * Retrieves incoming hospital requests for an authenticated donor.
     * Matches active hospital blood requests where canDonateTo(donor.bloodGroup, request.bloodGroup) is TRUE.
     */
    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getIncomingRequestsForDonor(Long donorUserId) {
        Donor donor = donorRepository.findByUserId(donorUserId).orElse(null);
        if (donor == null || donor.getBloodGroup() == null) {
            return Collections.emptyList();
        }

        List<BloodGroup> compatibleRecipientGroups = compatibilityService.getCompatibleRecipientsFor(donor.getBloodGroup());
        if (compatibleRecipientGroups.isEmpty()) {
            return Collections.emptyList();
        }

        return requestRepository.findByBloodGroupInAndStatusOrderByCreatedAtDesc(compatibleRecipientGroups, RequestStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Hospital sends a direct donation request to a matched compatible donor.
     */
    @Transactional
    public Donation sendRequestToDonor(Long requestId, Long donorId, Long hospitalUserId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + donorId));

        if (!compatibilityService.canDonateTo(donor.getBloodGroup(), request.getBloodGroup())) {
            throw new BadRequestException("Incompatible blood group: Donor (" + donor.getBloodGroup().getDisplayValue() +
                    ") cannot donate to requested blood group (" + request.getBloodGroup().getDisplayValue() + ")");
        }

        Hospital hospital = hospitalRepository.findByUserId(hospitalUserId).orElse(request.getHospital());

        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setRequest(request);
        donation.setHospital(hospital);
        donation.setDonationDate(request.getRequiredDate() != null ? request.getRequiredDate() : LocalDate.now());
        donation.setUnits(1);
        donation.setStatus(DonationStatus.REQUESTED);
        donation.setNotes("Donation request sent from " + request.getHospitalName());

        return donationRepository.save(donation);
    }

    /**
     * Donor responds [ACCEPT] or [REJECT] to an incoming hospital request.
     */
    @Transactional
    public Donation respondToRequest(Long requestId, Long donorUserId, DonorRequestResponseDto dto) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        Donor donor = donorRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for user id: " + donorUserId));

        if (!compatibilityService.canDonateTo(donor.getBloodGroup(), request.getBloodGroup())) {
            throw new BadRequestException("Incompatible blood group: Donor (" + donor.getBloodGroup().getDisplayValue() +
                    ") cannot donate to requested blood group (" + request.getBloodGroup().getDisplayValue() + ")");
        }

        // Validate scheduling deadline if accepted
        if (dto.getStatus() == DonationStatus.ACCEPTED) {
            LocalDate scheduledDate = dto.getScheduledDate();
            if (scheduledDate != null && request.getRequiredDate() != null) {
                if (scheduledDate.isAfter(request.getRequiredDate())) {
                    throw new BadRequestException("Donation schedule must be before the request deadline.");
                }
            }
        }

        DonationStatus newStatus = dto.getStatus() == DonationStatus.ACCEPTED ?
                (dto.getScheduledDate() != null ? DonationStatus.SCHEDULED : DonationStatus.ACCEPTED) : DonationStatus.REJECTED;

        LocalDate donationDate = dto.getScheduledDate() != null ? dto.getScheduledDate() :
                (request.getRequiredDate() != null ? request.getRequiredDate() : LocalDate.now());

        Donation donation = donationRepository.findByRequestIdOrderByDonationDateDesc(requestId)
                .stream()
                .filter(d -> d.getDonor().getId().equals(donor.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Donation d = new Donation();
                    d.setDonor(donor);
                    d.setRequest(request);
                    d.setHospital(request.getHospital());
                    return d;
                });

        donation.setStatus(newStatus);
        donation.setDonationDate(donationDate);
        
        String notes = dto.getNotes() != null ? dto.getNotes() : "";
        if (dto.getScheduledTime() != null && !dto.getScheduledTime().trim().isEmpty()) {
            notes = (notes.isEmpty() ? "" : notes + " | ") + "Scheduled Time: " + dto.getScheduledTime().trim();
        }
        if (!notes.isEmpty()) {
            donation.setNotes(notes);
        }

        return donationRepository.save(donation);
    }

    /**
     * Calculates highly demanded blood groups dynamically from active hospital blood requests in the database.
     */
    @Transactional(readOnly = true)
    public List<BloodGroupDemandResponse> getHighlyDemandedBloodGroups() {
        List<BloodRequest> activeRequests = requestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.ACTIVE);

        if (activeRequests.isEmpty()) {
            return Collections.emptyList();
        }

        java.util.Map<BloodGroup, List<BloodRequest>> grouped = activeRequests.stream()
                .filter(r -> r.getBloodGroup() != null)
                .collect(Collectors.groupingBy(BloodRequest::getBloodGroup));

        List<BloodGroupDemandResponse> demandList = new java.util.ArrayList<>();

        for (java.util.Map.Entry<BloodGroup, List<BloodRequest>> entry : grouped.entrySet()) {
            BloodGroup bg = entry.getKey();
            List<BloodRequest> reqs = entry.getValue();

            long totalUnits = reqs.stream().mapToLong(r -> r.getRequiredUnits() != null ? r.getRequiredUnits() : 1).sum();
            long count = reqs.size();

            RequestUrgency highestUrgency = reqs.stream()
                    .map(BloodRequest::getUrgency)
                    .filter(java.util.Objects::nonNull)
                    .min(java.util.Comparator.comparingInt(Enum::ordinal))
                    .orElse(RequestUrgency.HIGH);

            demandList.add(new BloodGroupDemandResponse(bg, count, totalUnits, highestUrgency));
        }

        demandList.sort(java.util.Comparator.comparing(BloodGroupDemandResponse::getHighestUrgency, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                .thenComparing(java.util.Comparator.comparingLong(BloodGroupDemandResponse::getTotalUnitsNeeded).reversed())
                .thenComparing(java.util.Comparator.comparingLong(BloodGroupDemandResponse::getActiveRequestsCount).reversed()));

        return demandList;
    }

    @Transactional
    public BloodRequestResponse updateStatus(Long id, RequestStatus status) {
        BloodRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + id));

        request.setStatus(status);
        BloodRequest saved = requestRepository.save(request);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteRequest(Long id) {
        if (!requestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Blood request not found with id: " + id);
        }
        requestRepository.deleteById(id);
    }

    public BloodRequestResponse mapToResponse(BloodRequest request) {
        BloodRequestResponse response = new BloodRequestResponse();
        response.setId(request.getId());
        if (request.getHospital() != null) {
            response.setHospitalId(request.getHospital().getId());
        }
        if (request.getRequester() != null) {
            response.setRequesterId(request.getRequester().getId());
            response.setRequesterName(request.getRequester().getName());
        }
        response.setHospitalName(request.getHospitalName());
        response.setBloodGroup(request.getBloodGroup());
        response.setRequiredUnits(request.getRequiredUnits());
        response.setHospitalLocation(request.getHospitalLocation());
        response.setUrgency(request.getUrgency());
        response.setRequiredDate(request.getRequiredDate());
        response.setContactPhone(request.getContactPhone());
        response.setNotes(request.getNotes());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt());

        // Calculate matched donors count
        if (request.getBloodGroup() != null) {
            List<BloodGroup> compatibleDonorGroups = compatibilityService.getCompatibleDonorsFor(request.getBloodGroup());
            long count = donorRepository.findByBloodGroupInAndAvailabilityStatusTrue(compatibleDonorGroups)
                    .stream()
                    .filter(eligibilityService::isEligible)
                    .count();
            response.setMatchedDonorsCount(count);
        }

        return response;
    }
}
