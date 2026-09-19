package com.aurionpro.ticketboard.risk.repository;

import com.aurionpro.ticketboard.risk.entity.IssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueHistoryRepository extends JpaRepository<IssueHistory, Long> {
    List<IssueHistory> findByIssueIdOrderByOccurredAtAsc(Long issueId);

    void deleteByIssueId(Long issueId);
}