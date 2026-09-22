package com.aurionpro.ticketboard.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessageDto {
    private Long id;
    private String userEmail;
    private String senderEmail;
    private String senderName;
    private String recipientTo;
    private String recipientCc;
    private String recipientBcc;
    private String subject;
    private String body;
    private String folder;
    private boolean read;
    private boolean starred;
    private boolean hasAttachments;
    private String attachmentsJson;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
