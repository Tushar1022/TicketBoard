import { Component, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { AppNotification } from '../../../core/models/api.models';
import { AppNotificationService } from '../../../core/services/notification.service';
import { ToastService } from '../toast/toast.service';

@Component({
  selector: 'app-notification-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatIconModule, MatButtonModule],
  templateUrl: './notification-detail-dialog.component.html',
  styleUrls: ['./notification-detail-dialog.component.scss']
})
export class NotificationDetailDialogComponent {
  public notification = signal<AppNotification>(this.data.notification);
  public isUpdating = signal<boolean>(false);

  constructor(
    public dialogRef: MatDialogRef<NotificationDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { notification: AppNotification },
    private notificationService: AppNotificationService,
    private toastService: ToastService,
    private router: Router
  ) {
    if (!this.notification().read) {
      this.markRead();
    }
  }

  public getIcon(type: string): string {
    switch (type) {
      case 'ALERT': return 'warning';
      case 'TASK': return 'task_alt';
      case 'ERP': return 'account_balance_wallet';
      case 'SECURITY': return 'security';
      case 'MENTION': return 'alternate_email';
      case 'SYSTEM': default: return 'dns';
    }
  }

  public getBadgeClass(type: string): string {
    switch (type) {
      case 'ALERT': return 'badge-red';
      case 'TASK': return 'badge-indigo';
      case 'ERP': return 'badge-emerald';
      case 'SECURITY': return 'badge-amber';
      case 'MENTION': return 'badge-purple';
      case 'SYSTEM': default: return 'badge-slate';
    }
  }

  public getPriorityClass(priority?: string): string {
    switch (priority) {
      case 'URGENT': return 'priority-urgent';
      case 'HIGH': return 'priority-high';
      case 'MEDIUM': return 'priority-medium';
      case 'LOW': default: return 'priority-low';
    }
  }

  public markRead(): void {
    const id = this.notification().id;
    this.isUpdating.set(true);
    this.notificationService.markAsRead(id).subscribe({
      next: (res) => {
        this.isUpdating.set(false);
        if (res && res.data) {
          this.notification.set(res.data);
        }
      },
      error: () => this.isUpdating.set(false)
    });
  }

  public deleteNotification(): void {
    const id = this.notification().id;
    this.isUpdating.set(true);
    this.notificationService.deleteNotification(id).subscribe({
      next: () => {
        this.isUpdating.set(false);
        this.toastService.success('Notification deleted.');
        this.dialogRef.close(true);
      },
      error: () => this.isUpdating.set(false)
    });
  }

  public navigateAction(): void {
    const url = this.notification().actionUrl;
    if (url) {
      this.dialogRef.close();
      this.router.navigateByUrl(url);
    }
  }

  public close(): void {
    this.dialogRef.close();
  }
}
