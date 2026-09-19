package com.aurionpro.ticketboard.risk.repository;

import com.aurionpro.ticketboard.risk.entity.Issue;
import com.aurionpro.ticketboard.risk.enums.IssueSeverity;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    Optional<Issue> findByIssueCode(String issueCode);
    List<Issue> findByProjectId(Long projectId);
    List<Issue> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<Issue> findByStatus(IssueStatus status);

    @Query("SELECT COALESCE(COUNT(i), 0) FROM Issue i WHERE i.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT i.severity, COUNT(i) FROM Issue i GROUP BY i.severity")
    List<Object[]> countBySeverityGrouped();

    @Query("SELECT i.severity, COUNT(i) FROM Issue i WHERE i.project.id = :projectId GROUP BY i.severity")
    List<Object[]> countBySeverityGroupedForProject(@Param("projectId") Long projectId);

    @Query("SELECT i.status, COUNT(i) FROM Issue i GROUP BY i.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT i.status, COUNT(i) FROM Issue i WHERE i.project.id = :projectId GROUP BY i.status")
    List<Object[]> countByStatusGroupedForProject(@Param("projectId") Long projectId);
}