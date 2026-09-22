package com.aurionpro.ticketboard.requirement.repository;

import com.aurionpro.ticketboard.requirement.entity.RequirementHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementHistoryRepository extends JpaRepository<RequirementHistory, Long> {
    List<RequirementHistory> findByRequirementIdOrderByChangedAtDesc(Long requirementId);

    @Modifying
    @Query("UPDATE RequirementHistory h SET h.changedBy = null WHERE h.changedBy.id = :userId")
    void detachChangedBy(@Param("userId") Long userId);
}
