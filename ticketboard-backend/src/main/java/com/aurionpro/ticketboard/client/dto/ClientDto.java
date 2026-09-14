package com.aurionpro.ticketboard.client.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {
    private Long id;

    @NotBlank(message = "Client code is required")
    private String clientCode;

    @NotBlank(message = "Client name is required")
    private String name;

    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String status;
    private Long accountManagerId;
    private String accountManagerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
