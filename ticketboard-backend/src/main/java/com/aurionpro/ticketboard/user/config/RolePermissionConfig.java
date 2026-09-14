package com.aurionpro.ticketboard.user.config;

import com.aurionpro.ticketboard.user.enums.PermissionCode;
import com.aurionpro.ticketboard.user.enums.RoleType;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class RolePermissionConfig {

    private static final Set<PermissionCode> SUPER_ADMIN_PERMISSIONS = allPermissions();

    private static final Set<PermissionCode> ADMIN_PERMISSIONS = EnumSet.of(
            PermissionCode.USER_VIEW,
            PermissionCode.USER_CREATE,
            PermissionCode.USER_EDIT,
            PermissionCode.USER_MANAGE_STATUS,
            PermissionCode.PROJECT_VIEW,
            PermissionCode.PROJECT_CREATE,
            PermissionCode.PROJECT_EDIT,
            PermissionCode.PROJECT_MANAGE_MEMBERS,
            PermissionCode.REQUIREMENT_VIEW,
            PermissionCode.REQUIREMENT_CREATE,
            PermissionCode.REQUIREMENT_EDIT,
            PermissionCode.REQUIREMENT_MANAGE_STATUS,
            PermissionCode.WORKITEM_VIEW,
            PermissionCode.WORKITEM_CREATE,
            PermissionCode.WORKITEM_EDIT,
            PermissionCode.WORKITEM_MANAGE_STATUS,
            PermissionCode.TIMELOG_VIEW,
            PermissionCode.TIMELOG_CREATE,
            PermissionCode.TIMELOG_EDIT,
            PermissionCode.TIMELOG_APPROVE,
            PermissionCode.RELEASE_VIEW,
            PermissionCode.RELEASE_CREATE,
            PermissionCode.RELEASE_EDIT,
            PermissionCode.RISK_VIEW,
            PermissionCode.RISK_CREATE,
            PermissionCode.RISK_EDIT,
            PermissionCode.BILLING_VIEW,
            PermissionCode.BILLING_CREATE,
            PermissionCode.BILLING_EDIT,
            PermissionCode.REPORT_VIEW,
            PermissionCode.REPORT_EXPORT,
            PermissionCode.CAPACITY_VIEW,
            PermissionCode.DASHBOARD_VIEW,
            PermissionCode.ADMIN_ACCESS,
            PermissionCode.ADMIN_MASTER_DATA,
            PermissionCode.AUDIT_VIEW,
            PermissionCode.MILESTONE_VIEW,
            PermissionCode.MILESTONE_CREATE,
            PermissionCode.MILESTONE_EDIT,
            PermissionCode.COMMENT_VIEW,
            PermissionCode.COMMENT_CREATE,
            PermissionCode.COMMENT_DELETE
    );

    private static final Set<PermissionCode> PROJECT_OWNER_PERMISSIONS = EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.PROJECT_EDIT,
            PermissionCode.REQUIREMENT_VIEW,
            PermissionCode.REQUIREMENT_CREATE,
            PermissionCode.REQUIREMENT_EDIT,
            PermissionCode.REQUIREMENT_MANAGE_STATUS,
            PermissionCode.WORKITEM_VIEW,
            PermissionCode.WORKITEM_CREATE,
            PermissionCode.WORKITEM_EDIT,
            PermissionCode.WORKITEM_MANAGE_STATUS,
            PermissionCode.TIMELOG_VIEW,
            PermissionCode.TIMELOG_CREATE,
            PermissionCode.RELEASE_VIEW,
            PermissionCode.RELEASE_CREATE,
            PermissionCode.RELEASE_EDIT,
            PermissionCode.RISK_VIEW,
            PermissionCode.RISK_CREATE,
            PermissionCode.RISK_EDIT,
            PermissionCode.REPORT_VIEW,
            PermissionCode.DASHBOARD_VIEW,
            PermissionCode.MILESTONE_VIEW,
            PermissionCode.MILESTONE_CREATE,
            PermissionCode.MILESTONE_EDIT,
            PermissionCode.COMMENT_VIEW,
            PermissionCode.COMMENT_CREATE,
            PermissionCode.COMMENT_DELETE
    );

    private static final Set<PermissionCode> PROJECT_MANAGER_PERMISSIONS = EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.PROJECT_CREATE,
            PermissionCode.PROJECT_EDIT,
            PermissionCode.PROJECT_MANAGE_MEMBERS,
            PermissionCode.REQUIREMENT_VIEW,
            PermissionCode.REQUIREMENT_CREATE,
            PermissionCode.REQUIREMENT_EDIT,
            PermissionCode.REQUIREMENT_MANAGE_STATUS,
            PermissionCode.WORKITEM_VIEW,
            PermissionCode.WORKITEM_CREATE,
            PermissionCode.WORKITEM_EDIT,
            PermissionCode.WORKITEM_MANAGE_STATUS,
            PermissionCode.TIMELOG_VIEW,
            PermissionCode.TIMELOG_CREATE,
            PermissionCode.TIMELOG_EDIT,
            PermissionCode.TIMELOG_APPROVE,
            PermissionCode.RELEASE_VIEW,
            PermissionCode.RELEASE_CREATE,
            PermissionCode.RELEASE_EDIT,
            PermissionCode.RISK_VIEW,
            PermissionCode.RISK_CREATE,
            PermissionCode.RISK_EDIT,
            PermissionCode.REPORT_VIEW,
            PermissionCode.REPORT_EXPORT,
            PermissionCode.CAPACITY_VIEW,
            PermissionCode.DASHBOARD_VIEW,
            PermissionCode.MILESTONE_VIEW,
            PermissionCode.MILESTONE_CREATE,
            PermissionCode.MILESTONE_EDIT,
            PermissionCode.COMMENT_VIEW,
            PermissionCode.COMMENT_CREATE,
            PermissionCode.COMMENT_DELETE
    );

    private static final Set<PermissionCode> TEAM_LEAD_PERMISSIONS = EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.REQUIREMENT_VIEW,
            PermissionCode.REQUIREMENT_EDIT,
            PermissionCode.WORKITEM_VIEW,
            PermissionCode.WORKITEM_EDIT,
            PermissionCode.WORKITEM_MANAGE_STATUS,
            PermissionCode.TIMELOG_VIEW,
            PermissionCode.TIMELOG_CREATE,
            PermissionCode.TIMELOG_EDIT,
            PermissionCode.TIMELOG_APPROVE,
            PermissionCode.RELEASE_VIEW,
            PermissionCode.RISK_VIEW,
            PermissionCode.RISK_CREATE,
            PermissionCode.RISK_EDIT,
            PermissionCode.REPORT_VIEW,
            PermissionCode.CAPACITY_VIEW,
            PermissionCode.DASHBOARD_VIEW,
            PermissionCode.MILESTONE_VIEW,
            PermissionCode.MILESTONE_EDIT,
            PermissionCode.COMMENT_VIEW,
            PermissionCode.COMMENT_CREATE,
            PermissionCode.COMMENT_DELETE
    );

    private static final Set<PermissionCode> BUSINESS_ANALYST_PERMISSIONS = EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.REQUIREMENT_VIEW,
            PermissionCode.REQUIREMENT_CREATE,
            PermissionCode.REQUIREMENT_EDIT,
            PermissionCode.REQUIREMENT_MANAGE_STATUS,
            PermissionCode.WORKITEM_VIEW,
            PermissionCode.WORKITEM_CREATE,
            PermissionCode.TIMELOG_VIEW,
            PermissionCode.TIMELOG_CREATE,
            PermissionCode.TIMELOG_EDIT,
            PermissionCode.RELEASE_VIEW,
            PermissionCode.RISK_VIEW,
            PermissionCode.RISK_CREATE,
            PermissionCode.DASHBOARD_VIEW,
            PermissionCode.MILESTONE_VIEW,
            PermissionCode.COMMENT_VIEW,
            PermissionCode.COMMENT_CREATE,
            PermissionCode.COMMENT_DELETE
    );

    private static final Set<PermissionCode> DEVELOPER_PERMISSIONS = EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.REQUIREMENT_VIEW,
            PermissionCode.WORKITEM_VIEW,
            PermissionCode.WORKITEM_CREATE,
            PermissionCode.WORKITEM_EDIT,
            PermissionCode.TIMELOG_VIEW,
            PermissionCode.TIMELOG_CREATE,
            PermissionCode.TIMELOG_EDIT,
            PermissionCode.RELEASE_VIEW,
            PermissionCode.RISK_VIEW,
            PermissionCode.DASHBOARD_VIEW,
            PermissionCode.MILESTONE_VIEW,
            PermissionCode.COMMENT_VIEW,
            PermissionCode.COMMENT_CREATE,
            PermissionCode.COMMENT_DELETE
    );

    private static final Set<PermissionCode> QA_TESTER_PERMISSIONS = EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.REQUIREMENT_VIEW,
            PermissionCode.WORKITEM_VIEW,
            PermissionCode.WORKITEM_CREATE,
            PermissionCode.WORKITEM_EDIT,
            PermissionCode.TIMELOG_VIEW,
            PermissionCode.TIMELOG_CREATE,
            PermissionCode.TIMELOG_EDIT,
            PermissionCode.RELEASE_VIEW,
            PermissionCode.RISK_VIEW,
            PermissionCode.DASHBOARD_VIEW,
            PermissionCode.MILESTONE_VIEW,
            PermissionCode.COMMENT_VIEW,
            PermissionCode.COMMENT_CREATE,
            PermissionCode.COMMENT_DELETE
    );

    private static final Set<PermissionCode> MANAGEMENT_PERMISSIONS = EnumSet.of(
            PermissionCode.PROJECT_VIEW,
            PermissionCode.REQUIREMENT_VIEW,
            PermissionCode.WORKITEM_VIEW,
            PermissionCode.TIMELOG_VIEW,
            PermissionCode.RELEASE_VIEW,
            PermissionCode.RISK_VIEW,
            PermissionCode.BILLING_VIEW,
            PermissionCode.REPORT_VIEW,
            PermissionCode.REPORT_EXPORT,
            PermissionCode.CAPACITY_VIEW,
            PermissionCode.DASHBOARD_VIEW,
            PermissionCode.MILESTONE_VIEW,
            PermissionCode.COMMENT_VIEW,
            PermissionCode.COMMENT_CREATE
    );

    public Set<PermissionCode> getPermissionsForRole(RoleType role) {
        return switch (role) {
            case ROLE_SUPER_ADMIN -> SUPER_ADMIN_PERMISSIONS;
            case ROLE_ADMIN -> ADMIN_PERMISSIONS;
            case ROLE_PROJECT_OWNER -> PROJECT_OWNER_PERMISSIONS;
            case ROLE_PROJECT_MANAGER -> PROJECT_MANAGER_PERMISSIONS;
            case ROLE_TEAM_LEAD -> TEAM_LEAD_PERMISSIONS;
            case ROLE_BUSINESS_ANALYST -> BUSINESS_ANALYST_PERMISSIONS;
            case ROLE_DEVELOPER -> DEVELOPER_PERMISSIONS;
            case ROLE_QA_TESTER -> QA_TESTER_PERMISSIONS;
            case ROLE_MANAGEMENT -> MANAGEMENT_PERMISSIONS;
        };
    }

    private static Set<PermissionCode> allPermissions() {
        return EnumSet.allOf(PermissionCode.class);
    }
}