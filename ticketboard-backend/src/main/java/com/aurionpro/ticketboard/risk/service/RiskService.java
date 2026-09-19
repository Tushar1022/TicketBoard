package com.aurionpro.ticketboard.risk.service;

import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.entity.Milestone;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.MilestoneRepository;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.project.service.ProjectService;
import com.aurionpro.ticketboard.risk.dto.*;
import com.aurionpro.ticketboard.risk.entity.*;
import com.aurionpro.ticketboard.risk.enums.IssueSeverity;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
import com.aurionpro.ticketboard.risk.enums.RiskStatus;
import com.aurionpro.ticketboard.risk.repository.*;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskRepository riskRepository;
    private final IssueRepository issueRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final IssueHistoryRepository issueHistoryRepository;
    private final IssueWatcherRepository issueWatcherRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<RiskDto> getAllRisks() {
        return riskRepository.findAll().stream()
                .map(this::mapRiskToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RiskDto> getRisksByProject(Long projectId) {
        return riskRepository.findByProjectId(projectId).stream()
                .map(this::mapRiskToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RiskDto createRisk(RiskDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        String riskCode = dto.getRiskCode();
        if (riskCode == null || riskCode.isBlank()) {
            riskCode = "RSK-" + (100 + riskRepository.count() + 1);
        } else {
            riskCode = riskCode.toUpperCase().trim();
            if (riskRepository.existsByRiskCode(riskCode)) {
                throw new BadRequestException("Risk code already exists: " + riskCode);
            }
        }

        User owner = null;
        if (dto.getOwnerId() != null) {
            owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
        }

        int prob = dto.getProbability() != null ? dto.getProbability() : 3;
        int impact = dto.getImpact() != null ? dto.getImpact() : 3;

        Risk risk = Risk.builder()
                .riskCode(riskCode)
                .project(project)
                .description(dto.getDescription())
                .probability(prob)
                .impact(impact)
                .riskScore(prob * impact)
                .owner(owner)
                .mitigationPlan(dto.getMitigationPlan())
                .targetDate(dto.getTargetDate())
                .status(dto.getStatus() != null ? dto.getStatus() : RiskStatus.IDENTIFIED)
                .build();

        Risk saved = riskRepository.save(risk);
        projectService.recalculateProjectMetrics(project);
        projectRepository.save(project);

        return mapRiskToDto(saved);
    }

    @Transactional
    public RiskDto updateRisk(Long id, RiskDto dto) {
        Risk risk = riskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Risk", "id", id));

        if (dto.getDescription() != null) risk.setDescription(dto.getDescription());
        if (dto.getProbability() != null) risk.setProbability(dto.getProbability());
        if (dto.getImpact() != null) risk.setImpact(dto.getImpact());
        risk.setRiskScore(risk.getProbability() * risk.getImpact());
        if (dto.getMitigationPlan() != null) risk.setMitigationPlan(dto.getMitigationPlan());
        if (dto.getTargetDate() != null) risk.setTargetDate(dto.getTargetDate());
        if (dto.getStatus() != null) risk.setStatus(dto.getStatus());

        if (dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
            risk.setOwner(owner);
        }

        Risk saved = riskRepository.save(risk);
        projectService.recalculateProjectMetrics(saved.getProject());
        projectRepository.save(saved.getProject());

        return mapRiskToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getIssuesByProject(Long projectId) {
        return issueRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapIssueToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getIssues(Long projectId, IssueSeverity severity, IssueStatus status,
                                    Long assigneeId, Long milestoneId, Boolean overdue, String search) {
        List<Issue> issues = projectId != null
                ? issueRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                : issueRepository.findAll();

        List<Issue> filtered = issues.stream()
                .filter(i -> severity == null || i.getSeverity() == severity)
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> assigneeId == null || (i.getAssignee() != null && i.getAssignee().getId().equals(assigneeId)))
                .filter(i -> milestoneId == null || (i.getMilestone() != null && i.getMilestone().getId().equals(milestoneId)))
                .filter(i -> overdue == null || !overdue || (
                        i.getDueDate() != null
                            && i.getDueDate().isBefore(java.time.LocalDate.now())
                            && i.getStatus() != IssueStatus.RESOLVED
                            && i.getStatus() != IssueStatus.CLOSED))
                .filter(i -> !StringUtils.hasText(search)
                        || (i.getIssueCode() != null && i.getIssueCode().toLowerCase().contains(search.toLowerCase()))
                        || (i.getTitle() != null && i.getTitle().toLowerCase().contains(search.toLowerCase()))
                        || (i.getDescription() != null && i.getDescription().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());

        return filtered.stream().map(this::mapIssueToDto).collect(Collectors.toList());
    }

    @Transactional
    public IssueDto createIssue(IssueDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        String issueCode = nextIssueCode(project);

        User reporter = resolveUser(dto.getReporterId());
        User assignee = resolveUser(dto.getAssigneeId());
        Milestone milestone = resolveMilestone(dto.getMilestoneId(), project);

        Issue issue = Issue.builder()
                .issueCode(issueCode)
                .project(project)
                .milestone(milestone)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .severity(dto.getSeverity() != null ? dto.getSeverity() : IssueSeverity.MEDIUM)
                .status(dto.getStatus() != null ? dto.getStatus() : IssueStatus.OPEN)
                .reporter(reporter)
                .assignee(assignee)
                .priority(dto.getPriority())
                .classification(dto.getClassification())
                .category(dto.getCategory())
                .environment(dto.getEnvironment())
                .affectedModule(dto.getAffectedModule())
                .affectedVersion(dto.getAffectedVersion())
                .expectedBehavior(dto.getExpectedBehavior())
                .actualBehavior(dto.getActualBehavior())
                .stepsToReproduce(dto.getStepsToReproduce())
                .resolution(dto.getResolution())
                .acceptanceCriteria(dto.getAcceptanceCriteria())
                .crValue(dto.getCrValue())
                .crManDays(dto.getCrManDays())
                .estimatedFixHours(dto.getEstimatedFixHours())
                .percentage(dto.getPercentage() != null ? dto.getPercentage() : 0)
                .dueDate(dto.getDueDate())
                .linkedTaskIds(joinTaskIds(dto.getLinkedTaskIds()))
                .build();

        Issue saved = issueRepository.save(issue);
        recordHistory(saved, reporter, "CREATED", null, null, null,
                "Issue created with code " + issueCode);
        return mapIssueToDto(saved);
    }

    @Transactional
    public IssueDto updateIssue(Long id, IssueDto dto) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", id));

        User reporter = issue.getReporter();
        if (dto.getTitle() != null) issue.setTitle(dto.getTitle());
        if (dto.getDescription() != null) issue.setDescription(dto.getDescription());
        if (dto.getSeverity() != null) {
            recordChange(issue, reporter, "severity",
                    issue.getSeverity() != null ? issue.getSeverity().name() : null,
                    dto.getSeverity().name());
            issue.setSeverity(dto.getSeverity());
        }
        if (dto.getResolution() != null) issue.setResolution(dto.getResolution());
        if (dto.getClassification() != null) issue.setClassification(dto.getClassification());
        if (dto.getCategory() != null) issue.setCategory(dto.getCategory());
        if (dto.getPriority() != null) issue.setPriority(dto.getPriority());
        if (dto.getEnvironment() != null) issue.setEnvironment(dto.getEnvironment());
        if (dto.getAffectedModule() != null) issue.setAffectedModule(dto.getAffectedModule());
        if (dto.getAffectedVersion() != null) issue.setAffectedVersion(dto.getAffectedVersion());
        if (dto.getExpectedBehavior() != null) issue.setExpectedBehavior(dto.getExpectedBehavior());
        if (dto.getActualBehavior() != null) issue.setActualBehavior(dto.getActualBehavior());
        if (dto.getStepsToReproduce() != null) issue.setStepsToReproduce(dto.getStepsToReproduce());
        if (dto.getAcceptanceCriteria() != null) issue.setAcceptanceCriteria(dto.getAcceptanceCriteria());
        if (dto.getCrValue() != null) issue.setCrValue(dto.getCrValue());
        if (dto.getCrManDays() != null) issue.setCrManDays(dto.getCrManDays());
        if (dto.getEstimatedFixHours() != null) issue.setEstimatedFixHours(dto.getEstimatedFixHours());
        if (dto.getPercentage() != null) issue.setPercentage(dto.getPercentage());
        if (dto.getDueDate() != null) issue.setDueDate(dto.getDueDate());
        if (dto.getLinkedTaskIds() != null) issue.setLinkedTaskIds(joinTaskIds(dto.getLinkedTaskIds()));

        if (dto.getReporterId() != null && !dto.getReporterId().equals(reporter != null ? reporter.getId() : null)) {
            reporter = resolveUser(dto.getReporterId());
            issue.setReporter(reporter);
        }
        if (dto.getAssigneeId() != null) {
            User assignee = resolveUser(dto.getAssigneeId());
            recordChange(issue, reporter, "assignee",
                    issue.getAssignee() != null ? issue.getAssignee().getFullName() : null,
                    assignee.getFullName());
            issue.setAssignee(assignee);
        }
        if (dto.getMilestoneId() != null) {
            Milestone milestone = resolveMilestone(dto.getMilestoneId(), issue.getProject());
            recordChange(issue, reporter, "milestone",
                    issue.getMilestone() != null ? issue.getMilestone().getName() : null,
                    milestone.getName());
            issue.setMilestone(milestone);
        }
        if (dto.getStatus() != null) {
            transitionStatus(issue, dto.getStatus(), reporter);
        }

        return mapIssueToDto(issueRepository.save(issue));
    }

    @Transactional(readOnly = true)
    public IssueDto getIssueById(Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", id));
        return mapIssueToDto(issue);
    }

    @Transactional
    public void deleteIssue(Long id) {
        if (!issueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Issue", "id", id);
        }
        issueCommentRepository.deleteByIssueId(id);
        issueHistoryRepository.deleteByIssueId(id);
        issueWatcherRepository.deleteByIssueId(id);
        issueRepository.deleteById(id);
    }

    @Transactional
    public void deleteRisk(Long id) {
        if (!riskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Risk", "id", id);
        }
        riskRepository.deleteById(id);
    }

    private void transitionStatus(Issue issue, IssueStatus target, User actor) {
        IssueStatus current = issue.getStatus();
        if (current == target) return;

        Map<IssueStatus, Set<IssueStatus>> allowed = new HashMap<>();
        allowed.put(IssueStatus.OPEN, EnumSet.of(IssueStatus.IN_PROGRESS, IssueStatus.RESOLVED, IssueStatus.CLOSED));
        allowed.put(IssueStatus.IN_PROGRESS, EnumSet.of(IssueStatus.RESOLVED, IssueStatus.CLOSED));
        allowed.put(IssueStatus.RESOLVED, EnumSet.of(IssueStatus.CLOSED, IssueStatus.REOPENED));
        allowed.put(IssueStatus.CLOSED, EnumSet.of(IssueStatus.REOPENED));
        allowed.put(IssueStatus.REOPENED, EnumSet.of(IssueStatus.IN_PROGRESS, IssueStatus.RESOLVED, IssueStatus.CLOSED));

        Set<IssueStatus> next = allowed.getOrDefault(current, EnumSet.noneOf(IssueStatus.class));
        if (!next.contains(target)) {
            throw new BadRequestException("Illegal issue transition: " + current + " -> " + target);
        }

        recordHistory(issue, actor, "STATUS_CHANGE", "status", current.name(), target.name(), null);

        if (target == IssueStatus.RESOLVED && issue.getResolvedAt() == null) {
            issue.setResolvedAt(LocalDateTime.now());
        }
        if (target == IssueStatus.CLOSED) {
            issue.setClosedAt(LocalDateTime.now());
            issue.setPercentage(100);
        }
        if (target == IssueStatus.REOPENED) {
            issue.setResolvedAt(null);
            issue.setClosedAt(null);
        }
        issue.setStatus(target);
    }

    // ---- Comments ----

    @Transactional(readOnly = true)
    public List<IssueCommentDto> getComments(Long issueId) {
        return issueCommentRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(c -> IssueCommentDto.builder()
                        .id(c.getId())
                        .issueId(issueId)
                        .authorId(c.getAuthor() != null ? c.getAuthor().getId() : null)
                        .authorName(c.getAuthor() != null ? c.getAuthor().getFullName() : null)
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public IssueCommentDto addComment(Long issueId, Long authorId, String content) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Comment content is required");
        }
        User author = resolveUser(authorId);
        IssueComment comment = issueCommentRepository.save(IssueComment.builder()
                .issue(issue)
                .author(author)
                .content(content.trim())
                .build());
        recordHistory(issue, author, "COMMENT_ADDED", null, null, null, "Added a comment");

        return IssueCommentDto.builder()
                .id(comment.getId())
                .issueId(issueId)
                .authorId(author != null ? author.getId() : null)
                .authorName(author != null ? author.getFullName() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    // ---- History ----

    @Transactional(readOnly = true)
    public List<IssueHistoryDto> getHistory(Long issueId) {
        return issueHistoryRepository.findByIssueIdOrderByOccurredAtAsc(issueId).stream()
                .map(h -> IssueHistoryDto.builder()
                        .id(h.getId())
                        .issueId(issueId)
                        .actorId(h.getActor() != null ? h.getActor().getId() : null)
                        .actorName(h.getActorName())
                        .actionType(h.getActionType())
                        .fieldName(h.getFieldName())
                        .oldValue(h.getOldValue())
                        .newValue(h.getNewValue())
                        .occurredAt(h.getOccurredAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ---- Watchers ----

    @Transactional(readOnly = true)
    public List<IssueWatcherDto> getWatchers(Long issueId) {
        return issueWatcherRepository.findByIssueId(issueId).stream()
                .map(w -> IssueWatcherDto.builder()
                        .userId(w.getUser().getId())
                        .userName(w.getUser().getFullName())
                        .addedAt(w.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public IssueWatcherDto addWatcher(Long issueId, Long userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        User user = resolveUser(userId);
        if (!issueWatcherRepository.existsByIssueIdAndUserId(issueId, userId)) {
            IssueWatcher watcher = issueWatcherRepository.save(IssueWatcher.builder()
                    .issue(issue)
                    .user(user)
                    .build());
            recordHistory(issue, user, "WATCHER_ADDED", "watchers", null, user.getFullName(), null);
            return IssueWatcherDto.builder()
                    .userId(user.getId())
                    .userName(user.getFullName())
                    .addedAt(watcher.getCreatedAt())
                    .build();
        }
        return issueWatcherRepository.findByIssueIdAndUserId(issueId, userId)
                .map(w -> IssueWatcherDto.builder()
                        .userId(w.getUser().getId())
                        .userName(w.getUser().getFullName())
                        .addedAt(w.getCreatedAt())
                        .build())
                .orElseThrow(() -> new ResourceNotFoundException("IssueWatcher", "issueId", issueId));
    }

    @Transactional
    public void removeWatcher(Long issueId, Long userId) {
        IssueWatcher watcher = issueWatcherRepository.findByIssueIdAndUserId(issueId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("IssueWatcher", "userId", userId));
        issueWatcherRepository.delete(watcher);
    }

    // ---- Helpers ----

    private void recordChange(Issue issue, User actor, String field, String oldValue, String newValue) {
        recordHistory(issue, actor, "FIELD_CHANGE", field, oldValue, newValue, null);
    }

    private void recordHistory(Issue issue, User actor, String actionType, String field,
                               String oldValue, String newValue, String detail) {
        IssueHistory history = IssueHistory.builder()
                .issue(issue)
                .actor(actor)
                .actorName(actor != null ? actor.getFullName() : "System")
                .actionType(actionType)
                .fieldName(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .occurredAt(LocalDateTime.now())
                .build();
        issueHistoryRepository.save(history);
    }

    private String nextIssueCode(Project project) {
        long seq = issueRepository.countByProjectId(project.getId()) + 1;
        String base = "ISS-" + (project.getProjectCode() != null ? project.getProjectCode().toUpperCase() : "P" + project.getId());
        String code = base + "-" + seq;
        while (issueRepository.findByIssueCode(code).isPresent()) {
            seq++;
            code = base + "-" + seq;
        }
        return code;
    }

    private User resolveUser(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Milestone resolveMilestone(Long milestoneId, Project project) {
        if (milestoneId == null) return null;
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", milestoneId));
        if (!milestone.getProject().getId().equals(project.getId())) {
            throw new BadRequestException("Milestone does not belong to the project");
        }
        return milestone;
    }

    public RiskDto mapRiskToDto(Risk r) {
        return RiskDto.builder()
                .id(r.getId())
                .riskCode(r.getRiskCode())
                .projectId(r.getProject() != null ? r.getProject().getId() : null)
                .projectCode(r.getProject() != null ? r.getProject().getProjectCode() : null)
                .projectName(r.getProject() != null ? r.getProject().getName() : null)
                .description(r.getDescription())
                .probability(r.getProbability())
                .impact(r.getImpact())
                .riskScore(r.getRiskScore())
                .ownerId(r.getOwner() != null ? r.getOwner().getId() : null)
                .ownerName(r.getOwner() != null ? r.getOwner().getFullName() : null)
                .mitigationPlan(r.getMitigationPlan())
                .targetDate(r.getTargetDate())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public IssueDto mapIssueToDto(Issue i) {
        return IssueDto.builder()
                .id(i.getId())
                .issueCode(i.getIssueCode())
                .projectId(i.getProject() != null ? i.getProject().getId() : null)
                .projectCode(i.getProject() != null ? i.getProject().getProjectCode() : null)
                .projectName(i.getProject() != null ? i.getProject().getName() : null)
                .milestoneId(i.getMilestone() != null ? i.getMilestone().getId() : null)
                .milestoneName(i.getMilestone() != null ? i.getMilestone().getName() : null)
                .title(i.getTitle())
                .description(i.getDescription())
                .severity(i.getSeverity())
                .status(i.getStatus())
                .reporterId(i.getReporter() != null ? i.getReporter().getId() : null)
                .reporterName(i.getReporter() != null ? i.getReporter().getFullName() : null)
                .assigneeId(i.getAssignee() != null ? i.getAssignee().getId() : null)
                .assigneeName(i.getAssignee() != null ? i.getAssignee().getFullName() : null)
                .priority(i.getPriority())
                .classification(i.getClassification())
                .category(i.getCategory())
                .environment(i.getEnvironment())
                .affectedModule(i.getAffectedModule())
                .affectedVersion(i.getAffectedVersion())
                .expectedBehavior(i.getExpectedBehavior())
                .actualBehavior(i.getActualBehavior())
                .stepsToReproduce(i.getStepsToReproduce())
                .resolution(i.getResolution())
                .acceptanceCriteria(i.getAcceptanceCriteria())
                .crValue(i.getCrValue())
                .crManDays(i.getCrManDays())
                .estimatedFixHours(i.getEstimatedFixHours())
                .percentage(i.getPercentage())
                .dueDate(i.getDueDate())
                .resolvedAt(i.getResolvedAt())
                .closedAt(i.getClosedAt())
                .linkedTaskIds(splitTaskIds(i.getLinkedTaskIds()))
                .commentCount(issueCommentRepository.countByIssueId(i.getId()))
                .watcherCount(issueWatcherRepository.countByIssueId(i.getId()))
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    private String joinTaskIds(List<String> ids) {
        return ids != null ? String.join(",", ids) : null;
    }

    private List<String> splitTaskIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}