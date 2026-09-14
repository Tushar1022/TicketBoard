package com.aurionpro.ticketboard.risk.service;

import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.project.service.ProjectService;
import com.aurionpro.ticketboard.risk.dto.IssueDto;
import com.aurionpro.ticketboard.risk.dto.RiskDto;
import com.aurionpro.ticketboard.risk.entity.Issue;
import com.aurionpro.ticketboard.risk.entity.Risk;
import com.aurionpro.ticketboard.risk.enums.IssueSeverity;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
import com.aurionpro.ticketboard.risk.enums.RiskStatus;
import com.aurionpro.ticketboard.risk.repository.IssueRepository;
import com.aurionpro.ticketboard.risk.repository.RiskRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskRepository riskRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
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

        risk.setDescription(dto.getDescription());
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
        return issueRepository.findByProjectId(projectId).stream()
                .map(this::mapIssueToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public IssueDto createIssue(IssueDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        String issueCode = "ISS-" + (100 + issueRepository.count() + 1);

        User owner = null;
        if (dto.getOwnerId() != null) {
            owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
        }

        Issue issue = Issue.builder()
                .issueCode(issueCode)
                .project(project)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .severity(dto.getSeverity() != null ? dto.getSeverity() : IssueSeverity.MEDIUM)
                .status(dto.getStatus() != null ? dto.getStatus() : IssueStatus.OPEN)
                .owner(owner)
                .resolution(dto.getResolution())
                .classification(dto.getClassification())
                .stepsToReproduce(dto.getStepsToReproduce())
                .crValue(dto.getCrValue())
                .crManDays(dto.getCrManDays())
                .dueDate(dto.getDueDate())
                .linkedTaskIds(joinTaskIds(dto.getLinkedTaskIds()))
                .build();

        return mapIssueToDto(issueRepository.save(issue));
    }

    @Transactional
    public IssueDto updateIssue(Long id, IssueDto dto) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", id));

        issue.setDescription(dto.getDescription());
        issue.setTitle(dto.getTitle() != null ? dto.getTitle() : issue.getTitle());
        if (dto.getSeverity() != null) issue.setSeverity(dto.getSeverity());
        if (dto.getStatus() != null) issue.setStatus(dto.getStatus());
        if (dto.getResolution() != null) issue.setResolution(dto.getResolution());
        if (dto.getClassification() != null) issue.setClassification(dto.getClassification());
        if (dto.getStepsToReproduce() != null) issue.setStepsToReproduce(dto.getStepsToReproduce());
        if (dto.getCrValue() != null) issue.setCrValue(dto.getCrValue());
        if (dto.getCrManDays() != null) issue.setCrManDays(dto.getCrManDays());
        if (dto.getDueDate() != null) issue.setDueDate(dto.getDueDate());
        if (dto.getLinkedTaskIds() != null) issue.setLinkedTaskIds(joinTaskIds(dto.getLinkedTaskIds()));

        if (dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getOwnerId()));
            issue.setOwner(owner);
        }

        return mapIssueToDto(issueRepository.save(issue));
    }

    @Transactional
    public void deleteRisk(Long id) {
        if (!riskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Risk", "id", id);
        }
        riskRepository.deleteById(id);
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
                .title(i.getTitle())
                .description(i.getDescription())
                .severity(i.getSeverity())
                .status(i.getStatus())
                .ownerId(i.getOwner() != null ? i.getOwner().getId() : null)
                .ownerName(i.getOwner() != null ? i.getOwner().getFullName() : null)
                .resolution(i.getResolution())
                .classification(i.getClassification())
                .stepsToReproduce(i.getStepsToReproduce())
                .crValue(i.getCrValue())
                .crManDays(i.getCrManDays())
                .dueDate(i.getDueDate())
                .linkedTaskIds(splitTaskIds(i.getLinkedTaskIds()))
                .createdAt(i.getCreatedAt())
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
