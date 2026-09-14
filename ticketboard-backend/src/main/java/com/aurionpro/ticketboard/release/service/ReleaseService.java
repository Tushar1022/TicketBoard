package com.aurionpro.ticketboard.release.service;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.release.dto.ReleaseCreateDto;
import com.aurionpro.ticketboard.release.dto.ReleaseDto;
import com.aurionpro.ticketboard.release.entity.Release;
import com.aurionpro.ticketboard.release.entity.ReleaseItem;
import com.aurionpro.ticketboard.release.enums.ReleaseEnvironment;
import com.aurionpro.ticketboard.release.enums.ReleaseStatus;
import com.aurionpro.ticketboard.release.repository.ReleaseItemRepository;
import com.aurionpro.ticketboard.release.repository.ReleaseRepository;
import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ReleaseItemRepository releaseItemRepository;
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final WorkItemRepository workItemRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<ReleaseDto> getAllReleases() {
        return releaseRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReleaseDto> getReleasesByProject(Long projectId) {
        return releaseRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReleaseDto getReleaseById(Long id) {
        Release release = releaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release", "id", id));
        return mapToDto(release);
    }

    @Transactional
    public ReleaseDto createRelease(ReleaseCreateDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        String version = dto.getReleaseVersion();
        if (version == null || version.isBlank()) {
            version = String.format("REL-%s-%03d", LocalDate.now().getYear(), releaseRepository.count() + 1);
        } else {
            version = version.toUpperCase().trim();
            if (releaseRepository.existsByReleaseVersion(version)) {
                throw new BadRequestException("Release version already exists: " + version);
            }
        }

        User owner = null;
        if (dto.getOwnerId() != null) {
            owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
        }

        Release release = Release.builder()
                .releaseVersion(version)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .project(project)
                .environment(dto.getEnvironment() != null ? dto.getEnvironment() : ReleaseEnvironment.PRODUCTION)
                .plannedDate(dto.getPlannedDate())
                .actualDate(dto.getActualDate())
                .status(dto.getStatus() != null ? dto.getStatus() : ReleaseStatus.PLANNED)
                .deploymentResult(dto.getDeploymentResult())
                .rollbackRequired(dto.getRollbackRequired() != null ? dto.getRollbackRequired() : false)
                .owner(owner)
                .build();

        Release saved = releaseRepository.save(release);

        // Add release items
        if (dto.getRequirementIds() != null) {
            for (Long reqId : dto.getRequirementIds()) {
                Requirement req = requirementRepository.findById(reqId).orElse(null);
                if (req != null) {
                    ReleaseItem item = ReleaseItem.builder()
                            .release(saved)
                            .requirement(req)
                            .notes("Included in " + saved.getReleaseVersion())
                            .build();
                    releaseItemRepository.save(item);
                }
            }
        }

        if (dto.getWorkItemIds() != null) {
            for (Long itemId : dto.getWorkItemIds()) {
                WorkItem item = workItemRepository.findById(itemId).orElse(null);
                if (item != null) {
                    ReleaseItem relItem = ReleaseItem.builder()
                            .release(saved)
                            .workItem(item)
                            .notes("Included in " + saved.getReleaseVersion())
                            .build();
                    releaseItemRepository.save(relItem);
                }
            }
        }

        activityLogService.logEvent(
                "RELEASE",
                saved.getId(),
                saved.getReleaseVersion(),
                TimelineEventType.RELEASED,
                String.format("Release %s created for project %s (%s)", saved.getReleaseVersion(), project.getName(), saved.getEnvironment()),
                saved.getTitle(),
                null,
                saved.getStatus().name()
        );

        return mapToDto(saved);
    }

    @Transactional
    public ReleaseDto updateRelease(Long id, ReleaseCreateDto dto) {
        Release release = releaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release", "id", id));

        release.setTitle(dto.getTitle());
        release.setDescription(dto.getDescription());
        if (dto.getEnvironment() != null) release.setEnvironment(dto.getEnvironment());
        if (dto.getPlannedDate() != null) release.setPlannedDate(dto.getPlannedDate());
        if (dto.getActualDate() != null) release.setActualDate(dto.getActualDate());
        if (dto.getStatus() != null) release.setStatus(dto.getStatus());
        if (dto.getDeploymentResult() != null) release.setDeploymentResult(dto.getDeploymentResult());
        if (dto.getRollbackRequired() != null) release.setRollbackRequired(dto.getRollbackRequired());

        if (dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
            release.setOwner(owner);
        }

        Release saved = releaseRepository.save(release);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteRelease(Long id) {
        if (!releaseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Release", "id", id);
        }
        releaseRepository.deleteById(id);
    }

    public ReleaseDto mapToDto(Release r) {
        List<ReleaseItem> items = releaseItemRepository.findByReleaseId(r.getId());
        List<Long> reqIds = new ArrayList<>();
        List<Long> itemIds = new ArrayList<>();

        for (ReleaseItem item : items) {
            if (item.getRequirement() != null) reqIds.add(item.getRequirement().getId());
            if (item.getWorkItem() != null) itemIds.add(item.getWorkItem().getId());
        }

        return ReleaseDto.builder()
                .id(r.getId())
                .releaseVersion(r.getReleaseVersion())
                .title(r.getTitle())
                .description(r.getDescription())
                .projectId(r.getProject() != null ? r.getProject().getId() : null)
                .projectCode(r.getProject() != null ? r.getProject().getProjectCode() : null)
                .projectName(r.getProject() != null ? r.getProject().getName() : null)
                .environment(r.getEnvironment())
                .plannedDate(r.getPlannedDate())
                .actualDate(r.getActualDate())
                .status(r.getStatus())
                .deploymentResult(r.getDeploymentResult())
                .rollbackRequired(r.getRollbackRequired())
                .ownerId(r.getOwner() != null ? r.getOwner().getId() : null)
                .ownerName(r.getOwner() != null ? r.getOwner().getFullName() : null)
                .requirementIds(reqIds)
                .workItemIds(itemIds)
                .itemCount(items.size())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
