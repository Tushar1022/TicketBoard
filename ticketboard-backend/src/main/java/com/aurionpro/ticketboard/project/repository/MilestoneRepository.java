package com.aurionpro.ticketboard.project.repository;

import com.aurionpro.ticketboard.project.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByProjectId(Long projectId);

    @Query("SELECT COUNT(m) FROM Milestone m")
    long countMilestones();

    @Query("SELECT m.status, COUNT(m) FROM Milestone m GROUP BY m.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT m.status, COUNT(m) FROM Milestone m WHERE m.project.id = :projectId GROUP BY m.status")
    List<Object[]> countByStatusGroupedForProject(@Param("projectId") Long projectId);
}
