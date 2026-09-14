package com.aurionpro.ticketboard.risk.repository;

import com.aurionpro.ticketboard.risk.entity.Issue;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    Optional<Issue> findByIssueCode(String issueCode);
    List<Issue> findByProjectId(Long projectId);
    List<Issue> findByStatus(IssueStatus status);
}
