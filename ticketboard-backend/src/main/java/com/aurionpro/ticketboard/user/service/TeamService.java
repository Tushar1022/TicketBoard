package com.aurionpro.ticketboard.user.service;

import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.user.dto.TeamDto;
import com.aurionpro.ticketboard.user.entity.Department;
import com.aurionpro.ticketboard.user.entity.Team;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.DepartmentRepository;
import com.aurionpro.ticketboard.user.repository.TeamRepository;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeamDto> getTeamsByDepartment(Long departmentId) {
        return teamRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamDto getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
        return mapToDto(team);
    }

    @Transactional
    public TeamDto createTeam(TeamDto dto) {
        if (teamRepository.existsByCode(dto.getCode())) {
            throw new BadRequestException("Team code already exists: " + dto.getCode());
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", dto.getDepartmentId()));
        }

        User teamLead = null;
        if (dto.getTeamLeadId() != null) {
            teamLead = userRepository.findById(dto.getTeamLeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getTeamLeadId()));
        }

        Team team = Team.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .department(department)
                .teamLead(teamLead)
                .build();

        return mapToDto(teamRepository.save(team));
    }

    @Transactional
    public TeamDto updateTeam(Long id, TeamDto dto) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));

        team.setName(dto.getName());
        team.setDescription(dto.getDescription());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", dto.getDepartmentId()));
            team.setDepartment(department);
        }

        if (dto.getTeamLeadId() != null) {
            User teamLead = userRepository.findById(dto.getTeamLeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getTeamLeadId()));
            team.setTeamLead(teamLead);
        }

        return mapToDto(teamRepository.save(team));
    }

    @Transactional
    public void deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Team", "id", id);
        }
        teamRepository.deleteById(id);
    }

    public TeamDto mapToDto(Team team) {
        return TeamDto.builder()
                .id(team.getId())
                .name(team.getName())
                .code(team.getCode())
                .description(team.getDescription())
                .departmentId(team.getDepartment() != null ? team.getDepartment().getId() : null)
                .departmentName(team.getDepartment() != null ? team.getDepartment().getName() : null)
                .teamLeadId(team.getTeamLead() != null ? team.getTeamLead().getId() : null)
                .teamLeadName(team.getTeamLead() != null ? team.getTeamLead().getFullName() : null)
                .build();
    }
}
