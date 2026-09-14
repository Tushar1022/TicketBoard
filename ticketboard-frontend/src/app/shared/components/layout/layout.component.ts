import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService } from '../../../core/services/project.service';
import { SupportTicketService } from '../../../core/services/support-ticket.service';
import { LogTimeDialogComponent } from '../log-time-dialog/log-time-dialog.component';
import { RaiseTicketDialogComponent } from '../../../features/support/raise-ticket-dialog/raise-ticket-dialog.component';
import { Project } from '../../../core/models/api.models';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    MatDialogModule,
    MatDividerModule
  ],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit {
  public sidebarCollapsed = signal<boolean>(false);
  public currentUser = computed(() => this.authService.currentUser());
  public recentProjects = signal<Project[]>([]);

  public openSupportTicketsCount = computed(() => this.supportTicketService.openTicketsCount());

  public notifications = signal<{ text: string; icon: string; color: string; time: string; read: boolean }[]>([]);

  public unreadCount = computed(() => this.notifications().filter(n => !n.read).length);

  public workspaceName = computed(() => {
    const email = this.currentUser()?.email || '';
    const domain = email.split('@')[1];
    if (domain) {
      const name = domain.split('.')[0];
      return name.charAt(0).toUpperCase() + name.slice(1);
    }
    return 'Workspace';
  });

  public activeProjectContext = signal<string>('');

  public primaryRole = computed(() => {
    const roles = this.currentUser()?.roles || [];
    if (roles.includes('ROLE_SUPER_ADMIN')) return 'super-admin';
    if (roles.includes('ROLE_ADMIN')) return 'admin';
    if (roles.includes('ROLE_PROJECT_OWNER')) return 'owner';
    if (roles.includes('ROLE_PROJECT_MANAGER')) return 'pm';
    if (roles.includes('ROLE_TEAM_LEAD')) return 'lead';
    if (roles.includes('ROLE_BUSINESS_ANALYST')) return 'ba';
    if (roles.includes('ROLE_QA_TESTER')) return 'qa';
    return 'developer';
  });

  public displayRole = computed(() => {
    const role = this.primaryRole();
    const roleDisplayMap: Record<string, string> = {
      'super-admin': 'Super Admin',
      'admin': 'Admin',
      'owner': 'Project Owner',
      'pm': 'Project Manager',
      'lead': 'Team Lead',
      'ba': 'Business Analyst',
      'qa': 'QA Tester',
      'developer': 'Developer'
    };
    return roleDisplayMap[role] || role;
  });

  constructor(
    public authService: AuthService,
    private projectService: ProjectService,
    public supportTicketService: SupportTicketService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRecentProjects();
  }

  public loadRecentProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.recentProjects.set(res.data.slice(0, 3));
        }
      },
      error: () => {
        this.recentProjects.set([]);
      }
    });
  }

  public toggleSidebar(): void {
    this.sidebarCollapsed.update((val) => !val);
  }

  public openLogTimeDialog(): void {
    this.dialog.open(LogTimeDialogComponent, {
      width: '560px',
      disableClose: false
    });
  }

  public openRaiseTicketDialog(targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN' = 'ROLE_SUPER_ADMIN'): void {
    const dialogRef = this.dialog.open(RaiseTicketDialogComponent, {
      width: '680px',
      data: { targetRole }
    });

    dialogRef.afterClosed().subscribe(ticket => {
      if (ticket) {
        this.router.navigate(['/support/tickets']);
      }
    });
  }

  public logout(): void {
    this.authService.logout();
  }

  public markAllRead(): void {
    this.notifications.update(items => items.map(n => ({ ...n, read: true })));
  }

  public canShowAdmin(): boolean {
    return this.authService.hasPermission('admin:access');
  }
}