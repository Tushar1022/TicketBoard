package com.aurionpro.ticketboard.workitem.service;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.document.storage.DocumentStorageService;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.project.service.ProjectService;
import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.workitem.dto.*;
import com.aurionpro.ticketboard.workitem.entity.TaskDocument;
import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.entity.WorkItemBlocker;
import com.aurionpro.ticketboard.workitem.entity.WorkItemDependency;
import com.aurionpro.ticketboard.workitem.enums.WorkItemPriority;
import com.aurionpro.ticketboard.workitem.enums.WorkItemSeverity;
import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import com.aurionpro.ticketboard.workitem.enums.WorkItemType;
import com.aurionpro.ticketboard.workitem.repository.TaskDocumentRepository;
import com.aurionpro.ticketboard.workitem.repository.WorkItemBlockerRepository;
import com.aurionpro.ticketboard.workitem.repository.WorkItemDependencyRepository;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemDependencyRepository dependencyRepository;
    private final WorkItemBlockerRepository blockerRepository;
    private final TaskDocumentRepository taskDocumentRepository;
    private final DocumentStorageService documentStorageService;
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<WorkItemDto> getAllWorkItems() {
        return workItemRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkItemDto> getWorkItemsByProject(Long projectId) {
        return workItemRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkItemDto> getWorkItemsByRequirement(Long requirementId) {
        return workItemRepository.findByRequirementId(requirementId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkItemDto> getWorkItemsByAssignee(Long assigneeId) {
        return workItemRepository.findByAssigneeId(assigneeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkItemDto getWorkItemById(Long id) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));
        return mapToDto(item);
    }

    @Transactional
    public WorkItemDto createWorkItem(WorkItemCreateDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        Requirement requirement = null;
        if (dto.getRequirementId() != null) {
            requirement = requirementRepository.findById(dto.getRequirementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requirement", "id", dto.getRequirementId()));
        }

        WorkItem parentTask = null;
        if (dto.getParentTaskId() != null) {
            parentTask = workItemRepository.findById(dto.getParentTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("ParentTask", "id", dto.getParentTaskId()));
        }

        User assignee = null;
        if (dto.getAssigneeId() != null) {
            assignee = userRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getAssigneeId()));
        }

        User reporter = getCurrentUser();
        if (dto.getReporterId() != null) {
            reporter = userRepository.findById(dto.getReporterId()).orElse(reporter);
        }

        WorkItemType type = dto.getType() != null ? dto.getType() : WorkItemType.TASK;

        String ticketNumber = dto.getTicketNumber();
        if (ticketNumber == null || ticketNumber.isBlank()) {
            String prefix = type == WorkItemType.BUG ? "BUG" : (project.getProjectCode() != null ? project.getProjectCode() : "CR");
            int count = workItemRepository.countByProjectId(project.getId()) + 1;
            ticketNumber = prefix + "#" + count;
        } else {
            ticketNumber = ticketNumber.trim();
            if (workItemRepository.existsByTicketNumber(ticketNumber)) {
                throw new BadRequestException("Ticket number already exists: " + ticketNumber);
            }
        }

        WorkItem item = WorkItem.builder()
                .ticketNumber(ticketNumber)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(type)
                .priority(dto.getPriority() != null ? dto.getPriority() : WorkItemPriority.MEDIUM)
                .severity(dto.getSeverity() != null ? dto.getSeverity() : WorkItemSeverity.MEDIUM)
                .status(dto.getStatus() != null ? dto.getStatus() : WorkItemStatus.TODO)
                .project(project)
                .requirement(requirement)
                .parentTask(parentTask)
                .assignee(assignee)
                .reporter(reporter)
                .estimatedHours(dto.getEstimatedHours() != null ? dto.getEstimatedHours() : 0.0)
                .actualHours(0.0)
                .startDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now())
                .dueDate(dto.getDueDate())
                .devExitDate(dto.getDevExitDate())
                .sitExitDate(dto.getSitExitDate())
                .uatExitDate(dto.getUatExitDate())
                .sdDeliveryDate(dto.getSdDeliveryDate())
                .goLiveDate(dto.getGoLiveDate())
                .devEffortDays(dto.getDevEffortDays() != null ? dto.getDevEffortDays() : 3.0)
                .qcEffortDays(dto.getQcEffortDays() != null ? dto.getQcEffortDays() : 3.0)
                .durationDays(dto.getDurationDays() != null ? dto.getDurationDays() : 14)
                .completionPercentage(dto.getCompletionPercentage() != null ? dto.getCompletionPercentage() : 0)
                .billingType(dto.getBillingType() != null ? dto.getBillingType() : "None")
                .associatedTeam(dto.getAssociatedTeam() != null ? dto.getAssociatedTeam() : null)
                .jiraTaskId(dto.getJiraTaskId())
                .jiraStatus(dto.getJiraStatus() != null ? dto.getJiraStatus() : "Not Created")
                .tags(dto.getTags() != null ? dto.getTags() : "P1")
                .reminder(dto.getReminder() != null ? dto.getReminder() : "None")
                .recurrence(dto.getRecurrence() != null ? dto.getRecurrence() : "None")
                .labels(dto.getLabels())
                .build();

        WorkItem saved = workItemRepository.save(item);

        updateProjectProgressAndTotals(project);

        activityLogService.logEvent(
                "WORK_ITEM",
                saved.getId(),
                saved.getTicketNumber(),
                TimelineEventType.CREATED,
                String.format("[%s] %s created: %s", saved.getType(), saved.getTicketNumber(), saved.getTitle()),
                "Assigned to: " + (assignee != null ? assignee.getFullName() : "Unassigned"),
                null,
                saved.getStatus().name()
        );

        return mapToDto(saved);
    }

    @Transactional
    public WorkItemDto updateWorkItem(Long id, WorkItemCreateDto dto) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));

        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        if (dto.getType() != null) item.setType(dto.getType());
        if (dto.getPriority() != null) item.setPriority(dto.getPriority());
        if (dto.getSeverity() != null) item.setSeverity(dto.getSeverity());
        if (dto.getStatus() != null) item.setStatus(dto.getStatus());
        if (dto.getEstimatedHours() != null) item.setEstimatedHours(dto.getEstimatedHours());
        if (dto.getStartDate() != null) item.setStartDate(dto.getStartDate());
        if (dto.getDueDate() != null) item.setDueDate(dto.getDueDate());
        if (dto.getDevExitDate() != null) item.setDevExitDate(dto.getDevExitDate());
        if (dto.getSitExitDate() != null) item.setSitExitDate(dto.getSitExitDate());
        if (dto.getUatExitDate() != null) item.setUatExitDate(dto.getUatExitDate());
        if (dto.getSdDeliveryDate() != null) item.setSdDeliveryDate(dto.getSdDeliveryDate());
        if (dto.getGoLiveDate() != null) item.setGoLiveDate(dto.getGoLiveDate());
        if (dto.getDevEffortDays() != null) item.setDevEffortDays(dto.getDevEffortDays());
        if (dto.getQcEffortDays() != null) item.setQcEffortDays(dto.getQcEffortDays());
        if (dto.getDurationDays() != null) item.setDurationDays(dto.getDurationDays());
        if (dto.getCompletionPercentage() != null) item.setCompletionPercentage(dto.getCompletionPercentage());
        if (dto.getBillingType() != null) item.setBillingType(dto.getBillingType());
        if (dto.getAssociatedTeam() != null) item.setAssociatedTeam(dto.getAssociatedTeam());
        if (dto.getJiraTaskId() != null) item.setJiraTaskId(dto.getJiraTaskId());
        if (dto.getJiraStatus() != null) item.setJiraStatus(dto.getJiraStatus());
        if (dto.getTags() != null) item.setTags(dto.getTags());
        if (dto.getReminder() != null) item.setReminder(dto.getReminder());
        if (dto.getRecurrence() != null) item.setRecurrence(dto.getRecurrence());
        if (dto.getLabels() != null) item.setLabels(dto.getLabels());

        if (dto.getAssigneeId() != null) {
            User assignee = userRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getAssigneeId()));
            item.setAssignee(assignee);
        }

        if (dto.getRequirementId() != null) {
            Requirement requirement = requirementRepository.findById(dto.getRequirementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requirement", "id", dto.getRequirementId()));
            item.setRequirement(requirement);
        }

        WorkItem saved = workItemRepository.save(item);
        updateProjectProgressAndTotals(saved.getProject());
        return mapToDto(saved);
    }

    @Transactional
    public WorkItemDto updateStatus(Long id, WorkItemStatusUpdateDto dto) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));

        WorkItemStatus oldStatus = item.getStatus();
        WorkItemStatus newStatus = dto.getStatus();

        item.setStatus(newStatus);
        User currentUser = getCurrentUser();

        if (newStatus == WorkItemStatus.BLOCKED) {
            LocalDateTime now = LocalDateTime.now();
            item.setBlockedSince(now);
            item.setBlockedReason(dto.getBlockedReason());
            item.setBlockedOwner(dto.getBlockedOwner());

            WorkItemBlocker blocker = WorkItemBlocker.builder()
                    .workItem(item)
                    .reason(dto.getBlockedReason() != null ? dto.getBlockedReason() : "Blocked")
                    .owner(dto.getBlockedOwner() != null ? dto.getBlockedOwner() : "Unassigned")
                    .blockedSince(now)
                    .expectedResolutionDate(dto.getExpectedResolutionDate())
                    .build();
            blockerRepository.save(blocker);

            activityLogService.logEvent(
                    "WORK_ITEM",
                    item.getId(),
                    item.getTicketNumber(),
                    TimelineEventType.BLOCKED,
                    String.format("%s marked BLOCKED: %s", item.getTicketNumber(), dto.getBlockedReason()),
                    "Blocked owner: " + dto.getBlockedOwner(),
                    oldStatus.name(),
                    newStatus.name()
            );
        } else {
            if (oldStatus == WorkItemStatus.BLOCKED) {
                List<WorkItemBlocker> activeBlockers = blockerRepository.findByWorkItemId(item.getId());
                for (WorkItemBlocker b : activeBlockers) {
                    if (b.getResolvedAt() == null) {
                        b.setResolvedAt(LocalDateTime.now());
                        b.setResolvedBy(currentUser);
                        b.setResolutionNotes(dto.getComment() != null ? dto.getComment() : "Unblocked");
                        Duration duration = Duration.between(b.getBlockedSince(), b.getResolvedAt());
                        b.setDurationHours(duration.toMinutes() / 60.0);
                        blockerRepository.save(b);
                    }
                }
                item.setBlockedSince(null);
                item.setBlockedReason(null);
                item.setBlockedOwner(null);

                activityLogService.logEvent(
                        "WORK_ITEM",
                        item.getId(),
                        item.getTicketNumber(),
                        TimelineEventType.UNBLOCKED,
                        String.format("%s UNBLOCKED -> status: %s", item.getTicketNumber(), newStatus),
                        dto.getComment(),
                        oldStatus.name(),
                        newStatus.name()
                );
            } else {
                activityLogService.logEvent(
                        "WORK_ITEM",
                        item.getId(),
                        item.getTicketNumber(),
                        TimelineEventType.STATUS_CHANGED,
                        String.format("%s status changed: %s -> %s", item.getTicketNumber(), oldStatus, newStatus),
                        dto.getComment(),
                        oldStatus.name(),
                        newStatus.name()
                );
            }
        }

        if (newStatus == WorkItemStatus.COMPLETED || newStatus == WorkItemStatus.CLOSED || newStatus == WorkItemStatus.GO_LIVE) {
            item.setCompletedDate(LocalDate.now());
            item.setCompletionPercentage(100);
        }

        WorkItem saved = workItemRepository.save(item);
        updateProjectProgressAndTotals(saved.getProject());
        return mapToDto(saved);
    }

    @Transactional
    public WorkItemDto syncWithJira(Long id) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", id));

        String jiraKey = "JIRA-" + (1000 + (int)(Math.random() * 8999));
        item.setJiraTaskId(jiraKey);
        item.setJiraStatus("Created");

        WorkItem saved = workItemRepository.save(item);

        activityLogService.logEvent(
                "WORK_ITEM",
                saved.getId(),
                saved.getTicketNumber(),
                TimelineEventType.STATUS_CHANGED,
                String.format("Synced to Jira -> Task ID %s generated", jiraKey),
                "Jira creation status: Created",
                null,
                "JIRA_SYNCED"
        );

        return mapToDto(saved);
    }

    @Transactional
    public TaskDocumentDto addDocument(Long workItemId, String fileName, String fileType, Long fileSize, String fileUrl) {
        WorkItem item = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", workItemId));

        User currentUser = getCurrentUser();

        TaskDocument doc = TaskDocument.builder()
                .workItem(item)
                .fileName(fileName)
                .fileType(fileType != null ? fileType : "application/pdf")
                .fileSize(fileSize != null ? fileSize : 1048576L)
                .fileUrl(fileUrl != null ? fileUrl : "/documents/" + fileName)
                .uploadedBy(currentUser)
                .build();

        TaskDocument saved = taskDocumentRepository.save(doc);

        activityLogService.logEvent(
                "WORK_ITEM",
                item.getId(),
                item.getTicketNumber(),
                TimelineEventType.COMMENT_ADDED,
                String.format("Document uploaded: %s (%d KB)", fileName, saved.getFileSize() / 1024),
                "Uploaded by: " + (currentUser != null ? currentUser.getFullName() : "User"),
                null,
                "DOCUMENT_ATTACHED"
        );

        return mapDocumentToDto(saved);
    }

    @Transactional
    public TaskDocumentDto uploadDocument(Long workItemId, org.springframework.web.multipart.MultipartFile file) {
        WorkItem item = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", "id", workItemId));

        String storedPath = documentStorageService.store(file, "work-items");
        User currentUser = getCurrentUser();

        TaskDocument doc = TaskDocument.builder()
                .workItem(item)
                .fileName(StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"))
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileUrl("/api/v1/work-items/documents/" + file.getOriginalFilename())
                .filePath(storedPath)
                .uploadedBy(currentUser)
                .build();

        TaskDocument saved = taskDocumentRepository.save(doc);

        activityLogService.logEvent(
                "WORK_ITEM",
                item.getId(),
                item.getTicketNumber(),
                TimelineEventType.COMMENT_ADDED,
                String.format("Document uploaded: %s (%d KB)", saved.getFileName(), saved.getFileSize() / 1024),
                "Uploaded by: " + (currentUser != null ? currentUser.getFullName() : "User"),
                null,
                "DOCUMENT_ATTACHED"
        );

        return mapDocumentToDto(saved);
    }

    @Transactional
    public TaskDocument getDocumentEntity(Long docId) {
        return taskDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));
    }

    @Transactional
    public com.aurionpro.ticketboard.document.storage.StoredDocumentInfo loadDocument(Long docId) {
        TaskDocument doc = getDocumentEntity(docId);
        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new BadRequestException("Document was attached as a link without stored file");
        }
        org.springframework.core.io.Resource resource = documentStorageService.loadAsResource(doc.getFilePath());
        return new com.aurionpro.ticketboard.document.storage.StoredDocumentInfo(
                doc.getFileName(),
                doc.getFileType() != null ? doc.getFileType() : "application/octet-stream",
                resource);
    }

    @Transactional(readOnly = true)
    public List<TaskDocumentDto> getDocuments(Long workItemId) {
        return taskDocumentRepository.findByWorkItemIdOrderByCreatedAtDesc(workItemId).stream()
                .map(this::mapDocumentToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(Long docId) {
        TaskDocument doc = taskDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));
        documentStorageService.delete(doc.getFilePath());
        taskDocumentRepository.delete(doc);
    }

    private TaskDocumentDto mapDocumentToDto(TaskDocument d) {
        return TaskDocumentDto.builder()
                .id(d.getId())
                .workItemId(d.getWorkItem() != null ? d.getWorkItem().getId() : null)
                .fileName(d.getFileName())
                .fileType(d.getFileType())
                .fileSize(d.getFileSize())
                .fileUrl(d.getFileUrl())
                .downloadUrl("/api/v1/work-items/documents/" + d.getId() + "/download")
                .uploadedById(d.getUploadedBy() != null ? d.getUploadedBy().getId() : null)
                .uploadedByName(d.getUploadedBy() != null ? d.getUploadedBy().getFullName() : "User")
                .uploadedAt(d.getCreatedAt())
                .build();
    }

    @Transactional
    public DependencyDto addDependency(DependencyDto dto) {
        WorkItem source = workItemRepository.findById(dto.getSourceItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Source WorkItem", "id", dto.getSourceItemId()));
        WorkItem target = workItemRepository.findById(dto.getTargetItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Target WorkItem", "id", dto.getTargetItemId()));

        WorkItemDependency dependency = WorkItemDependency.builder()
                .sourceItem(source)
                .targetItem(target)
                .dependencyType(dto.getDependencyType() != null ? dto.getDependencyType() : com.aurionpro.ticketboard.workitem.enums.DependencyType.BLOCKS)
                .build();

        WorkItemDependency saved = dependencyRepository.save(dependency);

        return DependencyDto.builder()
                .id(saved.getId())
                .sourceItemId(source.getId())
                .sourceTicketNumber(source.getTicketNumber())
                .sourceTitle(source.getTitle())
                .targetItemId(target.getId())
                .targetTicketNumber(target.getTicketNumber())
                .targetTitle(target.getTitle())
                .dependencyType(saved.getDependencyType())
                .build();
    }

    @Transactional
    public void removeDependency(Long dependencyId) {
        if (!dependencyRepository.existsById(dependencyId)) {
            throw new ResourceNotFoundException("WorkItemDependency", "id", dependencyId);
        }
        dependencyRepository.deleteById(dependencyId);
    }

    @Transactional
    public void deleteWorkItem(Long id) {
        if (!workItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("WorkItem", "id", id);
        }
        workItemRepository.deleteById(id);
    }

    private void updateProjectProgressAndTotals(Project project) {
        if (project == null) return;
        Double totalEstimated = workItemRepository.sumEstimatedHoursByProject(project.getId());
        Double completedEstimated = workItemRepository.sumCompletedEstimatedHoursByProject(project.getId());

        if (totalEstimated != null && totalEstimated > 0) {
            project.setEstimatedHours(totalEstimated);
            if (completedEstimated != null) {
                double pct = (completedEstimated / totalEstimated) * 100.0;
                project.setCompletionPercentage(Math.round(pct * 10.0) / 10.0);
            }
        }
        projectService.recalculateProjectMetrics(project);
        projectRepository.save(project);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public WorkItemDto mapToDto(WorkItem w) {
        List<WorkItemDto> subtaskDtos = w.getSubtasks() != null ?
                w.getSubtasks().stream().map(this::mapToDtoSummary).collect(Collectors.toList()) : List.of();

        List<DependencyDto> depDtos = dependencyRepository.findBySourceItemId(w.getId()).stream()
                .map(d -> DependencyDto.builder()
                        .id(d.getId())
                        .sourceItemId(w.getId())
                        .sourceTicketNumber(w.getTicketNumber())
                        .sourceTitle(w.getTitle())
                        .targetItemId(d.getTargetItem().getId())
                        .targetTicketNumber(d.getTargetItem().getTicketNumber())
                        .targetTitle(d.getTargetItem().getTitle())
                        .dependencyType(d.getDependencyType())
                        .build())
                .collect(Collectors.toList());

        List<TaskDocumentDto> docs = taskDocumentRepository.findByWorkItemIdOrderByCreatedAtDesc(w.getId()).stream()
                .map(this::mapDocumentToDto)
                .collect(Collectors.toList());

        double est = w.getEstimatedHours() != null ? w.getEstimatedHours() : 0.0;
        double act = w.getActualHours() != null ? w.getActualHours() : 0.0;
        double diff = Math.round((est - act) * 10.0) / 10.0;

        return WorkItemDto.builder()
                .id(w.getId())
                .ticketNumber(w.getTicketNumber())
                .title(w.getTitle())
                .description(w.getDescription())
                .type(w.getType())
                .priority(w.getPriority())
                .severity(w.getSeverity())
                .status(w.getStatus())
                .projectId(w.getProject() != null ? w.getProject().getId() : null)
                .projectCode(w.getProject() != null ? w.getProject().getProjectCode() : null)
                .projectName(w.getProject() != null ? w.getProject().getName() : null)
                .requirementId(w.getRequirement() != null ? w.getRequirement().getId() : null)
                .reqNumber(w.getRequirement() != null ? w.getRequirement().getReqNumber() : null)
                .reqTitle(w.getRequirement() != null ? w.getRequirement().getTitle() : null)
                .parentTaskId(w.getParentTask() != null ? w.getParentTask().getId() : null)
                .parentTaskTitle(w.getParentTask() != null ? w.getParentTask().getTitle() : null)
                .assigneeId(w.getAssignee() != null ? w.getAssignee().getId() : null)
                .assigneeName(w.getAssignee() != null ? w.getAssignee().getFullName() : "Unassigned")
                .reporterId(w.getReporter() != null ? w.getReporter().getId() : null)
                .reporterName(w.getReporter() != null ? w.getReporter().getFullName() : "System")
                .estimatedHours(est)
                .actualHours(act)
                .differenceHours(diff)
                .startDate(w.getStartDate())
                .dueDate(w.getDueDate())
                .completedDate(w.getCompletedDate())
                .devExitDate(w.getDevExitDate())
                .sitExitDate(w.getSitExitDate())
                .uatExitDate(w.getUatExitDate())
                .sdDeliveryDate(w.getSdDeliveryDate())
                .goLiveDate(w.getGoLiveDate())
                .devEffortDays(w.getDevEffortDays())
                .qcEffortDays(w.getQcEffortDays())
                .durationDays(w.getDurationDays())
                .completionPercentage(w.getCompletionPercentage() != null ? w.getCompletionPercentage() : 0)
                .billingType(w.getBillingType() != null ? w.getBillingType() : "None")
                .associatedTeam(w.getAssociatedTeam() != null ? w.getAssociatedTeam() : null)
                .jiraTaskId(w.getJiraTaskId())
                .jiraStatus(w.getJiraStatus() != null ? w.getJiraStatus() : "Not Created")
                .tags(w.getTags())
                .reminder(w.getReminder())
                .recurrence(w.getRecurrence())
                .blockedSince(w.getBlockedSince())
                .blockedReason(w.getBlockedReason())
                .blockedOwner(w.getBlockedOwner())
                .labels(w.getLabels())
                .documentsCount(docs.size())
                .commentsCount(0)
                .subtasks(subtaskDtos)
                .dependencies(depDtos)
                .documents(docs)
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    private WorkItemDto mapToDtoSummary(WorkItem w) {
        return WorkItemDto.builder()
                .id(w.getId())
                .ticketNumber(w.getTicketNumber())
                .title(w.getTitle())
                .type(w.getType())
                .priority(w.getPriority())
                .status(w.getStatus())
                .assigneeId(w.getAssignee() != null ? w.getAssignee().getId() : null)
                .assigneeName(w.getAssignee() != null ? w.getAssignee().getFullName() : null)
                .estimatedHours(w.getEstimatedHours())
                .actualHours(w.getActualHours())
                .completionPercentage(w.getCompletionPercentage() != null ? w.getCompletionPercentage() : 0)
                .build();
    }
}
