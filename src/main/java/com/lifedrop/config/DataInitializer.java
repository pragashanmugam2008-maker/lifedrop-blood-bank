package com.lifedrop.config;

import com.lifedrop.entity.*;
import com.lifedrop.enums.*;
import com.lifedrop.repository.*;
import com.lifedrop.service.DonorEligibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodInventoryRepository inventoryRepository;
    private final BloodRequestRepository requestRepository;
    private final DonationRepository donationRepository;
    private final ReferralRepository referralRepository;
    private final PasswordEncoder passwordEncoder;
    private final DonorEligibilityService eligibilityService;

    public DataInitializer(UserRepository userRepository,
                           DonorRepository donorRepository,
                           HospitalRepository hospitalRepository,
                           BloodInventoryRepository inventoryRepository,
                           BloodRequestRepository requestRepository,
                           DonationRepository donationRepository,
                           ReferralRepository referralRepository,
                           PasswordEncoder passwordEncoder,
                           DonorEligibilityService eligibilityService) {
        this.userRepository = userRepository;
        this.donorRepository = donorRepository;
        this.hospitalRepository = hospitalRepository;
        this.inventoryRepository = inventoryRepository;
        this.requestRepository = requestRepository;
        this.donationRepository = donationRepository;
        this.referralRepository = referralRepository;
        this.passwordEncoder = passwordEncoder;
        this.eligibilityService = eligibilityService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Checking and initializing LifeDrop database...");

        initBloodInventory();

        if (userRepository.count() == 0) {
            initUsersAndEntities();
            initBloodRequests();
            initDonations();
            initReferrals();
            logger.info("LifeDrop database seeded successfully with Hospital-to-Donor data!");
        } else {
            logger.info("Database already contains data, skipping seed.");
        }
    }

    private void initBloodInventory() {
        seedInventoryIfMissing(BloodGroup.O_POS, 8, 15);
        seedInventoryIfMissing(BloodGroup.O_NEG, 3, 15);
        seedInventoryIfMissing(BloodGroup.A_POS, 22, 15);
        seedInventoryIfMissing(BloodGroup.A_NEG, 12, 15);
        seedInventoryIfMissing(BloodGroup.B_POS, 18, 15);
        seedInventoryIfMissing(BloodGroup.B_NEG, 4, 15);
        seedInventoryIfMissing(BloodGroup.AB_POS, 35, 15);
        seedInventoryIfMissing(BloodGroup.AB_NEG, 6, 15);
    }

    private void seedInventoryIfMissing(BloodGroup bloodGroup, int available, int minimum) {
        if (!inventoryRepository.existsByBloodGroup(bloodGroup)) {
            BloodInventory inv = new BloodInventory(bloodGroup, available, minimum);
            inventoryRepository.save(inv);
        }
    }

    private void initUsersAndEntities() {
        // 1. Admin Account
        User admin = new User("LifeDrop Administrator", "admin@lifedrop.org",
                passwordEncoder.encode("admin123"), "+91 9876543210", Role.ADMIN);
        userRepository.save(admin);

        // 2. Hospital Accounts
        createHospital("Apollo Speciality Hospital", "apollo@lifedrop.org", "hospital123", "+91 44 28290200",
                "LIC-APOLLO-CHE-2024", "Chennai", "21 Greams Lane, Thousand Lights, Chennai",
                "+91 44 28290200", "Dr. K. Ramachandran");

        createHospital("Fortis Multi-Speciality Hospital", "fortis@lifedrop.org", "hospital123", "+91 80 66214444",
                "LIC-FORTIS-BLR-2023", "Bangalore", "154/9 Bannerghatta Road, Bangalore",
                "+91 80 66214444", "Dr. Sunita Deshmukh");

        createHospital("Lilavati Hospital & Research Centre", "lilavati@lifedrop.org", "hospital123", "+91 22 26751000",
                "LIC-LILAVATI-MUM-2022", "Mumbai", "A-791 Bandra Reclamation, Bandra West, Mumbai",
                "+91 22 26751000", "Dr. Rajesh Shah");

        // 3. Donor Accounts & Profiles
        createDonor("Rahul Kumar", "rahul@lifedrop.org", "donor123", "+91 9876500001",
                BloodGroup.O_POS, 28, "Male", "Chennai", "12 Anna Salai, Chennai",
                LocalDate.now().minusMonths(4), 8, 4.9, 40, true);

        createDonor("Ananya Sharma", "ananya@lifedrop.org", "donor123", "+91 9876500002",
                BloodGroup.A_POS, 24, "Female", "Bangalore", "45 MG Road, Bangalore",
                LocalDate.now().minusDays(45), 4, 4.8, 20, true);

        createDonor("Karthik Raj", "karthik@lifedrop.org", "donor123", "+91 9876500003",
                BloodGroup.B_POS, 32, "Male", "Mumbai", "88 Marine Drive, Mumbai",
                LocalDate.now().minusMonths(5), 12, 5.0, 80, true);

        createDonor("Sneha Reddy", "sneha@lifedrop.org", "donor123", "+91 9876500004",
                BloodGroup.O_NEG, 29, "Female", "Hyderabad", "22 Banjara Hills, Hyderabad",
                LocalDate.now().minusMonths(6), 7, 4.9, 50, true);

        createDonor("Vikram Malhotra", "vikram@lifedrop.org", "donor123", "+91 9876500005",
                BloodGroup.AB_POS, 35, "Male", "Delhi", "14 Connaught Place, New Delhi",
                LocalDate.now().minusDays(20), 2, 4.7, 10, true);

        createDonor("Deepika Sundar", "deepika@lifedrop.org", "donor123", "+91 9876500006",
                BloodGroup.B_NEG, 26, "Female", "Chennai", "7 T Nagar, Chennai",
                LocalDate.now().minusMonths(4), 5, 4.9, 30, true);

        createDonor("Arjun Nair", "arjun@lifedrop.org", "donor123", "+91 9876500007",
                BloodGroup.A_NEG, 31, "Male", "Kochi", "19 Marine Drive, Kochi",
                LocalDate.now().minusMonths(7), 9, 4.8, 60, true);

        createDonor("Meera Patel", "meera@lifedrop.org", "donor123", "+91 9876500008",
                BloodGroup.AB_NEG, 27, "Female", "Ahmedabad", "5 SG Highway, Ahmedabad",
                LocalDate.now().minusMonths(4), 3, 4.9, 20, true);
    }

    private void createHospital(String hospitalName, String email, String password, String phone,
                                String licenseNumber, String city, String address, String helpline, String contactPerson) {
        User user = new User(hospitalName, email, passwordEncoder.encode(password), phone, Role.HOSPITAL);
        User savedUser = userRepository.save(user);

        Hospital hospital = new Hospital(savedUser, hospitalName, licenseNumber, city, address, helpline, contactPerson);
        hospitalRepository.save(hospital);
    }

    private void createDonor(String name, String email, String password, String phone,
                             BloodGroup bloodGroup, int age, String gender, String city, String address,
                             LocalDate lastDonationDate, int totalDonations, double rating, int stars, boolean available) {
        User user = new User(name, email, passwordEncoder.encode(password), phone, Role.DONOR);
        User savedUser = userRepository.save(user);

        Donor donor = new Donor();
        donor.setUser(savedUser);
        donor.setBloodGroup(bloodGroup);
        donor.setAge(age);
        donor.setGender(gender);
        donor.setCity(city);
        donor.setAddress(address);
        donor.setLastDonationDate(lastDonationDate);
        donor.setTotalDonations(totalDonations);
        donor.setAvailabilityStatus(available);
        donor.setRating(rating);
        donor.setStars(stars);
        donor.setBadge(DonorBadge.calculateBadge(totalDonations));
        donor.setEligible(eligibilityService.isEligible(donor));

        donorRepository.save(donor);
    }

    private void initBloodRequests() {
        User apolloUser = userRepository.findByEmail("apollo@lifedrop.org").orElse(null);
        Hospital apolloHospital = apolloUser != null ? hospitalRepository.findByUserId(apolloUser.getId()).orElse(null) : null;

        User fortisUser = userRepository.findByEmail("fortis@lifedrop.org").orElse(null);
        Hospital fortisHospital = fortisUser != null ? hospitalRepository.findByUserId(fortisUser.getId()).orElse(null) : null;

        if (apolloUser != null && apolloHospital != null) {
            BloodRequest req1 = new BloodRequest(
                    apolloUser,
                    apolloHospital,
                    apolloHospital.getHospitalName(),
                    BloodGroup.O_NEG,
                    2,
                    apolloHospital.getCity(),
                    RequestUrgency.CRITICAL,
                    LocalDate.now().plusDays(1),
                    apolloHospital.getEmergencyHelpline()
            );
            req1.setNotes("Critical requirement for emergency cardiac surgery at Apollo Thousand Lights.");
            requestRepository.save(req1);
        }

        if (fortisUser != null && fortisHospital != null) {
            BloodRequest req2 = new BloodRequest(
                    fortisUser,
                    fortisHospital,
                    fortisHospital.getHospitalName(),
                    BloodGroup.B_POS,
                    3,
                    fortisHospital.getCity(),
                    RequestUrgency.HIGH,
                    LocalDate.now().plusDays(2),
                    fortisHospital.getEmergencyHelpline()
            );
            req2.setNotes("Scheduled orthopedic trauma procedure at Fortis Bangalore.");
            requestRepository.save(req2);
        }
    }

    private void initDonations() {
        donorRepository.findAll().forEach(donor -> {
            if (donor.getLastDonationDate() != null) {
                Donation donation = new Donation(
                        donor,
                        null,
                        null,
                        donor.getLastDonationDate(),
                        1,
                        DonationStatus.COMPLETED,
                        "Voluntary donation at LifeDrop Partner Center"
                );
                donationRepository.save(donation);
            }
        });
    }

    private void initReferrals() {
        User rahul = userRepository.findByEmail("rahul@lifedrop.org").orElse(null);
        User ananya = userRepository.findByEmail("ananya@lifedrop.org").orElse(null);
        User sneha = userRepository.findByEmail("sneha@lifedrop.org").orElse(null);

        if (rahul != null) {
            Referral r1 = new Referral(rahul, "PRAGA123");
            if (ananya != null) {
                r1.setReferredUser(ananya);
                r1.setStatus(ReferralStatus.COMPLETED);
                r1.setStarsEarned(10);
                r1.setCompletedAt(LocalDateTime.now().minusMonths(1));
            }
            referralRepository.save(r1);
        }

        if (sneha != null) {
            Referral r2 = new Referral(sneha, "LIFEDROP42");
            r2.setStatus(ReferralStatus.CREATED);
            referralRepository.save(r2);
        }
    }
}
