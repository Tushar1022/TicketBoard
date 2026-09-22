package com.aurionpro.ticketboard.risk.repository;

import com.aurionpro.ticketboard.risk.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, Long> {
    List<IssueComment> findByIssueIdOrderByCreatedAtAsc(Long issueId);

    long countByIssueId(Long issueId);

    void deleteByIssueId(Long issueId);

    @Modifying
    @Query("UPDATE IssueComment c SET c.author = null WHERE c.author.id = :userId")
    void detachAuthor(@Param("userId") Long userId);
}