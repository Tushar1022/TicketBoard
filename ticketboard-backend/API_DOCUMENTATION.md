# TicketBoard / DeliveryHub – Comprehensive REST API Documentation

## 1. Overview & Setup

- **Base URL**: `http://localhost:8080`
- **Authentication**: JWT Bearer Token (`Authorization: Bearer <token>`)
- **Content-Type**: `application/json`
- **Postman Collection File**: [`TicketBoard_Postman_Collection.json`](file:///D:/TUSHAR_OWN_PROJECTS/TicketBoard/ticketboard-backend/TicketBoard_Postman_Collection.json)
- **Postman Environment File**: [`TicketBoard_Postman_Environment.json`](file:///D:/TUSHAR_OWN_PROJECTS/TicketBoard/ticketboard-backend/TicketBoard_Postman_Environment.json)

---

## 2. Seed User Accounts

All seed user accounts have the default password: **`Admin@123`**

| Role | Email | Name | Capabilities |
|---|---|---|---|
| **Super Admin** | `admin@ticketboard.com` | Tushar Shinde | Full system access, client & project governance, all modules |
| **Project Manager** | `pm@ticketboard.com` | Amit Sharma | Project creation, milestones, requirements, timesheet reviews |
| **Business Analyst** | `ba@ticketboard.com` | Pooja Verma | BRD/FRD requirements, scope versioning, acceptance criteria |
| **Team Lead** | `lead@ticketboard.com` | Suresh Patil | Task assignment, timesheet approval, blocker triage |
| **Senior Developer** | `dev1@ticketboard.com` | Rahul Nair | Task execution, daily time logging, timesheet submission |
| **Backend Developer** | `dev2@ticketboard.com` | Sneha Rao | Task execution, daily time logging, timesheet submission |
| **QA Automation** | `qa@ticketboard.com` | Vikram Joshi | QA dashboard, test execution, defect logging |
| **Executive Mgmt** | `mgmt@ticketboard.com` | Rajesh Mehta | Executive dashboard, KPIs, delivery forecasts |

---

## 3. Standard Response Envelope

All API endpoints return JSON wrapped in the following format:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-08-14T03:09:33.546"
}
```

---

## 4. API Endpoints Reference

### 🔐 4.1 Authentication & Security (`/api/v1/auth`)

#### `POST /api/v1/auth/login`
- **Public**: Yes
- **Description**: Authenticate user credentials and retrieve a JWT token.
- **Request Body**:
```json
{
  "email": "admin@ticketboard.com",
  "password": "Admin@123"
}
```
- **Response**:
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "employeeId": "EMP-1001",
      "firstName": "Tushar",
      "lastName": "Shinde",
      "email": "admin@ticketboard.com",
      "roles": ["ROLE_SUPER_ADMIN", "ROLE_PROJECT_MANAGER"]
    }
  }
}
```

#### `GET /api/v1/auth/me`
- **Auth**: Required
- **Description**: Fetch current authenticated user profile.

#### `POST /api/v1/auth/register`
- **Auth**: `ROLE_SUPER_ADMIN`
- **Request Body**:
```json
{
  "employeeId": "EMP-2001",
  "firstName": "Arjun",
  "lastName": "Reddy",
  "email": "arjun@ticketboard.com",
  "password": "Admin@123",
  "phone": "+91-9988776655",
  "designation": "Senior Fullstack Developer",
  "departmentId": 1,
  "teamId": 1,
  "dailyCapacityHours": 8.0,
  "hourlyCost": 65.0,
  "skills": "Java, Spring Boot, Angular, MySQL",
  "roleNames": ["ROLE_DEVELOPER"]
}
```

#### `POST /api/v1/auth/change-password`
- **Auth**: Required
- **Request Body**:
```json
{
  "oldPassword": "Admin@123",
  "newPassword": "NewSecurePassword@999"
}
```

---

### 📊 4.2 Dashboards & Executive KPIs (`/api/v1/dashboards`)

#### `GET /api/v1/dashboards/executive`
- **Auth**: Required
- **Description**: Answers the 5 Core Leadership Questions in real time:
  1. *What is happening?* (Active projects, requirements, tasks)
  2. *What is delayed?* (Delayed project count, critical risks, overdue tasks)
  3. *What is coming?* (Upcoming deliveries in next 7 & 30 days)
  4. *Who is overloaded?* (List of engineers with $>100\%$ capacity allocation)
  5. *Are we on schedule?* (On-time delivery rate %, estimated vs actual hours, effort variance %)

#### `GET /api/v1/dashboards/developer?userId={id}`
- **Auth**: Required
- **Description**: Returns personalized active tasks, open bugs, blocked tasks, hours logged this week vs weekly capacity (40h), and upcoming deadlines.

#### `GET /api/v1/dashboards/qa`
- **Auth**: Required
- **Description**: Returns QA testing queue, open defects, blocker bugs, and resolved bugs awaiting re-test.

---

### 📁 4.3 Project Management & Milestones (`/api/v1/projects`, `/api/v1/milestones`)

#### `GET /api/v1/projects`
- **Auth**: Required
- **Query Params**: `status`, `managerId`, `userId`
- **Description**: Returns all projects with calculated completion %, health (`GREEN`/`AMBER`/`RED`), member allocations, and milestones.

#### `POST /api/v1/projects`
- **Auth**: `ROLE_SUPER_ADMIN`, `ROLE_PROJECT_MANAGER`
- **Request Body**:
```json
{
  "projectCode": "NEFT-2026",
  "name": "National NEFT Clearing Gateway 2.0",
  "description": "Real-time 24x7 batch clearing pipeline with automated reconciliation engine.",
  "clientId": 1,
  "projectManagerId": 2,
  "startDate": "2026-08-15",
  "plannedEndDate": "2026-11-30",
  "priority": "HIGH",
  "budget": 150000.0,
  "estimatedHours": 900.0
}
```

#### `POST /api/v1/projects/{projectId}/members`
- **Auth**: `ROLE_SUPER_ADMIN`, `ROLE_PROJECT_MANAGER`
- **Request Body**:
```json
{
  "userId": 5,
  "projectRole": "Senior Java Developer",
  "allocatedHoursPerDay": 8.0
}
```

#### `GET /api/v1/milestones/project/{projectId}`
- **Auth**: Required
- **Description**: Retrieves all project milestones with achievement status and completion percentage.

---

### 📝 4.4 Requirement Management & Scope Creep (`/api/v1/requirements`)

#### `GET /api/v1/requirements`
- **Auth**: Required
- **Query Params**: `projectId`, `status`, `ownerId`

#### `POST /api/v1/requirements`
- **Auth**: `ROLE_SUPER_ADMIN`, `ROLE_PROJECT_MANAGER`, `ROLE_BUSINESS_ANALYST`
- **Request Body**:
```json
{
  "reqNumber": "REQ-2026-005",
  "title": "Multi-Currency Settlement Gateway",
  "description": "Enable FX rate lookups and settlement in USD, EUR, and GBP.",
  "businessObjective": "Allow cross-border corporate payments without intermediaries.",
  "acceptanceCriteria": "1. Fetch live rates from Treasury API\n2. Calculate spread\n3. Lock rate for 120 seconds",
  "priority": "HIGH",
  "requester": "International Treasury Desk",
  "projectId": 1,
  "ownerId": 3,
  "estimatedEffortHours": 50.0,
  "plannedStartDate": "2026-08-18",
  "plannedEndDate": "2026-08-28",
  "deliveryVersion": "REL-2026-015"
}
```

#### `PUT /api/v1/requirements/{id}`
- **Auth**: Required
- **Description**: Updating `estimatedEffortHours` $>20\%$ automatically flags `scopeCreepFlag = true`, increments `scopeVersion`, and logs a version snapshot in `RequirementHistory`.
- **Request Body**:
```json
{
  "title": "Multi-Currency Settlement Gateway with Real-Time Rates",
  "description": "Expanded to include live Redis caching and auto-hedge execution.",
  "estimatedEffortHours": 75.0,
  "reasonForChange": "Client requested real-time auto-hedge integration"
}
```

#### `PATCH /api/v1/requirements/{id}/status`
- **Auth**: Required
- **Request Body**:
```json
{
  "status": "APPROVED",
  "comment": "All acceptance criteria verified and approved by bank stakeholder"
}
```

#### `GET /api/v1/requirements/{id}/history`
- **Auth**: Required
- **Description**: Retrieves full version history audit log for requirement changes and scope creep events.

---

### ⚙️ 4.5 Work Items, Tasks, Bugs & Blockers (`/api/v1/work-items`)

#### `GET /api/v1/work-items`
- **Auth**: Required
- **Query Params**: `projectId`, `requirementId`, `assigneeId`, `status`, `type`

#### `POST /api/v1/work-items`
- **Auth**: Required
- **Request Body**:
```json
{
  "ticketNumber": "CR91-104",
  "title": "Implement Redis Cache for IFSC Lookup",
  "description": "Store IFSC records with 24-hour TTL in clustered Redis cache.",
  "type": "TASK",
  "priority": "HIGH",
  "severity": "MEDIUM",
  "projectId": 1,
  "requirementId": 1,
  "assigneeId": 5,
  "reporterId": 4,
  "estimatedHours": 12.0,
  "startDate": "2026-08-15",
  "dueDate": "2026-08-18",
  "labels": "redis,caching,performance"
}
```

#### `POST /api/v1/work-items/{id}/block`
- **Auth**: Required
- **Request Body**:
```json
{
  "reason": "Database server disk full on SIT environment (0 MB remaining)",
  "owner": "Database Administration Team",
  "expectedResolutionDate": "2026-08-15T18:00:00"
}
```

#### `POST /api/v1/work-items/{id}/unblock`
- **Auth**: Required
- **Request Body**:
```json
{
  "reason": "DBA allocated 100GB additional storage. Partition purged."
}
```

#### `POST /api/v1/work-items/{id}/dependencies`
- **Auth**: Required
- **Request Body**:
```json
{
  "targetWorkItemId": 3,
  "dependencyType": "BLOCKS"
}
```

---

### ⏱️ 4.6 Time Tracking & Timesheet Governance (`/api/v1/time-entries`, `/api/v1/timesheets`)

#### `GET /api/v1/time-entries/my`
- **Auth**: Required
- **Description**: Returns all time entries logged by the authenticated user.

#### `POST /api/v1/time-entries`
- **Auth**: Required
- **Description**: Logs daily effort. Total hours are automatically calculated: $(\text{End Time} - \text{Start Time} - \text{Break Minutes})$.
- **Request Body**:
```json
{
  "projectId": 1,
  "requirementId": 1,
  "workItemId": 1,
  "workDate": "2026-08-14",
  "startTime": "09:30",
  "endTime": "18:00",
  "breakMinutes": 60,
  "description": "Implemented Spring Data repository and added indexing tests"
}
```

#### `GET /api/v1/time-entries/variance`
- **Auth**: Required
- **Description**: Returns effort variance analysis: $(\text{Actual Hours} - \text{Estimated Hours})$ across tasks, requirements, and projects.

#### `GET /api/v1/timesheets/week`
- **Auth**: Required
- **Query Params**: `userId` (optional), `dateInWeek` (optional, format: `YYYY-MM-DD`)
- **Description**: Returns or auto-generates weekly timesheet (Monday–Sunday) with grouped daily entries.

#### `POST /api/v1/timesheets/{id}/submit`
- **Auth**: Required
- **Description**: Submits weekly timesheet for Manager / Team Lead review.

#### `POST /api/v1/timesheets/{id}/review`
- **Auth**: `ROLE_SUPER_ADMIN`, `ROLE_PROJECT_MANAGER`, `ROLE_TEAM_LEAD`
- **Request Body**:
```json
{
  "status": "APPROVED",
  "rejectionReason": null
}
```

---

### 🚀 4.7 Releases & Deliveries (`/api/v1/releases`)

#### `GET /api/v1/releases`
- **Auth**: Required
- **Query Params**: `projectId`

#### `POST /api/v1/releases`
- **Auth**: `ROLE_SUPER_ADMIN`, `ROLE_PROJECT_MANAGER`
- **Request Body**:
```json
{
  "releaseVersion": "REL-2026-016",
  "title": "CR91 Production Hotfix 1.4.1",
  "description": "Hotfix patch for Redis caching connection pool timeouts.",
  "projectId": 1,
  "environment": "PRODUCTION",
  "plannedDate": "2026-08-25",
  "status": "PLANNED",
  "ownerId": 2,
  "requirementIds": [1],
  "workItemIds": [1, 2]
}
```

#### `PUT /api/v1/releases/{id}`
- **Auth**: `ROLE_SUPER_ADMIN`, `ROLE_PROJECT_MANAGER`
- **Request Body**:
```json
{
  "title": "CR91 IFSC Compliance Production Release",
  "projectId": 1,
  "environment": "PRODUCTION",
  "plannedDate": "2026-08-24",
  "actualDate": "2026-08-24",
  "status": "DEPLOYED",
  "deploymentResult": "Success - Zero downtime deployment completed",
  "rollbackRequired": false
}
```

---

### ⚠️ 4.8 Risk & Issue Governance (`/api/v1/risks`)

#### `GET /api/v1/risks`
- **Auth**: Required
- **Query Params**: `projectId`

#### `POST /api/v1/risks`
- **Auth**: Required
- **Description**: Creates a risk. Automated calculation: $\text{Probability } (1-5) \times \text{Impact } (1-5) = \text{Risk Score } (1-25)$.
- **Request Body**:
```json
{
  "riskCode": "RSK-103",
  "projectId": 1,
  "description": "Third-party IFSC database API downtime during national clearing cycle",
  "probability": 4,
  "impact": 4,
  "ownerId": 4,
  "mitigationPlan": "Local failover database replica with hourly delta synchronization",
  "targetDate": "2026-08-20",
  "status": "IDENTIFIED"
}
```

#### `GET /api/v1/risks/issues/project/{projectId}`
- **Auth**: Required
- **Description**: Returns all active blockers and operational issues on the project.

#### `POST /api/v1/risks/issues`
- **Auth**: Required
- **Request Body**:
```json
{
  "projectId": 1,
  "description": "MQ broker port 1414 firewall blocking test packets from SIT subnet",
  "severity": "HIGH",
  "status": "OPEN",
  "ownerId": 6,
  "resolution": "Raised emergency firewall change request CR-8832"
}
```

---

### 📈 4.9 Capacity Planning, Workload & Forecasting (`/api/v1/capacity`)

#### `GET /api/v1/capacity/workload`
- **Auth**: Required
- **Description**: Returns resource capacity utilization percentage for every active employee:
  $$\text{Utilization \%} = \frac{\sum \text{Allocated Task Hours}}{\text{Monthly Capacity Hours (160h)}} \times 100$$
  Marks `isOverloaded = true` when utilization $> 100\%$.

#### `GET /api/v1/capacity/teams`
- **Auth**: Required
- **Description**: Aggregates team monthly capacity vs demand effort, calculating shortage/gap.

#### `GET /api/v1/capacity/projections`
- **Auth**: Required
- **Description**: Calculates dynamically:
  $$\text{Days Remaining} = \left\lceil \frac{\text{Remaining Effort}}{\text{Team Daily Capacity}} \right\rceil$$
  $$\text{Projected Delivery Date} = \text{Today} + \text{Days Remaining}$$
  Compares projected date with planned delivery date to compute `delayDays`.

#### `GET /api/v1/capacity/demand-forecast`
- **Auth**: Required
- **Description**: Returns demand effort vs team capacity availability for upcoming **30, 60, and 90-day** periods.

---

### 💬 4.10 Collaboration & Audit Timeline (`/api/v1/comments`, `/api/v1/audit-logs`)

#### `GET /api/v1/comments?entityType=PROJECT&entityId=1`
- **Auth**: Required

#### `POST /api/v1/comments`
- **Auth**: Required
- **Request Body**:
```json
{
  "entityType": "PROJECT",
  "entityId": 1,
  "content": "Reviewed sprint velocity. Development is 72% complete and on schedule for UAT signoff."
}
```

#### `GET /api/v1/audit-logs?entityType=PROJECT&entityId=1`
- **Auth**: Required
- **Description**: Returns chronological audit trail of all lifecycle changes, status transitions, approvals, blockers, and comments.
