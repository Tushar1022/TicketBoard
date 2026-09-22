package com.aurionpro.ticketboard.erp.repository;

import com.aurionpro.ticketboard.erp.entity.OKRGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OKRGoalRepository extends JpaRepository<OKRGoal, Long> {
    List<OKRGoal> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
