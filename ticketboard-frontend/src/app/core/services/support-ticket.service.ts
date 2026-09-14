import { Injectable, signal, computed } from '@angular/core';
import { Observable, of } from 'rxjs';
import { SupportTicket, SupportCategory, TicketPriority, TicketStatus, TicketComment } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class SupportTicketService {
  private initialTickets: SupportTicket[] = [
    {
      id: 1001,
      ticketCode: 'SUP-1001',
      subject: 'Request for Super Admin Privileges — SBI CR Phase 2 Production Access',
      category: 'ACCESS_REQUEST',
      priority: 'HIGH',
      targetRole: 'ROLE_SUPER_ADMIN',
      status: 'OPEN',
      createdById: 4,
      createdByName: 'Tushar Shinde',
      createdByEmail: 'tushar.shinde@aurionpro.com',
      description: 'Need elevated Super Admin permissions to execute database telemetry resets and approve UAT exit criteria for SBI CR Project AP-586.',
      systemDiagnostics: 'Tenant: aurionpro | Project: SBI CR (AP-586) | OS: mac | Role: Project Owner',
      createdAt: '2026-09-14 09:30:00',
      updatedAt: '2026-09-14 09:30:00',
      comments: [
        {
          id: 1,
          ticketId: 1001,
          authorId: 4,
          authorName: 'Tushar Shinde',
          authorRole: 'Project Owner',
          commentText: 'Raised to Super Admin for priority escalation.',
          createdAt: '2026-09-14 09:35:00'
        }
      ]
    },
    {
      id: 1002,
      ticketCode: 'SUP-1002',
      subject: 'Jira Sync Discrepancy — SBI Task AP-586-121 status mismatch',
      category: 'SYSTEM_DEFECT',
      priority: 'URGENT',
      targetRole: 'ROLE_ADMIN',
      status: 'IN_REVIEW',
      createdById: 2,
      createdByName: 'Kunal Salve',
      createdByEmail: 'kunal.salve@aurionpro.com',
      assignedToId: 1,
      assignedToName: 'Ashwini Shinde (Admin)',
      description: 'The Jira synchronization webhook intermittently drops status updates for AP-586-121 when transitioning to UAT Exit.',
      resolutionNotes: 'Under investigation by Admin infrastructure team. Webhook listener restarted.',
      systemDiagnostics: 'Webhook Service: v2.4.1 | Host: telemetry.aurionpro.internal',
      createdAt: '2026-09-13 14:15:00',
      updatedAt: '2026-09-14 10:20:00',
      comments: []
    },
    {
      id: 1003,
      ticketCode: 'SUP-1003',
      subject: 'Capacity Planning Hours Over-Allocation Warning for BA Roster',
      category: 'DATA_QUERY',
      priority: 'MEDIUM',
      targetRole: 'ROLE_ADMIN',
      status: 'RESOLVED',
      createdById: 3,
      createdByName: 'Snehal Patil',
      createdByEmail: 'snehal.patil@aurionpro.com',
      assignedToId: 1,
      assignedToName: 'Ashwini Shinde (Admin)',
      description: 'Monthly capacity calculations for Business Analysts show 118% utilization. Requesting verification of baseline max monthly capacity.',
      resolutionNotes: 'Verified baseline monthly capacity is set to 160 hours per BA. Over-allocation was caused by concurrent SBI & HDFC change request tasks.',
      createdAt: '2026-09-12 11:00:00',
      updatedAt: '2026-09-13 16:45:00',
      comments: []
    }
  ];

  public tickets = signal<SupportTicket[]>(this.initialTickets);

  public openTicketsCount = computed(() =>
    this.tickets().filter(t => t.status === 'OPEN' || t.status === 'IN_REVIEW').length
  );

  public urgentTicketsCount = computed(() =>
    this.tickets().filter(t => t.priority === 'URGENT' && t.status !== 'RESOLVED' && t.status !== 'CLOSED').length
  );

  constructor() {}

  public getTickets(): Observable<SupportTicket[]> {
    return of(this.tickets());
  }

  public getTicketsForUser(userEmail: string): Observable<SupportTicket[]> {
    return of(this.tickets().filter(t => t.createdByEmail.toLowerCase() === userEmail.toLowerCase()));
  }

  public getTicketsForAdmin(): Observable<SupportTicket[]> {
    return of(this.tickets());
  }

  public createTicket(payload: {
    subject: string;
    category: SupportCategory;
    priority: TicketPriority;
    targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN';
    description: string;
    createdByName: string;
    createdByEmail: string;
    createdById?: number;
    systemDiagnostics?: string;
  }): Observable<SupportTicket> {
    const current = this.tickets();
    const nextId = current.length > 0 ? Math.max(...current.map(t => t.id)) + 1 : 1001;
    const newCode = `SUP-${nextId}`;

    const newTicket: SupportTicket = {
      id: nextId,
      ticketCode: newCode,
      subject: payload.subject,
      category: payload.category,
      priority: payload.priority,
      targetRole: payload.targetRole,
      status: 'OPEN',
      createdById: payload.createdById || 99,
      createdByName: payload.createdByName,
      createdByEmail: payload.createdByEmail,
      description: payload.description,
      systemDiagnostics: payload.systemDiagnostics || `Tenant: aurionpro | Workspace: SBI CR AP-586 | Timestamp: ${new Date().toISOString()}`,
      createdAt: new Date().toISOString().replace('T', ' ').substring(0, 19),
      updatedAt: new Date().toISOString().replace('T', ' ').substring(0, 19),
      comments: []
    };

    this.tickets.update(list => [newTicket, ...list]);
    return of(newTicket);
  }

  public updateTicketStatus(ticketId: number, status: TicketStatus, resolutionNotes?: string, adminName?: string): Observable<SupportTicket | null> {
    let updatedTicket: SupportTicket | null = null;
    this.tickets.update(list =>
      list.map(t => {
        if (t.id === ticketId) {
          updatedTicket = {
            ...t,
            status,
            resolutionNotes: resolutionNotes !== undefined ? resolutionNotes : t.resolutionNotes,
            assignedToName: adminName || t.assignedToName || 'Super Admin',
            updatedAt: new Date().toISOString().replace('T', ' ').substring(0, 19)
          };
          return updatedTicket;
        }
        return t;
      })
    );
    return of(updatedTicket);
  }

  public addComment(ticketId: number, authorName: string, authorRole: string, text: string): Observable<TicketComment> {
    const newComment: TicketComment = {
      id: Date.now(),
      ticketId,
      authorId: 99,
      authorName,
      authorRole,
      commentText: text,
      createdAt: new Date().toISOString().replace('T', ' ').substring(0, 19)
    };

    this.tickets.update(list =>
      list.map(t => {
        if (t.id === ticketId) {
          const comments = t.comments ? [...t.comments, newComment] : [newComment];
          return { ...t, comments, updatedAt: new Date().toISOString().replace('T', ' ').substring(0, 19) };
        }
        return t;
      })
    );

    return of(newComment);
  }
}
