package com.lifedrop.repository;

import com.lifedrop.entity.BloodInventory;
import com.lifedrop.enums.BloodGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {

    Optional<BloodInventory> findByBloodGroup(BloodGroup bloodGroup);

    boolean existsByBloodGroup(BloodGroup bloodGroup);

    @Query("SELECT COALESCE(SUM(bi.availableUnits), 0) FROM BloodInventory bi")
    long sumTotalAvailableUnits();
}
