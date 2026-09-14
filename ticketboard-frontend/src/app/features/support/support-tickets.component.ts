import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { SupportTicketService } from '../../core/services/support-ticket.service';
import { AuthService } from '../../core/services/auth.service';
import { SupportTicket, TicketStatus } from '../../core/models/api.models';
import { RaiseTicketDialogComponent } from './raise-ticket-dialog/raise-ticket-dialog.component';

@Component({
  selector: 'app-support-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatIconModule, MatDialogModule],
  templateUrl: './support-tickets.component.html',
  styleUrls: ['./support-tickets.component.scss']
})
export class SupportTicketsComponent implements OnInit {
  public activeTab = signal<'MY_TICKETS' | 'ADMIN_QUEUE'>('MY_TICKETS');
  public searchQuery = signal<string>('');
  public selectedStatus = signal<string>('ALL');

  public tickets = signal<SupportTicket[]>([]);
  public selectedTicket = signal<SupportTicket | null>(null);

  // Admin resolution form inputs
  public resolutionNotesInput = '';
  public statusUpdateInput: TicketStatus = 'RESOLVED';
  public commentInput = '';

  public currentUser = computed(() => this.authService.currentUser());

  public isAdminUser = computed(() => {
    const roles = this.currentUser()?.roles || [];
    return roles.includes('ROLE_SUPER_ADMIN') || roles.includes('ROLE_ADMIN');
  });

  public isSuperAdmin = computed(() => {
    return (this.currentUser()?.roles || []).includes('ROLE_SUPER_ADMIN');
  });

  constructor(
    public supportTicketService: SupportTicketService,
    public authService: AuthService,
    private dialog: MatDialog,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Check query params or route to default to admin queue if requested
    this.route.queryParams.subscribe(params => {
      if (params['view'] === 'admin' && this.isAdminUser()) {
        this.activeTab.set('ADMIN_QUEUE');
      }
    });

    this.loadTickets();
  }

  public loadTickets(): void {
    this.supportTicketService.getTickets().subscribe(list => {
      this.tickets.set(list);
      if (list.length > 0 && !this.selectedTicket()) {
        this.selectedTicket.set(list[0]);
      }
    });
  }

  public filteredTickets(): SupportTicket[] {
    const userEmail = (this.currentUser()?.email || '').toLowerCase();
    const tab = this.activeTab();
    const q = this.searchQuery().toLowerCase().trim();
    const status = this.selectedStatus();

    return this.tickets().filter(t => {
      // Tab filter
      const matchesTab = tab === 'ADMIN_QUEUE' ? true : t.createdByEmail.toLowerCase() === userEmail;

      // Status filter
      const matchesStatus = status === 'ALL' || t.status === status;

      // Search filter
      const matchesSearch =
        !q ||
        t.ticketCode.toLowerCase().includes(q) ||
        t.subject.toLowerCase().includes(q) ||
        t.createdByName.toLowerCase().includes(q) ||
        t.category.toLowerCase().includes(q);

      return matchesTab && matchesStatus && matchesSearch;
    });
  }

  public selectTicket(ticket: SupportTicket): void {
    this.selectedTicket.set(ticket);
    this.resolutionNotesInput = ticket.resolutionNotes || '';
    this.statusUpdateInput = ticket.status;
  }

  public openRaiseTicketDialog(targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN' = 'ROLE_SUPER_ADMIN'): void {
    const dialogRef = this.dialog.open(RaiseTicketDialogComponent, {
      width: '680px',
      data: { targetRole }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) {
        this.loadTickets();
        this.selectedTicket.set(res);
      }
    });
  }

  public updateTicketStatus(): void {
    const ticket = this.selectedTicket();
    if (!ticket) return;

    const adminName = `${this.currentUser()?.fullName} (${this.isSuperAdmin() ? 'Super Admin' : 'Admin'})`;

    this.supportTicketService.updateTicketStatus(
      ticket.id,
      this.statusUpdateInput,
      this.resolutionNotesInput.trim(),
      adminName
    ).subscribe(updated => {
      if (updated) {
        this.loadTickets();
        this.selectedTicket.set(updated);
      }
    });
  }

  public addComment(): void {
    const ticket = this.selectedTicket();
    if (!ticket || !this.commentInput.trim()) return;

    const user = this.currentUser();
    const roleName = this.isSuperAdmin() ? 'Super Admin' : (this.isAdminUser() ? 'Admin' : 'User');

    this.supportTicketService.addComment(
      ticket.id,
      user?.fullName || 'User',
      roleName,
      this.commentInput.trim()
    ).subscribe(() => {
      this.commentInput = '';
      this.loadTickets();
      // Keep selected ticket updated
      const updated = this.tickets().find(t => t.id === ticket.id);
      if (updated) this.selectedTicket.set(updated);
    });
  }
}
