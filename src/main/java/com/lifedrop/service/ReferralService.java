package com.lifedrop.service;

import com.lifedrop.dto.response.ReferralResponse;
import com.lifedrop.entity.Donor;
import com.lifedrop.entity.Referral;
import com.lifedrop.entity.User;
import com.lifedrop.enums.ReferralStatus;
import com.lifedrop.exception.BadRequestException;
import com.lifedrop.exception.ResourceNotFoundException;
import com.lifedrop.repository.DonorRepository;
import com.lifedrop.repository.ReferralRepository;
import com.lifedrop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final int starsReward;

    public ReferralService(ReferralRepository referralRepository,
                           UserRepository userRepository,
                           DonorRepository donorRepository,
                           @Value("${lifedrop.referral.stars-reward:10}") int starsReward) {
        this.referralRepository = referralRepository;
        this.userRepository = userRepository;
        this.donorRepository = donorRepository;
        this.starsReward = starsReward;
    }

    /**
     * Generates a unique referral code for the user.
     * Example: PRAGA123, LIFEDROP42
     */
    @Transactional
    public ReferralResponse generateReferralCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Generate unique code based on user's name or prefix
        String cleanName = user.getName().replaceAll("[^a-zA-Z]", "").toUpperCase(Locale.ROOT);
        String prefix = cleanName.length() >= 4 ? cleanName.substring(0, 4) : "LIFE";
        String referralCode;
        Random random = new Random();

        do {
            int randomNum = 100 + random.nextInt(900);
            referralCode = prefix + randomNum;
        } while (referralRepository.existsByReferralCode(referralCode));

        Referral referral = new Referral(user, referralCode);
        Referral saved = referralRepository.save(referral);

        return mapToResponse(saved);
    }

    /**
     * Attaches a referral code during a new user's registration.
     * Status transitions from CREATED -> REGISTERED. Stars are NOT awarded yet.
     */
    @Transactional
    public void processReferralRegistration(String referralCode, User newUser) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return;
        }

        String code = referralCode.trim().toUpperCase(Locale.ROOT);
        Optional<Referral> optReferral = referralRepository.findByReferralCode(code);

        if (optReferral.isEmpty()) {
            return; // invalid code ignored silently or logged
        }

        Referral referral = optReferral.get();
        if (referral.getReferrer().getId().equals(newUser.getId())) {
            throw new BadRequestException("You cannot refer yourself");
        }

        if (referral.getStatus() == ReferralStatus.CREATED) {
            referral.setReferredUser(newUser);
            referral.setStatus(ReferralStatus.REGISTERED);
            referralRepository.save(referral);
        }
    }

    /**
     * Completes a referral when the referred user completes their verified donation.
     * Awards +10 stars to the referrer and marks referral status as COMPLETED.
     * Prevents duplicate rewards.
     */
    @Transactional
    public void completeReferralOnDonation(Long referredUserId) {
        Optional<Referral> optReferral = referralRepository.findByReferredUserId(referredUserId);

        if (optReferral.isPresent()) {
            Referral referral = optReferral.get();

            if (referral.getStatus() == ReferralStatus.REGISTERED && referral.getStarsEarned() == 0) {
                referral.setStatus(ReferralStatus.COMPLETED);
                referral.setStarsEarned(starsReward);
                referral.setCompletedAt(LocalDateTime.now());
                referralRepository.save(referral);

                // Add stars to referrer donor profile if exists
                User referrer = referral.getReferrer();
                donorRepository.findByUserId(referrer.getId()).ifPresent(donor -> {
                    donor.setStars(donor.getStars() + starsReward);
                    donorRepository.save(donor);
                });
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> getReferralsByUser(Long userId) {
        return referralRepository.findByReferrerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> getAllReferrals() {
        return referralRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ReferralResponse mapToResponse(Referral referral) {
        ReferralResponse response = new ReferralResponse();
        response.setId(referral.getId());
        response.setReferralCode(referral.getReferralCode());
        response.setStatus(referral.getStatus());
        response.setStarsEarned(referral.getStarsEarned());
        response.setReferrerId(referral.getReferrer().getId());
        response.setReferrerName(referral.getReferrer().getName());
        if (referral.getReferredUser() != null) {
            response.setReferredUserId(referral.getReferredUser().getId());
            response.setReferredUserName(referral.getReferredUser().getName());
        }
        response.setCreatedAt(referral.getCreatedAt());
        response.setCompletedAt(referral.getCompletedAt());
        return response;
    }
}
