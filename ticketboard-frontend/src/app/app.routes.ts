import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { LoginComponent } from './features/auth/login/login.component';
import { UnauthorizedComponent } from './features/auth/unauthorized/unauthorized.component';
import { AdminLayoutComponent } from './features/admin/admin-layout/admin-layout.component';
export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'unauthorized',
    component: UnauthorizedComponent
  },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    canActivateChild: [permissionGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard/executive',
        pathMatch: 'full'
      },
      {
        path: 'dashboard/executive',
        loadComponent: () => import('./features/dashboard/executive/executive-dashboard.component').then(m => m.ExecutiveDashboardComponent),
        data: { permission: 'dashboard:view' }
      },
      {
        path: 'my-workspace',
        loadComponent: () => import('./features/dashboard/developer/my-workspace.component').then(m => m.MyWorkspaceComponent)
      },
      {
        path: 'projects',
        loadComponent: () => import('./features/projects/project-list/project-list.component').then(m => m.ProjectListComponent),
        data: { permission: 'project:view' }
      },
      {
        path: 'projects/:id',
        loadComponent: () => import('./features/projects/project-detail/project-detail.component').then(m => m.ProjectDetailComponent),
        data: { permission: 'project:view' }
      },
      {
        path: 'requirements',
        loadComponent: () => import('./features/requirements/requirement-list/requirement-list.component').then(m => m.RequirementListComponent),
        data: { permission: 'requirement:view' }
      },
      {
        path: 'work-items',
        loadComponent: () => import('./features/work-items/work-item-board/work-item-board.component').then(m => m.WorkItemBoardComponent),
        data: { permission: 'workitem:view' }
      },

      {
        path: 'timetracking',
        loadComponent: () => import('./features/timetracking/timetracking.component').then(m => m.TimetrackingComponent),
        data: { permission: 'timelog:view' }
      },
      {
        path: 'releases',
        loadComponent: () => import('./features/releases/release-list/release-list.component').then(m => m.ReleaseListComponent),
        data: { permission: 'release:view' }
      },
      {
        path: 'risks',
        loadComponent: () => import('./features/risks/risk-management.component').then(m => m.RiskManagementComponent),
        data: { permission: 'risk:view' }
      },
      {
        path: 'capacity',
        loadComponent: () => import('./features/capacity/capacity-planning.component').then(m => m.CapacityPlanningComponent),
        data: { permission: 'capacity:view' }
      },
      {
        path: 'reports',
        loadComponent: () => import('./features/reports/reports-hub.component').then(m => m.ReportsHubComponent),
        data: { permission: 'report:view' }
      },
      {
        path: 'billing',
        loadComponent: () => import('./features/billing/billing.component').then(m => m.BillingComponent),
        data: { permission: 'billing:view' }
      },
      {
        path: 'support',
        loadComponent: () => import('./features/support/support-hub.component').then(m => m.SupportHubComponent)
      },
      {
        path: 'support/tickets',
        loadComponent: () => import('./features/support/support-tickets.component').then(m => m.SupportTicketsComponent)
      },
      // ─── Admin Panel ──────────────────────────────────────────────────
      {
        path: 'admin',
        component: AdminLayoutComponent,
        canActivate: [permissionGuard],
        canActivateChild: [permissionGuard],
        data: { permission: 'admin:access' },
        children: [
          {
            path: '',
            redirectTo: 'users',
            pathMatch: 'full'
          },
          {
            path: 'users',
            loadComponent: () => import('./features/admin/user-management/user-list.component').then(m => m.UserListComponent),
            data: { permission: 'user:view' }
          },
          {
            path: 'master-data',
            loadComponent: () => import('./features/admin/master-data/master-data.component').then(m => m.MasterDataComponent),
            data: { permission: 'admin:master-data' }
          },
          {
            path: 'roles',
            loadComponent: () => import('./features/admin/role-management/role-permissions.component').then(m => m.RolePermissionsComponent),
            data: { permission: 'admin:roles-permissions' }
          },
          {
            path: 'support-tickets',
            loadComponent: () => import('./features/admin/support-tickets/admin-support-tickets.component').then(m => m.AdminSupportTicketsComponent),
            data: { permission: 'admin:access' }
          }
        ]
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard/executive'
  }
];