package com.aurionpro.ticketboard.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSmtpRequest {
    @NotBlank(message = "Test recipient email is required")
    @Email(message = "Invalid email format")
    private String testRecipient;

    private String smtpHost;
    private Integer smtpPort;
    private String username;
    private String password;
    private String encryptionType;
}
