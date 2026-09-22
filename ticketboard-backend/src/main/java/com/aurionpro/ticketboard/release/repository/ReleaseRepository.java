package com.aurionpro.ticketboard.release.repository;

import com.aurionpro.ticketboard.release.entity.Release;
import com.aurionpro.ticketboard.release.enums.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {

    Optional<Release> findByReleaseVersion(String releaseVersion);

    boolean existsByReleaseVersion(String releaseVersion);

    List<Release> findByProjectId(Long projectId);

    List<Release> findByStatus(ReleaseStatus status);

    @Query("SELECT r FROM Release r WHERE r.plannedDate BETWEEN :startDate AND :endDate")
    List<Release> findUpcomingReleases(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM Release r WHERE r.status = com.aurionpro.ticketboard.release.enums.ReleaseStatus.DEPLOYED AND (r.rollbackRequired = false OR r.rollbackRequired IS NULL)")
    long countSuccessfulReleases();

    @Query("SELECT COUNT(r) FROM Release r WHERE r.status IN (com.aurionpro.ticketboard.release.enums.ReleaseStatus.DEPLOYED, com.aurionpro.ticketboard.release.enums.ReleaseStatus.CLOSED)")
    long countTotalCompletedReleases();

    @Modifying
    @Query("UPDATE Release r SET r.owner = null WHERE r.owner.id = :userId")
    void detachOwner(@Param("userId") Long userId);
}
