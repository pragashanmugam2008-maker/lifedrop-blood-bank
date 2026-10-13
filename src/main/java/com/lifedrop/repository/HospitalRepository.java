package com.lifedrop.repository;

import com.lifedrop.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByUserId(Long userId);
    List<Hospital> findByCityIgnoreCase(String city);
    boolean existsByUserId(Long userId);
    List<Hospital> findByHospitalNameContainingIgnoreCaseOrCityContainingIgnoreCase(String name, String city);
}
