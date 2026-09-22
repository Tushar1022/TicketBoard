package com.aurionpro.ticketboard.mail.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_messages", indexes = {
        @Index(name = "idx_email_user_folder", columnList = "user_email, folder"),
        @Index(name = "idx_email_starred", columnList = "user_email, is_starred"),
        @Index(name = "idx_email_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "sender_email", nullable = false, length = 150)
    private String senderEmail;

    @Column(name = "sender_name", length = 150)
    private String senderName;

    @Column(name = "recipient_to", nullable = false, length = 255)
    private String recipientTo;

    @Column(name = "recipient_cc", length = 255)
    private String recipientCc;

    @Column(name = "recipient_bcc", length = 255)
    private String recipientBcc;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "folder", nullable = false, length = 30)
    private String folder; // INBOX, SENT, DRAFTS, SPAM, TRASH, ARCHIVE

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "is_starred", nullable = false)
    @Builder.Default
    private boolean isStarred = false;

    @Column(name = "has_attachments", nullable = false)
    @Builder.Default
    private boolean hasAttachments = false;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "priority", length = 30)
    @Builder.Default
    private String priority = "NORMAL"; // NORMAL, HIGH, URGENT

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
