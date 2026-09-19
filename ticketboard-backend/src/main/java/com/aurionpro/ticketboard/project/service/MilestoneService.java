package com.aurionpro.ticketboard.project.service;

import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.dto.MilestoneDto;
import com.aurionpro.ticketboard.project.entity.Milestone;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.enums.MilestoneProgressSource;
import com.aurionpro.ticketboard.project.enums.MilestoneStatus;
import com.aurionpro.ticketboard.project.repository.MilestoneRepository;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.release.entity.Release;
import com.aurionpro.ticketboard.release.repository.ReleaseRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReleaseRepository releaseRepository;

    @Transactional(readOnly = true)
    public List<MilestoneDto> getMilestonesByProject(Long projectId, MilestoneStatus status, String flag, Boolean overdue) {
        List<Milestone> milestones;
        if (status != null) {
            milestones = milestoneRepository.findByProjectIdAndStatusOrderByPlannedDateAsc(projectId, status);
        } else if (flag != null && !flag.isBlank()) {
            milestones = milestoneRepository.findByProjectIdAndFlagOrderByPlannedDateAsc(projectId, flag);
        } else {
            milestones = milestoneRepository.findByProjectIdOrderByPlannedDateAsc(projectId);
        }

        LocalDate today = LocalDate.now();
        return milestones.stream()
                .filter(m -> overdue == null || !overdue
                        ? true
                        : m.getPlannedDate() != null
                            && m.getPlannedDate().isBefore(today)
                            && m.getStatus() != MilestoneStatus.ACHIEVED
                            && m.getStatus() != MilestoneStatus.CANCELLED)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MilestoneDto getMilestoneById(Long id) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", id));
        return mapToDto(milestone);
    }

    @Transactional
    public MilestoneDto createMilestone(MilestoneDto dto) {
        if (dto.getProjectId() == null) {
            throw new BadRequestException("Project ID is required");
        }
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        User owner = resolveUser(dto.getOwnerId());
        Milestone parent = resolveMilestone(dto.getParentMilestoneId());
        Release release = resolveRelease(dto.getReleaseId(), project);

        String milestoneCode = nextMilestoneCode(project);

        Milestone milestone = Milestone.builder()
                .milestoneCode(milestoneCode)
                .project(project)
                .name(dto.getName())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .plannedDate(dto.getPlannedDate())
                .targetDate(dto.getTargetDate())
                .actualDate(dto.getActualDate())
                .priority(dto.getPriority())
                .flag(dto.getFlag())
                .parent(parent)
                .release(release)
                .owner(owner)
                .status(dto.getStatus() != null ? dto.getStatus() : MilestoneStatus.PLANNED)
                .completionPercentage(dto.getCompletionPercentage() != null ? dto.getCompletionPercentage() : 0.0)
                .progressSource(dto.getProgressSource() != null ? dto.getProgressSource() : MilestoneProgressSource.MANUAL)
                .build();

        return mapToDto(milestoneRepository.save(milestone));
    }

    @Transactional
    public MilestoneDto updateMilestone(Long id, MilestoneDto dto) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", id));

        if (dto.getName() != null) milestone.setName(dto.getName());
        if (dto.getDescription() != null) milestone.setDescription(dto.getDescription());
        if (dto.getStartDate() != null) milestone.setStartDate(dto.getStartDate());
        if (dto.getPlannedDate() != null) milestone.setPlannedDate(dto.getPlannedDate());
        if (dto.getTargetDate() != null) milestone.setTargetDate(dto.getTargetDate());
        if (dto.getActualDate() != null) milestone.setActualDate(dto.getActualDate());
        if (dto.getPriority() != null) milestone.setPriority(dto.getPriority());
        if (dto.getFlag() != null) milestone.setFlag(dto.getFlag());
        if (dto.getStatus() != null) milestone.setStatus(dto.getStatus());
        if (dto.getCompletionPercentage() != null) milestone.setCompletionPercentage(dto.getCompletionPercentage());
        if (dto.getProgressSource() != null) milestone.setProgressSource(dto.getProgressSource());
        if (dto.getOwnerId() != null) milestone.setOwner(resolveUser(dto.getOwnerId()));
        if (dto.getParentMilestoneId() != null) milestone.setParent(resolveMilestone(dto.getParentMilestoneId()));
        if (dto.getReleaseId() != null) milestone.setRelease(resolveRelease(dto.getReleaseId(), milestone.getProject()));

        if (milestone.getProgressSource() == MilestoneProgressSource.AUTO) {
            milestone.setCompletionPercentage(computeAutoProgress(milestone.getId()));
        }

        return mapToDto(milestoneRepository.save(milestone));
    }

    @Transactional
    public void deleteMilestone(Long id) {
        if (!milestoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Milestone", "id", id);
        }
        milestoneRepository.detachWorkItems(id);
        milestoneRepository.detachIssues(id);
        milestoneRepository.deleteById(id);
    }

    private double computeAutoProgress(Long milestoneId) {
        return Math.round(milestoneRepository.averageLinkedTaskProgress(milestoneId) * 10) / 10.0;
    }

    private String nextMilestoneCode(Project project) {
        long seq = milestoneRepository.countByProjectId(project.getId()) + 1;
        String base = "MS-" + (project.getProjectCode() != null ? project.getProjectCode().toUpperCase() : "P" + project.getId());
        String code = base + "-" + seq;
        while (milestoneRepository.findByMilestoneCode(code).isPresent()) {
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

    private Milestone resolveMilestone(Long milestoneId) {
        if (milestoneId == null) return null;
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", milestoneId));
    }

    private Release resolveRelease(Long releaseId, Project project) {
        if (releaseId == null) return null;
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Release", "id", releaseId));
        if (!release.getProject().getId().equals(project.getId())) {
            throw new BadRequestException("Release does not belong to the project");
        }
        return release;
    }

    public MilestoneDto mapToDto(Milestone m) {
        Long linkedTasks = milestoneRepository.countLinkedTasks(m.getId());
        Long linkedIssues = milestoneRepository.countLinkedIssues(m.getId());
        Double completion = m.getCompletionPercentage() != null ? m.getCompletionPercentage() : 0.0;
        if (m.getProgressSource() == MilestoneProgressSource.AUTO) {
            completion = computeAutoProgress(m.getId());
        }

        return MilestoneDto.builder()
                .id(m.getId())
                .milestoneCode(m.getMilestoneCode())
                .projectId(m.getProject() != null ? m.getProject().getId() : null)
                .projectCode(m.getProject() != null ? m.getProject().getProjectCode() : null)
                .projectName(m.getProject() != null ? m.getProject().getName() : null)
                .name(m.getName())
                .description(m.getDescription())
                .startDate(m.getStartDate())
                .plannedDate(m.getPlannedDate())
                .targetDate(m.getTargetDate())
                .actualDate(m.getActualDate())
                .priority(m.getPriority())
                .flag(m.getFlag())
                .parentMilestoneId(m.getParent() != null ? m.getParent().getId() : null)
                .parentName(m.getParent() != null ? m.getParent().getName() : null)
                .releaseId(m.getRelease() != null ? m.getRelease().getId() : null)
                .releaseVersion(m.getRelease() != null ? m.getRelease().getReleaseVersion() : null)
                .ownerId(m.getOwner() != null ? m.getOwner().getId() : null)
                .ownerName(m.getOwner() != null ? m.getOwner().getFullName() : null)
                .status(m.getStatus())
                .completionPercentage(completion)
                .progressSource(m.getProgressSource())
                .linkedTaskCount(linkedTasks)
                .linkedIssueCount(linkedIssues)
                .build();
    }
}