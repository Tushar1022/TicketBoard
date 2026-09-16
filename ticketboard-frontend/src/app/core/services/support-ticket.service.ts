import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  ApiResponse,
  SupportCategory,
  SupportStats,
  SupportTicket,
  TicketPriority,
  TicketStatus
} from '../models/api.models';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class SupportTicketService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/support/tickets';

  public myTickets = signal<SupportTicket[]>([]);
  public adminTickets = signal<SupportTicket[]>([]);
  public stats = signal<SupportStats | null>(null);
  public isLoading = signal<boolean>(false);
  public loadError = signal<string>('');

  public tickets = computed<SupportTicket[]>(() => {
    const merged = [...this.adminTickets(), ...this.myTickets()];
    const seen = new Set<number>();
    return merged.filter(t => {
      if (seen.has(t.id)) return false;
      seen.add(t.id);
      return true;
    });
  });

  public openTicketsCount = computed(() =>
    this.tickets().filter(t => t.status === 'OPEN' || t.status === 'IN_REVIEW').length
  );

  public urgentTicketsCount = computed(() =>
    this.tickets().filter(t => t.priority === 'URGENT' && t.status !== 'RESOLVED' && t.status !== 'CLOSED').length
  );

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  public isAdminUser(): boolean {
    const roles = this.authService.currentUser()?.roles || [];
    return roles.includes('ROLE_SUPER_ADMIN') || roles.includes('ROLE_ADMIN') || roles.includes('ROLE_PROJECT_MANAGER') || roles.includes('ROLE_TEAM_LEAD');
  }

  public refreshAll(): void {
    this.isLoading.set(true);
    this.loadError.set('');

    const admin$ = this.isAdminUser()
      ? this.http.get<ApiResponse<SupportTicket[]>>(`${this.baseUrl}/admin`)
      : of(null);

    const my$ = this.http.get<ApiResponse<SupportTicket[]>>(`${this.baseUrl}/my`);

    forkJoin({ mine: my$, admins: admin$ }).subscribe({
      next: (res) => {
        this.myTickets.set(res.mine?.data ?? []);
        this.adminTickets.set(Array.isArray(res.admins?.data) ? res.admins!.data : []);
        if (this.isAdminUser()) {
          this.refreshStats();
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.loadError.set(err?.message || 'Failed to load support tickets.');
      }
    });
  }

  public refreshStats(): void {
    this.http.get<ApiResponse<SupportStats>>(`${this.baseUrl}/admin/stats`).subscribe({
      next: (res) => {
        if (res.success && res.data) this.stats.set(res.data);
      },
      error: () => {
        this.stats.set(null);
      }
    });
  }

  public getMyTickets(): Observable<SupportTicket[]> {
    return this.http.get<ApiResponse<SupportTicket[]>>(`${this.baseUrl}/my`).pipe(map(res => res.data ?? []));
  }

  public getAdminQueue(): Observable<SupportTicket[]> {
    return this.http.get<ApiResponse<SupportTicket[]>>(`${this.baseUrl}/admin`).pipe(map(res => res.data ?? []));
  }

  public getTicketById(id: number): Observable<SupportTicket> {
    return this.http.get<ApiResponse<SupportTicket>>(`${this.baseUrl}/${id}`).pipe(map(res => res.data));
  }

  public getStats(): Observable<SupportStats> {
    return this.http.get<ApiResponse<SupportStats>>(`${this.baseUrl}/admin/stats`).pipe(map(res => res.data));
  }

  public createTicket(payload: {
    subject: string;
    category: SupportCategory;
    priority: TicketPriority;
    targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN';
    description: string;
    systemDiagnostics?: string;
    customCategoryName?: string;
    projectId?: number;
    projectName?: string;
    moduleName?: string;
  }): Observable<SupportTicket> {
    return this.http.post<ApiResponse<SupportTicket>>(this.baseUrl, payload).pipe(
      map(res => {
        this.refreshAll();
        return res.data;
      })
    );
  }

  public uploadAttachment(ticketId: number, file: File): Observable<SupportTicket> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<SupportTicket>>(`${this.baseUrl}/${ticketId}/attachments`, formData).pipe(
      map(res => {
        this.refreshAll();
        return res.data;
      })
    );
  }

  public updateTicketStatus(
    ticketId: number,
    status: TicketStatus,
    resolutionNotes?: string,
    adminName?: string
  ): Observable<SupportTicket | null> {
    const body: any = { status };
    if (resolutionNotes !== undefined) body.resolutionNotes = resolutionNotes;
    if (adminName) body.assignedToName = adminName;

    return this.http.put<ApiResponse<SupportTicket>>(`${this.baseUrl}/${ticketId}/status`, body).pipe(
      map(res => {
        this.refreshAll();
        return res.success ? res.data : null;
      })
    );
  }

  public addComment(ticketId: number, commentText: string): Observable<unknown> {
    return this.http.post<ApiResponse<unknown>>(`${this.baseUrl}/${ticketId}/comments`, { commentText }).pipe(
      map(res => {
        this.refreshAll();
        return res.data;
      })
    );
  }

  public getOpenCount(): Observable<number> {
    return this.http.get<ApiResponse<number>>(`${this.baseUrl}/open-count`).pipe(map(res => res.data ?? 0));
  }
}