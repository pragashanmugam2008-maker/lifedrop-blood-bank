package com.lifedrop.repository;

import com.lifedrop.entity.Donor;
import com.lifedrop.enums.BloodGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

    Optional<Donor> findByUserId(Long userId);

    List<Donor> findByBloodGroup(BloodGroup bloodGroup);

    List<Donor> findByBloodGroupIn(Collection<BloodGroup> bloodGroups);

    List<Donor> findByBloodGroupInAndAvailabilityStatusTrue(Collection<BloodGroup> bloodGroups);

    List<Donor> findByBloodGroupInAndAvailabilityStatusTrueAndEligibleTrue(Collection<BloodGroup> bloodGroups);

    long countByAvailabilityStatusTrue();

    long countByEligibleTrue();

    @Query("SELECT d FROM Donor d JOIN d.user u WHERE " +
           "(:query IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.city) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:bloodGroup IS NULL OR d.bloodGroup = :bloodGroup) AND " +
           "(:city IS NULL OR LOWER(d.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:available IS NULL OR d.availabilityStatus = :available) AND " +
           "(:eligible IS NULL OR d.eligible = :eligible) " +
           "ORDER BY d.rating DESC, d.totalDonations DESC")
    List<Donor> searchDonors(@Param("query") String query,
                            @Param("bloodGroup") BloodGroup bloodGroup,
                            @Param("city") String city,
                            @Param("available") Boolean available,
                            @Param("eligible") Boolean eligible);
}
