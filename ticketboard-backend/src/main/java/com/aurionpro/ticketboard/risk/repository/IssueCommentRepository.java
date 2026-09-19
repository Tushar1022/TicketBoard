package com.aurionpro.ticketboard.risk.repository;

import com.aurionpro.ticketboard.risk.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, Long> {
    List<IssueComment> findByIssueIdOrderByCreatedAtAsc(Long issueId);

    long countByIssueId(Long issueId);

    void deleteByIssueId(Long issueId);
}