import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule
  ],
  template: `
    <div class="admin-root">
      <!-- Administration Header + Section Tabs -->
      <header class="admin-header">
        <div class="admin-title-block">
          <div class="admin-badge">
            <mat-icon>admin_panel_settings</mat-icon>
          </div>
          <div class="admin-titles">
            <h1>Administration</h1>
            <p>Manage users, roles, permissions and organisation data</p>
          </div>
        </div>

        <nav class="admin-tabs">
          <a
            routerLink="/admin/users"
            routerLinkActive="active"
            class="admin-tab"
            *ngIf="authService.hasPermission('user:view')"
          >
            <mat-icon>group</mat-icon>
            <span>Users</span>
          </a>
          <a
            routerLink="/admin/master-data"
            routerLinkActive="active"
            class="admin-tab"
            *ngIf="authService.hasPermission('admin:master-data')"
          >
            <mat-icon>settings</mat-icon>
            <span>Master Data</span>
          </a>
          <a
            routerLink="/admin/roles"
            routerLinkActive="active"
            class="admin-tab"
            *ngIf="authService.hasPermission('admin:roles-permissions')"
          >
            <mat-icon>admin_panel_settings</mat-icon>
            <span>Roles &amp; Permissions</span>
          </a>
          <a
            routerLink="/admin/support-tickets"
            routerLinkActive="active"
            class="admin-tab"
          >
            <mat-icon>headset_mic</mat-icon>
            <span>Support Tickets</span>
          </a>
        </nav>
      </header>

      <main class="admin-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .admin-root {
      display: flex;
      flex-direction: column;
      gap: 18px;
    }

    /* ── Header ─────────────────────────────── */
    .admin-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
      flex-wrap: wrap;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--surface-border);
    }

    .admin-title-block {
      display: flex;
      align-items: center;
      gap: 13px;
    }

    .admin-badge {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      border-radius: 12px;
      color: #ffffff;
      background: linear-gradient(135deg, var(--primary-500), var(--primary-700));
      box-shadow: 0 6px 14px rgba(79, 70, 229, 0.28);
    }

    .admin-badge mat-icon {
      width: 22px;
      height: 22px;
      font-size: 22px;
    }

    .admin-titles h1 {
      margin: 0;
      font-size: 1.45rem;
      font-weight: 800;
      letter-spacing: -0.02em;
      color: var(--text-main);
    }

    .admin-titles p {
      margin: 2px 0 0;
      font-size: 0.8rem;
      color: var(--text-muted);
    }

    /* ── Tabs ───────────────────────────────── */
    .admin-tabs {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 5px;
      background: var(--surface-subtle);
      border: 1px solid var(--surface-border);
      border-radius: var(--radius-md);
    }

    .admin-tab {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 14px;
      border-radius: var(--radius-sm);
      border: 1px solid transparent;
      text-decoration: none;
      color: var(--text-muted);
      font-size: 0.8rem;
      font-weight: 700;
      white-space: nowrap;
      transition: all var(--transition-fast);

      mat-icon {
        width: 17px;
        height: 17px;
        font-size: 17px;
      }

      &:hover {
        color: var(--text-main);
        background: #ffffff;
      }

      &.active {
        background: #ffffff;
        color: var(--primary-700);
        border-color: var(--surface-border);
        box-shadow: var(--shadow-sm);
      }
    }

    /* ── Content ────────────────────────────── */
    .admin-content {
      min-width: 0;
      display: flex;
      flex-direction: column;
    }

    @media (max-width: 720px) {
      .admin-header {
        flex-direction: column;
        align-items: flex-start;
      }
      .admin-tabs {
        width: 100%;
        overflow-x: auto;
      }
    }
  `]
})
export class AdminLayoutComponent {
  constructor(public authService: AuthService) {}
}