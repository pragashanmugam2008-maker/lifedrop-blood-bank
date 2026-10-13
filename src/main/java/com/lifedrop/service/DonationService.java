package com.lifedrop.service;

import com.lifedrop.dto.request.DonationCreateDto;
import com.lifedrop.dto.response.DonationResponse;
import com.lifedrop.entity.BloodRequest;
import com.lifedrop.entity.Donation;
import com.lifedrop.entity.Donor;
import com.lifedrop.entity.Hospital;
import com.lifedrop.entity.User;
import com.lifedrop.enums.DonationStatus;
import com.lifedrop.enums.DonorBadge;
import com.lifedrop.enums.RequestStatus;
import com.lifedrop.exception.BadRequestException;
import com.lifedrop.exception.ResourceNotFoundException;
import com.lifedrop.repository.BloodRequestRepository;
import com.lifedrop.repository.DonationRepository;
import com.lifedrop.repository.DonorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DonationService {

    private final DonationRepository donationRepository;
    private final DonorRepository donorRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final InventoryService inventoryService;
    private final ReferralService referralService;
    private final DonorEligibilityService eligibilityService;
    private final BloodCompatibilityService compatibilityService;

    public DonationService(DonationRepository donationRepository,
                           DonorRepository donorRepository,
                           BloodRequestRepository bloodRequestRepository,
                           InventoryService inventoryService,
                           ReferralService referralService,
                           DonorEligibilityService eligibilityService,
                           BloodCompatibilityService compatibilityService) {
        this.donationRepository = donationRepository;
        this.donorRepository = donorRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.inventoryService = inventoryService;
        this.referralService = referralService;
        this.eligibilityService = eligibilityService;
        this.compatibilityService = compatibilityService;
    }

    @Transactional
    public DonationResponse recordDonation(DonationCreateDto dto, User verifiedBy) {
        Donor donor = donorRepository.findById(dto.getDonorId())
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + dto.getDonorId()));

        BloodRequest request = null;
        Hospital hospital = null;
        if (dto.getRequestId() != null) {
            request = bloodRequestRepository.findById(dto.getRequestId()).orElse(null);
            if (request != null) {
                if (!compatibilityService.canDonateTo(donor.getBloodGroup(), request.getBloodGroup())) {
                    throw new BadRequestException("Incompatible blood group: Donor (" +
                            donor.getBloodGroup().getDisplayValue() + ") cannot donate to requested blood group (" +
                            request.getBloodGroup().getDisplayValue() + ")");
                }
                hospital = request.getHospital();
            }
        }

        LocalDate donationDate = dto.getDonationDate() != null ? dto.getDonationDate() : LocalDate.now();
        int units = dto.getUnits() != null ? dto.getUnits() : 1;

        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setRequest(request);
        donation.setHospital(hospital);
        donation.setDonationDate(donationDate);
        donation.setUnits(units);
        donation.setStatus(DonationStatus.COMPLETED);
        donation.setNotes(dto.getNotes());
        donation.setVerifiedBy(verifiedBy);

        Donation saved = donationRepository.save(donation);

        processVerifiedDonation(donor, request, donationDate, units);

        return mapToResponse(saved);
    }

    @Transactional
    public void processVerifiedDonation(Donor donor, BloodRequest request, LocalDate donationDate, int units) {
        // 1. Update donor metrics
        donor.setLastDonationDate(donationDate);
        donor.setTotalDonations((donor.getTotalDonations() != null ? donor.getTotalDonations() : 0) + units);
        donor.setBadge(DonorBadge.calculateBadge(donor.getTotalDonations()));
        donor.setEligible(eligibilityService.isEligible(donor));
        donorRepository.save(donor);

        // 2. Add units to Blood Inventory
        inventoryService.addStock(donor.getBloodGroup(), units);

        // 3. If linked to blood request, update status to FULFILLED
        if (request != null && request.getStatus() == RequestStatus.ACTIVE) {
            request.setStatus(RequestStatus.FULFILLED);
            bloodRequestRepository.save(request);
        }

        // 4. Trigger referral completion reward if applicable (+10 stars to referrer)
        if (donor.getUser() != null) {
            referralService.completeReferralOnDonation(donor.getUser().getId());
        }
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getAllDonations() {
        return donationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getDonationsByDonor(Long donorId) {
        return donationRepository.findByDonorIdOrderByDonationDateDesc(donorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getDonationsByHospital(Long hospitalId) {
        return donationRepository.findByHospitalIdOrderByDonationDateDesc(hospitalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DonationResponse updateStatus(Long id, DonationStatus status, User verifiedBy) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation record not found with id: " + id));

        DonationStatus oldStatus = donation.getStatus();
        donation.setStatus(status);
        if (verifiedBy != null) {
            donation.setVerifiedBy(verifiedBy);
        }

        Donation saved = donationRepository.save(donation);

        if (oldStatus != DonationStatus.COMPLETED && status == DonationStatus.COMPLETED) {
            processVerifiedDonation(donation.getDonor(), donation.getRequest(), donation.getDonationDate(), donation.getUnits());
        }

        return mapToResponse(saved);
    }

    public DonationResponse mapToResponse(Donation donation) {
        DonationResponse response = new DonationResponse();
        response.setId(donation.getId());
        if (donation.getDonor() != null) {
            response.setDonorId(donation.getDonor().getId());
            if (donation.getDonor().getUser() != null) {
                response.setDonorName(donation.getDonor().getUser().getName());
            }
            response.setBloodGroup(donation.getDonor().getBloodGroup());
        }
        if (donation.getRequest() != null) {
            response.setRequestId(donation.getRequest().getId());
            response.setHospitalName(donation.getRequest().getHospitalName());
            response.setHospitalLocation(donation.getRequest().getHospitalLocation());
            response.setUrgency(donation.getRequest().getUrgency());
        } else if (donation.getHospital() != null) {
            response.setHospitalId(donation.getHospital().getId());
            response.setHospitalName(donation.getHospital().getHospitalName());
            response.setHospitalLocation(donation.getHospital().getCity());
        }
        response.setDonationDate(donation.getDonationDate());
        response.setUnits(donation.getUnits());
        response.setStatus(donation.getStatus());
        response.setNotes(donation.getNotes());
        if (donation.getVerifiedBy() != null) {
            response.setVerifiedByName(donation.getVerifiedBy().getName());
        }
        response.setCreatedAt(donation.getCreatedAt());
        return response;
    }
}
