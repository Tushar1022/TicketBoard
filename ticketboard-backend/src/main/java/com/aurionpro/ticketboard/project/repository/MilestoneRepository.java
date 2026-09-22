package com.aurionpro.ticketboard.project.repository;

import com.aurionpro.ticketboard.project.entity.Milestone;
import com.aurionpro.ticketboard.project.enums.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    Optional<Milestone> findByMilestoneCode(String milestoneCode);

    List<Milestone> findByProjectIdOrderByPlannedDateAsc(Long projectId);

    List<Milestone> findByProjectIdAndStatusOrderByPlannedDateAsc(Long projectId, MilestoneStatus status);

    List<Milestone> findByProjectIdAndFlagOrderByPlannedDateAsc(Long projectId, String flag);

    long countByProjectId(Long projectId);

    @Query("SELECT COUNT(m) FROM Milestone m")
    long countMilestones();

    @Query("SELECT m.status, COUNT(m) FROM Milestone m GROUP BY m.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT m.status, COUNT(m) FROM Milestone m WHERE m.project.id = :projectId GROUP BY m.status")
    List<Object[]> countByStatusGroupedForProject(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.milestone.id = :milestoneId")
    long countLinkedTasks(@Param("milestoneId") Long milestoneId);

    @Query("SELECT COALESCE(AVG(w.completionPercentage), 0) FROM WorkItem w WHERE w.milestone.id = :milestoneId")
    double averageLinkedTaskProgress(@Param("milestoneId") Long milestoneId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.milestone.id = :milestoneId")
    long countLinkedIssues(@Param("milestoneId") Long milestoneId);

    @Modifying
    @Query("UPDATE WorkItem w SET w.milestone = null WHERE w.milestone.id = :milestoneId")
    void detachWorkItems(@Param("milestoneId") Long milestoneId);

    @Modifying
    @Query("UPDATE Issue i SET i.milestone = null WHERE i.milestone.id = :milestoneId")
    void detachIssues(@Param("milestoneId") Long milestoneId);

    @Modifying
    @Query("UPDATE Milestone m SET m.owner = null WHERE m.owner.id = :userId")
    void detachOwner(@Param("userId") Long userId);
}