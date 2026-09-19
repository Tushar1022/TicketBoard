import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProjectService } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { Project, User } from '../../../core/models/api.models';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MatIconModule, MatTooltipModule],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss']
})
export class ProjectListComponent implements OnInit {
  private toastService = inject(ToastService);
  public projects = signal<Project[]>([]);
  public filteredProjects = signal<Project[]>([]);
  public users = signal<User[]>([]);
  public isLoading = signal<boolean>(true);
  public activeTab = signal<'active' | 'public'>('active');
  public filterScope = signal<string>('ALL');
  public searchQuery = signal<string>('');
  public sortField = signal<keyof Project | 'projectCode'>('name');
  public sortDir = signal<'asc' | 'desc'>('asc');
  public selectedIds = signal<Set<number>>(new Set());
  public pageIndex = signal<number>(1);
  public pageSize = signal<number>(8);

  // Create Project Modal State
  public showCreateModal = signal<boolean>(false);
  public newProject = {
    projectCode: '',
    name: '',
    description: '',
    clientId: 1,
    priority: 'HIGH',
    budget: 50000,
    estimatedHours: 400,
    startDate: new Date().toISOString().substring(0, 10),
    plannedEndDate: '',
    projectManagerId: null as number | null
  };

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadProjects();
  }

  public loadUsers(): void {
    this.userService.getUsers({ status: 'ACTIVE' }).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.users.set(res.data);
          if (!this.newProject.projectManagerId) {
             this.newProject.projectManagerId = this.authService.currentUser()?.id || null;
          }
        }
      }
    });
  }

  public loadProjects(): void {
    this.isLoading.set(true);
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.projects.set(res.data);
          this.applyFilters();
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  public setTab(tab: 'active' | 'public'): void {
    this.activeTab.set(tab);
    this.applyFilters();
  }

  public applyFilters(): void {
    let list = this.projects();
    const currentUserId = this.authService.currentUser()?.id;

    // Tab split: Active = owned by me; Public = owned by others
    if (this.activeTab() === 'active') {
      list = list.filter((p) => p.projectManagerId === currentUserId);
    } else {
      list = list.filter((p) => p.projectManagerId !== currentUserId);
    }

    if (this.searchQuery().trim()) {
      const q = this.searchQuery().toLowerCase();
      list = list.filter((p) => p.name.toLowerCase().includes(q) || p.projectCode.toLowerCase().includes(q));
    }
    if (this.filterScope() !== 'ALL') {
      list = list.filter((p) => p.status === this.filterScope());
    }
    this.filteredProjects.set(list);
    this.pageIndex.set(1);
  }

  public pagedProjects = computed<Project[]>(() => {
    const list = this.filteredProjects();
    const field = this.sortField();
    const dir = this.sortDir();
    const sorted = [...list].sort((a, b) => {
      const av = a[field];
      const bv = b[field];
      let cmp = 0;
      if (typeof av === 'number' && typeof bv === 'number') cmp = av - bv;
      else cmp = String(av ?? '').toLowerCase().localeCompare(String(bv ?? '').toLowerCase());
      return dir === 'asc' ? cmp : -cmp;
    });
    const size = this.pageSize();
    const start = (this.pageIndex() - 1) * size;
    return sorted.slice(start, start + size);
  });

  public totalPages = computed<number>(() => Math.max(1, Math.ceil(this.filteredProjects().length / this.pageSize())));
  public pageStart = computed<number>(() => this.filteredProjects().length === 0 ? 0 : (this.pageIndex() - 1) * this.pageSize() + 1);
  public pageEnd = computed<number>(() => Math.min(this.filteredProjects().length, this.pageIndex() * this.pageSize()));
  public isAllSelected = computed<boolean>(() => {
    const visible = this.pagedProjects();
    return visible.length > 0 && visible.every(p => this.selectedIds().has(p.id));
  });

  public sortBy(field: keyof Project | 'projectCode'): void {
    if (this.sortField() === field) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDir.set('asc');
    }
  }

  public sortIcon(field: keyof Project | 'projectCode'): string {
    if (this.sortField() !== field) return 'unfold_more';
    return this.sortDir() === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }

  public toggleAll(): void {
    const visible = this.pagedProjects();
    const current = new Set(this.selectedIds());
    const allSelected = this.isAllSelected();
    if (allSelected) {
      visible.forEach(p => current.delete(p.id));
    } else {
      visible.forEach(p => current.add(p.id));
    }
    this.selectedIds.set(new Set(current));
  }

  public toggleRow(id: number): void {
    const current = new Set(this.selectedIds());
    if (current.has(id)) current.delete(id);
    else current.add(id);
    this.selectedIds.set(new Set(current));
  }

  public isSelected(id: number): boolean {
    return this.selectedIds().has(id);
  }

  public hasSelection(): boolean {
    return this.selectedIds().size > 0;
  }

  public clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  public goToPage(page: number): void {
    this.pageIndex.set(Math.min(this.totalPages(), Math.max(1, page)));
  }

  public setPageSize(size: number): void {
    this.pageSize.set(size);
    this.pageIndex.set(1);
  }

  public bulkDeleteSelected(): void {
    const ids = [...this.selectedIds()];
    if (ids.length === 0) return;
    if (!confirm(`Delete ${ids.length} selected project(s)? This cannot be undone.`)) return;
    let remaining = ids.length;
    ids.forEach((id) => {
      this.projectService.deleteProject(id).pipe().subscribe({
        next: () => {
          remaining -= 1;
          if (remaining === 0) {
            this.toastService.success(`${ids.length} project(s) deleted successfully.`);
            this.clearSelection();
            this.loadProjects();
          }
        },
        error: () => {
          remaining -= 1;
          if (remaining === 0) {
            this.toastService.error('Failed to delete some selected projects.');
            this.clearSelection();
            this.loadProjects();
          }
        }
      });
    });
  }

  public isOwnedByMe(p: Project): boolean {
    return p.projectManagerId === this.authService.currentUser()?.id;
  }

  public statusLabel(status: string): string {
    return status.replace(/_/g, ' ');
  }

  public statusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
      case 'CLOSED':
        return 'green-pill';
      case 'ON_HOLD':
      case 'CANCELLED':
        return 'amber-pill';
      case 'IN_PROGRESS':
      case 'APPROVED':
        return 'blue-pill';
      default:
        return 'slate-pill';
    }
  }

  public taskCounts(p: Project): { closed: number; total: number } {
    const total = p.taskCount || 0;
    const closed = Math.round(total * ((p.completionPercentage || 0) / 100));
    return { closed, total };
  }

  public issueCounts(p: Project): { open: number; total: number } {
    const open = p.openBugCount || 0;
    return { open, total: open };
  }

  public openCreateModal(): void {
    this.showCreateModal.set(true);
  }

  public closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  public submitCreateProject(): void {
    this.projectService.createProject(this.newProject).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success(`Project ${res.data?.projectCode || this.newProject.projectCode} was created successfully.`);
          this.closeCreateModal();
          this.loadProjects();
        } else {
          this.toastService.error(res.message || 'Failed to create the project.');
        }
      },
      error: () => this.toastService.error('Project creation failed. Please try again.')
    });
  }
}
