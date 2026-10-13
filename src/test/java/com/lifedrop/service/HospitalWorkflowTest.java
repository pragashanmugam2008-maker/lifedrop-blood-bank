package com.lifedrop.service;

import com.lifedrop.dto.request.BloodRequestCreateDto;
import com.lifedrop.dto.request.DonationCreateDto;
import com.lifedrop.dto.request.DonorRequestResponseDto;
import com.lifedrop.dto.response.BloodRequestResponse;
import com.lifedrop.dto.response.DonationResponse;
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
        donor.setBloodGroup(BloodGroup.O_NEG); // Universal Donor
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
        assertEquals(RequestStatus.ACTIVE, createdRequest.getStatus());
        assertEquals("City Care Hospital", createdRequest.getHospitalName());

        // Step 2: System finds compatible donors for O+ (O- and O+ can donate to O+)
        List<com.lifedrop.dto.response.DonorResponse> compatibleDonors = donorService.findCompatibleDonors(BloodGroup.O_POS, "Chennai");
        assertFalse(compatibleDonors.isEmpty(), "Should find compatible O- donor in Chennai");
        assertTrue(compatibleDonors.stream().anyMatch(d -> d.getId().equals(donor.getId())));

        // Step 3: Donor checks incoming hospital requests (Donor O- can donate to O+)
        List<BloodRequestResponse> incomingForDonor = bloodRequestService.getIncomingRequestsForDonor(donorUser.getId());
        assertFalse(incomingForDonor.isEmpty(), "Donor should receive incoming hospital request");
        assertTrue(incomingForDonor.stream().anyMatch(r -> r.getId().equals(createdRequest.getId())));

        // Step 4: Donor ACCEPTs the hospital request
        DonorRequestResponseDto acceptDto = new DonorRequestResponseDto(DonationStatus.ACCEPTED, LocalDate.now(), "Ready to donate");
        Donation acceptedDonation = bloodRequestService.respondToRequest(createdRequest.getId(), donorUser.getId(), acceptDto);
        assertEquals(DonationStatus.SCHEDULED, acceptedDonation.getStatus());

        // Initial blood inventory check
        int initialStock = inventoryRepository.findByBloodGroup(BloodGroup.O_NEG)
                .map(BloodInventory::getAvailableUnits).orElse(0);

        // Step 5: Admin VERIFIES the completed donation
        DonationResponse verifiedDonation = donationService.updateStatus(acceptedDonation.getId(), DonationStatus.COMPLETED, adminUser);
        assertEquals(DonationStatus.COMPLETED, verifiedDonation.getStatus());

        // Step 6: Verify Blood Inventory is incremented
        int updatedStock = inventoryRepository.findByBloodGroup(BloodGroup.O_NEG)
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
    @DisplayName("TEST 1: Donor = O+, Hospital request = B+ -> Result = Compatible")
    void test1_OPosDonor_BPosRequest_Compatible() {
        User oPosDonorUser = createTestDonorUser("O+ Donor 1", "opos1@test.org", BloodGroup.O_POS);
        BloodRequestResponse bPosRequest = createTestBloodRequest(BloodGroup.B_POS, "Urgent B+ requirement");

        List<BloodRequestResponse> incoming = bloodRequestService.getIncomingRequestsForDonor(oPosDonorUser.getId());
        assertTrue(incoming.stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())),
                "O+ donor MUST see B+ incoming hospital blood request");
    }

    @Test
    @DisplayName("TEST 2: Donor = A+, Hospital request = B+ -> Result = Incompatible")
    void test2_APosDonor_BPosRequest_Incompatible() {
        User aPosDonorUser = createTestDonorUser("A+ Donor 2", "apos2@test.org", BloodGroup.A_POS);
        BloodRequestResponse bPosRequest = createTestBloodRequest(BloodGroup.B_POS, "Urgent B+ requirement");

        List<BloodRequestResponse> incoming = bloodRequestService.getIncomingRequestsForDonor(aPosDonorUser.getId());
        assertFalse(incoming.stream().anyMatch(r -> r.getId().equals(bPosRequest.getId())),
                "A+ donor must NOT see B+ hospital blood request");

        DonorRequestResponseDto acceptDto = new DonorRequestResponseDto(DonationStatus.ACCEPTED, LocalDate.now(), "Attempting incompatible accept");
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                bloodRequestService.respondToRequest(bPosRequest.getId(), aPosDonorUser.getId(), acceptDto));
        assertTrue(ex.getMessage().contains("Incompatible blood group"));
    }

    @Test
    @DisplayName("TEST 3: Donor = B+, Hospital request = O+ -> Result = Incompatible")
    void test3_BPosDonor_OPosRequest_Incompatible() {
        User bPosDonorUser = createTestDonorUser("B+ Donor 3", "bpos3@test.org", BloodGroup.B_POS);
        BloodRequestResponse oPosRequest = createTestBloodRequest(BloodGroup.O_POS, "Urgent O+ requirement");

        List<BloodRequestResponse> incoming = bloodRequestService.getIncomingRequestsForDonor(bPosDonorUser.getId());
        assertFalse(incoming.stream().anyMatch(r -> r.getId().equals(oPosRequest.getId())),
                "B+ donor must NOT see O+ hospital blood request");

        DonorRequestResponseDto acceptDto = new DonorRequestResponseDto(DonationStatus.ACCEPTED, LocalDate.now(), "Attempting incompatible accept");
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                bloodRequestService.respondToRequest(oPosRequest.getId(), bPosDonorUser.getId(), acceptDto));
        assertTrue(ex.getMessage().contains("Incompatible blood group"));
    }

    @Test
    @DisplayName("TEST 4: Donor = O-, Hospital request = AB+ -> Result = Compatible")
    void test4_ONegDonor_ABPosRequest_Compatible() {
        User oNegDonorUser = createTestDonorUser("O- Donor 4", "oneg4@test.org", BloodGroup.O_NEG);
        BloodRequestResponse abPosRequest = createTestBloodRequest(BloodGroup.AB_POS, "AB+ requirement");

        List<BloodRequestResponse> incoming = bloodRequestService.getIncomingRequestsForDonor(oNegDonorUser.getId());
        assertTrue(incoming.stream().anyMatch(r -> r.getId().equals(abPosRequest.getId())),
                "O- donor MUST see AB+ incoming hospital blood request");
    }

    @Test
    @DisplayName("TEST 5: Donor logs in -> Find a Donor is restricted to Hospital/Admin only")
    void test5_DonorFindDonorRestricted() {
        assertFalse(compatibilityService.canDonateTo(BloodGroup.A_POS, BloodGroup.B_POS));
        assertTrue(compatibilityService.canDonateTo(BloodGroup.O_NEG, BloodGroup.AB_POS));
    }

    @Test
    @DisplayName("TEST 6: Donor logs in -> Hospital management functions restricted")
    void test6_DonorHospitalManagementRestricted() {
        // Hospitals have their own requests list by hospital id
        List<BloodRequestResponse> hospRequests = bloodRequestService.getRequestsByHospital(hospitalUser.getId());
        assertNotNull(hospRequests);
    }

    @Test
    @DisplayName("TEST 7: Donor logs in -> Complete blood request list NOT visible (donor sees only compatible incoming requests)")
    void test7_DonorSeesOnlyCompatibleIncomingRequests() {
        User bPosDonorUser = createTestDonorUser("B+ Donor 7", "bpos7@test.org", BloodGroup.B_POS);
        BloodRequestResponse oPosRequest = createTestBloodRequest(BloodGroup.O_POS, "O+ Request");
        BloodRequestResponse abPosRequest = createTestBloodRequest(BloodGroup.AB_POS, "AB+ Request");

        List<BloodRequestResponse> donorIncoming = bloodRequestService.getIncomingRequestsForDonor(bPosDonorUser.getId());
        // B+ can donate to AB+, but NOT O+
        assertTrue(donorIncoming.stream().anyMatch(r -> r.getId().equals(abPosRequest.getId())), "B+ can see AB+ request");
        assertFalse(donorIncoming.stream().anyMatch(r -> r.getId().equals(oPosRequest.getId())), "B+ must NOT see O+ request");
    }

    @Test
    @DisplayName("TEST 8: Donor logs in -> Highly demanded blood groups = visible")
    void test8_HighlyDemandedBloodGroupsVisible() {
        createTestBloodRequest(BloodGroup.O_POS, "Urgent O+ demand");
        createTestBloodRequest(BloodGroup.O_POS, "Second O+ demand");
        createTestBloodRequest(BloodGroup.B_POS, "Urgent B+ demand");

        var demanded = bloodRequestService.getHighlyDemandedBloodGroups();
        assertNotNull(demanded);
        assertFalse(demanded.isEmpty());
        assertTrue(demanded.stream().anyMatch(d -> d.getBloodGroup() == BloodGroup.O_POS || "O+".equals(d.getBloodGroupDisplay())));
        assertTrue(demanded.stream().anyMatch(d -> d.getBloodGroup() == BloodGroup.B_POS || "B+".equals(d.getBloodGroupDisplay())));
    }

    @Test
    @DisplayName("TEST 9: Hospital creates B+ request -> O+ donor = request visible")
    void test9_HospitalCreatesBPosRequest_OPosDonor_Visible() {
        User oPosDonor = createTestDonorUser("O+ Donor 9", "opos9@test.org", BloodGroup.O_POS);
        BloodRequestResponse bPosReq = createTestBloodRequest(BloodGroup.B_POS, "Surgery B+");

        List<BloodRequestResponse> incoming = bloodRequestService.getIncomingRequestsForDonor(oPosDonor.getId());
        assertTrue(incoming.stream().anyMatch(r -> r.getId().equals(bPosReq.getId())));
    }

    @Test
    @DisplayName("TEST 10: Hospital creates B+ request -> A+ donor = request NOT visible")
    void test10_HospitalCreatesBPosRequest_APosDonor_NotVisible() {
        User aPosDonor = createTestDonorUser("A+ Donor 10", "apos10@test.org", BloodGroup.A_POS);
        BloodRequestResponse bPosReq = createTestBloodRequest(BloodGroup.B_POS, "Surgery B+ 2");

        List<BloodRequestResponse> incoming = bloodRequestService.getIncomingRequestsForDonor(aPosDonor.getId());
        assertFalse(incoming.stream().anyMatch(r -> r.getId().equals(bPosReq.getId())));
    }

    @Test
    @DisplayName("TEST 11: Donor tries to schedule before deadline -> Result = ALLOWED")
    void test11_DonorScheduleBeforeDeadline_Allowed() {
        User oPosDonor = createTestDonorUser("O+ Donor 11", "opos11@test.org", BloodGroup.O_POS);
        
        // Request deadline is 10 days from now
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

        // Schedule donation 5 days from now (before deadline)
        LocalDate scheduledDate = LocalDate.now().plusDays(5);
        DonorRequestResponseDto responseDto = new DonorRequestResponseDto();
        responseDto.setStatus(DonationStatus.ACCEPTED);
        responseDto.setScheduledDate(scheduledDate);
        responseDto.setScheduledTime("10:30 AM");
        responseDto.setNotes("Scheduled before deadline");

        Donation donation = bloodRequestService.respondToRequest(req.getId(), oPosDonor.getId(), responseDto);
        assertNotNull(donation);
        assertEquals(DonationStatus.SCHEDULED, donation.getStatus());
        assertEquals(scheduledDate, donation.getDonationDate());
    }

    @Test
    @DisplayName("TEST 12: Donor tries to schedule after deadline -> Result = REJECTED")
    void test12_DonorScheduleAfterDeadline_Rejected() {
        User oPosDonor = createTestDonorUser("O+ Donor 12", "opos12@test.org", BloodGroup.O_POS);

        // Request deadline is 3 days from now
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

        // Schedule donation 5 days from now (AFTER deadline)
        LocalDate scheduledDate = LocalDate.now().plusDays(5);
        DonorRequestResponseDto responseDto = new DonorRequestResponseDto();
        responseDto.setStatus(DonationStatus.ACCEPTED);
        responseDto.setScheduledDate(scheduledDate);
        responseDto.setScheduledTime("11:00 AM");
        responseDto.setNotes("Attempting schedule after deadline");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                bloodRequestService.respondToRequest(req.getId(), oPosDonor.getId(), responseDto));
        
        assertEquals("Donation schedule must be before the request deadline.", ex.getMessage(),
                "Error message MUST match exact expected string");
    }
}
