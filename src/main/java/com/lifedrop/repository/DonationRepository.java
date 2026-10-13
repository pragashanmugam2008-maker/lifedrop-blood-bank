package com.lifedrop.repository;

import com.lifedrop.entity.Donation;
import com.lifedrop.enums.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByDonorIdOrderByDonationDateDesc(Long donorId);

    List<Donation> findByHospitalIdOrderByDonationDateDesc(Long hospitalId);

    List<Donation> findByRequestIdOrderByDonationDateDesc(Long requestId);

    List<Donation> findByDonorIdAndStatusOrderByDonationDateDesc(Long donorId, DonationStatus status);

    List<Donation> findByStatusOrderByDonationDateDesc(DonationStatus status);

    long countByStatus(DonationStatus status);

    long countByDonorIdAndStatus(Long donorId, DonationStatus status);

    @Query("SELECT COALESCE(SUM(d.units), 0) FROM Donation d WHERE d.status = 'COMPLETED'")
    long sumCompletedDonationUnits();
}
