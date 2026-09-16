package com.aurionpro.ticketboard.support.dto;

import com.aurionpro.ticketboard.support.entity.SupportCategory;
import com.aurionpro.ticketboard.support.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSupportTicketRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotNull(message = "Category is required")
    private SupportCategory category;

    @NotNull(message = "Priority is required")
    private TicketPriority priority;

    @NotBlank(message = "Target role is required")
    private String targetRole; // ROLE_SUPER_ADMIN or ROLE_ADMIN

    @NotBlank(message = "Description is required")
    private String description;

    private String systemDiagnostics;

    private String customCategoryName;

    private Long projectId;

    private String projectName;

    private String moduleName;
}
