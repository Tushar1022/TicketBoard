import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  ApiResponse,
  DirectoryContact,
  EmailMessage,
  FolderCounts,
  MailAttachmentRef,
  MailFolder,
  SendEmailRequestPayload,
  SmtpConfig
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class MailService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/mail';

  public messages = signal<EmailMessage[]>([]);
  public activeFolder = signal<MailFolder>('INBOX');
  public counts = signal<FolderCounts | null>(null);
  public selectedMessage = signal<EmailMessage | null>(null);
  public smtpConfig = signal<SmtpConfig | null>(null);
  public isLoading = signal<boolean>(false);

  constructor(private http: HttpClient) {}

  public loadMessages(folder: MailFolder = 'INBOX', search?: string, starredOnly: boolean = false): void {
    this.isLoading.set(true);
    this.activeFolder.set(folder);

    let params = new HttpParams().set('folder', folder);
    if (search && search.trim()) params = params.set('search', search.trim());
    if (starredOnly) params = params.set('starred', 'true');

    this.http.get<ApiResponse<EmailMessage[]>>(`${this.baseUrl}/messages`, { params }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res && res.data) {
          this.messages.set(res.data);
          if (res.data.length > 0 && !this.selectedMessage()) {
            this.selectedMessage.set(res.data[0]);
          } else if (res.data.length === 0) {
            this.selectedMessage.set(null);
          }
        }
      },
      error: () => this.isLoading.set(false)
    });

    this.refreshCounts();
  }

  public getMessageById(id: number): Observable<ApiResponse<EmailMessage>> {
    return this.http.get<ApiResponse<EmailMessage>>(`${this.baseUrl}/messages/${id}`).pipe(
      tap((res) => {
        if (res && res.data) {
          this.selectedMessage.set(res.data);
          this.refreshCounts();
        }
      })
    );
  }

  public sendEmail(payload: SendEmailRequestPayload): Observable<ApiResponse<EmailMessage>> {
    return this.http.post<ApiResponse<EmailMessage>>(`${this.baseUrl}/send`, payload).pipe(
      tap(() => {
        this.loadMessages(this.activeFolder());
      })
    );
  }

  public saveDraft(payload: SendEmailRequestPayload): Observable<ApiResponse<EmailMessage>> {
    return this.http.post<ApiResponse<EmailMessage>>(`${this.baseUrl}/drafts`, payload).pipe(
      tap(() => {
        this.loadMessages('DRAFTS');
      })
    );
  }

  public updateFolder(id: number, folder: MailFolder): Observable<ApiResponse<EmailMessage>> {
    return this.http.patch<ApiResponse<EmailMessage>>(`${this.baseUrl}/messages/${id}/folder`, { folder }).pipe(
      tap(() => {
        this.loadMessages(this.activeFolder());
      })
    );
  }

  public toggleStar(id: number): Observable<ApiResponse<EmailMessage>> {
    return this.http.patch<ApiResponse<EmailMessage>>(`${this.baseUrl}/messages/${id}/star`, {}).pipe(
      tap((res) => {
        if (res && res.data) {
          const updated = this.messages().map((m) => (m.id === id ? { ...m, starred: res.data.starred } : m));
          this.messages.set(updated);
          if (this.selectedMessage()?.id === id) {
            this.selectedMessage.set({ ...this.selectedMessage()!, starred: res.data.starred });
          }
          this.refreshCounts();
        }
      })
    );
  }

  public toggleRead(id: number): Observable<ApiResponse<EmailMessage>> {
    return this.http.patch<ApiResponse<EmailMessage>>(`${this.baseUrl}/messages/${id}/read`, {}).pipe(
      tap((res) => {
        if (res && res.data) {
          const updated = this.messages().map((m) => (m.id === id ? { ...m, read: res.data.read } : m));
          this.messages.set(updated);
          if (this.selectedMessage()?.id === id) {
            this.selectedMessage.set({ ...this.selectedMessage()!, read: res.data.read });
          }
          this.refreshCounts();
        }
      })
    );
  }

  public deleteMessage(id: number): Observable<ApiResponse<string>> {
    return this.http.delete<ApiResponse<string>>(`${this.baseUrl}/messages/${id}`).pipe(
      tap(() => {
        this.loadMessages(this.activeFolder());
      })
    );
  }

  public refreshCounts(): void {
    this.http.get<ApiResponse<FolderCounts>>(`${this.baseUrl}/counts`).subscribe({
      next: (res) => {
        if (res && res.data) {
          this.counts.set(res.data);
        }
      }
    });
  }

  public getSmtpConfig(): Observable<ApiResponse<SmtpConfig>> {
    return this.http.get<ApiResponse<SmtpConfig>>(`${this.baseUrl}/smtp-config`).pipe(
      tap((res) => {
        if (res && res.data) this.smtpConfig.set(res.data);
      })
    );
  }

  public saveSmtpConfig(config: SmtpConfig): Observable<ApiResponse<SmtpConfig>> {
    return this.http.put<ApiResponse<SmtpConfig>>(`${this.baseUrl}/smtp-config`, config).pipe(
      tap((res) => {
        if (res && res.data) this.smtpConfig.set(res.data);
      })
    );
  }

  public testSmtpConfig(testRecipient: string, config?: SmtpConfig): Observable<ApiResponse<{ success: boolean }>> {
    const payload = {
      testRecipient,
      smtpHost: config?.smtpHost,
      smtpPort: config?.smtpPort,
      username: config?.username,
      password: config?.password,
      encryptionType: config?.encryptionType
    };
    return this.http.post<ApiResponse<{ success: boolean }>>(`${this.baseUrl}/smtp-config/test`, payload);
  }

  public uploadAttachment(file: File): Observable<ApiResponse<MailAttachmentRef>> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<ApiResponse<MailAttachmentRef>>(`${this.baseUrl}/attachments`, formData);
  }

  public getDirectory(): Observable<ApiResponse<DirectoryContact[]>> {
    return this.http.get<ApiResponse<DirectoryContact[]>>(`http://localhost:8080/api/v1/users`);
  }

  public downloadAttachment(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/attachments/${id}/download`, { responseType: 'blob' });
  }
}
