export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export type RoleType =
  | 'ROLE_SUPER_ADMIN'
  | 'ROLE_ADMIN'
  | 'ROLE_PROJECT_OWNER'
  | 'ROLE_PROJECT_MANAGER'
  | 'ROLE_BUSINESS_ANALYST'
  | 'ROLE_DEVELOPER'
  | 'ROLE_QA_TESTER'
  | 'ROLE_TEAM_LEAD'
  | 'ROLE_MANAGEMENT';

export interface User {
  id: number;
  employeeId: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  designation?: string;
  departmentId?: number;
  departmentName?: string;
  teamId?: number;
  teamName?: string;
  reportingManagerId?: number;
  reportingManagerName?: string;
  roles: RoleType[];
  permissions: string[];
  dailyCapacityHours: number;
  hourlyCost?: number;
  skills?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED';
  avatarUrl?: string;
  joiningDate?: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Department {
  id: number;
  name: string;
  code: string;
  description?: string;
}

export interface Team {
  id: number;
  name: string;
  code: string;
  description?: string;
  departmentId?: number;
  departmentName?: string;
  teamLeadId?: number;
  teamLeadName?: string;
  memberCount?: number;
}

export interface Client {
  id: number;
  clientCode: string;
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  status: string;
  accountManagerId?: number;
  accountManagerName?: string;
}

export type ProjectPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ProjectStatus = 'PROPOSED' | 'APPROVED' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED' | 'CLOSED';
export type ProjectHealth = 'GREEN' | 'AMBER' | 'RED';
export type MilestoneStatus = 'PLANNED' | 'IN_PROGRESS' | 'ACHIEVED' | 'MISSED' | 'CANCELLED';

export interface ProjectMember {
  id: number;
  projectId: number;
  userId: number;
  userName: string;
  userEmail: string;
  projectRole: string;
  allocatedHoursPerDay: number;
  allocationStartDate?: string;
  allocationEndDate?: string;
}

export interface Milestone {
  id: number;
  milestoneId?: string;
  projectId: number;
  name: string;
  description?: string;
  plannedDate: string;
  actualDate?: string;
  startDate?: string;
  dueDate?: string;
  ownerId?: number;
  ownerName?: string;
  status: MilestoneStatus;
  completionPercentage: number;
  flag?: 'Release Milestone' | 'Affected Milestone';
  linkedTaskCount?: number;
  linkedIssueCount?: number;
  linkedTaskIds?: string[];
  linkedIssueIds?: string[];
}

export interface Project {
  id: number;
  projectCode: string;
  name: string;
  projectName?: string;
  description?: string;
  clientId: number;
  clientName?: string;
  projectManagerId?: number;
  projectManagerName?: string;
  startDate?: string;
  plannedEndDate?: string;
  actualEndDate?: string;
  priority: ProjectPriority;
  status: ProjectStatus;
  health: ProjectHealth;
  budget?: number;
  estimatedHours?: number;
  actualHours?: number;
  completionPercentage: number;
  requirementCount?: number;
  taskCount?: number;
  openBugCount?: number;
  members: ProjectMember[];
  milestones: Milestone[];
  createdAt?: string;
  updatedAt?: string;
}

export type RequirementPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type RequirementStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'NEEDS_CLARIFICATION'
  | 'APPROVED'
  | 'REJECTED'
  | 'ESTIMATED'
  | 'PLANNED_FOR_SPRINT'
  | 'READY_FOR_DEV'
  | 'IN_DEVELOPMENT'
  | 'DEV_COMPLETE'
  | 'IN_QA'
  | 'UAT_READY'
  | 'UAT_APPROVED'
  | 'READY_FOR_RELEASE'
  | 'DELIVERED'
  | 'CLOSED'
  | 'CANCELLED';

export interface RequirementHistory {
  id: number;
  requirementId: number;
  versionNumber: number;
  title: string;
  description: string;
  estimatedEffortHours: number;
  reasonForChange?: string;
  changedById?: number;
  changedByName?: string;
  isScopeCreep: boolean;
  varianceHours?: number;
  createdAt: string;
}

export interface Requirement {
  id: number;
  reqNumber: string;
  title: string;
  description: string;
  businessObjective?: string;
  acceptanceCriteria?: string;
  priority: RequirementPriority;
  requester?: string;
  projectId: number;
  projectCode?: string;
  projectName?: string;
  ownerId?: number;
  ownerName?: string;
  estimatedEffortHours?: number;
  originalEstimateHours?: number;
  actualEffortHours?: number;
  plannedStartDate?: string;
  plannedEndDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  status: RequirementStatus;
  deliveryVersion?: string;
  scopeVersion: number;
  scopeCreepFlag: boolean;
  taskCount?: number;
  completedTaskCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export type WorkItemType = 'TASK' | 'BUG' | 'USER_STORY' | 'SUBTASK' | 'FEATURE' | 'ENHANCEMENT' | 'CHANGE_REQUEST';
export type WorkItemPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type WorkItemSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | 'MAJOR' | 'MINOR' | 'BLOCKER';
export type WorkItemStatus =
  | 'TODO'
  | 'IN_PROGRESS'
  | 'IN_ANALYSIS'
  | 'DEV_IN_PROGRESS'
  | 'DEV_EXIT'
  | 'IN_REVIEW'
  | 'TESTING'
  | 'SIT_IN_PROGRESS'
  | 'SIT_EXIT'
  | 'UAT_IN_PROGRESS'
  | 'UAT_EXIT'
  | 'PRE_PROD'
  | 'GO_LIVE'
  | 'BLOCKED'
  | 'COMPLETED'
  | 'CLOSED'
  | 'Go-Live to be planned'
  | 'BRD'
  | 'Dev in progress'
  | 'SD Signoff Awaited'
  | 'SIT in progress'
  | 'On Hold by Bank'
  | 'Clarification required from Bank'
  | 'Duplicate'
  | 'SD in progress'
  | string;

export type DependencyType = 'BLOCKS' | 'BLOCKED_BY' | 'DEPENDS_ON' | 'RELATED_TO' | 'DUPLICATES';

export interface BlockerDto {
  id?: number;
  reason: string;
  owner: string;
  blockedSince?: string;
  expectedResolutionDate?: string;
  resolvedAt?: string;
  resolutionNotes?: string;
}

export interface DependencyDto {
  id?: number;
  sourceItemId?: number;
  sourceTicketNumber?: string;
  sourceTitle?: string;
  targetItemId?: number;
  targetTicketNumber?: string;
  targetTitle?: string;
  dependencyType: DependencyType;
}

export interface WorkItem {
  id: number;
  ticketNumber: string;
  title: string;
  description?: string;
  type: WorkItemType;
  priority: WorkItemPriority;
  severity: WorkItemSeverity;
  status: WorkItemStatus;
  projectId: number;
  projectCode?: string;
  projectName?: string;
  requirementId?: number;
  reqNumber?: string;
  reqTitle?: string;
  assigneeId?: number;
  assigneeName?: string;
  reporterId?: number;
  reporterName?: string;
  parentTaskId?: number;
  parentTaskTitle?: string;
  estimatedHours: number;
  actualHours: number;
  differenceHours?: number;
  startDate?: string;
  dueDate?: string;
  completedDate?: string;

