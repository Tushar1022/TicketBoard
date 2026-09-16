package com.aurionpro.ticketboard.risk.repository;

import com.aurionpro.ticketboard.risk.entity.Risk;
import com.aurionpro.ticketboard.risk.enums.RiskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskRepository extends JpaRepository<Risk, Long> {

    Optional<Risk> findByRiskCode(String riskCode);

    boolean existsByRiskCode(String riskCode);

    List<Risk> findByProjectId(Long projectId);

    List<Risk> findByStatus(RiskStatus status);

    @Query("SELECT COUNT(r) FROM Risk r WHERE r.status != 'CLOSED' AND r.riskScore >= 15")
    long countCriticalRisks();

    @Query("SELECT r.status, COUNT(r) FROM Risk r GROUP BY r.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT r.status, COUNT(r) FROM Risk r WHERE r.project.id = :projectId GROUP BY r.status")
    List<Object[]> countByStatusGroupedForProject(@Param("projectId") Long projectId);
}
