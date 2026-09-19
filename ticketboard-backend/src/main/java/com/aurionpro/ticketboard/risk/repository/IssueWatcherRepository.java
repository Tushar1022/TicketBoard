package com.aurionpro.ticketboard.risk.repository;

import com.aurionpro.ticketboard.risk.entity.IssueWatcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueWatcherRepository extends JpaRepository<IssueWatcher, Long> {
    List<IssueWatcher> findByIssueId(Long issueId);

    Optional<IssueWatcher> findByIssueIdAndUserId(Long issueId, Long userId);

    boolean existsByIssueIdAndUserId(Long issueId, Long userId);

    long countByIssueId(Long issueId);

    void deleteByIssueId(Long issueId);
}