  // Enterprise Delivery Milestones
  allocatedBa?: string;
  devExitDate?: string;
  sitExitDate?: string;
  uatExitDate?: string;
  sdDeliveryDate?: string;
  goLiveDate?: string;
  devEffortDays?: number;
  qcEffortDays?: number;
  cursorEffortsDays?: number;
  durationDays?: number;
  completionPercentage: number;
  billingType?: string;
  associatedTeam?: string;
  jiraTaskId?: string;
  jiraStatus?: string;
  jiraSyncEnabled?: boolean;
  jiraCreationLog?: string;
  tags?: string;
  reminder?: string;
  recurrence?: string;

  blockedSince?: string;
  blockedReason?: string;
  blockedOwner?: string;
  labels?: string;
  commentsCount?: number;
  documentsCount?: number;
  subtasks?: WorkItem[];
  dependencies?: DependencyDto[];
  documents?: TaskDocument[];
  createdAt?: string;
  updatedAt?: string;
}

export type TimeEntryStatus = 'LOGGED' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
export type TimesheetStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface TimeEntry {
  id: number;
  userId: number;
  userName: string;
  projectId: number;
  projectCode: string;
  projectName: string;
  requirementId?: number;
  requirementNumber?: string;
  workItemId?: number;
  workItemTicketNumber?: string;
  workItemTitle?: string;
  workDate: string;
  startTime?: string;
  endTime?: string;
  breakMinutes?: number;
  totalHours: number;
  workHoursPlanned?: string;
  timelogTotal?: string;
  differenceHoursFormatted?: string;
  billingType?: string;
  completionDate?: string;
  description: string;
  status: TimeEntryStatus;
  timesheetId?: number;
  createdAt?: string;
}

export interface Timesheet {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  startDate: string;
  endDate: string;
  totalHours: number;
  status: TimesheetStatus;
  reviewerId?: number;
  reviewerName?: string;
  rejectionReason?: string;
  submittedAt?: string;
  reviewedAt?: string;
  entries: TimeEntry[];
}

export interface EffortVariance {
  entityType: 'TASK' | 'REQUIREMENT' | 'PROJECT';
  entityId: number;
  code: string;
  title: string;
  projectName: string;
  estimatedHours: number;
  actualHours: number;
  varianceHours: number;
  variancePercentage: number;
  isOverBudget: boolean;
}

export type ReleaseEnvironment = 'DEV' | 'SIT' | 'UAT' | 'PRE_PROD' | 'PRODUCTION';
export type ReleaseStatus = 'DRAFT' | 'PLANNED' | 'READY' | 'APPROVED' | 'DEPLOYING' | 'DEPLOYED' | 'VERIFIED' | 'CLOSED';

export interface Release {
  id: number;
  releaseVersion: string;
  title: string;
  description?: string;
  projectId: number;
  projectCode?: string;
  projectName?: string;
  environment: ReleaseEnvironment;
  plannedDate?: string;
  actualDate?: string;
  status: ReleaseStatus;
  deploymentResult?: string;
  rollbackRequired?: boolean;
  ownerId?: number;
  ownerName?: string;
  requirementIds?: number[];
  workItemIds?: number[];
  itemCount?: number;
  createdAt?: string;
}

export type RiskStatus = 'IDENTIFIED' | 'MITIGATED' | 'ACCEPTED' | 'CLOSED';
export type IssueSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IssueStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface Risk {
  id: number;
  riskCode: string;
  projectId: number;
  projectCode?: string;
  projectName?: string;
  description: string;
  probability: number;
  impact: number;
  riskScore: number;
  ownerId?: number;
  ownerName?: string;
  mitigationPlan?: string;
  targetDate?: string;
  status: RiskStatus;
  createdAt?: string;
}

export interface Issue {
  id: number;
  issueCode: string;
  projectId: number;
  projectCode?: string;
  projectName?: string;
  productName?: string;
  issueType?: string;
  releaseName?: string;
  releaseMilestone?: string;
  affectedMilestone?: string;
  moduleName?: string;
  phase?: string;
  crValue?: number;
  crManDays?: number;
  title?: string;
  description: string;
  stepsToReproduce?: string;
  actualResult?: string;
  expectedResult?: string;
  severity: IssueSeverity | string;
  status: IssueStatus | string;
  associatedTeam?: string;
  assigneeName?: string;
  classification?: string;
  reproducible?: string;
  dueDate?: string;
  flag?: string;
  linkedTaskIds?: string[];
  reporterId?: number;
  reporterName?: string;
  ownerId?: number;
  ownerName?: string;
  resolution?: string;
  tags?: string;
  createdAt?: string;
}

export interface EmployeeWorkload {
  userId: number;
  employeeId: string;
  employeeName: string;
  designation?: string;
  teamName?: string;
  monthlyCapacityHours: number;
  allocatedHours: number;
  actualHoursLogged: number;
  remainingCapacity: number;
  utilizationPercentage: number;
  isOverloaded: boolean;
  activeTaskCount: number;
  openBugCount: number;
}

export interface TeamCapacity {
  teamId: number;
  teamName: string;
  memberCount: number;
  totalMonthlyCapacityHours: number;
  totalAllocatedDemandHours: number;
  capacityGapHours: number;
  teamUtilizationPercentage: number;
  isShortage: boolean;
  members: EmployeeWorkload[];
}

export interface ProjectForecast {
  projectId: number;
  projectCode: string;
  projectName: string;
  totalEstimatedHours: number;
  actualHoursLogged: number;
  remainingEffortHours: number;
  teamDailyCapacity: number;
  projectedDaysRemaining: number;
  plannedDeliveryDate?: string;
  projectedDeliveryDate: string;
  delayDays: number;
  health: ProjectHealth;
  isDelayed: boolean;
}

export interface RequirementForecast {
  periodName: string;
  days: number;
  upcomingRequirementCount: number;
  estimatedDemandHours: number;
  availableTeamCapacityHours: number;
  capacityGapHours: number;
  utilizationPercentage: number;
  isShortage: boolean;
}

export interface ExecutiveDashboard {
  totalProjects: number;
  activeProjects: number;
  completedProjects: number;
  activeRequirements: number;
  totalWorkItems: number;
  delayedProjectsCount: number;
  atRiskProjectsCount: number;
  overdueWorkItemsCount: number;
  blockedWorkItemsCount: number;
  criticalRisksCount: number;
  upcomingDeliveries: Release[];
  upcomingDeliveriesNext7DaysCount: number;
  upcomingDeliveriesNext30DaysCount: number;
  overloadedEmployees: EmployeeWorkload[];
  onTimeDeliveryRate: number;
  totalEstimatedHours: number;
  totalActualHours: number;
  totalEffortVarianceHours: number;
  totalEffortVariancePercentage: number;
  averageTeamUtilization: number;
  projectForecasts: ProjectForecast[];
  topVariances: EffortVariance[];
}

export interface DeveloperDashboard {
  userId: number;
  userName: string;
  hoursLoggedThisWeek: number;
  weeklyCapacityHours: number;
  assignedTasksCount: number;
  openBugsCount: number;
  blockedTasksCount: number;
  myActiveTasks: WorkItem[];
  myBugs: WorkItem[];
  myBlockedTasks: WorkItem[];
  upcomingDeadlines: WorkItem[];
  recentTimeEntries: TimeEntry[];
}

export interface QaDashboard {
  testingQueueCount: number;
  openBugsCount: number;
  criticalBugsCount: number;
  blockerBugsCount: number;
  resolvedPendingVerificationCount: number;
  testingQueue: WorkItem[];
  openDefects: WorkItem[];
  criticalDefects: WorkItem[];
}

export interface Comment {
  id: number;
  entityType: string;
  entityId: number;
  authorId?: number;
  authorName?: string;
  authorEmail?: string;
  content: string;
  createdAt: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface ActivityLog {
  id: number;
  entityType: string;
  entityId: number;
  entityCode?: string;
  eventType: string;
  summary: string;
  details?: string;
  oldValue?: string;
  newValue?: string;
  performedById?: number;
  performedByName?: string;
  ipAddress?: string;
  createdAt: string;
}

export type InvoiceStatus = 'DRAFT' | 'SENT' | 'VIEWED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface InvoiceLineItemDto {
  id?: number;
  workItemId?: number;
  workItemNumber?: string;
  workItemTitle?: string;
  consultantId?: number;
  consultantName?: string;
  billingType?: string;
  description?: string;
  hours: number;
  rate: number;
  amount: number;
}

export interface InvoiceDto {
  id: number;
  invoiceNumber: string;
  clientId?: number;
  clientName?: string;
  clientContactPerson?: string;
  clientEmail?: string;
  clientAddress?: string;
  projectId: number;
  projectCode?: string;
  projectName?: string;
  fromDate: string;
  toDate: string;
  issuedDate: string;
  dueDate?: string;
  status: InvoiceStatus;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  total: number;
  currency?: string;
  notes?: string;
  createdByName?: string;
  lineItems: InvoiceLineItemDto[];
  createdAt?: string;
  updatedAt?: string;
}

export interface BillingPreview {
  projectId: number;
  projectCode: string;
  projectName: string;
  clientName: string;
  totalBillableHours: number;
  totalAmount: number;
  taxRate: number;
  taxAmount: number;
  grandTotal: number;
  lineItems: InvoiceLineItemDto[];
}

export interface ProjectBillingSummary {
  projectId: number;
  projectCode: string;
  projectName: string;
  clientName: string;
  totalBillableApprovedHours: number;
  billedHours: number;
  unbilledHours: number;
  avgHourlyRate: number;
  potentialRevenue: number;
}

export interface BillingSummary {
  totalRevenueBilled: number;
  totalRevenuePaid: number;
  totalOutstanding: number;
  invoiceCount: number;
  paidInvoiceCount: number;
  pendingInvoiceCount: number;
  projects: ProjectBillingSummary[];
}

export interface ProjectDocument {
  id: number;
  projectId: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  description?: string;
  fileUrl?: string;
  downloadUrl: string;
  uploadedById?: number;
  uploadedByName?: string;
  uploadedAt?: string;
}

export interface TaskDocument {
  id: number;
  workItemId: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  fileUrl: string;
  downloadUrl?: string;
  uploadedById?: number;
  uploadedByName?: string;
  uploadedAt?: string;
}

// ─── RBAC Models ───────────────────────────────────────────────────────
export interface RoleDto {
  id: number;
  name: RoleType;
  description: string;
  permissions: string[];
  userCount: number;
}

export interface PermissionDto {
  id: number;
  code: string;
  name: string;
  module: string;
  description: string;
}

// ─── Master Data / Lookup Models ───────────────────────────────────────
export interface LookupData {
  id: number;
  category: string;
  value: string;
  label: string;
  displayOrder: number;
  colorCode?: string;
  isActive: boolean;
  isDefault?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// ─── Support Ticket Models ─────────────────────────────────────────────
export type SupportCategory = 'SYSTEM_DEFECT' | 'ACCESS_REQUEST' | 'DATA_QUERY' | 'PERFORMANCE_ISSUE' | 'BILLING_SLA' | 'CUSTOM_ISSUE' | 'OTHER';
export type TicketPriority = 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW';
export type TicketStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'CLOSED';

export interface TicketComment {
  id: number;
  ticketId: number;
  authorId: number;
  authorName: string;
  authorRole: string;
  authorAvatar?: string;
  commentText: string;
  createdAt: string;
}

export interface SupportTicket {
  id: number;
  ticketCode: string; // e.g. SUP-1001
  subject: string;
  category: SupportCategory;
  priority: TicketPriority;
  targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN';
  status: TicketStatus;
  createdById: number;
  createdByName: string;
  createdByEmail: string;
  createdByAvatar?: string;
  assignedToId?: number;
  assignedToName?: string;
  description: string;
  resolutionNotes?: string;
  systemDiagnostics?: string;
  customCategoryName?: string;
  projectId?: number;
  projectName?: string;
  moduleName?: string;
  attachments?: string[];
  comments?: TicketComment[];
  createdAt: string;
  updatedAt: string;
}

export interface SupportStats {
  totalTickets: number;
  openTickets: number;
  inReviewTickets: number;
  resolvedTickets: number;
  closedTickets: number;
  urgentTickets: number;
  highPriorityTickets: number;
  mediumPriorityTickets: number;
  lowPriorityTickets: number;
  unassignedTickets: number;
  byStatus: Record<TicketStatus, number>;
  byPriority: Record<TicketPriority, number>;
  byCategory: Record<SupportCategory, number>;
  byTargetRole: Record<'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN', number>;
}

export interface ProjectStats {
  totalTasks: number;
  openTasks: number;
  completedTasks: number;
  totalRequirements: number;
  openBugs: number;
  totalMilestones: number;
  achievedMilestones: number;
  totalMembers: number;
  totalEstimatedHours: number;
  totalActualHours: number;
  completionPercentage: number;
  tasksByStatus: Record<string, number>;
  tasksByPriority: Record<string, number>;
  issuesBySeverity: Record<string, number>;
}


