package com.aurionpro.ticketboard.support.repository;

import com.aurionpro.ticketboard.support.entity.SupportTicket;
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

    @Query("SELECT MAX(t.id) FROM SupportTicket t")
    Long findMaxId();
}
