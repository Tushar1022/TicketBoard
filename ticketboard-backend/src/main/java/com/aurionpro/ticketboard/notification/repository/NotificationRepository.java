package com.aurionpro.ticketboard.notification.repository;

import com.aurionpro.ticketboard.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    long countByRecipientEmailAndIsReadFalse(String recipientEmail);

    Optional<Notification> findByIdAndRecipientEmail(Long id, String recipientEmail);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientEmail = :recipientEmail AND n.isRead = false")
    int markAllAsReadForUser(@Param("recipientEmail") String recipientEmail);
}
