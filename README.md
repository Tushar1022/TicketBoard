# 🚀 TicketBoard / DeliveryHub Platform

A modern, enterprise-grade **Project Delivery & Work Management Platform** inspired by Jira, Zoho, and ServiceNow, designed to govern:
> **Track projects → requirements → tasks → effort → timestamps → deliveries → delays → dependencies → team capacity → projections → reports.**

---

## 🏗️ Architecture & Technology Stack

| Layer | Technology | Key Capabilities |
|---|---|---|
| **Backend** | Spring Boot 3.4 / 4.1 + Java 17 | REST API, Spring Security, JWT stateless Bearer tokens, JPA / Hibernate ORM |
| **Database** | MySQL (with Oracle-ready schema) | Relational schema with auto-auditing, scope history, activity timelines |
| **Frontend** | Angular 17+ (Standalone, Signals, Material) | Modern reactive UI, custom design system, Kanban boards, Gantt/Forecasting |
| **Tooling** | Maven Wrapper, NPM, Postman Collection | Comprehensive v2.1.0 collection with auto token scripting |

---

## ⚡ Quick Start Instructions

### 1. Database Setup
Ensure MySQL is running on `localhost:3306`:
```sql
CREATE DATABASE IF NOT EXISTS ticket_board;
```
The database connection is configured in `ticketboard-backend/src/main/resources/application.properties`:
- URL: `jdbc:mysql://localhost:3306/ticket_board`
- User: `root`
- Password: `admin#9090`

### 2. Run Backend (Spring Boot)
Open a terminal in `ticketboard-backend`:
```powershell
cd D:\TUSHAR_OWN_PROJECTS\TicketBoard\ticketboard-backend
.\mvnw.cmd spring-boot:run
```
- Server starts on **`http://localhost:8080`**.
- Automatic `DataInitializer` seeds complete roles, users, clients, project `CR91`, requirements with scope tracking, work items, blockers, weekly timesheets, releases, and risks on first startup.

### 3. Run Frontend (Angular)
Open a terminal in `ticketboard-frontend`:
```powershell
cd D:\TUSHAR_OWN_PROJECTS\TicketBoard\ticketboard-frontend
npm start
```
- Open browser at **`http://localhost:4200`**.

---

## 🔑 Pre-Configured Test Personas

All accounts use the default password: **`Admin@123`**  
*(The login page and top navigation header also feature a 1-click persona switcher for instant testing)*

| Role | Email | Name | Designation |
|---|---|---|---|
| **Super Admin** | `admin@ticketboard.com` | Tushar Shinde | Solution Architect & Admin |
| **Project Manager** | `pm@ticketboard.com` | Amit Sharma | Senior Project Manager |
| **Lead Analyst** | `ba@ticketboard.com` | Pooja Verma | Lead Business Analyst |
| **Team Lead** | `lead@ticketboard.com` | Suresh Patil | Technical Lead |
| **Senior Developer** | `dev1@ticketboard.com` | Rahul Nair | Senior Java Developer |
| **Backend Developer** | `dev2@ticketboard.com` | Sneha Rao | Backend Developer |
| **QA Automation** | `qa@ticketboard.com` | Vikram Joshi | QA Automation Engineer |
| **VP Management** | `mgmt@ticketboard.com` | Rajesh Mehta | VP of Engineering |

---

## 📦 Key Frontend Modules & Features

1. **Executive Command Center (`/dashboard/executive`)**:
   - Answers the **5 Core Executive Questions**:
     1. *What is happening?* (Active projects, requirements, work items)
     2. *What is delayed & at risk?* (Delayed count, critical risks, overdue items)
     3. *What is coming?* (Deliveries in next 7 and 30 days)
     4. *Who is overloaded?* (Resource utilization $>100\%$)
     5. *Are we on schedule?* (On-time delivery %, effort variance)
   - Dynamic project delivery date forecast table.
   - Top effort variance analysis.

2. **My Workspace (`/my-workspace`)**:
   - Personalized developer cockpit with assigned sprint tasks, open bugs, and blocked tasks.
   - Live 40-hour weekly capacity progress bar and recent time entries.

3. **Projects & Milestones (`/projects`, `/projects/:id`)**:
   - Portfolio cards with health badges (`GREEN`, `AMBER`, `RED`), completion % progress bars, and budget metrics.
   - Deep 8-tab project workspace (Overview, Requirements, Kanban Board, Team Members, Deliveries, Risks & Issues, Audit Timeline, Discussions).

4. **Requirements & Scope Creep Engine (`/requirements`)**:
   - Full 16-state lifecycle tracking (`DRAFT` $\rightarrow$ `DELIVERED`).
   - Automated $>20\%$ estimate growth detection with `scopeCreepFlag = true` and scope version audit snapshots.

5. **Work Items & Kanban Board (`/work-items`)**:
   - Interactive Kanban board across 6 status columns (`TODO`, `IN_PROGRESS`, `IN_REVIEW`, `TESTING`, `BLOCKED`, `COMPLETED`).
   - Blocker modal recording owner, reason, and expected resolution date.

6. **Time Tracking & Timesheets (`/timetracking`)**:
   - Daily start/end time logging with automatic break deduction.
   - Monday–Sunday weekly timesheet grid with submission and Manager approval/rejection workflows.
   - Effort variance report ($\text{Actual Hours} - \text{Estimated Hours}$).

7. **Deliveries & Releases (`/releases`)**:
   - Multi-environment release pipeline (`DEV` $\rightarrow$ `SIT` $\rightarrow$ `UAT` $\rightarrow$ `PRE_PROD` $\rightarrow$ `PRODUCTION`).

8. **Risk & Issue Governance (`/risks`)**:
   - Interactive $5 \times 5$ Probability vs Impact heatmap grid.
   - Critical risk alerts ($\text{Risk Score} \ge 15$).

9. **Capacity Planning & Projections (`/capacity`)**:
   - Resource capacity utilization matrix ($>100\%$ overloaded indicator).
   - Mathematical delivery forecast: $\lceil \text{Remaining Effort} / \text{Team Daily Capacity} \rceil$.
   - 30 / 60 / 90-day demand vs capacity shortage horizon.

---

## 📮 Postman Collection
Import [`ticketboard-backend/TicketBoard_Postman_Collection.json`](file:///D:/TUSHAR_OWN_PROJECTS/TicketBoard/ticketboard-backend/TicketBoard_Postman_Collection.json) and [`TicketBoard_Postman_Environment.json`](file:///D:/TUSHAR_OWN_PROJECTS/TicketBoard/ticketboard-backend/TicketBoard_Postman_Environment.json) into Postman for full REST API testing.
