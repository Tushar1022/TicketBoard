package com.aurionpro.ticketboard.project.repository;

import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.enums.ProjectHealth;
import com.aurionpro.ticketboard.project.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByProjectCode(String projectCode);

    boolean existsByProjectCode(String projectCode);

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByHealth(ProjectHealth health);

    List<Project> findByProjectManagerId(Long projectManagerId);

    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.user.id = :userId")
    List<Project> findByMemberUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.status NOT IN (com.aurionpro.ticketboard.project.enums.ProjectStatus.COMPLETED, com.aurionpro.ticketboard.project.enums.ProjectStatus.CLOSED)")
    long countActiveProjects();

    @Query("SELECT COUNT(p) FROM Project p WHERE p.health = com.aurionpro.ticketboard.project.enums.ProjectHealth.RED")
    long countDelayedProjects();

    @Query("SELECT COUNT(p) FROM Project p WHERE p.health = com.aurionpro.ticketboard.project.enums.ProjectHealth.AMBER")
    long countAtRiskProjects();

    @Query("SELECT COUNT(p) FROM Project p WHERE p.status = com.aurionpro.ticketboard.project.enums.ProjectStatus.COMPLETED")
    long countCompletedProjects();

    @Query("SELECT p FROM Project p WHERE p.plannedEndDate BETWEEN :startDate AND :endDate")
    List<Project> findUpcomingDeliveries(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p.status, COUNT(p) FROM Project p GROUP BY p.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT p.health, COUNT(p) FROM Project p GROUP BY p.health")
    List<Object[]> countByHealthGrouped();

    @Modifying
    @Query("UPDATE Project p SET p.projectManager = null WHERE p.projectManager.id = :userId")
    void detachProjectManager(@Param("userId") Long userId);
}
