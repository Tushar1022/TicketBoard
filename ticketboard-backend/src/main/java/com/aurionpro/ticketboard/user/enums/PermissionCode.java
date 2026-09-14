package com.aurionpro.ticketboard.user.enums;

public enum PermissionCode {

    // User Module
    USER_VIEW("user:view", "View Users"),
    USER_CREATE("user:create", "Create Users"),
    USER_EDIT("user:edit", "Edit Users"),
    USER_DELETE("user:delete", "Delete Users"),
    USER_MANAGE_STATUS("user:manage-status", "Manage User Status"),

    // Project Module
    PROJECT_VIEW("project:view", "View Projects"),
    PROJECT_CREATE("project:create", "Create Projects"),
    PROJECT_EDIT("project:edit", "Edit Projects"),
    PROJECT_DELETE("project:delete", "Delete Projects"),
    PROJECT_MANAGE_MEMBERS("project:manage-members", "Manage Project Members"),

    // Requirement Module
    REQUIREMENT_VIEW("requirement:view", "View Requirements"),
    REQUIREMENT_CREATE("requirement:create", "Create Requirements"),
    REQUIREMENT_EDIT("requirement:edit", "Edit Requirements"),
    REQUIREMENT_DELETE("requirement:delete", "Delete Requirements"),
    REQUIREMENT_MANAGE_STATUS("requirement:manage-status", "Manage Requirement Status"),

    // Work Item Module
    WORKITEM_VIEW("workitem:view", "View Work Items"),
    WORKITEM_CREATE("workitem:create", "Create Work Items"),
    WORKITEM_EDIT("workitem:edit", "Edit Work Items"),
    WORKITEM_DELETE("workitem:delete", "Delete Work Items"),
    WORKITEM_MANAGE_STATUS("workitem:manage-status", "Manage Work Item Status"),

    // Time Tracking Module
    TIMELOG_VIEW("timelog:view", "View Time Logs"),
    TIMELOG_CREATE("timelog:create", "Create Time Logs"),
    TIMELOG_EDIT("timelog:edit", "Edit Time Logs"),
    TIMELOG_DELETE("timelog:delete", "Delete Time Logs"),
    TIMELOG_APPROVE("timelog:approve", "Approve Time Logs"),

    // Release Module
    RELEASE_VIEW("release:view", "View Releases"),
    RELEASE_CREATE("release:create", "Create Releases"),
    RELEASE_EDIT("release:edit", "Edit Releases"),
    RELEASE_DELETE("release:delete", "Delete Releases"),

    // Risk Module
    RISK_VIEW("risk:view", "View Risks"),
    RISK_CREATE("risk:create", "Create Risks"),
    RISK_EDIT("risk:edit", "Edit Risks"),
    RISK_DELETE("risk:delete", "Delete Risks"),

    // Billing Module
    BILLING_VIEW("billing:view", "View Billing"),
    BILLING_CREATE("billing:create", "Create Invoices"),
    BILLING_EDIT("billing:edit", "Edit Invoices"),
    BILLING_DELETE("billing:delete", "Delete Invoices"),

    // Report Module
    REPORT_VIEW("report:view", "View Reports"),
    REPORT_EXPORT("report:export", "Export Reports"),

    // Capacity Module
    CAPACITY_VIEW("capacity:view", "View Capacity Planning"),

    // Dashboard Module
    DASHBOARD_VIEW("dashboard:view", "View Dashboards"),

    // Admin Module
    ADMIN_ACCESS("admin:access", "Access Admin Panel"),
    ADMIN_MASTER_DATA("admin:master-data", "Manage Master Data"),
    ADMIN_ROLES_PERMISSIONS("admin:roles-permissions", "Manage Roles & Permissions"),
    AUDIT_VIEW("audit:view", "View Audit Logs"),

    // Milestone Module
    MILESTONE_VIEW("milestone:view", "View Milestones"),
    MILESTONE_CREATE("milestone:create", "Create Milestones"),
    MILESTONE_EDIT("milestone:edit", "Edit Milestones"),
    MILESTONE_DELETE("milestone:delete", "Delete Milestones"),

    // Comment Module
    COMMENT_VIEW("comment:view", "View Comments"),
    COMMENT_CREATE("comment:create", "Create Comments"),
    COMMENT_DELETE("comment:delete", "Delete Comments");

    private final String code;
    private final String description;

    PermissionCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PermissionCode fromCode(String code) {
        for (PermissionCode pc : values()) {
            if (pc.code.equals(code)) {
                return pc;
            }
        }
        throw new IllegalArgumentException("Unknown permission code: " + code);
    }
}
