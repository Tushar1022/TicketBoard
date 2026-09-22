package com.aurionpro.ticketboard.requirement.repository;

import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.requirement.enums.RequirementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, Long> {

    Optional<Requirement> findByReqNumber(String reqNumber);

    boolean existsByReqNumber(String reqNumber);

    List<Requirement> findByProjectId(Long projectId);

    List<Requirement> findByStatus(RequirementStatus status);

    List<Requirement> findByOwnerId(Long ownerId);

    @Query("SELECT COUNT(r) FROM Requirement r WHERE r.status NOT IN (com.aurionpro.ticketboard.requirement.enums.RequirementStatus.CLOSED, com.aurionpro.ticketboard.requirement.enums.RequirementStatus.REJECTED, com.aurionpro.ticketboard.requirement.enums.RequirementStatus.CANCELLED)")
    long countActiveRequirements();

    @Query("SELECT r FROM Requirement r WHERE r.status NOT IN (com.aurionpro.ticketboard.requirement.enums.RequirementStatus.CLOSED, com.aurionpro.ticketboard.requirement.enums.RequirementStatus.REJECTED, com.aurionpro.ticketboard.requirement.enums.RequirementStatus.CANCELLED) AND r.plannedEndDate < :now")
    List<Requirement> findDelayedRequirements(@Param("now") LocalDate now);

    @Query("SELECT r FROM Requirement r WHERE r.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Requirement> findForecastUpcomingRequirements(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(r.estimatedEffortHours) FROM Requirement r WHERE r.plannedStartDate BETWEEN :startDate AND :endDate")
    Double sumEstimatedEffortBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM Requirement r WHERE r.project.id = :projectId")
    int countByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT r.status, COUNT(r) FROM Requirement r GROUP BY r.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT r.status, COUNT(r) FROM Requirement r WHERE r.project.id = :projectId GROUP BY r.status")
    List<Object[]> countByStatusGroupedForProject(@Param("projectId") Long projectId);

    @Query("SELECT r.priority, COUNT(r) FROM Requirement r GROUP BY r.priority")
    List<Object[]> countByPriorityGrouped();

    @Query("SELECT r.priority, COUNT(r) FROM Requirement r WHERE r.project.id = :projectId GROUP BY r.priority")
    List<Object[]> countByPriorityGroupedForProject(@Param("projectId") Long projectId);

    @Modifying
    @Query("UPDATE Requirement r SET r.owner = null WHERE r.owner.id = :userId")
    void detachOwner(@Param("userId") Long userId);
}
