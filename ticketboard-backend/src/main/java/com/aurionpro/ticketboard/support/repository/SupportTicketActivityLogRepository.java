package com.aurionpro.ticketboard.support.repository;

import com.aurionpro.ticketboard.support.entity.SupportTicketActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketActivityLogRepository extends JpaRepository<SupportTicketActivityLog, Long> {

    List<SupportTicketActivityLog> findByTicketIdOrderByOccurredAtDesc(Long ticketId);

    @Query("SELECT a.ticketId, COUNT(a) FROM SupportTicketActivityLog a GROUP BY a.ticketId")
    List<Object[]> countByTicketIdGrouped();

    long countByActionType(String actionTypeDescriptor);
}
