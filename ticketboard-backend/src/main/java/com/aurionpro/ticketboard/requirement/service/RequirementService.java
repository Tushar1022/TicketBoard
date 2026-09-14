package com.aurionpro.ticketboard.requirement.service;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.requirement.dto.RequirementCreateDto;
import com.aurionpro.ticketboard.requirement.dto.RequirementDto;
import com.aurionpro.ticketboard.requirement.dto.RequirementHistoryDto;
import com.aurionpro.ticketboard.requirement.dto.RequirementStatusUpdateDto;
import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.requirement.entity.RequirementHistory;
import com.aurionpro.ticketboard.requirement.enums.RequirementPriority;
import com.aurionpro.ticketboard.requirement.enums.RequirementStatus;
import com.aurionpro.ticketboard.requirement.repository.RequirementHistoryRepository;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final RequirementHistoryRepository historyRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<RequirementDto> getAllRequirements() {
        return requirementRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RequirementDto> getRequirementsByProject(Long projectId) {
        return requirementRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RequirementDto getRequirementById(Long id) {
        Requirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", "id", id));
        return mapToDto(requirement);
    }

    @Transactional
    public RequirementDto createRequirement(RequirementCreateDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        String reqNumber = dto.getReqNumber();
        if (reqNumber == null || reqNumber.isBlank()) {
            int count = requirementRepository.countByProjectId(project.getId()) + 1;
            reqNumber = String.format("REQ-%s-%03d", LocalDate.now().getYear(), count);
        } else {
            reqNumber = reqNumber.toUpperCase().trim();
            if (requirementRepository.existsByReqNumber(reqNumber)) {
                throw new BadRequestException("Requirement number already exists: " + reqNumber);
            }
        }

        User owner = null;
        if (dto.getOwnerId() != null) {
            owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
        }

        Double estimate = dto.getEstimatedEffortHours() != null ? dto.getEstimatedEffortHours() : 0.0;

        Requirement requirement = Requirement.builder()
                .reqNumber(reqNumber)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .businessObjective(dto.getBusinessObjective())
                .acceptanceCriteria(dto.getAcceptanceCriteria())
                .priority(dto.getPriority() != null ? dto.getPriority() : RequirementPriority.MEDIUM)
                .requester(dto.getRequester())
                .project(project)
                .owner(owner)
                .estimatedEffortHours(estimate)
                .originalEstimateHours(estimate)
                .actualEffortHours(0.0)
                .plannedStartDate(dto.getPlannedStartDate())
                .plannedEndDate(dto.getPlannedEndDate())
                .actualStartDate(dto.getActualStartDate())
                .actualEndDate(dto.getActualEndDate())
                .status(dto.getStatus() != null ? dto.getStatus() : RequirementStatus.DRAFT)
                .deliveryVersion(dto.getDeliveryVersion())
                .scopeVersion(1)
                .scopeCreepFlag(false)
                .build();

        Requirement saved = requirementRepository.save(requirement);

        // Audit timeline event
        activityLogService.logEvent(
                "REQUIREMENT",
                saved.getId(),
                saved.getReqNumber(),
                TimelineEventType.CREATED,
                "Requirement " + saved.getReqNumber() + " created: " + saved.getTitle(),
                "Initial status: " + saved.getStatus(),
                null,
                saved.getStatus().name()
        );

        return mapToDto(saved);
    }

    @Transactional
    public RequirementDto updateRequirement(Long id, RequirementCreateDto dto) {
        Requirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", "id", id));

        User currentUser = getCurrentUser();

        // Scope Creep & Estimate Change detection (Section 81 & 82)
        if (dto.getEstimatedEffortHours() != null && !dto.getEstimatedEffortHours().equals(requirement.getEstimatedEffortHours())) {
            Double oldEstimate = requirement.getEstimatedEffortHours();
            Double newEstimate = dto.getEstimatedEffortHours();
            Double original = requirement.getOriginalEstimateHours() != null ? requirement.getOriginalEstimateHours() : oldEstimate;

            boolean isCreep = (original > 0 && newEstimate > original * 1.25);
            requirement.setScopeCreepFlag(isCreep);
            requirement.setScopeVersion(requirement.getScopeVersion() + 1);

            RequirementHistory history = RequirementHistory.builder()
                    .requirement(requirement)
                    .versionNumber(requirement.getScopeVersion())
                    .fieldChanged("ESTIMATED_EFFORT")
                    .oldValue(String.valueOf(oldEstimate))
                    .newValue(String.valueOf(newEstimate))
                    .changeReason(dto.getChangeReason() != null ? dto.getChangeReason() : "Scope adjustment")
                    .changedBy(currentUser)
                    .changedAt(LocalDateTime.now())
                    .build();
            historyRepository.save(history);

            activityLogService.logEvent(
                    "REQUIREMENT",
                    requirement.getId(),
                    requirement.getReqNumber(),
                    TimelineEventType.SCOPE_CHANGED,
                    String.format("Requirement %s effort changed from %.1f hrs to %.1f hrs", requirement.getReqNumber(), oldEstimate, newEstimate),
                    dto.getChangeReason(),
                    String.valueOf(oldEstimate),
                    String.valueOf(newEstimate)
            );

            requirement.setEstimatedEffortHours(newEstimate);
        }

        requirement.setTitle(dto.getTitle());
        requirement.setDescription(dto.getDescription());
        requirement.setBusinessObjective(dto.getBusinessObjective());
        requirement.setAcceptanceCriteria(dto.getAcceptanceCriteria());
        if (dto.getPriority() != null) requirement.setPriority(dto.getPriority());
        if (dto.getRequester() != null) requirement.setRequester(dto.getRequester());
        if (dto.getPlannedStartDate() != null) requirement.setPlannedStartDate(dto.getPlannedStartDate());
        if (dto.getPlannedEndDate() != null) requirement.setPlannedEndDate(dto.getPlannedEndDate());
        if (dto.getActualStartDate() != null) requirement.setActualStartDate(dto.getActualStartDate());
        if (dto.getActualEndDate() != null) requirement.setActualEndDate(dto.getActualEndDate());
        if (dto.getDeliveryVersion() != null) requirement.setDeliveryVersion(dto.getDeliveryVersion());

        if (dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
            requirement.setOwner(owner);
        }

        return mapToDto(requirementRepository.save(requirement));
    }

    @Transactional
    public RequirementDto updateStatus(Long id, RequirementStatusUpdateDto dto) {
        Requirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", "id", id));

        RequirementStatus oldStatus = requirement.getStatus();
        RequirementStatus newStatus = dto.getStatus();

        requirement.setStatus(newStatus);

        if (newStatus == RequirementStatus.IN_PROGRESS && requirement.getActualStartDate() == null) {
            requirement.setActualStartDate(LocalDate.now());
        } else if ((newStatus == RequirementStatus.CLOSED || newStatus == RequirementStatus.RELEASED) && requirement.getActualEndDate() == null) {
            requirement.setActualEndDate(LocalDate.now());
        }

        Requirement saved = requirementRepository.save(requirement);

        activityLogService.logEvent(
                "REQUIREMENT",
                saved.getId(),
                saved.getReqNumber(),
                newStatus == RequirementStatus.APPROVED ? TimelineEventType.APPROVED : TimelineEventType.STATUS_CHANGED,
                String.format("Requirement %s status changed: %s -> %s", saved.getReqNumber(), oldStatus, newStatus),
                dto.getComment(),
                oldStatus.name(),
                newStatus.name()
        );

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RequirementHistoryDto> getRequirementHistory(Long requirementId) {
        return historyRepository.findByRequirementIdOrderByChangedAtDesc(requirementId).stream()
                .map(h -> RequirementHistoryDto.builder()
                        .id(h.getId())
                        .requirementId(requirementId)
                        .versionNumber(h.getVersionNumber())
                        .fieldChanged(h.getFieldChanged())
                        .oldValue(h.getOldValue())
                        .newValue(h.getNewValue())
                        .changeReason(h.getChangeReason())
                        .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System")
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRequirement(Long id) {
        if (!requirementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Requirement", "id", id);
        }
        requirementRepository.deleteById(id);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public RequirementDto mapToDto(Requirement r) {
        return RequirementDto.builder()
                .id(r.getId())
                .reqNumber(r.getReqNumber())
                .title(r.getTitle())
                .description(r.getDescription())
                .businessObjective(r.getBusinessObjective())
                .acceptanceCriteria(r.getAcceptanceCriteria())
                .priority(r.getPriority())
                .requester(r.getRequester())
                .projectId(r.getProject() != null ? r.getProject().getId() : null)
                .projectCode(r.getProject() != null ? r.getProject().getProjectCode() : null)
                .projectName(r.getProject() != null ? r.getProject().getName() : null)
                .ownerId(r.getOwner() != null ? r.getOwner().getId() : null)
                .ownerName(r.getOwner() != null ? r.getOwner().getFullName() : null)
                .estimatedEffortHours(r.getEstimatedEffortHours())
                .actualEffortHours(r.getActualEffortHours())
                .plannedStartDate(r.getPlannedStartDate())
                .plannedEndDate(r.getPlannedEndDate())
                .actualStartDate(r.getActualStartDate())
                .actualEndDate(r.getActualEndDate())
                .status(r.getStatus())
                .deliveryVersion(r.getDeliveryVersion())
                .scopeVersion(r.getScopeVersion())
                .originalEstimateHours(r.getOriginalEstimateHours())
                .scopeCreepFlag(r.getScopeCreepFlag())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
