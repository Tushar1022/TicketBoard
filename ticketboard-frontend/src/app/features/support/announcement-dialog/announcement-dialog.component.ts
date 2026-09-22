import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { AnnouncementType, SupportAnnouncement } from '../../../core/models/api.models';
import { SupportTicketService } from '../../../core/services/support-ticket.service';

@Component({
  selector: 'app-announcement-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatDialogModule],
  template: `
    <div class="announcement-dialog">
      <div class="dialog-header">
        <div class="header-title">
          <mat-icon class="header-icon">campaign</mat-icon>
          <div>
            <h3>{{ isEditMode ? 'Manage Support Announcement' : 'Post Support Notice' }}</h3>
            <p class="dialog-subtitle">Configure system notice for long-term support, governance, and maintenance</p>
          </div>
        </div>
        <button type="button" class="close-btn" (click)="closeDialog()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <div class="dialog-body">
        <div *ngIf="errorMessage" class="error-banner">
          <mat-icon>error_outline</mat-icon>
          <span>{{ errorMessage }}</span>
        </div>

        <div class="form-group">
          <label>Announcement Type <span class="required">*</span></label>
          <div class="type-selector">
            <button
              type="button"
              class="type-btn"
              [class.selected]="type === 'LONG_TERM_SUPPORT'"
              (click)="type = 'LONG_TERM_SUPPORT'"
            >
              <mat-icon>verified</mat-icon>
              <span>Long-Term Support</span>
            </button>

            <button
              type="button"
              class="type-btn"
              [class.selected]="type === 'MAINTENANCE'"
              (click)="type = 'MAINTENANCE'"
            >
              <mat-icon>build</mat-icon>
              <span>Maintenance</span>
            </button>

            <button
              type="button"
              class="type-btn"
              [class.selected]="type === 'ADVISORY'"
              (click)="type = 'ADVISORY'"
            >
              <mat-icon>warning_amber</mat-icon>
              <span>Advisory</span>
            </button>

            <button
              type="button"
              class="type-btn"
              [class.selected]="type === 'INFO'"
              (click)="type = 'INFO'"
            >
              <mat-icon>info</mat-icon>
              <span>General Info</span>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label for="noticeTitle">Notice Title <span class="required">*</span></label>
          <input
            id="noticeTitle"
            type="text"
            [(ngModel)]="title"
            placeholder="e.g. Long-Term Support & Governance Notice (v2.5)"
            class="form-control"
          />
        </div>

        <div class="form-group">
          <label for="noticeMessage">Notice Message & SLA Details <span class="required">*</span></label>
          <textarea
            id="noticeMessage"
            rows="4"
            [(ngModel)]="message"
            placeholder="Provide detailed support instructions, SLA timelines, or maintenance windows..."
            class="form-control"
          ></textarea>
        </div>

        <div class="form-group-checkbox">
          <label class="checkbox-container">
            <input type="checkbox" [(ngModel)]="active" />
            <span class="checkmark"></span>
            <span class="label-text">Publish as active announcement immediately</span>
          </label>
        </div>
      </div>

      <div class="dialog-actions">
        <button type="button" class="btn btn-secondary" (click)="closeDialog()">Cancel</button>
        <button type="button" class="btn btn-primary" [disabled]="isSubmitting" (click)="submitAnnouncement()">
          <mat-icon *ngIf="!isSubmitting">publish</mat-icon>
          <span *ngIf="!isSubmitting">{{ isEditMode ? 'Update Announcement' : 'Post Announcement' }}</span>
          <span *ngIf="isSubmitting">Saving...</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .announcement-dialog {
      padding: 24px;
      font-family: inherit;
      background: var(--surface-card, #ffffff);
      color: var(--text-main, #0f172a);
      border-radius: 12px;
    }

    .dialog-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--surface-border, #e2e8f0);
      margin-bottom: 20px;

      .header-title {
        display: flex;
        align-items: center;
        gap: 12px;

        .header-icon {
          font-size: 28px;
          width: 28px;
          height: 28px;
          color: var(--primary-600, #4f46e5);
        }

        h3 {
          margin: 0;
          font-size: 1.15rem;
          font-weight: 700;
          color: var(--text-main, #0f172a);
        }

        .dialog-subtitle {
          margin: 2px 0 0 0;
          font-size: 0.8rem;
          color: var(--text-muted, #64748b);
        }
      }

      .close-btn {
        background: transparent;
        border: none;
        color: var(--text-muted, #64748b);
        cursor: pointer;
        padding: 4px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.15s;

        &:hover {
          background: var(--surface-subtle, #f1f5f9);
          color: var(--text-main, #0f172a);
        }
      }
    }

    .dialog-body {
      display: flex;
      flex-direction: column;
      gap: 18px;

      .error-banner {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 14px;
        background: rgba(239, 68, 68, 0.08);
        border: 1px solid rgba(239, 68, 68, 0.2);
        border-radius: 8px;
        color: #dc2626;
        font-size: 0.82rem;

        mat-icon { font-size: 18px; width: 18px; height: 18px; }
      }

      .form-group {
        display: flex;
        flex-direction: column;
        gap: 6px;

        label {
          font-size: 0.82rem;
          font-weight: 600;
          color: var(--text-main, #334155);

          .required { color: #dc2626; }
        }

        .form-control {
          width: 100%;
          padding: 10px 12px;
          border-radius: 8px;
          border: 1px solid var(--surface-border, #cbd5e1);
          background: var(--surface-card, #ffffff);
          color: var(--text-main, #0f172a);
          font-size: 0.88rem;
          font-family: inherit;
          transition: border-color 0.15s, box-shadow 0.15s;

          &:focus {
            outline: none;
            border-color: var(--primary-500, #6366f1);
            box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
          }
        }
      }

      .type-selector {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 10px;

        .type-btn {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 10px 12px;
          border-radius: 8px;
          border: 1px solid var(--surface-border, #cbd5e1);
          background: var(--surface-card, #ffffff);
          color: var(--text-muted, #475569);
          font-size: 0.82rem;
          font-weight: 600;
          font-family: inherit;
          cursor: pointer;
          transition: all 0.15s ease;

          mat-icon { font-size: 18px; width: 18px; height: 18px; }

          &:hover {
            border-color: var(--primary-400, #818cf8);
            background: var(--surface-subtle, #f8fafc);
          }

          &.selected {
            background: rgba(79, 70, 229, 0.08);
            border-color: var(--primary-600, #4f46e5);
            color: var(--primary-700, #4338ca);
          }
        }
      }

      .form-group-checkbox {
        .checkbox-container {
          display: inline-flex;
          align-items: center;
          gap: 10px;
          cursor: pointer;
          user-select: none;
          font-size: 0.84rem;
          color: var(--text-main, #334155);

          input { cursor: pointer; }
        }
      }
    }

    .dialog-actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid var(--surface-border, #e2e8f0);

      .btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 8px 16px;
        border-radius: 8px;
        font-size: 0.84rem;
        font-weight: 600;
        cursor: pointer;
        font-family: inherit;
        border: 1px solid transparent;
        transition: all 0.15s ease;

        mat-icon { font-size: 18px; width: 18px; height: 18px; }

        &.btn-secondary {
          background: var(--surface-subtle, #f1f5f9);
          color: var(--text-muted, #475569);
          border-color: var(--surface-border, #cbd5e1);

          &:hover { background: #e2e8f0; color: #0f172a; }
        }

        &.btn-primary {
          background: var(--primary-600, #4f46e5);
          color: #ffffff;

          &:hover { background: var(--primary-700, #4338ca); }

          &:disabled {
            opacity: 0.6;
            cursor: not-allowed;
          }
        }
      }
    }
  `]
})
export class AnnouncementDialogComponent implements OnInit {
  public title = '';
  public message = '';
  public type: AnnouncementType = 'LONG_TERM_SUPPORT';
  public active = true;
  public isSubmitting = false;
  public errorMessage = '';
  public isEditMode = false;

  constructor(
    private dialogRef: MatDialogRef<AnnouncementDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { announcement?: SupportAnnouncement } | null,
    private supportService: SupportTicketService
  ) {}

  ngOnInit(): void {
    if (this.data?.announcement) {
      this.isEditMode = true;
      this.title = this.data.announcement.title;
      this.message = this.data.announcement.message;
      this.type = this.data.announcement.type;
      this.active = this.data.announcement.active;
    }
  }

  public closeDialog(result?: SupportAnnouncement): void {
    this.dialogRef.close(result);
  }

  public submitAnnouncement(): void {
    if (!this.title.trim()) {
      this.errorMessage = 'Please enter a title for the announcement.';
      return;
    }
    if (!this.message.trim()) {
      this.errorMessage = 'Please enter announcement details or SLA information.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const payload = {
      title: this.title.trim(),
      message: this.message.trim(),
      type: this.type,
      active: this.active
    };

    const req$ = (this.isEditMode && this.data?.announcement)
      ? this.supportService.updateAnnouncement(this.data.announcement.id, payload)
      : this.supportService.createAnnouncement(payload);

    req$.subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.closeDialog(res);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err?.error?.message || 'Failed to save announcement.';
      }
    });
  }
}
