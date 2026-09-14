package com.aurionpro.ticketboard.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddTicketCommentRequest {

    @NotBlank(message = "Comment text is required")
    private String commentText;
}
