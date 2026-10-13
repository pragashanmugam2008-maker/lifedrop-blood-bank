package com.lifedrop.repository;

import com.lifedrop.entity.BloodRequest;
import com.lifedrop.enums.BloodGroup;
import com.lifedrop.enums.RequestStatus;
import com.lifedrop.enums.RequestUrgency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<BloodRequest> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    List<BloodRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<BloodRequest> findByBloodGroupInAndStatusOrderByCreatedAtDesc(List<BloodGroup> bloodGroups, RequestStatus status);

    List<BloodRequest> findByBloodGroupAndStatus(BloodGroup bloodGroup, RequestStatus status);

    long countByStatus(RequestStatus status);

    long countByUrgency(RequestUrgency urgency);

    long countByStatusAndUrgency(RequestStatus status, RequestUrgency urgency);
}
