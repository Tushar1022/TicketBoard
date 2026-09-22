import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { ApiResponse, AppNotification } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class AppNotificationService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/notifications';

  public notifications = signal<AppNotification[]>([]);
  public unreadCount = signal<number>(0);
  public isLoading = signal<boolean>(false);

  constructor(private http: HttpClient) {
    this.loadNotifications();
  }

  public loadNotifications(): void {
    this.isLoading.set(true);
    this.http.get<ApiResponse<AppNotification[]>>(this.baseUrl).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res && res.data) {
          this.notifications.set(res.data);
          this.recalculateUnreadCount();
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  public markAsRead(id: number): Observable<ApiResponse<AppNotification>> {
    return this.http.patch<ApiResponse<AppNotification>>(`${this.baseUrl}/${id}/read`, {}).pipe(
      tap((res) => {
        if (res && res.data) {
          const updatedList = this.notifications().map((n) => (n.id === id ? { ...n, read: true } : n));
          this.notifications.set(updatedList);
          this.recalculateUnreadCount();
        }
      })
    );
  }

  public markAllAsRead(): Observable<ApiResponse<string>> {
    return this.http.patch<ApiResponse<string>>(`${this.baseUrl}/read-all`, {}).pipe(
      tap(() => {
        const updatedList = this.notifications().map((n) => ({ ...n, read: true }));
        this.notifications.set(updatedList);
        this.unreadCount.set(0);
      })
    );
  }

  public createNotification(payload: {
    title: string;
    message: string;
    type?: string;
    priority?: string;
    actionUrl?: string;
    recipientEmail?: string;
  }): Observable<ApiResponse<AppNotification>> {
    return this.http.post<ApiResponse<AppNotification>>(this.baseUrl, payload).pipe(
      tap((res) => {
        if (res && res.data) {
          this.notifications.set([res.data, ...this.notifications()]);
          this.recalculateUnreadCount();
        }
      })
    );
  }

  public deleteNotification(id: number): Observable<ApiResponse<string>> {
    return this.http.delete<ApiResponse<string>>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        const updatedList = this.notifications().filter((n) => n.id !== id);
        this.notifications.set(updatedList);
        this.recalculateUnreadCount();
      })
    );
  }

  private recalculateUnreadCount(): void {
    const count = this.notifications().filter((n) => !n.read).length;
    this.unreadCount.set(count);
  }
}
