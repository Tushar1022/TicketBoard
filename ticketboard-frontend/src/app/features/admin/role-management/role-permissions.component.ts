import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { RoleDto, PermissionDto } from '../../../core/models/api.models';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-role-permissions',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatTooltipModule
  ],
  templateUrl: './role-permissions.component.html',
  styleUrls: ['./role-permissions.component.scss']
})
export class RolePermissionsComponent implements OnInit {
  public roles = signal<RoleDto[]>([]);
  public permissions = signal<PermissionDto[]>([]);
  public modules = signal<string[]>([]);
  public selectedRole = signal<RoleDto | null>(null);
  public rolePerms = signal<Set<string>>(new Set());
  public loading = signal<boolean>(false);
  public saving = signal<boolean>(false);

  constructor(
    private userService: UserService,
    private toastService: ToastService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.userService.getAllRoles().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.roles.set(res.data);
          if (res.data.length > 0 && !this.selectedRole()) {
            this.selectRole(res.data[0]);
          }
        }
      }
    });

    this.userService.getAllPermissions().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.permissions.set(res.data);
          this.modules.set([...new Set(res.data.map(p => p.module))].sort());
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  selectRole(role: RoleDto): void {
    this.selectedRole.set(role);
    this.rolePerms.set(new Set(role.permissions || []));
  }

  hasPerm(code: string): boolean {
    return this.rolePerms().has(code);
  }

  togglePerm(code: string): void {
    const perms = new Set(this.rolePerms());
    if (perms.has(code)) {
      perms.delete(code);
    } else {
      perms.add(code);
    }
    this.rolePerms.set(perms);
  }

  isModuleFull(module: string): boolean {
    const modulePerms = this.permissions().filter(p => p.module === module);
    const perms = this.rolePerms();
    return modulePerms.every(p => perms.has(p.code));
  }

  isModulePartial(module: string): boolean {
    const modulePerms = this.permissions().filter(p => p.module === module);
    const perms = this.rolePerms();
    const checked = modulePerms.filter(p => perms.has(p.code)).length;
    return checked > 0 && checked < modulePerms.length;
  }

  toggleModule(module: string): void {
    const modulePerms = this.permissions().filter(p => p.module === module);
    const perms = new Set(this.rolePerms());
    const allChecked = this.isModuleFull(module);

    modulePerms.forEach(p => {
      if (allChecked) {
        perms.delete(p.code);
      } else {
        perms.add(p.code);
      }
    });

    this.rolePerms.set(perms);
  }

  toggleAll(): void {
    const allPerms = this.permissions().map(p => p.code);
    const perms = new Set(this.rolePerms());
    const allChecked = allPerms.every(c => perms.has(c));

    allPerms.forEach(c => {
      if (allChecked) {
        perms.delete(c);
      } else {
        perms.add(c);
      }
    });

    this.rolePerms.set(perms);
  }

  save(): void {
    const role = this.selectedRole();
    if (!role) return;

    this.saving.set(true);
    this.userService.updateRolePermissions(role.id, [...this.rolePerms()]).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const updated = res.data;
          this.roles.update(list =>
            list.map(r => r.id === updated.id ? { ...r, permissions: updated.permissions } : r)
          );
          this.selectRole(updated);
          this.toastService.success('Permissions saved successfully.');
        }
        this.saving.set(false);
      },
      error: (err) => {
        this.saving.set(false);
        this.toastService.error(err.error?.message || 'Failed to save permissions');
      }
    });
  }

  modulePerms(module: string): PermissionDto[] {
    return this.permissions().filter(p => p.module === module);
  }

  roleLabel(role: RoleDto): string {
    return (role.name || '').replace('ROLE_', '').replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  moduleIcon(module: string): string {
    const map: Record<string, string> = {
      USER: 'people',
      PROJECT: 'folder_special',
      REQUIREMENT: 'description',
      WORK_ITEM: 'task',
      TIMELOG: 'timer',
      TIME: 'timer',
      RELEASE: 'rocket',
      RISK: 'warning',
      BILLING: 'receipt',
      REPORT: 'analytics',
      CAPACITY: 'data_usage',
      DASHBOARD: 'dashboard',
      ADMIN: 'admin_panel_settings',
      AUDIT: 'history',
      COMMENT: 'comment',
      MILESTONE: 'flag'
    };
    return map[module] || 'settings';
  }

  permCount(): { total: number; checked: number } {
    return {
      total: this.permissions().length,
      checked: [...this.rolePerms()].length
    };
  }

  permPercent(): number {
    const { total, checked } = this.permCount();
    return total ? Math.round((checked / total) * 100) : 0;
  }
}