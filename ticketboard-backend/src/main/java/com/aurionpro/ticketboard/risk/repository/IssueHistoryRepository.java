package com.aurionpro.ticketboard.risk.repository;

import com.aurionpro.ticketboard.risk.entity.IssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueHistoryRepository extends JpaRepository<IssueHistory, Long> {
    List<IssueHistory> findByIssueIdOrderByOccurredAtAsc(Long issueId);

    void deleteByIssueId(Long issueId);

    @Modifying
    @Query("UPDATE IssueHistory h SET h.actor = null WHERE h.actor.id = :userId")
    void detachActor(@Param("userId") Long userId);
}