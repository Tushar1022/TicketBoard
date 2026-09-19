package com.aurionpro.ticketboard.dashboard.service;

import com.aurionpro.ticketboard.dashboard.dto.*;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.MilestoneRepository;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.risk.entity.Issue;
import com.aurionpro.ticketboard.risk.entity.Risk;
import com.aurionpro.ticketboard.risk.repository.IssueRepository;
import com.aurionpro.ticketboard.risk.repository.RiskRepository;
import com.aurionpro.ticketboard.support.entity.SupportTicket;
import com.aurionpro.ticketboard.support.repository.SupportTicketRepository;
import com.aurionpro.ticketboard.timetracking.entity.TimeEntry;
import com.aurionpro.ticketboard.timetracking.repository.TimeEntryRepository;
import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ProjectRepository projectRepository;
    private final WorkItemRepository workItemRepository;
    private final RequirementRepository requirementRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final RiskRepository riskRepository;
    private final IssueRepository issueRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final MilestoneRepository milestoneRepository;

    @Override
    public ReportDataResponse getReportData(String category, Long projectId, String status, String startDate, String endDate) {
        String cat = category != null ? category.toLowerCase().trim() : "workitems";

        switch (cat) {
            case "projects":
                return buildProjectsReport(status);
            case "requirements":
                return buildRequirementsReport(projectId);
            case "timelogs":
            case "effort":
                return buildTimeLogsReport(projectId);
            case "risks":
            case "issues":
                return buildRisksAndIssuesReport(projectId);
            case "support":
            case "support-tickets":
                return buildSupportTicketsReport(status);
            case "workitems":
            case "tasks":
            default:
                return buildWorkItemsReport(projectId, status);
        }
    }

    @Override
    public List<ReportCatalogItemDto> getReportCatalog() {
        return List.of(
                ReportCatalogItemDto.builder()
                        .category("projects")
                        .title("Project Portfolio Status Report")
                        .subtitle("Real-time delivery progress, health indicators, and effort variance across projects")
                        .chartType("bar")
                        .orientation("landscape")
                        .summaryLabels(List.of("Total Projects", "Active Delivery Projects", "Average Portfolio Completion"))
                        .columns(columnDefs(
                                col("projectCode", "Project Code"),
                                col("projectName", "Project Name"),
                                col("client", "Client Name"),
                                col("status", "Status"),
                                col("health", "Delivery Health"),
                                col("completion", "Completion %"),
                                col("estimatedHours", "Est Budget Hours", "number"),
                                col("actualHours", "Logged Hours", "number")))
                        .build(),
                ReportCatalogItemDto.builder()
                        .category("workitems")
                        .title("Work Items & Delivery Board Report")
                        .subtitle("Real-time tracking of tasks, bugs, assignees, and effort hours")
                        .chartType("bar")
                        .orientation("landscape")
                        .summaryLabels(List.of("Total Tasks & Bugs", "Delivered / Closed", "Total Estimated Hours", "Total Logged Hours"))
                        .columns(columnDefs(
                                col("ticketNumber", "Ticket #"),
                                col("title", "Title"),
                                col("project", "Project"),
                                col("status", "Status"),
                                col("priority", "Priority"),
                                col("assignee", "Assignee"),
                                col("estimatedHours", "Est Hours", "number"),
                                col("actualHours", "Act Hours", "number"),
                                col("billingType", "Billing Type"),
                                col("jiraId", "Jira ID")))
                        .build(),
                ReportCatalogItemDto.builder()
                        .category("requirements")
                        .title("Requirements & Scope Register Report")
                        .subtitle("Change requests, approval statuses, and effort variance")
                        .chartType("donut")
                        .orientation("portrait")
                        .summaryLabels(List.of("Total Requirements", "Approved Requirements"))
                        .columns(columnDefs(
                                col("reqNumber", "Req Number"),
                                col("title", "Title"),
                                col("project", "Project"),
                                col("status", "Status"),
                                col("priority", "Priority"),
                                col("requester", "Requester"),
                                col("estimatedEffort", "Est Effort", "number"),
                                col("actualEffort", "Actual Effort", "number")))
                        .build(),
                ReportCatalogItemDto.builder()
                        .category("timelogs")
                        .title("Time Tracking & Effort Audit Report")
                        .subtitle("Logged hours, work dates, and approval tracking")
                        .chartType("area")
                        .orientation("landscape")
                        .summaryLabels(List.of("Total Time Log Entries", "Total Logged Effort"))
                        .columns(columnDefs(
                                col("id", "Log ID", "number"),
                                col("user", "Employee Name"),
                                col("project", "Project"),
                                col("workDate", "Work Date", "date"),
                                col("hours", "Logged Hours", "number"),
                                col("description", "Task Activity"),
                                col("status", "Approval Status")))
                        .build(),
                ReportCatalogItemDto.builder()
                        .category("risks")
                        .title("Risks & Issues Register Report")
                        .subtitle("Risk matrix, open issues, severity scoring, and mitigation ownership")
                        .chartType("donut")
                        .orientation("portrait")
                        .summaryLabels(List.of("Identified Risks", "Active Issues"))
                        .columns(columnDefs(
                                col("code", "Code"),
                                col("type", "Type (Risk/Issue)"),
                                col("project", "Project"),
                                col("description", "Description"),
                                col("severity", "Severity Score"),
                                col("status", "Status"),
                                col("owner", "Risk Owner")))
                        .build(),
                ReportCatalogItemDto.builder()
                        .category("support-tickets")
                        .title("Support Tickets & Queries Report")
                        .subtitle("Real-time support ticket resolution telemetry for Super Admin & Admin queues")
                        .chartType("bar")
                        .orientation("landscape")
                        .summaryLabels(List.of("Total Support Tickets", "Active Open Queries"))
                        .columns(columnDefs(
                                col("ticketCode", "Ticket Code"),
                                col("subject", "Subject"),
                                col("category", "Category"),
                                col("priority", "Priority"),
                                col("targetRole", "Target Admin Role"),
                                col("status", "Resolution Status"),
                                col("raisedBy", "Raised By User"),
                                col("assignedAdmin", "Assigned Admin")))
                        .build()
        );
    }

    private static List<ReportColumnDefDto> columnDefs(ReportColumnDefDto... cols) {
        return Arrays.asList(cols);
    }

    private static ReportColumnDefDto col(String key, String label) {
        return ReportColumnDefDto.builder().key(key).label(label).align("left").format("text").build();
    }

    private static ReportColumnDefDto col(String key, String label, String format) {
        return ReportColumnDefDto.builder().key(key).label(label).align("right").format(format).build();
    }

    private static List<SeriesPointDto> toSeries(List<Object[]> grouped) {
        if (grouped == null) {
            return new ArrayList<>();
        }
        return grouped.stream()
                .map(o -> SeriesPointDto.builder()
                        .label(String.valueOf(o[0]))
                        .count(((Number) o[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    private ReportDataResponse buildProjectsReport(String statusFilter) {
        List<Project> projects = projectRepository.findAll();
        if (statusFilter != null && !statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            projects = projects.stream()
                    .filter(p -> p.getStatus().name().equalsIgnoreCase(statusFilter))
                    .collect(Collectors.toList());
        }

        long activeCount = projects.stream().filter(p -> "IN_PROGRESS".equals(p.getStatus().name())).count();
        double avgCompletion = projects.isEmpty() ? 0 : projects.stream().mapToDouble(p -> p.getCompletionPercentage() != null ? p.getCompletionPercentage() : 0).average().orElse(0);

        List<Map<String, Object>> kpis = List.of(
                Map.of("label", "Total Projects", "value", projects.size()),
                Map.of("label", "Active Delivery Projects", "value", activeCount),
                Map.of("label", "Average Portfolio Completion", "value", String.format("%.1f%%", avgCompletion))
        );

        List<String> cols = List.of("Project Code", "Project Name", "Client", "Status", "Health", "Completion", "Estimated Hours", "Actual Hours");

        List<Map<String, Object>> rows = projects.stream().map(p -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Project Code", p.getProjectCode());
            r.put("Project Name", p.getName());
            r.put("Client", p.getClient() != null ? p.getClient().getName() : "N/A");
            r.put("Status", p.getStatus() != null ? p.getStatus().name() : "IN_PROGRESS");
            r.put("Health", p.getHealth() != null ? p.getHealth().name() : "GREEN");
            r.put("Completion", String.format("%.0f%%", p.getCompletionPercentage() != null ? p.getCompletionPercentage() : 0));
            r.put("Estimated Hours", p.getEstimatedHours() != null ? p.getEstimatedHours() : 0.0);
            r.put("Actual Hours", p.getActualHours() != null ? p.getActualHours() : 0.0);
            return r;
        }).collect(Collectors.toList());

        return ReportDataResponse.builder()
                .reportCategory("projects")
                .title("Project Portfolio Status Report")
                .subtitle("Real-time delivery progress, health indicators, and effort variance across projects")
                .summaryKpis(kpis)
                .columns(cols)
                .rows(rows)
                .totalRecords(rows.size())
                .reportSeries(toSeries(projectRepository.countByStatusGrouped()))
                .chartType("bar")
                .orientation("landscape")
                .build();
    }

    private ReportDataResponse buildWorkItemsReport(Long projectId, String statusFilter) {
        List<WorkItem> items = workItemRepository.findAll();

        if (projectId != null) {
            items = items.stream().filter(w -> w.getProject() != null && w.getProject().getId().equals(projectId)).collect(Collectors.toList());
        }
        if (statusFilter != null && !statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            items = items.stream().filter(w -> w.getStatus() != null && w.getStatus().name().equalsIgnoreCase(statusFilter)).collect(Collectors.toList());
        }

        long completedCount = items.stream().filter(w -> "COMPLETED".equals(w.getStatus().name()) || "UAT_EXIT".equals(w.getStatus().name()) || "SIT_EXIT".equals(w.getStatus().name())).count();
        double totalEst = items.stream().mapToDouble(w -> w.getEstimatedHours() != null ? w.getEstimatedHours() : 0).sum();
        double totalAct = items.stream().mapToDouble(w -> w.getActualHours() != null ? w.getActualHours() : 0).sum();

        List<Map<String, Object>> kpis = List.of(
                Map.of("label", "Total Tasks & Bugs", "value", items.size()),
                Map.of("label", "Delivered / Closed", "value", completedCount),
                Map.of("label", "Total Estimated Hours", "value", String.format("%.1f hrs", totalEst)),
                Map.of("label", "Total Logged Hours", "value", String.format("%.1f hrs", totalAct))
        );

        List<String> cols = List.of("Ticket #", "Title", "Project", "Status", "Priority", "Assignee", "Est Hours", "Act Hours", "Billing Type", "Jira ID");

        List<Map<String, Object>> rows = items.stream().map(w -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("Ticket #", w.getTicketNumber());
            r.put("Title", w.getTitle());
            r.put("Project", w.getProject() != null ? w.getProject().getProjectCode() : "N/A");
            r.put("Status", w.getStatus() != null ? w.getStatus().name() : "TODO");
            r.put("Priority", w.getPriority() != null ? w.getPriority().name() : "MEDIUM");
            r.put("Assignee", w.getAssignee() != null ? w.getAssignee().getFirstName() + " " + w.getAssignee().getLastName() : "Unassigned");
            r.put("Est Hours", w.getEstimatedHours() != null ? w.getEstimatedHours() : 0.0);
            r.put("Act Hours", w.getActualHours() != null ? w.getActualHours() : 0.0);
            r.put("Billing Type", w.getBillingType() != null ? w.getBillingType() : "Billable");
            r.put("Jira ID", w.getJiraTaskId() != null ? w.getJiraTaskId() : "-");
            return r;
        }).collect(Collectors.toList());

        return ReportDataResponse.builder()
                .reportCategory("workitems")
                .title("Work Items & Delivery Board Report")
                .subtitle("Real-time tracking of tasks, bugs, assignees, and effort hours")
                .summaryKpis(kpis)
                .columns(cols)
                .rows(rows)
                .totalRecords(rows.size())
                .reportSeries(projectId != null
                        ? toSeries(workItemRepository.countByStatusGroupedForProject(projectId))
                        : toSeries(workItemRepository.countByStatusGrouped()))
                .chartType("bar")
                .orientation("landscape")
                .build();
    }

    private ReportDataResponse buildRequirementsReport(Long projectId) {
        List<Requirement> reqs = requirementRepository.findAll();

        if (projectId != null) {
            reqs = reqs.stream().filter(r -> r.getProject() != null && r.getProject().getId().equals(projectId)).collect(Collectors.toList());
        }

        List<Map<String, Object>> kpis = List.of(
                Map.of("label", "Total Requirements", "value", reqs.size()),
                Map.of("label", "Approved Requirements", "value", reqs.stream().filter(r -> "APPROVED".equals(r.getStatus().name())).count())
        );

        List<String> cols = List.of("Req Number", "Title", "Project", "Status", "Priority", "Requester", "Est Effort", "Actual Effort");

        List<Map<String, Object>> rows = reqs.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Req Number", r.getReqNumber());
            map.put("Title", r.getTitle());
            map.put("Project", r.getProject() != null ? r.getProject().getProjectCode() : "N/A");
            map.put("Status", r.getStatus() != null ? r.getStatus().name() : "DRAFT");
            map.put("Priority", r.getPriority() != null ? r.getPriority().name() : "MEDIUM");
            map.put("Requester", r.getRequester() != null ? r.getRequester() : "N/A");
            map.put("Est Effort", r.getEstimatedEffortHours() != null ? r.getEstimatedEffortHours() : 0.0);
            map.put("Actual Effort", r.getActualEffortHours() != null ? r.getActualEffortHours() : 0.0);
            return map;
        }).collect(Collectors.toList());

        return ReportDataResponse.builder()
                .reportCategory("requirements")
                .title("Requirements & Scope Register Report")
                .subtitle("Change requests, approval statuses, and effort variance")
                .summaryKpis(kpis)
                .columns(cols)
                .rows(rows)
                .totalRecords(rows.size())
                .reportSeries(projectId != null
                        ? toSeries(requirementRepository.countByStatusGroupedForProject(projectId))
                        : toSeries(requirementRepository.countByStatusGrouped()))
                .chartType("donut")
                .orientation("portrait")
                .build();
    }

    private ReportDataResponse buildTimeLogsReport(Long projectId) {
        List<TimeEntry> entries = timeEntryRepository.findAll();

        if (projectId != null) {
            entries = entries.stream().filter(e -> e.getProject() != null && e.getProject().getId().equals(projectId)).collect(Collectors.toList());
        }

        double totalHours = entries.stream().mapToDouble(TimeEntry::getTotalHours).sum();

        List<Map<String, Object>> kpis = List.of(
                Map.of("label", "Total Time Log Entries", "value", entries.size()),
                Map.of("label", "Total Logged Effort", "value", String.format("%.1f hrs", totalHours))
        );

        List<String> cols = List.of("ID", "User", "Project", "Work Date", "Hours", "Description", "Status");

        List<Map<String, Object>> rows = entries.stream().map(e -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("ID", e.getId());
            map.put("User", e.getUser() != null ? e.getUser().getFirstName() + " " + e.getUser().getLastName() : "User");
            map.put("Project", e.getProject() != null ? e.getProject().getProjectCode() : "N/A");
            map.put("Work Date", e.getWorkDate() != null ? e.getWorkDate().toString() : "-");
            map.put("Hours", e.getTotalHours());
            map.put("Description", e.getDescription());
            map.put("Status", e.getStatus() != null ? e.getStatus().name() : "APPROVED");
            return map;
        }).collect(Collectors.toList());

        return ReportDataResponse.builder()
                .reportCategory("timelogs")
                .title("Time Tracking & Effort Audit Report")
                .subtitle("Logged hours, work dates, and approval tracking")
                .summaryKpis(kpis)
                .columns(cols)
                .rows(rows)
                .totalRecords(rows.size())
                .reportSeries(projectId != null
                        ? toHoursSeries(timeEntryRepository.hoursGroupedByDateForProject(projectId))
                        : toHoursSeries(timeEntryRepository.hoursGroupedByDate()))
                .chartType("area")
                .orientation("landscape")
                .build();
    }

    private static List<SeriesPointDto> toHoursSeries(List<Object[]> grouped) {
        if (grouped == null) {
            return new ArrayList<>();
        }
        return grouped.stream()
                .map(o -> SeriesPointDto.builder()
                        .label(String.valueOf(o[0]))
                        .count(((Number) o[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    private ReportDataResponse buildRisksAndIssuesReport(Long projectId) {
        List<Risk> risks = riskRepository.findAll();
        List<Issue> issues = issueRepository.findAll();

        List<Map<String, Object>> kpis = List.of(
                Map.of("label", "Identified Risks", "value", risks.size()),
                Map.of("label", "Active Issues", "value", issues.size())
        );

        List<String> cols = List.of("Code", "Type", "Project", "Description", "Severity / Score", "Status", "Owner");

        List<Map<String, Object>> rows = new ArrayList<>();

        for (Risk r : risks) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Code", r.getRiskCode());
            map.put("Type", "RISK");
            map.put("Project", r.getProject() != null ? r.getProject().getProjectCode() : "N/A");
            map.put("Description", r.getDescription());
            map.put("Severity / Score", "Score: " + (r.getRiskScore() != null ? r.getRiskScore() : 12));
            map.put("Status", r.getStatus() != null ? r.getStatus().name() : "IDENTIFIED");
            map.put("Owner", r.getOwner() != null ? r.getOwner().getFirstName() + " " + r.getOwner().getLastName() : "PM");
            rows.add(map);
        }

        for (Issue i : issues) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Code", i.getIssueCode());
            map.put("Type", "ISSUE");
            map.put("Project", i.getProject() != null ? i.getProject().getProjectCode() : "N/A");
            map.put("Description", i.getDescription());
            map.put("Severity / Score", i.getSeverity() != null ? i.getSeverity().name() : "HIGH");
            map.put("Status", i.getStatus() != null ? i.getStatus().name() : "OPEN");
            map.put("Owner", i.getReporter() != null ? i.getReporter().getFirstName() + " " + i.getReporter().getLastName() : "N/A");
            rows.add(map);
        }

        List<SeriesPointDto> series = new ArrayList<>();
        series.addAll(toSeries(projectId != null ? riskRepository.countByStatusGroupedForProject(projectId) : riskRepository.countByStatusGrouped()));
        series.addAll(toSeries(projectId != null ? issueRepository.countBySeverityGroupedForProject(projectId) : issueRepository.countBySeverityGrouped()));

        return ReportDataResponse.builder()
                .reportCategory("risks")
                .title("Risks & Issues Register Report")
                .subtitle("Risk matrix, open issues, severity scoring, and mitigation ownership")
                .summaryKpis(kpis)
                .columns(cols)
                .rows(rows)
                .totalRecords(rows.size())
                .reportSeries(series)
                .chartType("donut")
                .orientation("portrait")
                .build();
    }

    private ReportDataResponse buildSupportTicketsReport(String statusFilter) {
        List<SupportTicket> tickets = supportTicketRepository.findAllByOrderByCreatedAtDesc();

        if (statusFilter != null && !statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            tickets = tickets.stream().filter(t -> t.getStatus().name().equalsIgnoreCase(statusFilter)).collect(Collectors.toList());
        }

        long openCount = tickets.stream().filter(t -> "OPEN".equals(t.getStatus().name()) || "IN_REVIEW".equals(t.getStatus().name())).count();

        List<Map<String, Object>> kpis = List.of(
                Map.of("label", "Total Support Tickets", "value", tickets.size()),
                Map.of("label", "Active Open Queries", "value", openCount)
        );

        List<String> cols = List.of("Ticket Code", "Subject", "Category", "Priority", "Target Role", "Status", "Raised By", "Assigned Admin");

        List<Map<String, Object>> rows = tickets.stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Ticket Code", t.getTicketCode());
            map.put("Subject", t.getSubject());
            map.put("Category", t.getCategory().name());
            map.put("Priority", t.getPriority().name());
            map.put("Target Role", t.getTargetRole());
            map.put("Status", t.getStatus().name());
            map.put("Raised By", t.getCreatedByName());
            map.put("Assigned Admin", t.getAssignedToName() != null ? t.getAssignedToName() : "Super Admin");
            return map;
        }).collect(Collectors.toList());

        return ReportDataResponse.builder()
                .reportCategory("support-tickets")
                .title("Support Tickets & Queries Report")
                .subtitle("Real-time support ticket resolution telemetry for Super Admin & Admin queues")
                .summaryKpis(kpis)
                .columns(cols)
                .rows(rows)
                .totalRecords(rows.size())
                .reportSeries(toSeries(supportTicketRepository.countByStatusGrouped()))
                .chartType("bar")
                .orientation("landscape")
                .build();
    }
}