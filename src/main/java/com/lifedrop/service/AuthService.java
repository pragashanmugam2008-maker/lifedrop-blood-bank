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
import com.lifedrop.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

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
        return register(request, UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String correlationId) {
        long totalStartTime = System.currentTimeMillis();
        String safeEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 0 - Incoming registration request for email: [{}], role: [{}]",
                correlationId, safeEmail, request.getRole());

        // Stage 1: Email uniqueness check
        long tCheckStart = System.currentTimeMillis();
        if (userRepository.existsByEmail(safeEmail)) {
            long checkDuration = System.currentTimeMillis() - tCheckStart;
            logger.warn("[LifeDrop Auth][CorrelationID: {}] Stage 1 - Duplicate email check FAILED in {} ms: [{}]",
                    correlationId, checkDuration, safeEmail);
            throw new DuplicateResourceException("An account with email " + safeEmail + " already exists");
        }
        long checkDuration = System.currentTimeMillis() - tCheckStart;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 1 - Email uniqueness confirmed in {} ms", correlationId, checkDuration);

        // Stage 2: Password hashing & user creation
        long tHashStart = System.currentTimeMillis();
        Role role = request.getRole() != null ? request.getRole() : Role.DONOR;
        User user = new User(
                request.getName(),
                safeEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getPhone(),
                role
        );
        User savedUser = userRepository.save(user);
        long hashDuration = System.currentTimeMillis() - tHashStart;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 2 - Password hashed and user saved in {} ms (User ID: {})",
                correlationId, hashDuration, savedUser.getId());

        // Stage 3: Process referral if applicable
        long tRefStart = System.currentTimeMillis();
        if (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) {
            referralService.processReferralRegistration(request.getReferralCode().trim(), savedUser);
        }

        // Stage 4: Create role-specific entity
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

        // Generate user's referral code automatically for donors
        String referralCode = null;
        if (role == Role.DONOR) {
            try {
                referralCode = referralService.generateReferralCodeForUser(savedUser).getReferralCode();
            } catch (Exception ignored) {
            }
        }
        long roleDuration = System.currentTimeMillis() - tRefStart;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 4 - Role entity profile created in {} ms",
                correlationId, roleDuration);

        // Stage 5: JWT generation
        long tJwtStart = System.currentTimeMillis();
        String token = jwtTokenProvider.generateTokenFromUserId(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
        long jwtDuration = System.currentTimeMillis() - tJwtStart;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 5 - JWT generation in {} ms", correlationId, jwtDuration);

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 6 - Registration completed successfully in {} ms for User ID: {}",
                correlationId, totalDuration, savedUser.getId());

        return new AuthResponse(token, savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
                savedUser.getPhone(), savedUser.getRole(), donorId, hospitalId, hospitalName, referralCode);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        return login(request, UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String correlationId) {
        long totalStartTime = System.currentTimeMillis();
        String safeEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 0 - Incoming login request for user: [{}]", correlationId, safeEmail);

        // Stage 1: User lookup
        long tLookupStart = System.currentTimeMillis();
        User user = userRepository.findByEmail(safeEmail)
                .orElseThrow(() -> {
                    long elapsed = System.currentTimeMillis() - tLookupStart;
                    logger.warn("[LifeDrop Auth][CorrelationID: {}] Stage 1 - User lookup FAILED (email not found) in {} ms for [{}]",
                            correlationId, elapsed, safeEmail);
                    return new BadCredentialsException("Bad credentials");
                });
        long lookupDuration = System.currentTimeMillis() - tLookupStart;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 1 - User lookup SUCCEEDED in {} ms (User ID: {})",
                correlationId, lookupDuration, user.getId());

        // Stage 2: Password verification
        long tPassStart = System.currentTimeMillis();
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        long passDuration = System.currentTimeMillis() - tPassStart;
        if (!passwordMatches) {
            logger.warn("[LifeDrop Auth][CorrelationID: {}] Stage 2 - Password verification FAILED (mismatch) in {} ms for User ID: {}",
                    correlationId, passDuration, user.getId());
            throw new BadCredentialsException("Bad credentials");
        }
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 2 - Password verification SUCCEEDED in {} ms for User ID: {}",
                correlationId, passDuration, user.getId());

        // Stage 3: JWT token generation
        long tJwtStart = System.currentTimeMillis();
        String token = jwtTokenProvider.generateTokenFromUserId(user.getId(), user.getEmail(), user.getRole().name());
        long jwtDuration = System.currentTimeMillis() - tJwtStart;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 3 - JWT generation SUCCEEDED in {} ms (Role: {})",
                correlationId, jwtDuration, user.getRole());

        // Stage 4: Profile & role metadata retrieval
        long tMetaStart = System.currentTimeMillis();
        Long userId = user.getId();
        Role role = user.getRole();
        Long donorId = null;
        Long hospitalId = null;
        String hospitalName = null;
        String referralCode = null;

        if (role == Role.DONOR) {
            donorId = donorRepository.findByUserId(userId).map(Donor::getId).orElse(null);
            referralCode = referralRepository.findFirstByReferrerIdOrderByCreatedAtDesc(userId)
                    .map(Referral::getReferralCode).orElse(null);
        } else if (role == Role.HOSPITAL) {
            Hospital hospital = hospitalRepository.findByUserId(userId).orElse(null);
            if (hospital != null) {
                hospitalId = hospital.getId();
                hospitalName = hospital.getHospitalName();
            }
        }
        long metaDuration = System.currentTimeMillis() - tMetaStart;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 4 - Profile metadata retrieval SUCCEEDED in {} ms (DonorId: {}, HospitalId: {})",
                correlationId, metaDuration, donorId, hospitalId);

        // Stage 5: Response completion
        long totalDuration = System.currentTimeMillis() - totalStartTime;
        logger.info("[LifeDrop Auth][CorrelationID: {}] Stage 5 - Login workflow COMPLETED successfully in {} ms for User ID: {}",
                correlationId, totalDuration, userId);

        return new AuthResponse(token, userId, user.getName(), user.getEmail(),
                user.getPhone(), role, donorId, hospitalId, hospitalName, referralCode);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Long donorId = null;
        Long hospitalId = null;
        String hospitalName = null;
        String referralCode = null;

        if (user.getRole() == Role.DONOR) {
            donorId = donorRepository.findByUserId(userId).map(Donor::getId).orElse(null);
            referralCode = referralRepository.findFirstByReferrerIdOrderByCreatedAtDesc(userId)
                    .map(Referral::getReferralCode).orElse(null);
        } else if (user.getRole() == Role.HOSPITAL) {
            Hospital hospital = hospitalRepository.findByUserId(userId).orElse(null);
            if (hospital != null) {
                hospitalId = hospital.getId();
                hospitalName = hospital.getHospitalName();
            }
        }

        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole(), donorId, hospitalId, hospitalName, referralCode, user.getCreatedAt());
    }
}
