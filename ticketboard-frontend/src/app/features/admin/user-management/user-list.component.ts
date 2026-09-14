import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { User, RoleType, Department, Team } from '../../../core/models/api.models';
import { UserFormDialogComponent } from './user-form-dialog.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTableModule,
    MatChipsModule,
    MatMenuModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  public users = signal<User[]>([]);
  public filteredUsers = signal<User[]>([]);
  public departments = signal<Department[]>([]);
  public teams = signal<Team[]>([]);
  public loading = signal<boolean>(false);

  public searchTerm = signal<string>('');
  public roleFilter = signal<RoleType | ''>('');
  public statusFilter = signal<string>('');
  public departmentFilter = signal<number | null>(null);

  public displayedColumns = ['avatar', 'name', 'email', 'department', 'team', 'roles', 'status', 'actions'];

  constructor(
    private userService: UserService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadReferenceData();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.userService.getUsers().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.users.set(res.data);
          this.applyFilters();
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load users', 'Close', { duration: 3000 });
      }
    });
  }

  loadReferenceData(): void {
    this.userService.getDepartments().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => { if (res.success && res.data) this.departments.set(res.data); }
    });
    this.userService.getTeams().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => { if (res.success && res.data) this.teams.set(res.data); }
    });
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.applyFilters();
  }

  onRoleChange(value: RoleType): void {
    this.roleFilter.set(value);
    this.applyFilters();
  }

  onStatusChange(value: string): void {
    this.statusFilter.set(value);
    this.applyFilters();
  }

  onDepartmentChange(value: number): void {
    this.departmentFilter.set(value ?? null);
    this.applyFilters();
  }

  applyFilters(): void {
    const term = this.searchTerm().toLowerCase();
    const role = this.roleFilter();
    const status = this.statusFilter();
    const dept = this.departmentFilter();

    this.filteredUsers.set(
      this.users().filter((u) => {
        if (term && !(`${u.fullName} ${u.email} ${u.designation || ''}`.toLowerCase().includes(term))) return false;
        if (role && !u.roles.includes(role)) return false;
        if (status && u.status !== status) return false;
        if (dept && u.departmentId !== dept) return false;
        return true;
      })
    );
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(UserFormDialogComponent, {
      width: '540px',
      maxWidth: '94vw',
      data: {
        departments: this.departments(),
        teams: this.teams()
      }
    });
    dialogRef.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((result) => {
      if (result?.error) {
        this.snackBar.open(result.error, 'Close', { duration: 4000 });
      } else if (result === true) {
        this.snackBar.open('User created successfully', 'Close', { duration: 3000 });
        this.loadUsers();
      }
    });
  }

  openEditDialog(user: User): void {
    const dialogRef = this.dialog.open(UserFormDialogComponent, {
      width: '540px',
      maxWidth: '94vw',
      data: {
        user,
        departments: this.departments(),
        teams: this.teams()
      }
    });
    dialogRef.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((result) => {
      if (result?.error) {
        this.snackBar.open(result.error, 'Close', { duration: 4000 });
      } else if (result === true) {
        this.snackBar.open('User updated successfully', 'Close', { duration: 3000 });
        this.loadUsers();
      }
    });
  }

  toggleStatus(user: User): void {
    const newStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.userService.toggleStatus(user.id, newStatus).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.snackBar.open(`${user.fullName} set to ${newStatus}`, 'Close', { duration: 3000 });
        this.loadUsers();
      },
      error: (err) => this.snackBar.open(err.error?.message || 'Failed to update status', 'Close', { duration: 4000 })
    });
  }

  roleChips(roles: RoleType[]): string[] {
    return roles.map(r => r.replace('ROLE_', '').replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase()));
  }

  roleClass(role: RoleType): string {
    const lower = role.toLowerCase().replace('role_', '');
    if (lower.includes('super')) return 'role-super-admin';
    if (lower.includes('admin')) return 'role-admin';
    if (lower.includes('owner')) return 'role-owner';
    if (lower.includes('manager')) return 'role-pm';
    if (lower.includes('lead')) return 'role-lead';
    if (lower.includes('analyst')) return 'role-ba';
    if (lower.includes('qa')) return 'role-qa';
    return 'role-dev';
  }

  formatRole(role: string): string {
    return role
      .replace('ROLE_', '')
      .replace(/_/g, ' ')
      .replace(/\b\w/g, c => c.toUpperCase());
  }

  canManageUsers(): boolean {
    return this.authService.hasPermission('user:create');
  }
}