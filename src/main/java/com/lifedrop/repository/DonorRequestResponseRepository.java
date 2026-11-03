package com.lifedrop.repository;

import com.lifedrop.entity.DonorRequestResponse;
import com.lifedrop.enums.DonorResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRequestResponseRepository extends JpaRepository<DonorRequestResponse, Long> {

    Optional<DonorRequestResponse> findByRequestIdAndDonorId(Long requestId, Long donorId);

    boolean existsByRequestIdAndDonorId(Long requestId, Long donorId);

    List<DonorRequestResponse> findByDonorId(Long donorId);

    List<DonorRequestResponse> findByDonorIdAndStatus(Long donorId, DonorResponseStatus status);

    List<DonorRequestResponse> findByRequestIdOrderByRespondedAtDesc(Long requestId);

    List<DonorRequestResponse> findByRequestIdAndStatusOrderByRespondedAtDesc(Long requestId, DonorResponseStatus status);

    @Query("SELECT r FROM DonorRequestResponse r WHERE r.request.requester.id = :requesterId ORDER BY r.respondedAt DESC")
    List<DonorRequestResponse> findByHospitalUserIdOrderByRespondedAtDesc(@Param("requesterId") Long requesterId);

    @Query("SELECT r FROM DonorRequestResponse r WHERE r.request.requester.id = :requesterId AND r.status = :status ORDER BY r.respondedAt DESC")
    List<DonorRequestResponse> findByHospitalUserIdAndStatusOrderByRespondedAtDesc(@Param("requesterId") Long requesterId, @Param("status") DonorResponseStatus status);

    @Query("SELECT r FROM DonorRequestResponse r WHERE r.request.hospital.id = :hospitalId ORDER BY r.respondedAt DESC")
    List<DonorRequestResponse> findByHospitalIdOrderByRespondedAtDesc(@Param("hospitalId") Long hospitalId);

    @Query("SELECT r FROM DonorRequestResponse r WHERE r.request.hospital.id = :hospitalId AND r.status = :status ORDER BY r.respondedAt DESC")
    List<DonorRequestResponse> findByHospitalIdAndStatusOrderByRespondedAtDesc(@Param("hospitalId") Long hospitalId, @Param("status") DonorResponseStatus status);

    long countByRequestIdAndStatus(Long requestId, DonorResponseStatus status);
}
