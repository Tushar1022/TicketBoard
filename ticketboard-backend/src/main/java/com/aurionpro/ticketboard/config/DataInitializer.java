package com.aurionpro.ticketboard.config;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.billing.entity.Invoice;
import com.aurionpro.ticketboard.billing.entity.InvoiceLineItem;
import com.aurionpro.ticketboard.billing.enums.InvoiceStatus;
import com.aurionpro.ticketboard.billing.repository.InvoiceRepository;
import com.aurionpro.ticketboard.client.entity.Client;
import com.aurionpro.ticketboard.client.repository.ClientRepository;
import com.aurionpro.ticketboard.project.entity.Milestone;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.entity.ProjectMember;
import com.aurionpro.ticketboard.project.enums.MilestoneStatus;
import com.aurionpro.ticketboard.project.enums.ProjectHealth;
import com.aurionpro.ticketboard.project.enums.ProjectPriority;
import com.aurionpro.ticketboard.project.enums.ProjectStatus;
import com.aurionpro.ticketboard.project.repository.MilestoneRepository;
import com.aurionpro.ticketboard.project.repository.ProjectMemberRepository;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.release.entity.Release;
import com.aurionpro.ticketboard.release.entity.ReleaseItem;
import com.aurionpro.ticketboard.release.enums.ReleaseEnvironment;
import com.aurionpro.ticketboard.release.enums.ReleaseStatus;
import com.aurionpro.ticketboard.release.repository.ReleaseItemRepository;
import com.aurionpro.ticketboard.release.repository.ReleaseRepository;
import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.requirement.enums.RequirementPriority;
import com.aurionpro.ticketboard.requirement.enums.RequirementStatus;
import com.aurionpro.ticketboard.requirement.repository.RequirementRepository;
import com.aurionpro.ticketboard.risk.entity.Issue;
import com.aurionpro.ticketboard.risk.entity.Risk;
import com.aurionpro.ticketboard.risk.enums.IssueSeverity;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
import com.aurionpro.ticketboard.risk.enums.RiskStatus;
import com.aurionpro.ticketboard.risk.repository.IssueRepository;
import com.aurionpro.ticketboard.risk.repository.RiskRepository;
import com.aurionpro.ticketboard.timetracking.entity.TimeEntry;
import com.aurionpro.ticketboard.timetracking.entity.Timesheet;
import com.aurionpro.ticketboard.timetracking.enums.TimeEntryStatus;
import com.aurionpro.ticketboard.timetracking.enums.TimesheetStatus;
import com.aurionpro.ticketboard.timetracking.repository.TimeEntryRepository;
import com.aurionpro.ticketboard.timetracking.repository.TimesheetRepository;
import com.aurionpro.ticketboard.user.entity.Department;
import com.aurionpro.ticketboard.user.entity.Role;
import com.aurionpro.ticketboard.user.entity.Team;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.enums.RoleType;
import com.aurionpro.ticketboard.user.enums.UserStatus;
import com.aurionpro.ticketboard.user.repository.DepartmentRepository;
import com.aurionpro.ticketboard.user.repository.RoleRepository;
import com.aurionpro.ticketboard.user.repository.TeamRepository;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.entity.WorkItemBlocker;
import com.aurionpro.ticketboard.workitem.enums.WorkItemPriority;
import com.aurionpro.ticketboard.workitem.enums.WorkItemSeverity;
import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import com.aurionpro.ticketboard.workitem.enums.WorkItemType;
import com.aurionpro.ticketboard.support.entity.SupportCategory;
import com.aurionpro.ticketboard.support.entity.SupportTicket;
import com.aurionpro.ticketboard.support.entity.TicketComment;
import com.aurionpro.ticketboard.support.entity.TicketPriority;
import com.aurionpro.ticketboard.support.entity.TicketStatus;
import com.aurionpro.ticketboard.support.repository.SupportTicketRepository;
import com.aurionpro.ticketboard.workitem.repository.WorkItemBlockerRepository;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MilestoneRepository milestoneRepository;
    private final RequirementRepository requirementRepository;
    private final WorkItemRepository workItemRepository;
    private final WorkItemBlockerRepository blockerRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final TimesheetRepository timesheetRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseItemRepository releaseItemRepository;
    private final RiskRepository riskRepository;
    private final IssueRepository issueRepository;
    private final InvoiceRepository invoiceRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting TicketBoard Data Initialization...");

        // 1. Initialize Roles
        for (RoleType roleType : RoleType.values()) {
            roleRepository.findByName(roleType).orElseGet(() ->
                    roleRepository.save(Role.builder()
                            .name(roleType)
                            .description(roleType.name().replace("ROLE_", ""))
                            .build())
            );
        }

        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial sample data generation.");
            return;
        }

        log.info("Seeding initial enterprise dataset...");

        // 2. Initialize Departments
        Department txnDept = departmentRepository.save(Department.builder()
                .name("Transaction Banking")
                .code("TXN_BANK")
                .description("Handles high-throughput payment pipelines, core routing, and clearing")
                .build());

        Department digiDept = departmentRepository.save(Department.builder()
                .name("Digital Banking")
                .code("DIGI_BANK")
                .description("Customer-facing web, mobile, and open banking API systems")
                .build());

        // 3. Initialize Teams
        Team paymentsTeam = teamRepository.save(Team.builder()
                .name("Payments Team")
                .code("PAY_TEAM")
                .description("Core payment messaging, ISO20022 and IFSC processing")
                .department(txnDept)
                .build());

        Team integrationTeam = teamRepository.save(Team.builder()
                .name("Integration Team")
                .code("INT_TEAM")
                .description("Middleware, MQ consumers, and API Gateway integration")
                .department(txnDept)
                .build());

        Team qaTeam = teamRepository.save(Team.builder()
                .name("QA & Automation Team")
                .code("QA_TEAM")
                .description("System Integration, Performance & Regression Testing")
                .department(txnDept)
                .build());

        // 4. Initialize Users
        Role superAdminRole = roleRepository.findByName(RoleType.ROLE_SUPER_ADMIN).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN).orElseThrow();
        Role ownerRole = roleRepository.findByName(RoleType.ROLE_PROJECT_OWNER).orElseThrow();
        Role pmRole = roleRepository.findByName(RoleType.ROLE_PROJECT_MANAGER).orElseThrow();
        Role baRole = roleRepository.findByName(RoleType.ROLE_BUSINESS_ANALYST).orElseThrow();
        Role leadRole = roleRepository.findByName(RoleType.ROLE_TEAM_LEAD).orElseThrow();
        Role devRole = roleRepository.findByName(RoleType.ROLE_DEVELOPER).orElseThrow();
        Role qaRole = roleRepository.findByName(RoleType.ROLE_QA_TESTER).orElseThrow();
        Role mgmtRole = roleRepository.findByName(RoleType.ROLE_MANAGEMENT).orElseThrow();

        String defaultPass = passwordEncoder.encode("Admin@123");

        User admin = userRepository.save(User.builder()
                .employeeId("EMP-1001")
                .firstName("Tushar")
                .lastName("Shinde")
                .email("admin@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543210")
                .designation("Platform Solution Architect & Admin")
                .department(txnDept)
                .team(paymentsTeam)
                .roles(Set.of(superAdminRole, pmRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(90.0)
                .skills("Java, Spring Boot, Microservices, Oracle, Solution Architecture")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2022, 1, 15))
                .build());

        User opsAdmin = userRepository.save(User.builder()
                .employeeId("EMP-1091")
                .firstName("Operations")
                .lastName("Admin")
                .email("opsadmin@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543111")
                .designation("Operations Administrator")
                .department(txnDept)
                .team(paymentsTeam)
                .roles(Set.of(adminRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(60.0)
                .skills("User Administration, Master Data Management, Reporting")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 9, 10))
                .build());

        User owner = userRepository.save(User.builder()
                .employeeId("EMP-1092")
                .firstName("Rohan")
                .lastName("Kapoor")
                .email("owner@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543222")
                .designation("Project Owner")
                .department(txnDept)
                .team(paymentsTeam)
                .roles(Set.of(ownerRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(70.0)
                .skills("Product Ownership, Stakeholder Management, Delivery Governance")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 2, 20))
                .build());

        User pm = userRepository.save(User.builder()
                .employeeId("EMP-1002")
                .firstName("Amit")
                .lastName("Sharma")
                .email("pm@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543211")
                .designation("Senior Project Manager")
                .department(txnDept)
                .team(paymentsTeam)
                .roles(Set.of(pmRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(80.0)
                .skills("Project Governance, Agile/Scrum, Delivery Tracking, Risk Management")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 3, 1))
                .build());

        User ba = userRepository.save(User.builder()
                .employeeId("EMP-1003")
                .firstName("Pooja")
                .lastName("Verma")
                .email("ba@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543212")
                .designation("Lead Business Analyst")
                .department(txnDept)
                .team(paymentsTeam)
                .roles(Set.of(baRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(70.0)
                .skills("BRD/FRD, ISO20022, Banking Workflows, Acceptance Criteria")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 6, 15))
                .build());

        User lead = userRepository.save(User.builder()
                .employeeId("EMP-1004")
                .firstName("Suresh")
                .lastName("Patil")
                .email("lead@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543213")
                .designation("Technical Team Lead")
                .department(txnDept)
                .team(paymentsTeam)
                .roles(Set.of(leadRole, devRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(75.0)
                .skills("Spring Cloud, Database Tuning, MQ, Code Review")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2022, 8, 1))
                .build());

        User dev1 = userRepository.save(User.builder()
                .employeeId("EMP-1005")
                .firstName("Rahul")
                .lastName("Nair")
                .email("dev1@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543214")
                .designation("Senior Java Developer")
                .department(txnDept)
                .team(paymentsTeam)
                .reportingManager(lead)
                .roles(Set.of(devRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(60.0)
                .skills("Java 21, Spring Boot, MySQL, Hibernate, JPA, Docker")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2024, 1, 10))
                .build());

        User dev2 = userRepository.save(User.builder()
                .employeeId("EMP-1006")
                .firstName("Sneha")
                .lastName("Rao")
                .email("dev2@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543215")
                .designation("Backend Developer")
                .department(txnDept)
                .team(integrationTeam)
                .reportingManager(lead)
                .roles(Set.of(devRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(55.0)
                .skills("Spring Boot, IBM MQ, REST APIs, Kafka, JUnit 5")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2024, 4, 1))
                .build());

        User qa = userRepository.save(User.builder()
                .employeeId("EMP-1007")
                .firstName("Vikram")
                .lastName("Joshi")
                .email("qa@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543216")
                .designation("Senior QA Automation Engineer")
                .department(txnDept)
                .team(qaTeam)
                .roles(Set.of(qaRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(50.0)
                .skills("Selenium, RestAssured, Postman, Performance Testing")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 11, 15))
                .build());

        User mgmt = userRepository.save(User.builder()
                .employeeId("EMP-1008")
                .firstName("Rajesh")
                .lastName("Mehta")
                .email("mgmt@ticketboard.com")
                .passwordHash(defaultPass)
                .phone("+91-9876543217")
                .designation("VP of Engineering & Delivery")
                .department(txnDept)
                .roles(Set.of(mgmtRole))
                .dailyCapacityHours(8.0)
                .hourlyCost(120.0)
                .skills("Executive Management, Strategic Planning, KPI Monitoring")
                .status(UserStatus.ACTIVE)
                .joiningDate(LocalDate.of(2021, 5, 1))
                .build());

        // Update team leads
        paymentsTeam.setTeamLead(lead);
        teamRepository.save(paymentsTeam);

        // 5. Initialize Clients
        Client client1 = clientRepository.save(Client.builder()
                .clientCode("ABC-BANK")
                .name("ABC National Commercial Bank")
                .contactPerson("Sunil Agarwal")
                .email("sunil.agarwal@abcbank.com")
                .phone("+91-22-66554433")
                .address("Financial Center, Bandra Kurla Complex, Mumbai")
                .status("ACTIVE")
                .accountManager(pm)
                .build());

        Client client2 = clientRepository.save(Client.builder()
                .clientCode("GPS-CORP")
                .name("Global Payment Solutions Corp")
                .contactPerson("Elena Rostova")
                .email("elena@gpscorp.com")
                .phone("+1-415-889-0123")
                .address("500 Market St, San Francisco, CA")
                .status("ACTIVE")
                .accountManager(pm)
                .build());

        // 6. Initialize Projects
        Project proj1 = projectRepository.save(Project.builder()
                .projectCode("CR91")
                .name("CR91 IFSC Code & Routing Engine Update")
                .description("National clearing IFSC migration, MQ consumer enhancements, and database routing table updates.")
                .client(client1)
                .projectManager(pm)
                .startDate(LocalDate.now().minusDays(20))
                .plannedEndDate(LocalDate.now().plusDays(10))
                .priority(ProjectPriority.HIGH)
                .status(ProjectStatus.IN_PROGRESS)
                .health(ProjectHealth.AMBER)
                .budget(50000.0)
                .estimatedHours(420.0)
                .actualHours(390.0)
                .completionPercentage(72.0)
                .build());

        Project proj2 = projectRepository.save(Project.builder()
                .projectCode("IPM-2026")
                .name("Instant Payments & MQ Messaging Engine")
                .description("Sub-second transaction routing with distributed circuit breaking and Kafka notification pipelines.")
                .client(client1)
                .projectManager(pm)
                .startDate(LocalDate.now().minusDays(40))
                .plannedEndDate(LocalDate.now().plusDays(35))
                .priority(ProjectPriority.CRITICAL)
                .status(ProjectStatus.IN_PROGRESS)
                .health(ProjectHealth.GREEN)
                .budget(95000.0)
                .estimatedHours(650.0)
                .actualHours(320.0)
                .completionPercentage(55.0)
                .build());

        Project proj3 = projectRepository.save(Project.builder()
                .projectCode("DCB-100")
                .name("Digital Core Banking Gateway v2")
                .description("Modern REST and OpenAPI layer for core banking account lookups and balance checks.")
                .client(client2)
                .projectManager(pm)
                .startDate(LocalDate.now().minusDays(60))
                .plannedEndDate(LocalDate.now().minusDays(5))
                .priority(ProjectPriority.MEDIUM)
                .status(ProjectStatus.COMPLETED)
                .health(ProjectHealth.GREEN)
                .budget(120000.0)
                .estimatedHours(800.0)
                .actualHours(780.0)
                .completionPercentage(100.0)
                .build());

        // Project Members
        projectMemberRepository.save(ProjectMember.builder().project(proj1).user(lead).projectRole("Technical Lead").allocatedHoursPerDay(4.0).build());
        projectMemberRepository.save(ProjectMember.builder().project(proj1).user(dev1).projectRole("Senior Developer").allocatedHoursPerDay(8.0).build());
        projectMemberRepository.save(ProjectMember.builder().project(proj1).user(dev2).projectRole("Backend Developer").allocatedHoursPerDay(8.0).build());
        projectMemberRepository.save(ProjectMember.builder().project(proj1).user(qa).projectRole("QA Tester").allocatedHoursPerDay(6.0).build());

        projectMemberRepository.save(ProjectMember.builder().project(proj2).user(lead).projectRole("Technical Lead").allocatedHoursPerDay(4.0).build());
        projectMemberRepository.save(ProjectMember.builder().project(proj2).user(dev1).projectRole("Senior Developer").allocatedHoursPerDay(4.0).build());
        projectMemberRepository.save(ProjectMember.builder().project(proj2).user(dev2).projectRole("Backend Developer").allocatedHoursPerDay(8.0).build());

        // Milestones
        milestoneRepository.save(Milestone.builder().project(proj1).name("Requirement Sign-off").plannedDate(LocalDate.now().minusDays(15)).actualDate(LocalDate.now().minusDays(14)).status(MilestoneStatus.ACHIEVED).completionPercentage(100.0).owner(ba).build());
        milestoneRepository.save(Milestone.builder().project(proj1).name("DB & MQ Core Development").plannedDate(LocalDate.now().minusDays(5)).actualDate(LocalDate.now().minusDays(4)).status(MilestoneStatus.ACHIEVED).completionPercentage(100.0).owner(dev1).build());
        milestoneRepository.save(Milestone.builder().project(proj1).name("SIT & Regression Testing").plannedDate(LocalDate.now().plusDays(3)).status(MilestoneStatus.IN_PROGRESS).completionPercentage(60.0).owner(qa).build());
        milestoneRepository.save(Milestone.builder().project(proj1).name("Production Go-Live").plannedDate(LocalDate.now().plusDays(10)).status(MilestoneStatus.PLANNED).completionPercentage(0.0).owner(pm).build());

        // 7. Initialize Requirements
        Requirement req1 = requirementRepository.save(Requirement.builder()
                .reqNumber("REQ-2026-001")
                .title("CR91 Dynamic IFSC Validation & Routing Service")
                .description("Support dynamic lookup of updated RTGS/NEFT IFSC codes from centralized database repository.")
                .businessObjective("Prevent payment rejections due to outdated bank IFSC codes during routing.")
                .acceptanceCriteria("1. Validate 11-character alphanumeric IFSC code against live DB\n2. Return clearing branch details within 50ms\n3. Cache results for 24 hours")
                .priority(RequirementPriority.HIGH)
                .requester("RBI Compliance Team")
                .project(proj1)
                .owner(ba)
                .estimatedEffortHours(45.0)
                .originalEstimateHours(40.0)
                .actualEffortHours(42.0)
                .plannedStartDate(LocalDate.now().minusDays(18))
                .plannedEndDate(LocalDate.now().minusDays(2))
                .actualStartDate(LocalDate.now().minusDays(18))
                .status(RequirementStatus.APPROVED)
                .deliveryVersion("REL-2026-014")
                .scopeVersion(2)
                .scopeCreepFlag(false)
                .build());

        Requirement req2 = requirementRepository.save(Requirement.builder()
                .reqNumber("REQ-2026-002")
                .title("MQ Consumer with Dead-Letter Handling")
                .description("Build resilient MQ listener with automatic retry backoff and dead-letter queue routing.")
                .businessObjective("Ensure 99.999% payment message processing reliability without dropped packets.")
                .acceptanceCriteria("1. Process 500 msgs/sec\n2. Retry 3 times with exponential backoff\n3. Route failed messages to DLQ with audit payload")
                .priority(RequirementPriority.CRITICAL)
                .requester("Payment Operations")
                .project(proj1)
                .owner(lead)
                .estimatedEffortHours(80.0)
                .originalEstimateHours(80.0)
                .actualEffortHours(65.0)
                .plannedStartDate(LocalDate.now().minusDays(12))
                .plannedEndDate(LocalDate.now().plusDays(5))
                .actualStartDate(LocalDate.now().minusDays(12))
                .status(RequirementStatus.IN_PROGRESS)
                .deliveryVersion("REL-2026-014")
                .scopeVersion(1)
                .scopeCreepFlag(false)
                .build());

        Requirement req3 = requirementRepository.save(Requirement.builder()
                .reqNumber("REQ-2026-003")
                .title("Audit Log Trail for High-Value Transactions")
                .description("Immutable audit log recording user, timestamp, IP, old value, and new value for high-value fund transfers.")
                .businessObjective("Compliance with SOC2 and RBI information security guidelines.")
                .acceptanceCriteria("All transfers > 500,000 INR must have timestamped cryptographic audit trail.")
                .priority(RequirementPriority.HIGH)
                .requester("Compliance Officer")
                .project(proj2)
                .owner(ba)
                .estimatedEffortHours(60.0)
                .originalEstimateHours(60.0)
                .actualEffortHours(35.0)
                .plannedStartDate(LocalDate.now().minusDays(10))
                .plannedEndDate(LocalDate.now().plusDays(15))
                .actualStartDate(LocalDate.now().minusDays(10))
                .status(RequirementStatus.IN_PROGRESS)
                .deliveryVersion("REL-2026-015")
                .scopeVersion(1)
                .build());

        // 8. Initialize Work Items (Tasks & Bugs)
        WorkItem task1 = workItemRepository.save(WorkItem.builder()
                .ticketNumber("CR91-101")
                .title("IFSC Database Schema Migration & Indexing")
                .description("Flyway scripts for IFSC tables with B-Tree indexes on code and branch_code.")
                .type(WorkItemType.TASK)
                .priority(WorkItemPriority.HIGH)
                .severity(WorkItemSeverity.MEDIUM)
                .status(WorkItemStatus.COMPLETED)
                .project(proj1)
                .requirement(req1)
                .assignee(dev1)
                .reporter(pm)
                .estimatedHours(16.0)
                .actualHours(18.0)
                .startDate(LocalDate.now().minusDays(16))
                .dueDate(LocalDate.now().minusDays(10))
                .completedDate(LocalDate.now().minusDays(9))
                .associatedTeam("Core Banking Squad")
                .billingType("Billable")
                .labels("database,flyway,performance")
                .build());

        WorkItem task2 = workItemRepository.save(WorkItem.builder()
                .ticketNumber("CR91-102")
                .title("MQ Consumer Implementation & Circuit Breaker")
                .description("Implement Spring JMS listener with resilience4j circuit breaker on queue connections.")
                .type(WorkItemType.TASK)
                .priority(WorkItemPriority.CRITICAL)
                .severity(WorkItemSeverity.MAJOR)
                .status(WorkItemStatus.IN_PROGRESS)
                .project(proj1)
                .requirement(req2)
                .assignee(dev2)
                .reporter(lead)
                .estimatedHours(40.0)
                .actualHours(32.0)
                .startDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().plusDays(4))
                .associatedTeam("Core Banking Squad")
                .billingType("Billable")
                .labels("mq,spring-jms,circuit-breaker")
                .build());

        WorkItem task3 = workItemRepository.save(WorkItem.builder()
                .ticketNumber("CR91-103")
                .title("SIT Integration Testing & Mock Simulator")
                .description("Run complete SIT suite with mock MQ message sender to simulate 2000 msgs/min.")
                .type(WorkItemType.TASK)
                .priority(WorkItemPriority.HIGH)
                .severity(WorkItemSeverity.MEDIUM)
                .status(WorkItemStatus.SIT_EXIT)
                .project(proj1)
                .requirement(req1)
                .assignee(qa)
                .reporter(lead)
                .estimatedHours(24.0)
                .actualHours(24.0)
                .startDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().minusDays(2))
                .devExitDate(LocalDate.now().minusDays(8))
                .sitExitDate(LocalDate.now().minusDays(2))
                .uatExitDate(LocalDate.now().plusDays(5))
                .completionPercentage(100)
                .associatedTeam("QA Automation Squad")
                .billingType("Billable")
                .tags("SIT, Automation, P1")
                .labels("sit,automation,testing")
                .build());

        // CR#3 matching exact user prompt
        WorkItem cr3 = workItemRepository.save(WorkItem.builder()
                .ticketNumber("CR#3")
                .title("Display of Total amount of Selected Files – Requirement to display the total amount of selected files at the authorization level")
                .description("Business requirement to calculate and display the aggregate currency total and count of batch transactions before multi-sign authorization.")
                .type(WorkItemType.TASK)
                .priority(WorkItemPriority.MEDIUM)
                .severity(WorkItemSeverity.MEDIUM)
                .status(WorkItemStatus.UAT_EXIT)
                .project(proj1)
                .requirement(req1)
                .assignee(dev1)
                .reporter(ba)
                .estimatedHours(331.5)
                .actualHours(331.5)
                .startDate(LocalDate.of(2026, 1, 5))
                .dueDate(LocalDate.of(2026, 2, 26))
                .devExitDate(LocalDate.of(2026, 2, 26))
                .sitExitDate(LocalDate.of(2026, 4, 9))
                .uatExitDate(LocalDate.of(2026, 2, 21))
                .sdDeliveryDate(LocalDate.of(2026, 2, 21))
                .devEffortDays(3.0)
                .qcEffortDays(3.0)
                .durationDays(39)
                .completionPercentage(100)
                .associatedTeam("Core Authorization & Banking Squad")
                .billingType("None")
                .jiraTaskId("SBI-4892")
                .jiraStatus("Created")
                .tags("P1, Top45, Batch4")
                .reminder("None")
                .recurrence("None")
                .labels("sbi,cr,auth,total-amount")
                .build());

        WorkItem bug1 = workItemRepository.save(WorkItem.builder()
                .ticketNumber("BUG-101")
                .title("MQ Connection Timeout under High Load")
                .description("Connection pool starvation occurring when concurrent transactions exceed 300 threads.")
                .type(WorkItemType.BUG)
                .priority(WorkItemPriority.CRITICAL)
                .severity(WorkItemSeverity.BLOCKER)
                .status(WorkItemStatus.BLOCKED)
                .project(proj1)
                .requirement(req2)
                .assignee(dev1)
                .reporter(qa)
                .estimatedHours(12.0)
                .actualHours(8.0)
                .startDate(LocalDate.now().minusDays(4))
                .dueDate(LocalDate.now().plusDays(2))
                .blockedSince(LocalDateTime.now().minusDays(2))
                .blockedReason("Waiting for MQ server cluster credentials and port opening from Infra Team")
                .blockedOwner("Infrastructure & Network Team")
                .associatedTeam("Infrastructure Squad")
                .billingType("Billable")
                .tags("Blocker, P1")
                .labels("defect,mq,infrastructure,blocker")
                .build());

        // Blocker record for BUG-101
        blockerRepository.save(WorkItemBlocker.builder()
                .workItem(bug1)
                .reason("Waiting for MQ server cluster credentials and port opening from Infra Team")
                .owner("Infrastructure & Network Team")
                .blockedSince(LocalDateTime.now().minusDays(2))
                .expectedResolutionDate(LocalDateTime.now().plusDays(1))
                .build());

        WorkItem bug2 = workItemRepository.save(WorkItem.builder()
                .ticketNumber("BUG-102")
                .title("Null Pointer on Special Characters in IFSC Code")
                .description("Sanitization logic fails when user sends hyphen in IFSC input field.")
                .type(WorkItemType.BUG)
                .priority(WorkItemPriority.MEDIUM)
                .severity(WorkItemSeverity.LOW)
                .status(WorkItemStatus.TODO)
                .project(proj1)
                .requirement(req1)
                .assignee(dev2)
                .reporter(qa)
                .estimatedHours(4.0)
                .actualHours(0.0)
                .startDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(3))
                .associatedTeam("Core Banking Squad")
                .billingType("Non-Billable")
                .tags("Bug, Validation")
                .labels("bug,validation")
                .build());

        // 9. Initialize Time Entries & Timesheet
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);

        timeEntryRepository.save(TimeEntry.builder()
                .user(dev1)
                .project(proj1)
                .requirement(req1)
                .workItem(task1)
                .workDate(monday)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .totalHours(7.5)
                .description("Designed database schema and wrote Flyway DDL migration scripts.")
                .status(TimeEntryStatus.APPROVED)
                .build());

        timeEntryRepository.save(TimeEntry.builder()
                .user(dev1)
                .project(proj1)
                .requirement(req2)
                .workItem(bug1)
                .workDate(monday.plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 30))
                .breakMinutes(60)
                .totalHours(7.5)
                .description("Investigated thread contention in connection pool. Logged blocker for infra.")
                .status(TimeEntryStatus.LOGGED)
                .build());

        timeEntryRepository.save(TimeEntry.builder()
                .user(dev2)
                .project(proj1)
                .requirement(req2)
                .workItem(task2)
                .workDate(monday)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 30))
                .breakMinutes(30)
                .totalHours(8.0)
                .description("Implemented Spring JMS container and listener for incoming payment notifications.")
                .status(TimeEntryStatus.APPROVED)
                .build());

        timeEntryRepository.save(TimeEntry.builder()
                .user(dev1)
                .project(proj1)
                .requirement(req2)
                .workItem(bug1)
                .workDate(monday.plusDays(2))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .breakMinutes(30)
                .totalHours(7.5)
                .description("Root cause analysis of connection pool starvation under high load.")
                .status(TimeEntryStatus.APPROVED)
                .build());

        timeEntryRepository.save(TimeEntry.builder()
                .user(qa)
                .project(proj1)
                .requirement(req1)
                .workItem(task3)
                .workDate(monday.plusDays(3))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(30)
                .totalHours(7.5)
                .description("Executed SIT regression suite against IFSC migration build.")
                .status(TimeEntryStatus.APPROVED)
                .build());

        timeEntryRepository.save(TimeEntry.builder()
                .user(dev2)
                .project(proj1)
                .requirement(req2)
                .workItem(task3)
                .workDate(monday.plusDays(4))
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(17, 30))
                .breakMinutes(30)
                .totalHours(7.5)
                .description("Assisted QA with mock simulator tuning and test data setup.")
                .status(TimeEntryStatus.APPROVED)
                .build());

        // Weekly Timesheet
        Timesheet sheet1 = timesheetRepository.save(Timesheet.builder()
                .user(dev1)
                .startDate(monday)
                .endDate(monday.plusDays(6))
                .totalHours(15.0)
                .status(TimesheetStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now().minusHours(4))
                .build());

        // 10. Initialize Releases
        Release rel1 = releaseRepository.save(Release.builder()
                .releaseVersion("REL-2026-014")
                .title("CR91 IFSC Compliance Production Release")
                .description("Production deployment of CR91 IFSC Routing engine and MQ updates.")
                .project(proj1)
                .environment(ReleaseEnvironment.PRODUCTION)
                .plannedDate(LocalDate.now().plusDays(10))
                .status(ReleaseStatus.READY)
                .deploymentResult("Pending Go-Live Approval")
                .rollbackRequired(false)
                .owner(pm)
                .build());

        releaseItemRepository.save(ReleaseItem.builder().release(rel1).requirement(req1).workItem(task1).notes("Core IFSC migration").build());
        releaseItemRepository.save(ReleaseItem.builder().release(rel1).requirement(req2).workItem(task2).notes("Resilient MQ consumer").build());

        // 11. Initialize Invoicing / Billing
        Invoice invoice1 = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-2026-001")
                .client(proj1.getClient())
                .project(proj1)
                .fromDate(monday.minusDays(14))
                .toDate(monday.minusDays(8))
                .issuedDate(monday.minusDays(7))
                .dueDate(monday.plusDays(7))
                .status(InvoiceStatus.SENT)
                .subtotal(3120.00)
                .taxRate(18.0)
                .taxAmount(561.60)
                .total(3681.60)
                .currency("USD")
                .notes("CR91 IFSC Compliance - development milestone billing")
                .createdByUser(pm)
                .build());

        InvoiceLineItem line1 = InvoiceLineItem.builder()
                .invoice(invoice1)
                .workItemId(task1.getId())
                .workItemNumber(task1.getTicketNumber())
                .workItemTitle(task1.getTitle())
                .consultantId(dev1.getId())
                .consultantName(dev1.getFullName())
                .billingType("Billable")
                .description(task1.getTitle())
                .hours(18.0)
                .rate(dev1.getHourlyCost())
                .amount(Math.round(18.0 * dev1.getHourlyCost() * 100.0) / 100.0)
                .build();
        InvoiceLineItem line2 = InvoiceLineItem.builder()
                .invoice(invoice1)
                .workItemId(task2.getId())
                .workItemNumber(task2.getTicketNumber())
                .workItemTitle(task2.getTitle())
                .consultantId(dev2.getId())
                .consultantName(dev2.getFullName())
                .billingType("Billable")
                .description(task2.getTitle())
                .hours(24.0)
                .rate(dev2.getHourlyCost())
                .amount(Math.round(24.0 * dev2.getHourlyCost() * 100.0) / 100.0)
                .build();
        invoiceRepository.save(invoice1); // cascade line items

        Invoice invoice2 = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-2026-002")
                .client(proj1.getClient())
                .project(proj1)
                .fromDate(LocalDate.now().with(DayOfWeek.MONDAY).minusDays(7))
                .toDate(LocalDate.now().with(DayOfWeek.MONDAY).minusDays(1))
                .issuedDate(LocalDate.now().minusDays(2))
                .dueDate(LocalDate.now().plusDays(12))
                .status(InvoiceStatus.PAID)
                .subtotal(2040.00)
                .taxRate(18.0)
                .taxAmount(367.20)
                .total(2407.20)
                .currency("USD")
                .notes("CR91 IFSC Compliance - testing milestone billing")
                .createdByUser(pm)
                .build());

        InvoiceLineItem line3 = InvoiceLineItem.builder()
                .invoice(invoice2)
                .workItemId(task3.getId())
                .workItemNumber(task3.getTicketNumber())
                .workItemTitle(task3.getTitle())
                .consultantId(qa.getId())
                .consultantName(qa.getFullName())
                .billingType("Billable")
                .description(task3.getTitle())
                .hours(24.0)
                .rate(qa.getHourlyCost())
                .amount(Math.round(24.0 * qa.getHourlyCost() * 100.0) / 100.0)
                .build();
        invoiceRepository.save(invoice2);

        // 11. Initialize Risks & Issues
        riskRepository.save(Risk.builder()
                .riskCode("RSK-101")
                .project(proj1)
                .description("Downstream payment gateway latency under peak Diwali traffic load")
                .probability(3)
                .impact(4)
                .riskScore(12)
                .owner(lead)
                .mitigationPlan("Add Redis caching layer and circuit breaker fallback to secondary clearing route")
                .targetDate(LocalDate.now().plusDays(20))
                .status(RiskStatus.IDENTIFIED)
                .build());

        riskRepository.save(Risk.builder()
                .riskCode("RSK-102")
                .project(proj1)
                .description("Central Bank IFSC master file delay from clearing house")
                .probability(4)
                .impact(4)
                .riskScore(16) // Critical Risk!
                .owner(pm)
                .mitigationPlan("Maintain fallback to previous month's IFSC table with manual override flags")
                .targetDate(LocalDate.now().plusDays(5))
                .status(RiskStatus.IDENTIFIED)
                .build());

        issueRepository.save(Issue.builder()
                .issueCode("ISS-101")
                .project(proj1)
                .description("Test MQ broker SSL certificate expired on SIT environment")
                .severity(IssueSeverity.HIGH)
                .status(IssueStatus.OPEN)
                .owner(dev2)
                .resolution("Security team renewing certificates today")
                .build());

        // 13. Initialize Support Tickets
        supportTicketRepository.save(SupportTicket.builder()
                .ticketCode("SUP-1001")
                .subject("Request for Super Admin Privileges — SBI CR Phase 2 Production Access")
                .category(SupportCategory.ACCESS_REQUEST)
                .priority(TicketPriority.HIGH)
                .targetRole("ROLE_SUPER_ADMIN")
                .status(TicketStatus.OPEN)
                .createdById(admin.getId())
                .createdByName(admin.getFirstName() + " " + admin.getLastName())
                .createdByEmail(admin.getEmail())
                .description("Need elevated Super Admin permissions to execute database telemetry resets and approve UAT exit criteria for SBI CR Project AP-586.")
                .systemDiagnostics("Tenant: aurionpro | Project: SBI CR (AP-586) | OS: mac | Role: Project Owner")
                .build());

        supportTicketRepository.save(SupportTicket.builder()
                .ticketCode("SUP-1002")
                .subject("Jira Sync Discrepancy — SBI Task AP-586-121 status mismatch")
                .category(SupportCategory.SYSTEM_DEFECT)
                .priority(TicketPriority.URGENT)
                .targetRole("ROLE_ADMIN")
                .status(TicketStatus.IN_REVIEW)
                .createdById(pm.getId())
                .createdByName(pm.getFirstName() + " " + pm.getLastName())
                .createdByEmail(pm.getEmail())
                .assignedToId(admin.getId())
                .assignedToName(admin.getFirstName() + " " + admin.getLastName())
                .description("The Jira synchronization webhook intermittently drops status updates for AP-586-121 when transitioning to UAT Exit.")
                .resolutionNotes("Under investigation by Admin infrastructure team. Webhook listener restarted.")
                .systemDiagnostics("Webhook Service: v2.4.1 | Host: telemetry.aurionpro.internal")
                .build());

        supportTicketRepository.save(SupportTicket.builder()
                .ticketCode("SUP-1003")
                .subject("Capacity Planning Hours Over-Allocation Warning for BA Roster")
                .category(SupportCategory.DATA_QUERY)
                .priority(TicketPriority.MEDIUM)
                .targetRole("ROLE_ADMIN")
                .status(TicketStatus.RESOLVED)
                .createdById(ba.getId())
                .createdByName(ba.getFirstName() + " " + ba.getLastName())
                .createdByEmail(ba.getEmail())
                .assignedToId(admin.getId())
                .assignedToName(admin.getFirstName() + " " + admin.getLastName())
                .description("Monthly capacity calculations for Business Analysts show 118% utilization. Requesting verification of baseline max monthly capacity.")
                .resolutionNotes("Verified baseline monthly capacity is set to 160 hours per BA. Over-allocation was caused by concurrent SBI & HDFC change request tasks.")
                .build());

        log.info("TicketBoard Data Initialization completed successfully! All sample users, projects, requirements, tasks, timesheets, releases, risks, and support tickets are seeded.");
    }
}

