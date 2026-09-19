package com.aurionpro.ticketboard.config;

import com.aurionpro.ticketboard.common.lookup.entity.LookupData;
import com.aurionpro.ticketboard.common.lookup.repository.LookupDataRepository;
import com.aurionpro.ticketboard.user.config.RolePermissionConfig;
import com.aurionpro.ticketboard.user.entity.Department;
import com.aurionpro.ticketboard.user.entity.Permission;
import com.aurionpro.ticketboard.user.entity.Role;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.enums.PermissionCode;
import com.aurionpro.ticketboard.user.enums.RoleType;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import com.aurionpro.ticketboard.user.repository.DepartmentRepository;
import com.aurionpro.ticketboard.user.repository.PermissionRepository;
import com.aurionpro.ticketboard.user.repository.RoleRepository;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SecurityDataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionConfig rolePermissionConfig;
    private final LookupDataRepository lookupDataRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissionsAndRoles();
        seedLookupData();
        seedRoleDemoUsers();
    }

    @Transactional
    public void seedPermissionsAndRoles() {
        log.info("Starting Security (RBAC) initialization...");

        for (RoleType roleType : RoleType.values()) {
            Role role = roleRepository.findByName(roleType).orElseGet(() ->
                    roleRepository.save(Role.builder()
                            .name(roleType)
                            .description(roleType.name().replace("ROLE_", " "))
                            .build()));

            Set<PermissionCode> permissionCodes = rolePermissionConfig.getPermissionsForRole(roleType);
            for (PermissionCode code : permissionCodes) {
                Permission permission = permissionRepository.findByCode(code).orElseGet(() ->
                        permissionRepository.save(Permission.builder()
                                .code(code)
                                .name(toTitleCase(code.getCode()))
                                .module(extractModule(code.getCode()))
                                .description(code.getDescription())
                                .build()));
                role.getPermissions().add(permission);
            }
            roleRepository.save(role);
        }

        log.info("Security (RBAC) initialization complete.");
    }

    @Transactional
    public void seedLookupData() {
        log.info("Starting Lookup Data initialization...");

        seedCategory("PRIORITY", List.of(
                entry("LOW", "Low", 1, "#4CAF50"),
                entry("MEDIUM", "Medium", 2, "#FF9800"),
                entry("HIGH", "High", 3, "#F44336"),
                entry("CRITICAL", "Critical", 4, "#9C27B0")
        ));

        seedCategory("WORK_ITEM_TYPE", List.of(
                entry("TASK", "Task", 1, "#2196F3"),
                entry("BUG", "Bug", 2, "#F44336"),
                entry("USER_STORY", "User Story", 3, "#4CAF50"),
                entry("SUBTASK", "Subtask", 4, "#9E9E9E"),
                entry("FEATURE", "Feature", 5, "#FF9800"),
                entry("ENHANCEMENT", "Enhancement", 6, "#3F51B5"),
                entry("CHANGE_REQUEST", "Change Request", 7, "#E91E63")
        ));

        seedCategory("WORK_ITEM_SEVERITY", List.of(
                entry("BLOCKER", "Blocker", 1, "#F44336"),
                entry("CRITICAL", "Critical", 2, "#E91E63"),
                entry("MAJOR", "Major", 3, "#FF9800"),
                entry("HIGH", "High", 4, "#FF5722"),
                entry("MEDIUM", "Medium", 5, "#9E9E9E"),
                entry("MINOR", "Minor", 6, "#8BC34A"),
                entry("LOW", "Low", 7, "#4CAF50")
        ));

        seedCategory("WORK_ITEM_STATUS", List.of(
                entry("TODO", "To Do", 1, "#9E9E9E"),
                entry("IN_ANALYSIS", "In Analysis", 2, "#2196F3"),
                entry("IN_PROGRESS", "In Progress", 3, "#FF9800"),
                entry("DEV_IN_PROGRESS", "Dev In Progress", 4, "#FF9800"),
                entry("DEV_EXIT", "Dev Exit", 5, "#4CAF50"),
                entry("IN_REVIEW", "In Review", 6, "#3F51B5"),
                entry("TESTING", "Testing", 7, "#00BCD4"),
                entry("SIT_IN_PROGRESS", "SIT In Progress", 8, "#009688"),
                entry("SIT_EXIT", "SIT Exit", 9, "#4CAF50"),
                entry("UAT_IN_PROGRESS", "UAT In Progress", 10, "#8BC34A"),
                entry("UAT_EXIT", "UAT Exit", 11, "#4CAF50"),
                entry("PRE_PROD", "Pre-Prod", 12, "#CDDC39"),
                entry("GO_LIVE", "Go Live", 13, "#FF5722"),
                entry("BLOCKED", "Blocked", 14, "#F44336"),
                entry("COMPLETED", "Completed", 15, "#4CAF50"),
                entry("CLOSED", "Closed", 16, "#607D8B")
        ));

        seedCategory("PROJECT_STATUS", List.of(
                entry("PROPOSED", "Proposed", 1, "#9E9E9E"),
                entry("APPROVED", "Approved", 2, "#2196F3"),
                entry("IN_PROGRESS", "In Progress", 3, "#FF9800"),
                entry("ON_HOLD", "On Hold", 4, "#FFC107"),
                entry("COMPLETED", "Completed", 5, "#4CAF50"),
                entry("CANCELLED", "Cancelled", 6, "#F44336"),
                entry("CLOSED", "Closed", 7, "#607D8B")
        ));

        seedCategory("PROJECT_HEALTH", List.of(
                entry("GREEN", "Green", 1, "#4CAF50"),
                entry("AMBER", "Amber", 2, "#FF9800"),
                entry("RED", "Red", 3, "#F44336")
        ));

        seedCategory("RELEASE_ENVIRONMENT", List.of(
                entry("DEV", "DEV", 1, "#2196F3"),
                entry("SIT", "SIT", 2, "#00BCD4"),
                entry("UAT", "UAT", 3, "#FF9800"),
                entry("PRE_PROD", "Pre-Prod", 4, "#9C27B0"),
                entry("PRODUCTION", "Production", 5, "#F44336")
        ));

        seedCategory("RELEASE_STATUS", List.of(
                entry("DRAFT", "Draft", 1, "#9E9E9E"),
                entry("PLANNED", "Planned", 2, "#2196F3"),
                entry("READY", "Ready", 3, "#FF9800"),
                entry("APPROVED", "Approved", 4, "#4CAF50"),
                entry("DEPLOYING", "Deploying", 5, "#FFC107"),
                entry("DEPLOYED", "Deployed", 6, "#4CAF50"),
                entry("VERIFIED", "Verified", 7, "#00BCD4"),
                entry("CLOSED", "Closed", 8, "#607D8B")
        ));

        seedCategory("RISK_STATUS", List.of(
                entry("IDENTIFIED", "Identified", 1, "#FF9800"),
                entry("MITIGATED", "Mitigated", 2, "#4CAF50"),
                entry("ACCEPTED", "Accepted", 3, "#2196F3"),
                entry("CLOSED", "Closed", 4, "#607D8B")
        ));

        seedCategory("INVOICE_STATUS", List.of(
                entry("DRAFT", "Draft", 1, "#9E9E9E"),
                entry("SENT", "Sent", 2, "#2196F3"),
                entry("VIEWED", "Viewed", 3, "#FFC107"),
                entry("PARTIALLY_PAID", "Partially Paid", 4, "#FF9800"),
                entry("PAID", "Paid", 5, "#4CAF50"),
                entry("OVERDUE", "Overdue", 6, "#F44336"),
                entry("CANCELLED", "Cancelled", 7, "#607D8B")
        ));

        seedCategory("BILLING_TYPE", List.of(
                entry("Billable", "Billable", 1, "#4CAF50"),
                entry("Non-Billable", "Non-Billable", 2, "#FF9800"),
                entry("Internal", "Internal", 3, "#2196F3")
        ));

        seedCategory("TIMESHEET_STATUS", List.of(
                entry("DRAFT", "Draft", 1, "#9E9E9E"),
                entry("SUBMITTED", "Submitted", 2, "#2196F3"),
                entry("APPROVED", "Approved", 3, "#4CAF50"),
                entry("REJECTED", "Rejected", 4, "#F44336")
        ));

        seedCategory("REQUIREMENT_STATUS", List.of(
                entry("DRAFT", "Draft", 1, "#9E9E9E"),
                entry("SUBMITTED", "Submitted", 2, "#2196F3"),
                entry("UNDER_REVIEW", "Under Review", 3, "#FF9800"),
                entry("NEEDS_CLARIFICATION", "Needs Clarification", 4, "#FF5722"),
                entry("APPROVED", "Approved", 5, "#4CAF50"),
                entry("REJECTED", "Rejected", 6, "#F44336"),
                entry("ESTIMATED", "Estimated", 7, "#00BCD4"),
                entry("PLANNED_FOR_SPRINT", "Planned For Sprint", 8, "#3F51B5"),
                entry("READY_FOR_DEV", "Ready For Dev", 9, "#009688"),
                entry("IN_DEVELOPMENT", "In Development", 10, "#FFC107"),
                entry("DEV_COMPLETE", "Dev Complete", 11, "#4CAF50"),
                entry("IN_QA", "In QA", 12, "#8BC34A"),
                entry("UAT_READY", "UAT Ready", 13, "#CDDC39"),
                entry("UAT_APPROVED", "UAT Approved", 14, "#4CAF50"),
                entry("READY_FOR_RELEASE", "Ready For Release", 15, "#2196F3"),
                entry("DELIVERED", "Delivered", 16, "#4CAF50"),
                entry("CLOSED", "Closed", 17, "#607D8B"),
                entry("CANCELLED", "Cancelled", 18, "#F44336")
        ));

        seedCategory("DESIGNATION", List.of(
                entry("Software Engineer", "Software Engineer", 1, null),
                entry("Senior Software Engineer", "Senior Software Engineer", 2, null),
                entry("Lead Software Engineer", "Lead Software Engineer", 3, null),
                entry("Technical Team Lead", "Technical Team Lead", 4, null),
                entry("Business Analyst", "Business Analyst", 5, null),
                entry("Senior Business Analyst", "Senior Business Analyst", 6, null),
                entry("QA Engineer", "QA Engineer", 7, null),
                entry("QA Automation Engineer", "QA Automation Engineer", 8, null),
                entry("Project Manager", "Project Manager", 9, null),
                entry("Project Owner", "Project Owner", 10, null),
                entry("Engineering Manager", "Engineering Manager", 11, null),
                entry("Solution Architect", "Solution Architect", 12, null),
                entry("Administrator", "Administrator", 13, null)
        ));

        seedCategory("SKILL", List.of(
                entry("Java", "Java", 1, null),
                entry("Spring Boot", "Spring Boot", 2, null),
                entry("Angular", "Angular", 3, null),
                entry("React", "React", 4, null),
                entry("MySQL", "MySQL", 5, null),
                entry("PostgreSQL", "PostgreSQL", 6, null),
                entry("MongoDB", "MongoDB", 7, null),
                entry("Microservices", "Microservices", 8, null),
                entry("Docker", "Docker", 9, null),
                entry("Kubernetes", "Kubernetes", 10, null),
                entry("AWS", "AWS", 11, null),
                entry("Python", "Python", 12, null),
                entry("JavaScript", "JavaScript", 13, null),
                entry("TypeScript", "TypeScript", 14, null),
                entry("Selenium", "Selenium", 15, null),
                entry("JMeter", "JMeter", 16, null),
                entry("Hibernate", "Hibernate", 17, null),
                entry("REST APIs", "REST APIs", 18, null),
                entry("Kafka", "Kafka", 19, null),
                entry("IBM MQ", "IBM MQ", 20, null)
        ));

        seedCategory("MILESTONE_STATUS", List.of(
                entry("PLANNED", "Planned", 1, "#64748B"),
                entry("IN_PROGRESS", "In Progress", 2, "#3B82F6"),
                entry("ACHIEVED", "Achieved", 3, "#10B981"),
                entry("DELAYED", "Delayed / Missed", 4, "#EF4444"),
                entry("CANCELLED", "Cancelled", 5, "#9E9E9E")
        ));

        seedCategory("MILESTONE_FLAG", List.of(
                entry("RELEASE_MILESTONE", "Release Milestone", 1, "#9C27B0"),
                entry("AFFECTED_MILESTONE", "Affected Milestone", 2, "#F44336")
        ));

        seedCategory("ISSUE_STATUS", List.of(
                entry("OPEN", "Open", 1, "#EF4444"),
                entry("IN_PROGRESS", "In Progress", 2, "#F59E0B"),
                entry("RESOLVED", "Resolved", 3, "#10B981"),
                entry("CLOSED", "Closed", 4, "#64748B"),
                entry("REOPENED", "Reopened", 5, "#E91E63")
        ));

        seedCategory("ISSUE_SEVERITY", List.of(
                entry("LOW", "Low", 1, "#10B981"),
                entry("MEDIUM", "Medium", 2, "#3B82F6"),
                entry("HIGH", "High", 3, "#F59E0B"),
                entry("CRITICAL", "Critical", 4, "#EF4444")
        ));

        seedCategory("ISSUE_CLASSIFICATION", List.of(
                entry("FUNCTIONAL_DEFECT", "Functional Defect", 1, "#EF4444"),
                entry("UI_DEFECT", "UI / UX Defect", 2, "#F59E0B"),
                entry("DATA_INTEGRITY", "Data Integrity", 3, "#3B82F6"),
                entry("BACKEND_LOGIC", "Backend Logic", 4, "#8B5CF6"),
                entry("REPORTING", "Reporting / Export", 5, "#06B6D4"),
                entry("PERFORMANCE_LOAD", "Performance / Load", 6, "#EC4899"),
                entry("SECURITY_VULNERABILITY", "Security Vulnerability", 7, "#DC2626"),
                entry("REQUIREMENTS_GAP", "Requirements Gap", 8, "#0D9488"),
                entry("ENVIRONMENT_CONFIG", "Environment / Configuration", 9, "#6366F1"),
                entry("DOCUMENTATION", "Documentation", 10, "#64748B")
        ));

        seedCategory("RISK_PROBABILITY", List.of(
                entry("1", "Rare", 1, "#10B981"),
                entry("2", "Unlikely", 2, "#3B82F6"),
                entry("3", "Moderate", 3, "#F59E0B"),
                entry("4", "Likely", 4, "#FF9800"),
                entry("5", "Almost Certain", 5, "#EF4444")
        ));

        seedCategory("RISK_IMPACT", List.of(
                entry("1", "Negligible", 1, "#10B981"),
                entry("2", "Minor", 2, "#3B82F6"),
                entry("3", "Moderate", 3, "#F59E0B"),
                entry("4", "Major", 4, "#FF9800"),
                entry("5", "Catastrophic", 5, "#EF4444")
        ));

        log.info("Lookup Data initialization complete.");
    }

    private void seedCategory(String category, List<LookupSeed> seeds) {
        for (LookupSeed seed : seeds) {
            if (!lookupDataRepository.existsByCategoryAndValue(category, seed.value)) {
                lookupDataRepository.save(LookupData.builder()
                        .category(category)
                        .value(seed.value)
                        .label(seed.label)
                        .displayOrder(seed.order)
                        .colorCode(seed.colorCode)
                        .isActive(true)
                        .isDefault(true)
                        .build());
            }
        }
    }

    private LookupSeed entry(String value, String label, int order, String colorCode) {
        return new LookupSeed(value, label, order, colorCode);
    }

    private record LookupSeed(String value, String label, int order, String colorCode) {}

    private String extractModule(String code) {
        if (code.startsWith("admin:")) return "ADMIN";
        if (code.startsWith("audit:")) return "ADMIN";
        return code.substring(0, code.indexOf(":")).toUpperCase();
    }

    private String toTitleCase(String code) {
        String[] parts = code.split(":");
        if (parts.length < 2) return code;
        return parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1)
                + " " + parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).replace("-", " ");
    }

    private void seedRoleDemoUsers() {
        if (userRepository.count() == 0) {
            log.info("Fresh database detected. Skipping role demo users (main DataInitializer will seed).");
            return;
        }

        String defaultPass = passwordEncoder.encode("Admin@123");
        Department firstDept = departmentRepository.findAll().stream().findFirst().orElse(null);

        if (!userRepository.existsByEmail("opsadmin@ticketboard.com")) {
            Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN).orElse(null);
            User user = User.builder()
                    .employeeId("EMP-1091")
                    .firstName("Operations")
                    .lastName("Admin")
                    .email("opsadmin@ticketboard.com")
                    .passwordHash(defaultPass)
                    .designation("Operations Administrator")
                    .department(firstDept)
                    .roles(adminRole != null ? Set.of(adminRole) : Set.of())
                    .dailyCapacityHours(8.0)
                    .hourlyCost(60.0)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            log.info("Seeded ROLE_ADMIN demo user: opsadmin@ticketboard.com");
        }

        if (!userRepository.existsByEmail("owner@ticketboard.com")) {
            Role ownerRole = roleRepository.findByName(RoleType.ROLE_PROJECT_OWNER).orElse(null);
            User user = User.builder()
                    .employeeId("EMP-1092")
                    .firstName("Rohan")
                    .lastName("Kapoor")
                    .email("owner@ticketboard.com")
                    .passwordHash(defaultPass)
                    .designation("Project Owner")
                    .department(firstDept)
                    .roles(ownerRole != null ? Set.of(ownerRole) : Set.of())
                    .dailyCapacityHours(8.0)
                    .hourlyCost(70.0)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            log.info("Seeded ROLE_PROJECT_OWNER demo user: owner@ticketboard.com");
        }

        if (!userRepository.existsByEmail("pm2@ticketboard.com")) {
            Role pmRole = roleRepository.findByName(RoleType.ROLE_PROJECT_MANAGER).orElse(null);
            User user = User.builder()
                    .employeeId("EMP-1093")
                    .firstName("Neha")
                    .lastName("Gupta")
                    .email("pm2@ticketboard.com")
                    .passwordHash(defaultPass)
                    .designation("Project Manager")
                    .department(firstDept)
                    .roles(pmRole != null ? Set.of(pmRole) : Set.of())
                    .dailyCapacityHours(8.0)
                    .hourlyCost(65.0)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            log.info("Seeded ROLE_PROJECT_MANAGER demo user: pm2@ticketboard.com");
        }
    }
}