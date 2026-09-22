import { Component, OnInit, OnDestroy, computed, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { MailService } from '../../../core/services/mail.service';
import { ProjectService } from '../../../core/services/project.service';
import { SupportTicketService } from '../../../core/services/support-ticket.service';
import { WorkItemService } from '../../../core/services/work-item.service';
import { UserService } from '../../../core/services/user.service';
import { AppNotificationService } from '../../../core/services/notification.service';
import { LogTimeDialogComponent } from '../log-time-dialog/log-time-dialog.component';
import { RaiseTicketDialogComponent } from '../../../features/support/raise-ticket-dialog/raise-ticket-dialog.component';
import { ThemeService } from '../../../core/services/theme.service';
import { ThemePickerDialogComponent } from '../theme-picker/theme-picker-dialog.component';
import { NotificationDetailDialogComponent } from '../notification-detail-dialog/notification-detail-dialog.component';
import { AppNotification, Project, Team, User, WorkItem } from '../../../core/models/api.models';

export interface GlobalSearchResult {
  group: string;
  icon: string;
  title: string;
  subtitle: string;
  link: string[];
  searchIndex: number;
}

interface SearchCache {
  projects: Project[];
  tasks: WorkItem[];
  teams: Team[];
  people: User[];
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
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
export class LayoutComponent implements OnInit, OnDestroy {
  public sidebarCollapsed = signal<boolean>(false);
  public currentUser = computed(() => this.authService.currentUser());
  public recentProjects = signal<Project[]>([]);

  public openSupportTicketsCount = computed(() => this.supportTicketService.openTicketsCount());
  public mailUnreadCount = computed(() => this.mailService.counts()?.inboxUnread ?? 0);

  // Real-time DB notifications via AppNotificationService
  public notifications = computed(() => this.appNotificationService.notifications());
  public unreadCount = computed(() => this.appNotificationService.unreadCount());
  public notifFilter = signal<'ALL' | 'UNREAD'>('ALL');

  public filteredNotifications = computed(() => {
    const list = this.notifications();
    if (this.notifFilter() === 'UNREAD') {
      return list.filter((n) => !n.read);
    }
    return list;
  });

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

  // ─── Global Search State ─────────────────────────────────────────────
  public searchQuery = signal<string>('');
  public searchOpen = signal<boolean>(false);
  public searchLoading = signal<boolean>(false);
  public searchActiveIndex = signal<number>(-1);
  public searchResults = signal<GlobalSearchResult[]>([]);
  private searchTimer: any = null;
  private searchCachePromise: Promise<SearchCache> | null = null;

  constructor(
    public authService: AuthService,
    private projectService: ProjectService,
    public supportTicketService: SupportTicketService,
    public mailService: MailService,
    public themeService: ThemeService,
    public appNotificationService: AppNotificationService,
    private dialog: MatDialog,
    private router: Router,
    private workItemService: WorkItemService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadRecentProjects();
    this.supportTicketService.refreshAll();
    this.appNotificationService.loadNotifications();
    this.mailService.refreshCounts();
  }

  ngOnDestroy(): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
  }

  @HostListener('document:keydown', ['$event'])
  onGlobalKeydown(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.onSearchFocus(null);
    }
    if (event.key === 'Escape') {
      this.searchOpen.set(false);
    }
  }

  public onSearchFocus(inputEl: HTMLInputElement | null): void {
    if (inputEl) inputEl.focus();
    if (this.searchQuery().trim()) this.searchOpen.set(true);
    document.querySelector<HTMLInputElement>('.search-box input')?.focus();
  }

  public onSearchInput(query: string): void {
    this.searchQuery.set(query);
    this.searchActiveIndex.set(-1);
    if (this.searchTimer) clearTimeout(this.searchTimer);
    if (!query.trim()) {
      this.searchResults.set([]);
      this.searchOpen.set(false);
      return;
    }
    this.searchTimer = setTimeout(() => {
      this.runGlobalSearch();
    }, 260);
  }

  public onSearchBlur(): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
    setTimeout(() => this.searchOpen.set(false), 180);
  }

  public clearSearch(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.searchQuery.set('');
    this.searchResults.set([]);
    this.searchOpen.set(false);
  }

  public onSearchKeydown(event: KeyboardEvent): void {
    const items = this.searchResults();
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.searchActiveIndex.set(Math.min(items.length - 1, this.searchActiveIndex() + 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.searchActiveIndex.set(Math.max(-1, this.searchActiveIndex() - 1));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const target = items[this.searchActiveIndex()] || items[0];
      if (target) this.selectSearchResult(target);
    }
  }

  public async runGlobalSearch(): Promise<void> {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) return;
    this.searchLoading.set(true);
    try {
      const cache = await this.ensureSearchCache();
      const results: GlobalSearchResult[] = [];

      cache.projects.forEach((p, idx) => {
        if (p.name.toLowerCase().includes(q) || p.projectCode.toLowerCase().includes(q)) {
          results.push({
            group: 'Projects',
            icon: 'topic',
            title: `${p.projectCode}: ${p.name}`,
            subtitle: `Status: ${p.status} • Health: ${p.health || 'GREEN'}`,
            link: ['/projects', p.id.toString()],
            searchIndex: results.length
          });
        }
      });

      cache.tasks.forEach((t) => {
        if (t.title.toLowerCase().includes(q) || (t.ticketNumber && t.ticketNumber.toLowerCase().includes(q))) {
          results.push({
            group: 'Work Items',
            icon: 'task_alt',
            title: `${t.ticketNumber || 'TASK'}: ${t.title}`,
            subtitle: `Status: ${t.status} • Assignee: ${t.assigneeName || 'Unassigned'}`,
            link: ['/work-items'],
            searchIndex: results.length
          });
        }
      });

      cache.people.forEach((u) => {
        if (u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)) {
          results.push({
            group: 'Team Members',
            icon: 'person',
            title: u.fullName,
            subtitle: `${u.designation || 'Member'} • ${u.email}`,
            link: ['/admin/users'],
            searchIndex: results.length
          });
        }
      });

      this.searchResults.set(results.slice(0, 8));
      this.searchOpen.set(results.length > 0);
    } catch {
      this.searchResults.set([]);
    } finally {
      this.searchLoading.set(false);
    }
  }

  private ensureSearchCache(): Promise<SearchCache> {
    if (this.searchCachePromise) return this.searchCachePromise;
    this.searchCachePromise = Promise.all([
      firstValueFrom(this.projectService.getAllProjects()).catch(() => ({ data: [] })),
      firstValueFrom(this.workItemService.getWorkItems()).catch(() => ({ data: [] })),
      firstValueFrom(this.userService.getTeams()).catch(() => ({ data: [] })),
      firstValueFrom(this.userService.getUsers()).catch(() => ({ data: [] }))
    ]).then(([p, w, t, u]: any[]) => ({
      projects: p?.data || [],
      tasks: w?.data || [],
      teams: t?.data || [],
      people: u?.data || []
    }));
    return this.searchCachePromise;
  }

  public selectSearchResult(result: GlobalSearchResult): void {
    this.searchOpen.set(false);
    this.searchQuery.set('');
    this.router.navigate(result.link);
  }

  public groupedSearchResults() {
    const groups: { group: string; items: GlobalSearchResult[] }[] = [];
    const map = new Map<string, GlobalSearchResult[]>();
    for (const r of this.searchResults()) {
      if (!map.has(r.group)) map.set(r.group, []);
      map.get(r.group)!.push(r);
    }
    map.forEach((items, group) => groups.push({ group, items }));
    return groups;
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

  public openThemePicker(): void {
    this.dialog.open(ThemePickerDialogComponent, {
      width: '720px',
      maxWidth: '92vw',
      panelClass: 'theme-picker-panel'
    });
  }

  public logout(): void {
    this.authService.logout();
  }

  public markAllRead(): void {
    this.appNotificationService.markAllAsRead().subscribe();
  }

  public openNotificationDetail(notification: AppNotification): void {
    this.dialog.open(NotificationDetailDialogComponent, {
      width: '580px',
      maxWidth: '92vw',
      data: { notification }
    });
  }

  public dismissNotification(notif: AppNotification, event: Event): void {
    event.stopPropagation();
    this.appNotificationService.deleteNotification(notif.id).subscribe();
  }

  public getNotifIcon(type: string): string {
    switch (type) {
      case 'ALERT': return 'warning';
      case 'TASK': return 'task_alt';
      case 'ERP': return 'account_balance_wallet';
      case 'SECURITY': return 'security';
      case 'MENTION': return 'alternate_email';
      case 'SYSTEM': default: return 'dns';
    }
  }

  public getNotifColor(type: string): string {
    switch (type) {
      case 'ALERT': return 'red';
      case 'TASK': return 'indigo';
      case 'ERP': return 'green';
      case 'SECURITY': return 'amber';
      case 'MENTION': return 'purple';
      case 'SYSTEM': default: return 'slate';
    }
  }

  public canShowAdmin(): boolean {
    return this.authService.hasPermission('admin:access');
  }
}