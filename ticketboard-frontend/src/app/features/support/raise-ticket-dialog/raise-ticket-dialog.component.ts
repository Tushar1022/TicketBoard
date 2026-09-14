import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { SupportTicketService } from '../../../core/services/support-ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { SupportCategory, SupportTicket, TicketPriority } from '../../../core/models/api.models';

@Component({
  selector: 'app-raise-ticket-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatIconModule],
  templateUrl: './raise-ticket-dialog.component.html',
  styleUrls: ['./raise-ticket-dialog.component.scss']
})
export class RaiseTicketDialogComponent implements OnInit {
  public subject = '';
  public category: SupportCategory = 'SYSTEM_DEFECT';
  public priority: TicketPriority = 'HIGH';
  public targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN' = 'ROLE_SUPER_ADMIN';
  public description = '';
  public includeDiagnostics = true;

  public isSubmitting = signal<boolean>(false);
  public successMessage = signal<string>('');
  public errorMessage = signal<string>('');

  constructor(
    public dialogRef: MatDialogRef<RaiseTicketDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private supportTicketService: SupportTicketService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (this.data?.targetRole) {
      this.targetRole = this.data.targetRole;
    }
  }

  public submitTicket(): void {
    if (!this.subject.trim()) {
      this.errorMessage.set('Subject is required.');
      return;
    }
    if (!this.description.trim()) {
      this.errorMessage.set('Description is required.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const user = this.authService.currentUser();

    const diagnostics = this.includeDiagnostics
      ? `Tenant: ${user?.email?.split('@')[1]?.split('.')[0] || 'Unknown'} | User: ${user?.fullName} (${user?.email}) | OS: ${navigator.platform} | Time: ${new Date().toLocaleString()}`
      : 'Diagnostics omitted by user.';

    this.supportTicketService.createTicket({
      subject: this.subject.trim(),
      category: this.category,
      priority: this.priority,
      targetRole: this.targetRole,
      description: this.description.trim(),
      createdByName: user?.fullName || 'Unknown User',
      createdByEmail: user?.email || '',
      createdById: user?.id || 0,
      systemDiagnostics: diagnostics
    }).subscribe({
      next: (ticket: SupportTicket) => {
        this.isSubmitting.set(false);
        this.successMessage.set(`Support ticket ${ticket.ticketCode} raised successfully!`);
        setTimeout(() => {
          this.dialogRef.close(ticket);
        }, 1200);
      },
      error: (err: any) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.message || 'Failed to submit support ticket.');
      }
    });
  }

  public cancel(): void {
    this.dialogRef.close();
  }
}
