package com.aurionpro.ticketboard.requirement.repository;

import com.aurionpro.ticketboard.requirement.entity.RequirementHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementHistoryRepository extends JpaRepository<RequirementHistory, Long> {
    List<RequirementHistory> findByRequirementIdOrderByChangedAtDesc(Long requirementId);
}
