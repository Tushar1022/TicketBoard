package com.aurionpro.ticketboard.notification.service;

import com.aurionpro.ticketboard.notification.dto.CreateNotificationRequest;
import com.aurionpro.ticketboard.notification.dto.NotificationDto;
import com.aurionpro.ticketboard.notification.entity.Notification;
import com.aurionpro.ticketboard.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForUser(String userEmail) {
        ensureInitialNotificationsSeeded(userEmail);
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        return notificationRepository.countByRecipientEmailAndIsReadFalse(userEmail);
    }

    @Transactional
    public NotificationDto markAsRead(Long id, String userEmail) {
        Notification notification = notificationRepository.findByIdAndRecipientEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found for id: " + id));
        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        notificationRepository.markAllAsReadForUser(userEmail);
    }

    @Transactional
    public NotificationDto createNotification(CreateNotificationRequest request, String defaultRecipient) {
        String recipient = (request.getRecipientEmail() != null && !request.getRecipientEmail().isBlank())
                ? request.getRecipientEmail()
                : defaultRecipient;

        Notification notification = Notification.builder()
                .recipientEmail(recipient)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType() != null ? request.getType() : "SYSTEM")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .isRead(false)
                .actionUrl(request.getActionUrl())
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteNotification(Long id, String userEmail) {
        Notification notification = notificationRepository.findByIdAndRecipientEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found for id: " + id));
        notificationRepository.delete(notification);
    }

    private void ensureInitialNotificationsSeeded(String userEmail) {
        List<Notification> existing = notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(userEmail);
        if (existing.isEmpty()) {
            Notification n1 = Notification.builder()
                    .recipientEmail(userEmail)
                    .title("SLA Breach Risk Alert: Task #TB-104")
                    .message("Task #TB-104 'Implement Payment Gateway OAuth2 Handshake' is approaching SLA resolution threshold in 45 minutes. Please review blocker status.")
                    .type("ALERT")
                    .priority("HIGH")
                    .isRead(false)
                    .actionUrl("/work-items")
                    .createdAt(LocalDateTime.now().minusMinutes(12))
                    .build();

            Notification n2 = Notification.builder()
                    .recipientEmail(userEmail)
                    .title("Sprint Release REL-2.4 Approved for UAT")
                    .message("Release Candidate REL-2.4 has passed SIT exit criteria and is promoted to UAT environment for user acceptance signoff.")
                    .type("TASK")
                    .priority("MEDIUM")
                    .isRead(false)
                    .actionUrl("/releases")
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();

            Notification n3 = Notification.builder()
                    .recipientEmail(userEmail)
                    .title("Expense Claim #EX-809 Approved")
                    .message("Your expense claim #EX-809 ($450.00 for Cloud Dev Infrastructure) was approved by Tech Lead and queued for disbursement.")
                    .type("ERP")
                    .priority("LOW")
                    .isRead(true)
                    .actionUrl("/my-workspace")
                    .createdAt(LocalDateTime.now().minusHours(5))
                    .build();

            Notification n4 = Notification.builder()
                    .recipientEmail(userEmail)
                    .title("System Maintenance Scheduled")
                    .message("TicketBoard platform maintenance window is scheduled for Saturday 02:00 UTC (Duration: 30 minutes). Database backups will execute automatically.")
                    .type("SYSTEM")
                    .priority("MEDIUM")
                    .isRead(false)
                    .actionUrl("/support")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            notificationRepository.saveAll(List.of(n1, n2, n3, n4));
        }
    }

    private NotificationDto mapToDto(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .recipientEmail(entity.getRecipientEmail())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .priority(entity.getPriority())
                .read(entity.isRead())
                .actionUrl(entity.getActionUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
