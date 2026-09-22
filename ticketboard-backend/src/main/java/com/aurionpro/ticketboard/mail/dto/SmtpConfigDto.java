package com.aurionpro.ticketboard.mail.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmtpConfigDto {
    private Long id;
    private String userEmail;

    @NotBlank(message = "SMTP host is required")
    private String smtpHost;

    private Integer smtpPort;

    @NotBlank(message = "Username is required")
    private String username;

    private String password;

    @NotBlank(message = "From email is required")
    private String fromEmail;

    private String fromName;
    private String encryptionType; // TLS, SSL, NONE
    private boolean active;
    private LocalDateTime updatedAt;
}
