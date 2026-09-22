package com.aurionpro.ticketboard.mail.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendEmailRequest {
    @NotBlank(message = "Recipient TO is required")
    private String recipientTo;

    private String recipientCc;
    private String recipientBcc;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body is required")
    private String body;

    private String priority; // NORMAL, HIGH, URGENT
    private boolean isDraft;

    @Builder.Default
    private List<AttachmentRef> attachments = new ArrayList<>();
}
