package com.aurionpro.ticketboard.dashboard.dto;

import com.aurionpro.ticketboard.workitem.dto.WorkItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaDashboardDto {
    private int testingQueueCount;
    private int openBugsCount;
    private int criticalBugsCount;
    private int blockerBugsCount;
    private int resolvedPendingVerificationCount;
    private List<WorkItemDto> testingQueue;
    private List<WorkItemDto> openDefects;
    private List<WorkItemDto> criticalDefects;
}
