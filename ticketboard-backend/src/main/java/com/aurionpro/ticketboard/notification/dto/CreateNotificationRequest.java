package com.aurionpro.ticketboard.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {
    private String recipientEmail;
    
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private String type; // TASK, MENTION, ALERT, SYSTEM, SECURITY, ERP
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private String actionUrl;
}
