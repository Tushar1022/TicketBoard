package com.aurionpro.ticketboard.mail.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mail_attachments", indexes = {
        @Index(name = "idx_attach_user", columnList = "user_email"),
        @Index(name = "idx_attach_email_msg", columnList = "email_message_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "email_message_id")
    private Long emailMessageId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "is_drive", nullable = false)
    @Builder.Default
    private boolean isDrive = false;

    @Column(name = "drive_url", length = 500)
    private String driveUrl;

    @Column(name = "stored_path", length = 500)
    private String storedPath;
}