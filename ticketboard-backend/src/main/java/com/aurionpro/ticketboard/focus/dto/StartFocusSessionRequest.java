package com.aurionpro.ticketboard.focus.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartFocusSessionRequest {

    @NotNull(message = "Work item ID is required")
    private Long workItemId;

    private String description;
}