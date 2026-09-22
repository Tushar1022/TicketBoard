package com.aurionpro.ticketboard.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private String recipientEmail;
    private String title;
    private String message;
    private String type;
    private String priority;
    private boolean read;
    private String actionUrl;
    private LocalDateTime createdAt;
}
