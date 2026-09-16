package com.aurionpro.ticketboard.support.repository;

import com.aurionpro.ticketboard.support.entity.SupportCategory;
import com.aurionpro.ticketboard.support.entity.SupportTicket;
import com.aurionpro.ticketboard.support.entity.TicketPriority;
import com.aurionpro.ticketboard.support.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByCreatedByEmailOrderByCreatedAtDesc(String createdByEmail);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    List<SupportTicket> findByTargetRoleOrderByCreatedAtDesc(String targetRole);

    Optional<SupportTicket> findByTicketCode(String ticketCode);

    long countByStatusIn(List<TicketStatus> statuses);

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    long countByCategory(SupportCategory category);

    long countByTargetRole(String targetRole);

    long countByAssignedToIdIsNull();

    @Query("SELECT MAX(t.id) FROM SupportTicket t")
    Long findMaxId();

    @Query("SELECT t.status, COUNT(t) FROM SupportTicket t GROUP BY t.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT t.priority, COUNT(t) FROM SupportTicket t GROUP BY t.priority")
    List<Object[]> countByPriorityGrouped();

    @Query("SELECT t.category, COUNT(t) FROM SupportTicket t GROUP BY t.category")
    List<Object[]> countByCategoryGrouped();
}
