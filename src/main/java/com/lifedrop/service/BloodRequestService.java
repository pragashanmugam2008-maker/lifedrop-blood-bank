package com.lifedrop.service;

import com.lifedrop.dto.request.BloodRequestCreateDto;
import com.lifedrop.dto.request.DonorRequestResponseDto;
import com.lifedrop.dto.response.*;
import com.lifedrop.entity.*;
import com.lifedrop.enums.*;
import com.lifedrop.exception.BadRequestException;
import com.lifedrop.exception.ResourceNotFoundException;
import com.lifedrop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    private final DonorRequestResponseRepository donorResponseRepository;
    private final NotificationRepository notificationRepository;

    public BloodRequestService(BloodRequestRepository requestRepository,
                               UserRepository userRepository,
                               HospitalRepository hospitalRepository,
                               DonorRepository donorRepository,
                               DonationRepository donationRepository,
                               BloodCompatibilityService compatibilityService,
                               DonorEligibilityService eligibilityService,
                               DonorRequestResponseRepository donorResponseRepository,
                               NotificationRepository notificationRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.compatibilityService = compatibilityService;
        this.eligibilityService = eligibilityService;
        this.donorResponseRepository = donorResponseRepository;
        this.notificationRepository = notificationRepository;
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
        request.setStatus(RequestStatus.PENDING);

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
        return mapToResponseWithDonorResponses(request);
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getRequestsByRequester(Long requesterId) {
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
                .stream()
                .map(this::mapToResponseWithDonorResponses)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getRequestsByHospital(Long hospitalUserId) {
        Optional<Hospital> optHospital = hospitalRepository.findByUserId(hospitalUserId);
        if (optHospital.isPresent()) {
            return requestRepository.findByHospitalIdOrderByCreatedAtDesc(optHospital.get().getId())
                    .stream()
                    .map(this::mapToResponseWithDonorResponses)
                    .collect(Collectors.toList());
        }
        return getRequestsByRequester(hospitalUserId);
    }

    /**
     * Retrieves incoming hospital requests for an authenticated donor.
     * Filtered strictly to exact matching blood group (e.g., B+ -> only B+ donors).
     * Requests that this donor has REJECTED are permanently excluded.
     * Requests that this donor has ACCEPTED remain marked with ACCEPTED response status.
     */
    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getIncomingRequestsForDonor(Long donorUserId) {
        Donor donor = donorRepository.findByUserId(donorUserId).orElse(null);
        if (donor == null || donor.getBloodGroup() == null) {
            return Collections.emptyList();
        }

        BloodGroup donorGroup = donor.getBloodGroup();

        // Exact matching blood group only, in active/pending/accepted statuses
        List<RequestStatus> openStatuses = List.of(RequestStatus.PENDING, RequestStatus.ACTIVE, RequestStatus.ACCEPTED);
        List<BloodRequest> requests = requestRepository.findByBloodGroupAndStatusInOrderByCreatedAtDesc(donorGroup, openStatuses);

        // Fetch all responses this donor has already made
        List<DonorRequestResponse> myResponses = donorResponseRepository.findByDonorId(donor.getId());
        Map<Long, DonorRequestResponse> responseByRequestId = myResponses.stream()
                .collect(Collectors.toMap(r -> r.getRequest().getId(), r -> r, (r1, r2) -> r1));

        return requests.stream()
                // Filter out requests this donor has rejected
                .filter(r -> {
                    DonorRequestResponse resp = responseByRequestId.get(r.getId());
                    return resp == null || resp.getStatus() != DonorResponseStatus.REJECTED;
                })
                .map(r -> {
                    BloodRequestResponse resp = mapToResponse(r);
                    DonorRequestResponse myResp = responseByRequestId.get(r.getId());
                    if (myResp != null) {
                        resp.setDonorResponseStatus(myResp.getStatus().name());
                        resp.setRespondedAt(myResp.getRespondedAt());
                    } else {
                        resp.setDonorResponseStatus(DonorResponseStatus.PENDING.name());
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    /**
     * Hospital sends a direct donation request to a matched donor.
     */
    @Transactional
    public Donation sendRequestToDonor(Long requestId, Long donorId, Long hospitalUserId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + donorId));

        if (donor.getBloodGroup() != request.getBloodGroup()) {
            throw new BadRequestException("Incompatible blood group: Donor (" + donor.getBloodGroup().getDisplayValue() +
                    ") does not match requested blood group (" + request.getBloodGroup().getDisplayValue() + "). Exact blood group match required.");
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
     * Donor responds [ACCEPT] or [REJECT] to an incoming hospital blood request.
     * Validates exact blood group match on backend.
     * Prevents duplicate responses from the same donor.
     * Stores response separately in donor_request_responses table.
     * On ACCEPT: notifies hospital and updates blood request status to ACCEPTED.
     * On REJECT: records rejection, request remains active for other donors.
     */
    @Transactional
    public Donation respondToRequest(Long requestId, Long donorUserId, DonorRequestResponseDto dto) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        Donor donor = donorRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for user id: " + donorUserId));

        // Strict backend blood group matching validation
        if (donor.getBloodGroup() != request.getBloodGroup()) {
            throw new BadRequestException("Incompatible blood group: Donor (" + donor.getBloodGroup().getDisplayValue() +
                    ") does not match requested blood group (" + request.getBloodGroup().getDisplayValue() + "). Blood requests are sent only to exact matching blood group donors.");
        }

        // Prevent duplicate responses from the same donor
        if (donorResponseRepository.existsByRequestIdAndDonorId(requestId, donor.getId())) {
            throw new BadRequestException("You have already responded to this blood request.");
        }

        DonorResponseStatus responseStatus = dto.getStatus() != null ? dto.getStatus() : DonorResponseStatus.ACCEPTED;

        // Validate scheduling deadline if accepted
        if (responseStatus == DonorResponseStatus.ACCEPTED) {
            LocalDate scheduledDate = dto.getScheduledDate();
            if (scheduledDate != null && request.getRequiredDate() != null) {
                if (scheduledDate.isAfter(request.getRequiredDate())) {
                    throw new BadRequestException("Donation schedule must be before the request deadline.");
                }
            }
        }

        // 1. Save donor's response in separate DonorRequestResponse entity
        DonorRequestResponse responseRecord = new DonorRequestResponse(
                request,
                donor,
                responseStatus,
                dto.getScheduledDate(),
                dto.getScheduledTime(),
                dto.getNotes()
        );
        donorResponseRepository.save(responseRecord);

        // 2. Handle ACCEPTED workflow
        if (responseStatus == DonorResponseStatus.ACCEPTED) {
            // Update overall request status to ACCEPTED if it was PENDING
            if (request.getStatus() == RequestStatus.PENDING || request.getStatus() == RequestStatus.ACTIVE) {
                request.setStatus(RequestStatus.ACCEPTED);
                requestRepository.save(request);
            }

            // Create notification for hospital user
            User hospitalUser = request.getRequester();
            if (hospitalUser != null) {
                Notification notif = new Notification(
                        hospitalUser,
                        "Blood Request Accepted",
                        "A donor has accepted your " + request.getBloodGroup().getDisplayValue() + " blood request.",
                        "DONOR_ACCEPTED",
                        request.getId(),
                        donor.getUser().getName(),
                        donor.getBloodGroup().getDisplayValue(),
                        donor.getUser().getPhone(),
                        donor.getUser().getEmail()
                );
                notificationRepository.save(notif);
            }

            // Maintain donation record for history
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

            donation.setStatus(dto.getScheduledDate() != null ? DonationStatus.SCHEDULED : DonationStatus.ACCEPTED);
            donation.setDonationDate(donationDate);

            String notes = dto.getNotes() != null ? dto.getNotes() : "";
            if (dto.getScheduledTime() != null && !dto.getScheduledTime().trim().isEmpty()) {
                notes = (notes.isEmpty() ? "" : notes + " | ") + "Scheduled Time: " + dto.getScheduledTime().trim();
            }
            if (!notes.isEmpty()) {
                donation.setNotes(notes);
            }

            return donationRepository.save(donation);
        } else {
            // 3. Handle REJECTED workflow
            // Note: Request overall status remains PENDING / ACCEPTED for other donors!
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

            donation.setStatus(DonationStatus.REJECTED);
            donation.setDonationDate(LocalDate.now());
            donation.setNotes(dto.getNotes() != null ? dto.getNotes() : "Rejected by donor");

            return donationRepository.save(donation);
        }
    }

    /**
     * Responds to request and returns comprehensive DonorResponseDetailDto
     */
    @Transactional
    public DonorResponseDetailDto processDonorResponse(Long requestId, Long donorUserId, DonorRequestResponseDto dto) {
        respondToRequest(requestId, donorUserId, dto);
        Donor donor = donorRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found"));
        DonorRequestResponse response = donorResponseRepository.findByRequestIdAndDonorId(requestId, donor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Response record not found"));
        return mapToDonorResponseDetailDto(response);
    }

    /**
     * Gets all donor responses for a specific blood request created by the authenticated hospital
     */
    @Transactional(readOnly = true)
    public List<DonorResponseDetailDto> getResponsesForRequest(Long requestId, Long hospitalUserId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        validateHospitalOwnership(request, hospitalUserId);

        return donorResponseRepository.findByRequestIdOrderByRespondedAtDesc(requestId)
                .stream()
                .map(this::mapToDonorResponseDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets all donor responses for all requests created by the authenticated hospital
     */
    @Transactional(readOnly = true)
    public List<DonorResponseDetailDto> getAllResponsesForHospital(Long hospitalUserId, DonorResponseStatus status) {
        List<DonorRequestResponse> responses = (status != null) ?
                donorResponseRepository.findByHospitalUserIdAndStatusOrderByRespondedAtDesc(hospitalUserId, status) :
                donorResponseRepository.findByHospitalUserIdOrderByRespondedAtDesc(hospitalUserId);

        return responses.stream()
                .map(this::mapToDonorResponseDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets notifications for the authenticated hospital user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getHospitalNotifications(Long hospitalUserId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(hospitalUserId)
                .stream()
                .map(this::mapToNotificationDto)
                .collect(Collectors.toList());
    }

    /**
     * Marks a notification as read
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId, Long hospitalUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(hospitalUserId)) {
            throw new BadRequestException("Unauthorized access to notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marks all notifications as read for the authenticated hospital
     */
    @Transactional
    public void markAllNotificationsAsRead(Long hospitalUserId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(hospitalUserId);
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    /**
     * Marks a blood request as COMPLETED
     */
    @Transactional
    public BloodRequestResponse completeRequest(Long requestId, Long hospitalUserId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        validateHospitalOwnership(request, hospitalUserId);

        request.setStatus(RequestStatus.COMPLETED);
        BloodRequest saved = requestRepository.save(request);
        return mapToResponseWithDonorResponses(saved);
    }

    private void validateHospitalOwnership(BloodRequest request, Long hospitalUserId) {
        boolean isOwner = (request.getRequester() != null && request.getRequester().getId().equals(hospitalUserId)) ||
                (request.getHospital() != null && request.getHospital().getUser() != null && request.getHospital().getUser().getId().equals(hospitalUserId));

        if (!isOwner) {
            User user = userRepository.findById(hospitalUserId).orElse(null);
            if (user == null || user.getRole() != Role.ADMIN) {
                throw new BadRequestException("Access denied: You can only manage your own hospital blood requests.");
            }
        }
    }

    /**
     * Calculates highly demanded blood groups dynamically from active/pending hospital blood requests.
     */
    @Transactional(readOnly = true)
    public List<BloodGroupDemandResponse> getHighlyDemandedBloodGroups() {
        List<BloodRequest> activeRequests = requestRepository.findAll().stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.ACTIVE || r.getStatus() == RequestStatus.ACCEPTED)
                .collect(Collectors.toList());

        if (activeRequests.isEmpty()) {
            return Collections.emptyList();
        }

        Map<BloodGroup, List<BloodRequest>> grouped = activeRequests.stream()
                .filter(r -> r.getBloodGroup() != null)
                .collect(Collectors.groupingBy(BloodRequest::getBloodGroup));

        List<BloodGroupDemandResponse> demandList = new ArrayList<>();

        for (Map.Entry<BloodGroup, List<BloodRequest>> entry : grouped.entrySet()) {
            BloodGroup bg = entry.getKey();
            List<BloodRequest> reqs = entry.getValue();

            long totalUnits = reqs.stream().mapToLong(r -> r.getRequiredUnits() != null ? r.getRequiredUnits() : 1).sum();
            long count = reqs.size();

            RequestUrgency highestUrgency = reqs.stream()
                    .map(BloodRequest::getUrgency)
                    .filter(Objects::nonNull)
                    .min(Comparator.comparingInt(Enum::ordinal))
                    .orElse(RequestUrgency.HIGH);

            demandList.add(new BloodGroupDemandResponse(bg, count, totalUnits, highestUrgency));
        }

        demandList.sort(Comparator.comparing(BloodGroupDemandResponse::getHighestUrgency, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Comparator.comparingLong(BloodGroupDemandResponse::getTotalUnitsNeeded).reversed())
                .thenComparing(Comparator.comparingLong(BloodGroupDemandResponse::getActiveRequestsCount).reversed()));

        return demandList;
    }

    @Transactional
    public BloodRequestResponse updateStatus(Long id, RequestStatus status) {
        BloodRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + id));

        request.setStatus(status);
        BloodRequest saved = requestRepository.save(request);
        return mapToResponseWithDonorResponses(saved);
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

        // Calculate exact matching available & eligible donors count
        if (request.getBloodGroup() != null) {
            long count = donorRepository.findByBloodGroupAndAvailabilityStatusTrue(request.getBloodGroup())
                    .stream()
                    .filter(eligibilityService::isEligible)
                    .count();
            response.setMatchedDonorsCount(count);
        }

        return response;
    }

    public BloodRequestResponse mapToResponseWithDonorResponses(BloodRequest request) {
        BloodRequestResponse response = mapToResponse(request);

        List<DonorRequestResponse> responses = donorResponseRepository.findByRequestIdOrderByRespondedAtDesc(request.getId());
        List<DonorResponseDetailDto> dtos = responses.stream()
                .map(this::mapToDonorResponseDetailDto)
                .collect(Collectors.toList());

        response.setDonorResponses(dtos);
        response.setAcceptedDonorsCount((int) responses.stream().filter(r -> r.getStatus() == DonorResponseStatus.ACCEPTED).count());
        response.setRejectedDonorsCount((int) responses.stream().filter(r -> r.getStatus() == DonorResponseStatus.REJECTED).count());

        return response;
    }

    public DonorResponseDetailDto mapToDonorResponseDetailDto(DonorRequestResponse r) {
        DonorResponseDetailDto dto = new DonorResponseDetailDto();
        dto.setId(r.getId());
        dto.setRequestId(r.getRequest().getId());
        dto.setHospitalName(r.getRequest().getHospitalName());
        dto.setRequestedBloodGroup(r.getRequest().getBloodGroup());
        dto.setDonorId(r.getDonor().getId());
        dto.setDonorName(r.getDonor().getUser().getName());
        dto.setDonorBloodGroup(r.getDonor().getBloodGroup());
        dto.setDonorPhone(r.getDonor().getUser().getPhone());
        dto.setDonorEmail(r.getDonor().getUser().getEmail());
        dto.setDonorCity(r.getDonor().getCity());
        dto.setStatus(r.getStatus());
        dto.setRespondedAt(r.getRespondedAt());
        dto.setScheduledDate(r.getScheduledDate());
        dto.setScheduledTime(r.getScheduledTime());
        dto.setNotes(r.getNotes());
        return dto;
    }

    public NotificationResponseDto mapToNotificationDto(Notification n) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setRelatedRequestId(n.getRelatedRequestId());
        dto.setDonorName(n.getDonorName());
        dto.setDonorBloodGroup(n.getDonorBloodGroup());
        dto.setDonorContact(n.getDonorContact());
        dto.setDonorEmail(n.getDonorEmail());
        dto.setIsRead(n.getIsRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
