package com.aurionpro.ticketboard.project.service;

import com.aurionpro.ticketboard.client.entity.Client;
import com.aurionpro.ticketboard.client.repository.ClientRepository;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.dto.MilestoneDto;
import com.aurionpro.ticketboard.project.dto.ProjectCreateDto;
import com.aurionpro.ticketboard.project.dto.ProjectDto;
import com.aurionpro.ticketboard.project.dto.ProjectMemberDto;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.entity.ProjectMember;
import com.aurionpro.ticketboard.project.enums.ProjectHealth;
import com.aurionpro.ticketboard.project.enums.ProjectPriority;
import com.aurionpro.ticketboard.project.enums.ProjectStatus;
import com.aurionpro.ticketboard.project.repository.ProjectMemberRepository;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return mapToDto(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsByMember(Long userId) {
        return projectRepository.findByMemberUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDto createProject(ProjectCreateDto dto) {
        String code = dto.getProjectCode();
        if (code == null || code.isBlank()) {
            code = "PROJ-" + (1000 + projectRepository.count() + 1);
        } else {
            code = code.toUpperCase().trim();
            if (projectRepository.existsByProjectCode(code)) {
                throw new BadRequestException("Project code already exists: " + code);
            }
        }

        Client client = null;
        if (dto.getClientId() != null) {
            client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", dto.getClientId()));
        }

        User manager = null;
        if (dto.getProjectManagerId() != null) {
            manager = userRepository.findById(dto.getProjectManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getProjectManagerId()));
        }

        Project project = Project.builder()
                .projectCode(code)
                .name(dto.getName())
                .description(dto.getDescription())
                .client(client)
                .projectManager(manager)
                .startDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now())
                .plannedEndDate(dto.getPlannedEndDate())
                .actualEndDate(dto.getActualEndDate())
                .priority(dto.getPriority() != null ? dto.getPriority() : ProjectPriority.MEDIUM)
                .status(dto.getStatus() != null ? dto.getStatus() : ProjectStatus.PLANNED)
                .health(dto.getHealth() != null ? dto.getHealth() : ProjectHealth.GREEN)
                .budget(dto.getBudget())
                .estimatedHours(dto.getEstimatedHours() != null ? dto.getEstimatedHours() : 0.0)
                .actualHours(0.0)
                .completionPercentage(0.0)
                .build();

        Project saved = projectRepository.save(project);

        // If manager assigned, add as member automatically
        if (manager != null) {
            ProjectMember member = ProjectMember.builder()
                    .project(saved)
                    .user(manager)
                    .projectRole("Project Manager")
                    .allocatedHoursPerDay(8.0)
                    .allocationStartDate(saved.getStartDate())
                    .build();
            projectMemberRepository.save(member);
        }

        return mapToDto(saved);
    }

    @Transactional
    public ProjectDto updateProject(Long id, ProjectCreateDto dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        project.setName(dto.getName());
        project.setDescription(dto.getDescription());

        if (dto.getClientId() != null) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", dto.getClientId()));
            project.setClient(client);
        }

        if (dto.getProjectManagerId() != null) {
            User manager = userRepository.findById(dto.getProjectManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getProjectManagerId()));
            project.setProjectManager(manager);
        }

        if (dto.getStartDate() != null) project.setStartDate(dto.getStartDate());
        if (dto.getPlannedEndDate() != null) project.setPlannedEndDate(dto.getPlannedEndDate());
        if (dto.getActualEndDate() != null) project.setActualEndDate(dto.getActualEndDate());
        if (dto.getPriority() != null) project.setPriority(dto.getPriority());
        if (dto.getStatus() != null) project.setStatus(dto.getStatus());
        if (dto.getHealth() != null) project.setHealth(dto.getHealth());
        if (dto.getBudget() != null) project.setBudget(dto.getBudget());
        if (dto.getEstimatedHours() != null) project.setEstimatedHours(dto.getEstimatedHours());

        recalculateProjectMetrics(project);

        return mapToDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectMemberDto addProjectMember(Long projectId, ProjectMemberDto memberDto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User user = userRepository.findById(memberDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", memberDto.getUserId()));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new BadRequestException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .projectRole(memberDto.getProjectRole() != null ? memberDto.getProjectRole() : "Team Member")
                .allocatedHoursPerDay(memberDto.getAllocatedHoursPerDay() != null ? memberDto.getAllocatedHoursPerDay() : 8.0)
                .allocationStartDate(memberDto.getAllocationStartDate() != null ? memberDto.getAllocationStartDate() : LocalDate.now())
                .allocationEndDate(memberDto.getAllocationEndDate())
                .build();

        return mapToMemberDto(projectMemberRepository.save(member));
    }

    @Transactional
    public void removeProjectMember(Long projectId, Long memberId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMember", "id", memberId));
        if (!member.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Member does not belong to project ID: " + projectId);
        }
        projectMemberRepository.delete(member);
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", "id", id);
        }
        projectRepository.deleteById(id);
    }

    public void recalculateProjectMetrics(Project project) {
        if (project.getPlannedEndDate() != null && LocalDate.now().isAfter(project.getPlannedEndDate())
                && project.getStatus() != ProjectStatus.COMPLETED && project.getStatus() != ProjectStatus.CLOSED) {
            project.setHealth(ProjectHealth.RED);
        } else if (project.getEstimatedHours() != null && project.getEstimatedHours() > 0
                && project.getActualHours() != null && project.getActualHours() > project.getEstimatedHours() * 1.15) {
            project.setHealth(ProjectHealth.AMBER);
        }
    }

    public ProjectDto mapToDto(Project project) {
        List<ProjectMemberDto> memberDtos = project.getMembers() != null ?
                project.getMembers().stream().map(this::mapToMemberDto).collect(Collectors.toList()) : List.of();

        List<MilestoneDto> milestoneDtos = project.getMilestones() != null ?
                project.getMilestones().stream().map(m -> MilestoneDto.builder()
                        .id(m.getId())
                        .projectId(project.getId())
                        .name(m.getName())
                        .description(m.getDescription())
                        .plannedDate(m.getPlannedDate())
                        .actualDate(m.getActualDate())
                        .ownerId(m.getOwner() != null ? m.getOwner().getId() : null)
                        .ownerName(m.getOwner() != null ? m.getOwner().getFullName() : null)
                        .status(m.getStatus())
                        .completionPercentage(m.getCompletionPercentage())
                        .build()).collect(Collectors.toList()) : List.of();

        return ProjectDto.builder()
                .id(project.getId())
                .projectCode(project.getProjectCode())
                .name(project.getName())
                .description(project.getDescription())
                .clientId(project.getClient() != null ? project.getClient().getId() : null)
                .clientName(project.getClient() != null ? project.getClient().getName() : null)
                .projectManagerId(project.getProjectManager() != null ? project.getProjectManager().getId() : null)
                .projectManagerName(project.getProjectManager() != null ? project.getProjectManager().getFullName() : null)
                .startDate(project.getStartDate())
                .plannedEndDate(project.getPlannedEndDate())
                .actualEndDate(project.getActualEndDate())
                .priority(project.getPriority())
                .status(project.getStatus())
                .health(project.getHealth())
                .budget(project.getBudget())
                .estimatedHours(project.getEstimatedHours())
                .actualHours(project.getActualHours())
                .completionPercentage(project.getCompletionPercentage())
                .members(memberDtos)
                .milestones(milestoneDtos)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public ProjectMemberDto mapToMemberDto(ProjectMember member) {
        return ProjectMemberDto.builder()
                .id(member.getId())
                .projectId(member.getProject().getId())
                .userId(member.getUser().getId())
                .userName(member.getUser().getFullName())
                .userEmail(member.getUser().getEmail())
                .projectRole(member.getProjectRole())
                .allocatedHoursPerDay(member.getAllocatedHoursPerDay())
                .allocationStartDate(member.getAllocationStartDate())
                .allocationEndDate(member.getAllocationEndDate())
                .build();
    }
}
