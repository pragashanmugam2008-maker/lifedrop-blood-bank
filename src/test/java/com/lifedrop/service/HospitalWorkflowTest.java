package com.lifedrop.service;

import com.lifedrop.dto.request.BloodRequestCreateDto;
import com.lifedrop.dto.request.DonorRequestResponseDto;
import com.lifedrop.dto.response.BloodRequestResponse;
import com.lifedrop.dto.response.DonationResponse;
import com.lifedrop.dto.response.DonorResponseDetailDto;
import com.lifedrop.dto.response.NotificationResponseDto;
import com.lifedrop.entity.*;
import com.lifedrop.enums.*;
import com.lifedrop.exception.BadRequestException;
import com.lifedrop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class HospitalWorkflowTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private DonationService donationService;

    @Autowired
    private DonorService donorService;

    @Autowired
    private BloodCompatibilityService compatibilityService;

    @Autowired
    private ReferralService referralService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User hospitalUser;
    private Hospital hospital;
    private User referrerUser;
    private User donorUser;
    private Donor donor;

    @BeforeEach
    void setUp() {
        // 1. Admin
        adminUser = new User("Admin", "testadmin@lifedrop.org", passwordEncoder.encode("pass"), "+91 9999999999", Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        // 2. Hospital
        hospitalUser = new User("City Care Hospital", "citycare@lifedrop.org", passwordEncoder.encode("pass"), "+91 44 11112222", Role.HOSPITAL);
        hospitalUser = userRepository.save(hospitalUser);
        hospital = new Hospital(hospitalUser, "City Care Hospital", "LIC-TEST-001", "Chennai", "100 Hospital Road", "+91 44 11112222", "Dr. Test");
        hospital = hospitalRepository.save(hospital);

        // 3. Referrer User (User A)
        referrerUser = new User("Referrer Donor", "referrer@lifedrop.org", passwordEncoder.encode("pass"), "+91 9888888888", Role.DONOR);
        referrerUser = userRepository.save(referrerUser);
        Donor refDonor = new Donor();
        refDonor.setUser(referrerUser);
        refDonor.setBloodGroup(BloodGroup.O_POS);
        refDonor.setAge(30);
        refDonor.setGender("Male");
        refDonor.setCity("Chennai");
        refDonor.setAvailabilityStatus(true);
        refDonor.setTotalDonations(5);
        refDonor.setStars(0);
        donorRepository.save(refDonor);

        // Generate referral code for User A
        String refCode = referralService.generateReferralCode(referrerUser.getId()).getReferralCode();

        // 4. Referred Donor (User B) registers using code
        donorUser = new User("New Hero Donor", "newhero@lifedrop.org", passwordEncoder.encode("pass"), "+91 9777777777", Role.DONOR);
        donorUser = userRepository.save(donorUser);
        referralService.processReferralRegistration(refCode, donorUser);

        donor = new Donor();
        donor.setUser(donorUser);
        donor.setBloodGroup(BloodGroup.O_POS);
        donor.setAge(24);
        donor.setCity("Chennai");
        donor.setAvailabilityStatus(true);
        donor.setTotalDonations(0);
        donor.setEligible(true);
        donor.setBadge(DonorBadge.NEW_DONOR);
        donor.setStars(0);
        donor = donorRepository.save(donor);
    }

    @Test
    @DisplayName("End-to-End Hospital-to-Donor Blood Request & Verified Donation Workflow")
    void testCompleteHospitalToDonorWorkflow() {
        // Step 1: Hospital creates Blood Request for O+ (2 units, CRITICAL)
        BloodRequestCreateDto createDto = new BloodRequestCreateDto();
        createDto.setHospitalName(hospital.getHospitalName());
        createDto.setBloodGroup(BloodGroup.O_POS);
        createDto.setRequiredUnits(2);
        createDto.setUrgency(RequestUrgency.CRITICAL);
        createDto.setHospitalLocation("Chennai");
        createDto.setRequiredDate(LocalDate.now().plusDays(1));
        createDto.setNotes("Critical emergency requirement");

        BloodRequestResponse createdRequest = bloodRequestService.createRequest(hospitalUser.getId(), createDto);
        assertNotNull(createdRequest.getId());
        assertEquals(RequestStatus.PENDING, createdRequest.getStatus());
        assertEquals("City Care Hospital", createdRequest.getHospitalName());

        // Step 2: System finds compatible donors for O+
        List<com.lifedrop.dto.response.DonorResponse> compatibleDonors = donorService.findCompatibleDonors(BloodGroup.O_POS, "Chennai");
        assertFalse(compatibleDonors.isEmpty(), "Should find compatible donor in Chennai");
        assertTrue(compatibleDonors.stream().anyMatch(d -> d.getId().equals(donor.getId())));

        // Step 3: Donor checks incoming hospital requests (Donor O+ receives O+ request)
        List<BloodRequestResponse> incomingForDonor = bloodRequestService.getIncomingRequestsForDonor(donorUser.getId());
        assertFalse(incomingForDonor.isEmpty(), "Donor should receive incoming hospital request");
        assertTrue(incomingForDonor.stream().anyMatch(r -> r.getId().equals(createdRequest.getId())));

        // Step 4: Donor ACCEPTs the hospital request
        DonorRequestResponseDto acceptDto = new DonorRequestResponseDto(DonorResponseStatus.ACCEPTED, LocalDate.now(), "Ready to donate");
        Donation acceptedDonation = bloodRequestService.respondToRequest(createdRequest.getId(), donorUser.getId(), acceptDto);
        assertEquals(DonationStatus.SCHEDULED, acceptedDonation.getStatus());

        // Blood request overall status updates to ACCEPTED
        BloodRequest updatedReq = requestRepository.findById(createdRequest.getId()).orElseThrow();
        assertEquals(RequestStatus.ACCEPTED, updatedReq.getStatus());

        // Initial blood inventory check
        int initialStock = inventoryRepository.findByBloodGroup(BloodGroup.O_POS)
                .map(BloodInventory::getAvailableUnits).orElse(0);

        // Step 5: Admin VERIFIES the completed donation
        DonationResponse verifiedDonation = donationService.updateStatus(acceptedDonation.getId(), DonationStatus.COMPLETED, adminUser);
        assertEquals(DonationStatus.COMPLETED, verifiedDonation.getStatus());

        // Step 6: Verify Blood Inventory is incremented
        int updatedStock = inventoryRepository.findByBloodGroup(BloodGroup.O_POS)
                .map(BloodInventory::getAvailableUnits).orElse(0);
        assertEquals(initialStock + 1, updatedStock, "Inventory stock must increase by donation units");

        // Step 7: Verify Hospital Blood Request is marked FULFILLED
        BloodRequest fulfilledRequest = requestRepository.findById(createdRequest.getId()).orElseThrow();
        assertEquals(RequestStatus.FULFILLED, fulfilledRequest.getStatus());

        // Step 8: Verify Donor total donations and badge recalculation
        Donor updatedDonor = donorRepository.findById(donor.getId()).orElseThrow();
        assertEquals(1, updatedDonor.getTotalDonations());
        assertEquals(DonorBadge.NEW_DONOR, updatedDonor.getBadge());

        // Step 9: Verify Referral Completion: Referrer User A receives +10 stars!
        List<Referral> referrals = referralRepository.findByReferrerIdOrderByCreatedAtDesc(referrerUser.getId());
        assertFalse(referrals.isEmpty());
        Referral refRecord = referrals.get(0);
        assertEquals(ReferralStatus.COMPLETED, refRecord.getStatus());
        assertEquals(10, refRecord.getStarsEarned());

        Donor updatedReferrerDonor = donorRepository.findByUserId(referrerUser.getId()).orElseThrow();
        assertEquals(10, updatedReferrerDonor.getStars(), "Referrer should receive 10 stars after verified donation");
    }

    private User createTestDonorUser(String name, String email, BloodGroup bloodGroup) {
        User u = new User(name, email, passwordEncoder.encode("pass"), "+91 9111111111", Role.DONOR);
        u = userRepository.save(u);
        Donor d = new Donor();
        d.setUser(u);
        d.setBloodGroup(bloodGroup);
        d.setAge(28);
        d.setCity("Chennai");
        d.setAvailabilityStatus(true);
        d.setTotalDonations(0);
        d.setEligible(true);
        donorRepository.save(d);
        return u;
    }

    private BloodRequestResponse createTestBloodRequest(BloodGroup bloodGroup, String notes) {
        BloodRequestCreateDto dto = new BloodRequestCreateDto();
        dto.setHospitalName(hospital.getHospitalName());
        dto.setBloodGroup(bloodGroup);
        dto.setRequiredUnits(1);
        dto.setUrgency(RequestUrgency.HIGH);
        dto.setHospitalLocation("Chennai");
        dto.setRequiredDate(LocalDate.now().plusDays(2));
        dto.setNotes(notes);
        return bloodRequestService.createRequest(hospitalUser.getId(), dto);
    }

    @Test
    @DisplayName("COMPLETE SCENARIOS 1-15: Blood Request & Donor Response Enhancement Workflow")
    void testEnhanced15ScenariosWorkflow() {
        // Scenario 1: Register a B+ donor (Donor 1)
        User bPosUser1 = createTestDonorUser("B+ Donor One", "bpos1@test.org", BloodGroup.B_POS);

        // Scenario 2: Register an O+ donor
        User oPosUser = createTestDonorUser("O+ Donor User", "opos_user@test.org", BloodGroup.O_POS);

        // Scenario 3: Register a hospital (Done in setUp as hospitalUser / hospital)
        assertNotNull(hospital);

        // Scenario 4: Hospital creates a B+ blood request
        BloodRequestResponse bPosRequest = createTestBloodRequest(BloodGroup.B_POS, "Urgent B+ requirement");
        assertNotNull(bPosRequest.getId());
        assertEquals(RequestStatus.PENDING, bPosRequest.getStatus());

        // Scenario 5: Verify only B+ donors receive the request
        List<BloodRequestResponse> bDonor1Requests = bloodRequestService.getIncomingRequestsForDonor(bPosUser1.getId());
        assertTrue(bDonor1Requests.stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())),
                "B+ donor MUST receive the B+ blood request");

        List<BloodRequestResponse> oDonorRequests = bloodRequestService.getIncomingRequestsForDonor(oPosUser.getId());
        assertFalse(oDonorRequests.stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())),
                "O+ donor must NOT receive the B+ blood request");

        // Scenario 6: B+ donor 1 rejects the request
        DonorRequestResponseDto rejectDto = new DonorRequestResponseDto(DonorResponseStatus.REJECTED, null, "Unable to donate at this time");
        Donation rejectResult = bloodRequestService.respondToRequest(bPosRequest.getId(), bPosUser1.getId(), rejectDto);
        assertEquals(DonationStatus.REJECTED, rejectResult.getStatus());

        // Scenario 7: Verify request disappears from that donor's dashboard
        List<BloodRequestResponse> bDonor1AfterReject = bloodRequestService.getIncomingRequestsForDonor(bPosUser1.getId());
        assertFalse(bDonor1AfterReject.stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())),
                "Request MUST disappear from donor 1's dashboard immediately after rejection");

        // Scenario 8: Refresh the page (re-query) and verify the rejected request does not reappear
        List<BloodRequestResponse> bDonor1Refreshed = bloodRequestService.getIncomingRequestsForDonor(bPosUser1.getId());
        assertFalse(bDonor1Refreshed.stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())),
                "Rejected request must NOT reappear after refresh");

        // Scenario 9: Verify the request is still visible to the hospital
        List<BloodRequestResponse> hospRequests = bloodRequestService.getRequestsByHospital(hospitalUser.getId());
        assertTrue(hospRequests.stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())),
                "Hospital MUST still see the blood request after donor rejection");

        // Scenario 10: Verify other matching B+ donors can still receive the request
        User bPosUser2 = createTestDonorUser("B+ Donor Two", "bpos2@test.org", BloodGroup.B_POS);
        List<BloodRequestResponse> bDonor2Requests = bloodRequestService.getIncomingRequestsForDonor(bPosUser2.getId());
        assertTrue(bDonor2Requests.stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())),
                "Other B+ donors MUST still receive the request");

        // Scenario 11: Another B+ donor (Donor 2) accepts the request
        DonorRequestResponseDto acceptDto = new DonorRequestResponseDto(DonorResponseStatus.ACCEPTED, LocalDate.now().plusDays(1), "10:00 AM", "Happy to donate!");
        Donation acceptResult = bloodRequestService.respondToRequest(bPosRequest.getId(), bPosUser2.getId(), acceptDto);
        assertEquals(DonationStatus.SCHEDULED, acceptResult.getStatus());

        // Verify request overall status updated to ACCEPTED
        BloodRequest updatedReq = requestRepository.findById(bPosRequest.getId()).orElseThrow();
        assertEquals(RequestStatus.ACCEPTED, updatedReq.getStatus());

        // Scenario 12: Verify hospital receives the acceptance notification
        List<NotificationResponseDto> notifications = bloodRequestService.getHospitalNotifications(hospitalUser.getId());
        assertFalse(notifications.isEmpty(), "Hospital must receive notifications");
        NotificationResponseDto acceptanceNotification = notifications.stream()
                .filter(n -> n.getRelatedRequestId().equals(bPosRequest.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(acceptanceNotification, "Hospital must receive notification for this request");
        assertTrue(acceptanceNotification.getMessage().contains("B+ blood request"));
        assertEquals("B+ Donor Two", acceptanceNotification.getDonorName());

        // Scenario 13: Verify hospital dashboard displays the accepted donor's details
        List<DonorResponseDetailDto> responses = bloodRequestService.getResponsesForRequest(bPosRequest.getId(), hospitalUser.getId());
        assertEquals(2, responses.size(), "Hospital should see both donor responses (rejected and accepted)");

        DonorResponseDetailDto acceptedDetail = responses.stream()
                .filter(r -> r.getStatus() == DonorResponseStatus.ACCEPTED)
                .findFirst()
                .orElse(null);
        assertNotNull(acceptedDetail);
        assertEquals("B+ Donor Two", acceptedDetail.getDonorName());
        assertEquals(BloodGroup.B_POS, acceptedDetail.getDonorBloodGroup());
        assertEquals("+91 9111111111", acceptedDetail.getDonorPhone());
        assertNotNull(acceptedDetail.getRespondedAt());
        assertEquals(DonorResponseStatus.ACCEPTED, acceptedDetail.getStatus());

        DonorResponseDetailDto rejectedDetail = responses.stream()
                .filter(r -> r.getStatus() == DonorResponseStatus.REJECTED)
                .findFirst()
                .orElse(null);
        assertNotNull(rejectedDetail);
        assertEquals("B+ Donor One", rejectedDetail.getDonorName());
        assertEquals(DonorResponseStatus.REJECTED, rejectedDetail.getStatus());

        // Scenario 14: Verify O+ donors cannot receive or accept the B+ request
        assertFalse(bloodRequestService.getIncomingRequestsForDonor(oPosUser.getId()).stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())));
        DonorRequestResponseDto oPosAcceptDto = new DonorRequestResponseDto(DonorResponseStatus.ACCEPTED, LocalDate.now(), "Attempting incompatible accept");
        BadRequestException mismatchEx = assertThrows(BadRequestException.class, () ->
                bloodRequestService.respondToRequest(bPosRequest.getId(), oPosUser.getId(), oPosAcceptDto));
        assertTrue(mismatchEx.getMessage().contains("Incompatible blood group"));

        // Scenario 15: Test duplicate responses and unauthorized access
        // Duplicate response from B+ donor 2:
        BadRequestException dupEx = assertThrows(BadRequestException.class, () ->
                bloodRequestService.respondToRequest(bPosRequest.getId(), bPosUser2.getId(), acceptDto));
        assertTrue(dupEx.getMessage().contains("already responded"));

        // Unauthorized access: Hospital B cannot access Hospital A's request responses
        User hospitalUserB = new User("Other Hospital", "otherhosp@lifedrop.org", passwordEncoder.encode("pass"), "+91 44 99998888", Role.HOSPITAL);
        hospitalUserB = userRepository.save(hospitalUserB);
        final Long unauthorizedUserId = hospitalUserB.getId();
        assertThrows(BadRequestException.class, () ->
                bloodRequestService.getResponsesForRequest(bPosRequest.getId(), unauthorizedUserId));
    }

    @Test
    @DisplayName("Biological Compatibility Matrix: canDonateTo verifies correctly")
    void testCompatibilityMatrix() {
        assertFalse(compatibilityService.canDonateTo(BloodGroup.A_POS, BloodGroup.B_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_NEG, BloodGroup.AB_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.B_POS));
    }

    @Test
    @DisplayName("Hospital creates B+ request -> Only B+ donors receive it, A+ donor does NOT")
    void testExactBloodGroupDistribution() {
        User bPosDonor = createTestDonorUser("B+ Exact", "bexact@test.org", BloodGroup.B_POS);
        User aPosDonor = createTestDonorUser("A+ Incompatible", "aincomp@test.org", BloodGroup.A_POS);
        BloodRequestResponse bReq = createTestBloodRequest(BloodGroup.B_POS, "Exact B+ request");

        List<BloodRequestResponse> bIncoming = bloodRequestService.getIncomingRequestsForDonor(bPosDonor.getId());
        assertTrue(bIncoming.stream().anyMatch(r -> r.getId().equals(bReq.getId())));

        List<BloodRequestResponse> aIncoming = bloodRequestService.getIncomingRequestsForDonor(aPosDonor.getId());
        assertFalse(aIncoming.stream().anyMatch(r -> r.getId().equals(bReq.getId())));
    }

    @Test
    @DisplayName("Donor tries to schedule before deadline -> Allowed")
    void testDonorScheduleBeforeDeadline_Allowed() {
        User bPosDonor = createTestDonorUser("B+ Donor Schedule", "bsched@test.org", BloodGroup.B_POS);
        LocalDate deadline = LocalDate.now().plusDays(10);
        BloodRequestCreateDto dto = new BloodRequestCreateDto();
        dto.setHospitalName(hospital.getHospitalName());
        dto.setBloodGroup(BloodGroup.B_POS);
        dto.setRequiredUnits(2);
        dto.setUrgency(RequestUrgency.HIGH);
        dto.setHospitalLocation("Chennai");
        dto.setRequiredDate(deadline);
        dto.setNotes("Deadline test");
        BloodRequestResponse req = bloodRequestService.createRequest(hospitalUser.getId(), dto);

        LocalDate scheduledDate = LocalDate.now().plusDays(5);
        DonorRequestResponseDto responseDto = new DonorRequestResponseDto(DonorResponseStatus.ACCEPTED, scheduledDate, "10:30 AM", "Scheduled before deadline");

        Donation donation = bloodRequestService.respondToRequest(req.getId(), bPosDonor.getId(), responseDto);
        assertNotNull(donation);
        assertEquals(DonationStatus.SCHEDULED, donation.getStatus());
        assertEquals(scheduledDate, donation.getDonationDate());
    }

    @Test
    @DisplayName("Donor tries to schedule after deadline -> Rejected")
    void testDonorScheduleAfterDeadline_Rejected() {
        User bPosDonor = createTestDonorUser("B+ Donor Late", "blate@test.org", BloodGroup.B_POS);
        LocalDate deadline = LocalDate.now().plusDays(3);
        BloodRequestCreateDto dto = new BloodRequestCreateDto();
        dto.setHospitalName(hospital.getHospitalName());
        dto.setBloodGroup(BloodGroup.B_POS);
        dto.setRequiredUnits(1);
        dto.setUrgency(RequestUrgency.HIGH);
        dto.setHospitalLocation("Chennai");
        dto.setRequiredDate(deadline);
        dto.setNotes("Deadline test 2");
        BloodRequestResponse req = bloodRequestService.createRequest(hospitalUser.getId(), dto);

        LocalDate scheduledDate = LocalDate.now().plusDays(5);
        DonorRequestResponseDto responseDto = new DonorRequestResponseDto(DonorResponseStatus.ACCEPTED, scheduledDate, "11:00 AM", "Attempting schedule after deadline");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                bloodRequestService.respondToRequest(req.getId(), bPosDonor.getId(), responseDto));

        assertEquals("Donation schedule must be before the request deadline.", ex.getMessage());
    }

    @Test
    @DisplayName("Hospital completes request -> Status is COMPLETED")
    void testCompleteRequest() {
        BloodRequestResponse req = createTestBloodRequest(BloodGroup.B_POS, "To complete");
        BloodRequestResponse completed = bloodRequestService.completeRequest(req.getId(), hospitalUser.getId());
        assertEquals(RequestStatus.COMPLETED, completed.getStatus());
    }
}
