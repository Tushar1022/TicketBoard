import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TimeTrackingService } from '../../core/services/timetracking.service';
import { AuthService } from '../../core/services/auth.service';
import { EffortVariance, TimeEntry, Timesheet } from '../../core/models/api.models';
import { LogTimeDialogComponent } from '../../shared/components/log-time-dialog/log-time-dialog.component';

@Component({
  selector: 'app-timetracking',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule, MatDialogModule],
  templateUrl: './timetracking.component.html',
  styleUrls: ['./timetracking.component.scss']
})
export class TimetrackingComponent implements OnInit {
  public timesheet = signal<Timesheet | null>(null);
  public pendingTimesheets = signal<Timesheet[]>([]);
  public variances = signal<EffortVariance[]>([]);
  public isLoading = signal<boolean>(true);
  public activeTab = signal<'WEEKLY' | 'PENDING' | 'VARIANCE'>('WEEKLY');

  public weeklyCapacity = computed(() => {
    const daily = this.authService.currentUser()?.dailyCapacityHours;
    return daily ? daily * 5 : null;
  });

  // Review Modal State
  public showReviewModal = signal<boolean>(false);
  public activeReviewTimesheet = signal<Timesheet | null>(null);
  public rejectionReason = signal<string>('');

  constructor(
    private timeTrackingService: TimeTrackingService,
    public authService: AuthService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadTimesheetData();
  }

  public setTab(tab: 'WEEKLY' | 'PENDING' | 'VARIANCE'): void {
    this.activeTab.set(tab);
    if (tab === 'PENDING') {
      this.loadPendingTimesheets();
    } else if (tab === 'VARIANCE') {
      this.loadVariances();
    }
  }

  public loadTimesheetData(): void {
    this.isLoading.set(true);
    this.timeTrackingService.getWeeklyTimesheet().subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.timesheet.set(res.data);
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  public loadPendingTimesheets(): void {
    this.timeTrackingService.getPendingTimesheets().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.pendingTimesheets.set(res.data);
        }
      }
    });
  }

  public loadVariances(): void {
    this.timeTrackingService.getEffortVariances().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.variances.set(res.data);
        }
      }
    });
  }

  public openLogTime(): void {
    const ref = this.dialog.open(LogTimeDialogComponent, { width: '560px' });
    ref.afterClosed().subscribe((saved: boolean) => {
      if (saved) {
        this.loadTimesheetData();
      }
    });
  }

  public submitCurrentTimesheet(): void {
    const sheet = this.timesheet();
    if (!sheet) return;

    this.timeTrackingService.submitTimesheet(sheet.id).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.loadTimesheetData();
        }
      }
    });
  }

  public approveTimesheet(sheet: Timesheet): void {
    this.timeTrackingService.reviewTimesheet(sheet.id, 'APPROVED').subscribe({
      next: (res: any) => {
        if (res.success) {
          this.loadPendingTimesheets();
        }
      }
    });
  }

  public openRejectModal(sheet: Timesheet): void {
    this.activeReviewTimesheet.set(sheet);
    this.rejectionReason.set('');
    this.showReviewModal.set(true);
  }

  public closeRejectModal(): void {
    this.showReviewModal.set(false);
    this.activeReviewTimesheet.set(null);
  }

  public confirmReject(): void {
    const sheet = this.activeReviewTimesheet();
    if (!sheet) return;

    this.timeTrackingService.reviewTimesheet(sheet.id, 'REJECTED', this.rejectionReason()).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeRejectModal();
          this.loadPendingTimesheets();
        }
      }
    });
  }
}
