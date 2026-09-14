package com.aurionpro.ticketboard.workitem.dto;

import com.aurionpro.ticketboard.workitem.enums.DependencyType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyDto {
    private Long id;

    @NotNull(message = "Source work item ID is required")
    private Long sourceItemId;
    private String sourceTicketNumber;
    private String sourceTitle;

    @NotNull(message = "Target work item ID is required")
    private Long targetItemId;
    private String targetTicketNumber;
    private String targetTitle;

    private DependencyType dependencyType;
}
