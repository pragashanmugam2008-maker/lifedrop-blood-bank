package com.lifedrop.repository;

import com.lifedrop.entity.Referral;
import com.lifedrop.enums.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    Optional<Referral> findByReferralCode(String referralCode);

    List<Referral> findByReferrerIdOrderByCreatedAtDesc(Long referrerId);

    Optional<Referral> findByReferredUserId(Long referredUserId);

    boolean existsByReferralCode(String referralCode);

    long countByReferrerId(Long referrerId);

    long countByStatus(ReferralStatus status);

    @Query("SELECT COALESCE(SUM(r.starsEarned), 0) FROM Referral r WHERE r.referrer.id = :referrerId")
    long sumStarsByReferrerId(@Param("referrerId") Long referrerId);

    @Query("SELECT COALESCE(SUM(r.starsEarned), 0) FROM Referral r WHERE r.status = 'COMPLETED'")
    long sumTotalStarsDistributed();
}
