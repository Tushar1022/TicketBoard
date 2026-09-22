package com.aurionpro.ticketboard.erp.repository;

import com.aurionpro.ticketboard.erp.entity.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {
    List<ExpenseClaim> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
