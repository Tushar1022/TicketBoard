package com.aurionpro.ticketboard.project.service;

import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.dto.MilestoneDto;
import com.aurionpro.ticketboard.project.entity.Milestone;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.enums.MilestoneStatus;
import com.aurionpro.ticketboard.project.repository.MilestoneRepository;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MilestoneDto> getMilestonesByProject(Long projectId) {
        return milestoneRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MilestoneDto createMilestone(MilestoneDto dto) {
        if (dto.getProjectId() == null) {
            throw new BadRequestException("Project ID is required");
        }
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        User owner = null;
        if (dto.getOwnerId() != null) {
            owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
        }

        Milestone milestone = Milestone.builder()
                .project(project)
                .name(dto.getName())
                .description(dto.getDescription())
                .plannedDate(dto.getPlannedDate())
                .actualDate(dto.getActualDate())
                .owner(owner)
                .status(dto.getStatus() != null ? dto.getStatus() : MilestoneStatus.PLANNED)
                .completionPercentage(dto.getCompletionPercentage() != null ? dto.getCompletionPercentage() : 0.0)
                .build();

        return mapToDto(milestoneRepository.save(milestone));
    }

    @Transactional
    public MilestoneDto updateMilestone(Long id, MilestoneDto dto) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", id));

        milestone.setName(dto.getName());
        milestone.setDescription(dto.getDescription());
        milestone.setPlannedDate(dto.getPlannedDate());
        milestone.setActualDate(dto.getActualDate());

        if (dto.getStatus() != null) {
            milestone.setStatus(dto.getStatus());
        }
        if (dto.getCompletionPercentage() != null) {
            milestone.setCompletionPercentage(dto.getCompletionPercentage());
        }
        if (dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
            milestone.setOwner(owner);
        }

        return mapToDto(milestoneRepository.save(milestone));
    }

    @Transactional
    public void deleteMilestone(Long id) {
        if (!milestoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Milestone", "id", id);
        }
        milestoneRepository.deleteById(id);
    }

    public MilestoneDto mapToDto(Milestone m) {
        return MilestoneDto.builder()
                .id(m.getId())
                .projectId(m.getProject() != null ? m.getProject().getId() : null)
                .name(m.getName())
                .description(m.getDescription())
                .plannedDate(m.getPlannedDate())
                .actualDate(m.getActualDate())
                .ownerId(m.getOwner() != null ? m.getOwner().getId() : null)
                .ownerName(m.getOwner() != null ? m.getOwner().getFullName() : null)
                .status(m.getStatus())
                .completionPercentage(m.getCompletionPercentage())
                .build();
    }
}
