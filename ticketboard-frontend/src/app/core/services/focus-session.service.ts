import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, FocusSession, TimeEntry } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class FocusSessionService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/focus-sessions';

  constructor(private readonly http: HttpClient) {}

  public getCurrent(): Observable<ApiResponse<FocusSession>> {
    return this.http.get<ApiResponse<FocusSession>>(`${this.baseUrl}/current`);
  }

  public getHistory(): Observable<ApiResponse<FocusSession[]>> {
    return this.http.get<ApiResponse<FocusSession[]>>(`${this.baseUrl}/history`);
  }

  public start(workItemId: number, description?: string): Observable<ApiResponse<FocusSession>> {
    return this.http.post<ApiResponse<FocusSession>>(`${this.baseUrl}/start`, { workItemId, description });
  }

  public pause(): Observable<ApiResponse<FocusSession>> {
    return this.http.post<ApiResponse<FocusSession>>(`${this.baseUrl}/pause`, {});
  }

  public resume(): Observable<ApiResponse<FocusSession>> {
    return this.http.post<ApiResponse<FocusSession>>(`${this.baseUrl}/resume`, {});
  }

  public reset(): Observable<ApiResponse<FocusSession>> {
    return this.http.post<ApiResponse<FocusSession>>(`${this.baseUrl}/reset`, {});
  }

  public logTime(description?: string): Observable<ApiResponse<TimeEntry>> {
    return this.http.post<ApiResponse<TimeEntry>>(`${this.baseUrl}/log`, { description });
  }
}