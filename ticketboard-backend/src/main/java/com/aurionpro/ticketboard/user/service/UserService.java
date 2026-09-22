package com.aurionpro.ticketboard.user.service;

import com.aurionpro.ticketboard.audit.repository.ActivityLogRepository;
import com.aurionpro.ticketboard.billing.repository.InvoiceRepository;
import com.aurionpro.ticketboard.client.repository.ClientRepository;
import com.aurionpro.ticketboard.comment.repository.CommentRepository;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.erp.repository.ERPAssetRepository;
import com.aurionpro.ticketboard.erp.repository.ExpenseClaimRepository;
import com.aurionpro.ticketboard.erp.repository.LeaveRequestRepository;
import com.aurionpro.ticketboard.erp.repository.OKRGoalRepository;
import com.aurionpro.ticketboard.project.repository.MilestoneRepository;
import com.aurionpro.ticketboard.project.repository.ProjectDocumentRepository;
import com.aurionpro.ticketboard.project.repository.ProjectMemberRepository;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.release.repository.ReleaseRepository;
import com.aurionpro.ticketboard.requirement.repository.RequirementHistoryRepository;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.risk.repository.IssueCommentRepository;
import com.aurionpro.ticketboard.risk.repository.IssueHistoryRepository;
import com.aurionpro.ticketboard.risk.repository.IssueRepository;
import com.aurionpro.ticketboard.risk.repository.IssueWatcherRepository;
import com.aurionpro.ticketboard.risk.repository.RiskRepository;
import com.aurionpro.ticketboard.timetracking.repository.TimeEntryRepository;
import com.aurionpro.ticketboard.timetracking.repository.TimesheetRepository;
import com.aurionpro.ticketboard.user.dto.UserCreateDto;
import com.aurionpro.ticketboard.user.dto.UserDto;
import com.aurionpro.ticketboard.user.entity.Department;
import com.aurionpro.ticketboard.user.entity.Role;
import com.aurionpro.ticketboard.user.entity.Team;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.enums.RoleType;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import com.aurionpro.ticketboard.user.repository.DepartmentRepository;
import com.aurionpro.ticketboard.user.repository.RoleRepository;
import com.aurionpro.ticketboard.user.repository.TeamRepository;
import com.aurionpro.ticketboard.user.repository.UserCredentialRepository;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.user.repository.UserSessionRepository;
import com.aurionpro.ticketboard.workitem.repository.WorkItemBlockerRepository;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import com.aurionpro.ticketboard.workitem.repository.TaskDocumentRepository;
import com.aurionpro.ticketboard.devtools.repository.DeveloperSnippetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final DeveloperSnippetRepository developerSnippetRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final IssueWatcherRepository issueWatcherRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final TimesheetRepository timesheetRepository;
    private final CommentRepository commentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ERPAssetRepository erpAssetRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final OKRGoalRepository okrGoalRepository;
    private final WorkItemRepository workItemRepository;
    private final WorkItemBlockerRepository workItemBlockerRepository;
    private final TaskDocumentRepository taskDocumentRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final MilestoneRepository milestoneRepository;
    private final ReleaseRepository releaseRepository;
    private final RiskRepository riskRepository;
    private final RequirementRepository requirementRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final InvoiceRepository invoiceRepository;
    private final RequirementHistoryRepository requirementHistoryRepository;
    private final IssueHistoryRepository issueHistoryRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(RoleType role) {
        return userRepository.findByRoleName(role).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getDevelopers() {
        return userRepository.findAllActiveDevelopers().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto createUser(UserCreateDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Email already exists: " + dto.getEmail());
        }

        String employeeId = dto.getEmployeeId();
        if (employeeId == null || employeeId.isBlank()) {
            employeeId = "EMP-" + (1000 + userRepository.count() + 1);
        } else if (userRepository.existsByEmployeeId(employeeId)) {
            throw new BadRequestException("Employee ID already exists: " + employeeId);
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", dto.getDepartmentId()));
        }

        Team team = null;
        if (dto.getTeamId() != null) {
            team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", dto.getTeamId()));
        }

        User manager = null;
        if (dto.getManagerId() != null) {
            manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getManagerId()));
        }

        Set<Role> roles = new HashSet<>();
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            for (RoleType roleType : dto.getRoles()) {
                Role role = roleRepository.findByName(roleType)
                        .orElseGet(() -> roleRepository.save(Role.builder().name(roleType).description(roleType.name()).build()));
                roles.add(role);
            }
        } else {
            Role devRole = roleRepository.findByName(RoleType.ROLE_DEVELOPER)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DEVELOPER).description("Developer").build()));
            roles.add(devRole);
        }

        // TODO: Force password change on first login
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank()) ? dto.getPassword() : "ChangeMe@" + System.currentTimeMillis();

        User user = User.builder()
                .employeeId(employeeId)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .phone(dto.getPhone())
                .designation(dto.getDesignation())
                .department(department)
                .team(team)
                .reportingManager(manager)
                .roles(roles)
                .skills(dto.getSkills())
                .dailyCapacityHours(dto.getDailyCapacityHours() != null ? dto.getDailyCapacityHours() : 8.0)
                .hourlyCost(dto.getHourlyCost() != null ? dto.getHourlyCost() : 50.0)
                .status(dto.getStatus() != null ? dto.getStatus() : UserStatus.ACTIVE)
                .joiningDate(dto.getJoiningDate())
                .build();

        return mapToDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(Long id, UserCreateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        user.setDesignation(dto.getDesignation());
        user.setSkills(dto.getSkills());

        if (dto.getDailyCapacityHours() != null) {
            user.setDailyCapacityHours(dto.getDailyCapacityHours());
        }
        if (dto.getHourlyCost() != null) {
            user.setHourlyCost(dto.getHourlyCost());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        if (dto.getJoiningDate() != null) {
            user.setJoiningDate(dto.getJoiningDate());
        }

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", dto.getDepartmentId()));
            user.setDepartment(department);
        }

        if (dto.getTeamId() != null) {
            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", dto.getTeamId()));
            user.setTeam(team);
        }

        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getManagerId()));
            user.setReportingManager(manager);
        }

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (RoleType roleType : dto.getRoles()) {
                Role role = roleRepository.findByName(roleType)
                        .orElseGet(() -> roleRepository.save(Role.builder().name(roleType).description(roleType.name()).build()));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return mapToDto(userRepository.save(user));
    }

    @Transactional
    public void toggleUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        if (id.equals(currentUserId())) {
            throw new BadRequestException("You cannot change the status of your own account");
        }
        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        if (id.equals(currentUserId())) {
            throw new BadRequestException("You cannot delete your own account");
        }

        // detach nullable references so business data is preserved
        workItemRepository.detachAssignee(id);
        workItemRepository.detachReporter(id);
        workItemBlockerRepository.detachResolvedBy(id);
        taskDocumentRepository.detachUploadedBy(id);
        issueRepository.detachReporter(id);
        issueRepository.detachAssignee(id);
        projectRepository.detachProjectManager(id);
        teamRepository.detachTeamLead(id);
        milestoneRepository.detachOwner(id);
        releaseRepository.detachOwner(id);
        riskRepository.detachOwner(id);
        requirementRepository.detachOwner(id);
        clientRepository.detachAccountManager(id);
        timesheetRepository.detachReviewer(id);
        activityLogRepository.detachUser(id);
        projectDocumentRepository.detachUploadedBy(id);
        invoiceRepository.detachCreatedBy(id);
        requirementHistoryRepository.detachChangedBy(id);
        issueHistoryRepository.detachActor(id);
        issueCommentRepository.detachAuthor(id);
        userRepository.detachReportingManager(id);

        // remove records owned by the user
        timeEntryRepository.deleteByUserId(id);
        timesheetRepository.deleteByUserId(id);
        commentRepository.deleteByAuthorId(id);
        projectMemberRepository.deleteByUserId(id);
        issueWatcherRepository.deleteByUserId(id);
        developerSnippetRepository.deleteByUserId(id);
        leaveRequestRepository.deleteByUserId(id);
        erpAssetRepository.deleteByUserId(id);
        expenseClaimRepository.deleteByUserId(id);
        okrGoalRepository.deleteByUserId(id);
        userSessionRepository.deleteByUserId(id);
        userCredentialRepository.deleteByUserId(id);

        // user_roles join rows are removed with the entity
        userRepository.deleteById(id);
    }

    @Transactional
    public UserDto updateUserRoles(Long id, Set<RoleType> roleTypes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        if (roleTypes != null && !roleTypes.isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (RoleType roleType : roleTypes) {
                Role role = roleRepository.findByName(roleType)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleType));
                roles.add(role);
            }
            user.setRoles(roles);
        }
        return mapToDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getProjectOwners() {
        return userRepository.findByRoleName(RoleType.ROLE_PROJECT_OWNER).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRoleAndStatus(RoleType role, UserStatus status) {
        if (role == null && status == null) return getAllUsers();
        if (role != null && status != null) {
            return userRepository.findByRolesNameAndStatus(role, status).stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }
        if (role != null) return getUsersByRole(role);
        return userRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private Long currentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.aurionpro.ticketboard.security.CustomUserDetails details) {
            return details.getId();
        }
        return null;
    }

    public UserDto mapToDto(User user) {
        if (user == null) return null;
        Set<RoleType> roleTypes = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(perm -> perm.getCode().getCode())
                .collect(Collectors.toSet());

        return UserDto.builder()
                .id(user.getId())
                .employeeId(user.getEmployeeId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .designation(user.getDesignation())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .teamId(user.getTeam() != null ? user.getTeam().getId() : null)
                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)
                .managerId(user.getReportingManager() != null ? user.getReportingManager().getId() : null)
                .managerName(user.getReportingManager() != null ? user.getReportingManager().getFullName() : null)
                .roles(roleTypes)
                .permissions(permissions)
                .skills(user.getSkills())
                .dailyCapacityHours(user.getDailyCapacityHours())
                .hourlyCost(user.getHourlyCost())
                .status(user.getStatus())
                .joiningDate(user.getJoiningDate())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
