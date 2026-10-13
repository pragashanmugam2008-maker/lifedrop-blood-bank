package com.lifedrop.service;

import com.lifedrop.dto.request.LoginRequest;
import com.lifedrop.dto.request.RegisterRequest;
import com.lifedrop.dto.response.AuthResponse;
import com.lifedrop.dto.response.UserResponse;
import com.lifedrop.entity.Donor;
import com.lifedrop.entity.Hospital;
import com.lifedrop.entity.Referral;
import com.lifedrop.entity.User;
import com.lifedrop.enums.DonorBadge;
import com.lifedrop.enums.Role;
import com.lifedrop.exception.DuplicateResourceException;
import com.lifedrop.exception.ResourceNotFoundException;
import com.lifedrop.repository.DonorRepository;
import com.lifedrop.repository.HospitalRepository;
import com.lifedrop.repository.ReferralRepository;
import com.lifedrop.repository.UserRepository;
import com.lifedrop.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final HospitalRepository hospitalRepository;
    private final ReferralRepository referralRepository;
    private final ReferralService referralService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       DonorRepository donorRepository,
                       HospitalRepository hospitalRepository,
                       ReferralRepository referralRepository,
                       ReferralService referralService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.donorRepository = donorRepository;
        this.hospitalRepository = hospitalRepository;
        this.referralRepository = referralRepository;
        this.referralService = referralService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with email " + request.getEmail() + " already exists");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.DONOR;

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhone(),
                role
        );

        User savedUser = userRepository.save(user);

        // Process referral code if provided for donor
        if (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) {
            referralService.processReferralRegistration(request.getReferralCode().trim(), savedUser);
        }

        Long donorId = null;
        Long hospitalId = null;
        String hospitalName = null;

        if (role == Role.DONOR) {
            Donor donor = new Donor();
            donor.setUser(savedUser);
            donor.setBloodGroup(request.getBloodGroup());
            donor.setAge(request.getAge() != null ? request.getAge() : 25);
            donor.setGender(request.getGender() != null ? request.getGender() : "Not Specified");
            donor.setCity(request.getCity() != null ? request.getCity() : "Chennai");
            donor.setAddress(request.getAddress());
            donor.setTotalDonations(0);
            donor.setEligible(true);
            donor.setAvailabilityStatus(true);
            donor.setRating(5.0);
            donor.setStars(0);
            donor.setBadge(DonorBadge.NEW_DONOR);

            Donor savedDonor = donorRepository.save(donor);
            donorId = savedDonor.getId();
        } else if (role == Role.HOSPITAL) {
            String hospName = request.getHospitalName() != null && !request.getHospitalName().trim().isEmpty() ?
                    request.getHospitalName().trim() : savedUser.getName();

            Hospital hospital = new Hospital(
                    savedUser,
                    hospName,
                    request.getLicenseNumber(),
                    request.getCity() != null ? request.getCity() : "Chennai",
                    request.getAddress(),
                    request.getEmergencyHelpline() != null ? request.getEmergencyHelpline() : savedUser.getPhone(),
                    request.getContactPerson() != null ? request.getContactPerson() : savedUser.getName()
            );

            Hospital savedHospital = hospitalRepository.save(hospital);
            hospitalId = savedHospital.getId();
            hospitalName = savedHospital.getHospitalName();
        }

        // Generate user's referral code automatically
        String referralCode = null;
        try {
            referralCode = referralService.generateReferralCode(savedUser.getId()).getReferralCode();
        } catch (Exception ignored) {
        }

        String token = jwtTokenProvider.generateTokenFromUserId(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(token, savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
                savedUser.getPhone(), savedUser.getRole(), donorId, hospitalId, hospitalName, referralCode);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long donorId = donorRepository.findByUserId(user.getId()).map(Donor::getId).orElse(null);
        Hospital hospital = hospitalRepository.findByUserId(user.getId()).orElse(null);
        Long hospitalId = hospital != null ? hospital.getId() : null;
        String hospitalName = hospital != null ? hospital.getHospitalName() : null;

        String referralCode = referralRepository.findByReferrerIdOrderByCreatedAtDesc(user.getId())
                .stream().findFirst().map(Referral::getReferralCode).orElse(null);

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(),
                user.getPhone(), user.getRole(), donorId, hospitalId, hospitalName, referralCode);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Long donorId = donorRepository.findByUserId(userId).map(Donor::getId).orElse(null);
        Hospital hospital = hospitalRepository.findByUserId(userId).orElse(null);
        Long hospitalId = hospital != null ? hospital.getId() : null;
        String hospitalName = hospital != null ? hospital.getHospitalName() : null;

        String referralCode = referralRepository.findByReferrerIdOrderByCreatedAtDesc(userId)
                .stream().findFirst().map(Referral::getReferralCode).orElse(null);

        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole(), donorId, hospitalId, hospitalName, referralCode, user.getCreatedAt());
    }
}
