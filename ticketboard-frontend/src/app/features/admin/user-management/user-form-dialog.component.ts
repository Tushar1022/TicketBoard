import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { User, RoleType } from '../../../core/models/api.models';
import { UserService } from '../../../core/services/user.service';

export interface UserFormData {
  user?: User;
  departments: { id: number; name: string }[];
  teams: { id: number; name: string }[];
}

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatChipsModule,
    MatIconModule
  ],
  template: `
    <div class="user-dialog-header">
      <div class="title-with-icon">
        <mat-icon color="primary">{{ data.user ? 'manage_accounts' : 'person_add' }}</mat-icon>
        <h2>{{ data.user ? 'Edit User Profile' : 'Create New User' }}</h2>
      </div>
      <button mat-icon-button mat-dialog-close class="close-btn"><mat-icon>close</mat-icon></button>
    </div>

    <mat-dialog-content class="user-form-content">
      <div class="compact-form-grid">
        <div class="form-item">
          <label>Employee ID</label>
          <input type="text" class="mini-input" [(ngModel)]="form.employeeId" name="employeeId" placeholder="e.g. EMP-1010" />
        </div>

        <div class="form-item">
          <label>First Name *</label>
          <input type="text" class="mini-input" [(ngModel)]="form.firstName" name="firstName" placeholder="First Name" required />
        </div>

        <div class="form-item">
          <label>Last Name *</label>
          <input type="text" class="mini-input" [(ngModel)]="form.lastName" name="lastName" placeholder="Last Name" required />
        </div>

        <div class="form-item">
          <label>Email Address *</label>
          <input type="email" class="mini-input" [(ngModel)]="form.email" name="email" placeholder="user@company.com" required />
        </div>

        <div class="form-item" *ngIf="!data.user">
          <label>Password</label>
          <input type="password" class="mini-input" [(ngModel)]="form.password" name="password" placeholder="Default password if blank" />
        </div>

        <div class="form-item">
          <label>Phone Number</label>
          <input type="text" class="mini-input" [(ngModel)]="form.phone" name="phone" placeholder="+91-XXXXXXXXXX" />
        </div>

        <div class="form-item">
          <label>Designation</label>
          <input type="text" class="mini-input" [(ngModel)]="form.designation" name="designation" placeholder="e.g. Senior Software Engineer" />
        </div>

        <div class="form-item">
          <label>Department</label>
          <select class="mini-select" [(ngModel)]="form.departmentId" name="departmentId">
            <option [ngValue]="null">Select Department...</option>
            <option *ngFor="let d of data.departments" [ngValue]="d.id">{{ d.name }}</option>
          </select>
        </div>

        <div class="form-item">
          <label>Assigned Team</label>
          <select class="mini-select" [(ngModel)]="form.teamId" name="teamId">
            <option [ngValue]="null">Select Team...</option>
            <option *ngFor="let t of data.teams" [ngValue]="t.id">{{ t.name }}</option>
          </select>
        </div>

        <div class="form-item">
          <label>Account Status</label>
          <select class="mini-select" [(ngModel)]="form.status" name="status">
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="LOCKED">Locked</option>
          </select>
        </div>

        <div class="form-item">
          <label>Daily Capacity (Hrs)</label>
          <input type="number" class="mini-input" [(ngModel)]="form.dailyCapacityHours" name="dailyCapacityHours" />
        </div>
      </div>

      <div class="roles-section">
        <span class="roles-title">Assigned Security Roles</span>
        <div class="role-grid">
          <label *ngFor="let role of allRoles" class="role-pill" [class.selected]="form.roles?.includes(role)">
            <input
              type="checkbox"
              [checked]="form.roles?.includes(role)"
              (change)="onRoleToggle(role, $event)"
            />
            <span>{{ roleLabel(role) }}</span>
          </label>
        </div>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button mat-dialog-close class="mini-btn">Cancel</button>
      <button mat-raised-button color="primary" class="mini-btn primary" [disabled]="!isValid()" (click)="save()">Save User</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .user-dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.85rem 1.25rem;
      border-bottom: 1px solid #e2e8f0;
      background: #f8fafc;

      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 0.5rem;

        mat-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
        }

        h2 {
          margin: 0;
          font-size: 1.05rem;
          font-weight: 700;
          color: #0f172a;
        }
      }

      .close-btn {
        width: 28px;
        height: 28px;
        line-height: 28px;
        mat-icon { font-size: 16px; width: 16px; height: 16px; }
      }
    }

    .user-form-content {
      padding: 1rem 1.25rem;
      max-width: 620px;
    }

    .compact-form-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.65rem 0.85rem;

      .form-item {
        display: flex;
        flex-direction: column;
        gap: 0.2rem;

        label {
          font-size: 0.75rem;
          font-weight: 600;
          color: #475569;
        }

        .mini-input, .mini-select {
          height: 30px;
          padding: 0 0.6rem;
          border-radius: 6px;
          border: 1px solid #cbd5e1;
          font-size: 0.8rem;
          color: #0f172a;
          background: #ffffff;
          outline: none;
          transition: border-color 0.15s;

          &:focus {
            border-color: #3b82f6;
            box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
          }
        }
      }
    }

    .roles-section {
      margin-top: 0.85rem;
      padding-top: 0.75rem;
      border-top: 1px dashed #e2e8f0;

      .roles-title {
        font-size: 0.725rem;
        font-weight: 700;
        color: #64748b;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        display: block;
        margin-bottom: 0.5rem;
      }

      .role-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 0.35rem 0.5rem;

        .role-pill {
          display: inline-flex;
          align-items: center;
          gap: 0.35rem;
          padding: 0.25rem 0.55rem;
          border-radius: 6px;
          border: 1px solid #cbd5e1;
          background: #f8fafc;
          font-size: 0.75rem;
          font-weight: 500;
          color: #334155;
          cursor: pointer;

          input[type="checkbox"] {
            margin: 0;
            width: 13px;
            height: 13px;
            cursor: pointer;
          }

          &.selected {
            background: #eff6ff;
            border-color: #93c5fd;
            color: #1d4ed8;
            font-weight: 600;
          }
        }
      }
    }

    .dialog-actions {
      padding: 0.65rem 1.25rem;
      border-top: 1px solid #e2e8f0;
      margin: 0;

      .mini-btn {
        height: 32px;
        line-height: 32px;
        font-size: 0.775rem;
        padding: 0 0.85rem;
      }
    }
  `]
})
export class UserFormDialogComponent implements OnInit {
  public form: any = {};
  public allRoles: RoleType[] = [
    'ROLE_SUPER_ADMIN',
    'ROLE_ADMIN',
    'ROLE_PROJECT_OWNER',
    'ROLE_PROJECT_MANAGER',
    'ROLE_BUSINESS_ANALYST',
    'ROLE_DEVELOPER',
    'ROLE_QA_TESTER',
    'ROLE_TEAM_LEAD',
    'ROLE_MANAGEMENT'
  ];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: UserFormData,
    private dialogRef: MatDialogRef<UserFormDialogComponent>,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    if (this.data.user) {
      this.form = {
        employeeId: this.data.user.employeeId,
        firstName: this.data.user.firstName,
        lastName: this.data.user.lastName,
        email: this.data.user.email,
        phone: this.data.user.phone || '',
        designation: this.data.user.designation || '',
        departmentId: this.data.user.departmentId || null,
        teamId: this.data.user.teamId || null,
        status: this.data.user.status,
        dailyCapacityHours: this.data.user.dailyCapacityHours,
        roles: [...this.data.user.roles]
      };
    } else {
      this.form = {
        employeeId: '',
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        phone: '',
        designation: '',
        departmentId: null,
        teamId: null,
        status: 'ACTIVE',
        dailyCapacityHours: 8,
        roles: []
      };
    }
  }

  onRoleToggle(role: RoleType, event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.form.roles = this.form.roles || [];
    if (isChecked) {
      if (!this.form.roles.includes(role)) {
        this.form.roles.push(role);
      }
    } else {
      this.form.roles = this.form.roles.filter((r: RoleType) => r !== role);
    }
  }

  roleLabel(role: RoleType): string {
    return role.replace('ROLE_', '').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }

  isValid(): boolean {
    return !!this.form.firstName && !!this.form.lastName && !!this.form.email;
  }

  save(): void {
    const payload = { ...this.form };
    if (this.data.user) {
      delete payload.password;
      this.userService.updateUser(this.data.user.id, payload).subscribe({
        next: () => this.dialogRef.close(true),
        error: (err) => this.dialogRef.close({ error: err.error?.message || 'Failed to update user' })
      });
    } else {
      this.userService.createUser(payload).subscribe({
        next: () => this.dialogRef.close(true),
        error: (err) => this.dialogRef.close({ error: err.error?.message || 'Failed to create user' })
      });
    }
  }
}