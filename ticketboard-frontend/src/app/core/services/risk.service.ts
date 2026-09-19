import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Issue, IssueComment, IssueHistory, IssueWatcher, Risk } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class RiskService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/risks';

  constructor(private http: HttpClient) {}

  public getRisks(projectId?: number): Observable<ApiResponse<Risk[]>> {
    let params = new HttpParams();
    if (projectId) params = params.set('projectId', projectId.toString());
    return this.http.get<ApiResponse<Risk[]>>(this.baseUrl, { params });
  }

  public createRisk(payload: any): Observable<ApiResponse<Risk>> {
    return this.http.post<ApiResponse<Risk>>(this.baseUrl, payload);
  }

  public updateRisk(id: number, payload: any): Observable<ApiResponse<Risk>> {
    return this.http.put<ApiResponse<Risk>>(`${this.baseUrl}/${id}`, payload);
  }

  public deleteRisk(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  public getIssues(filters?: {
    projectId?: number;
    severity?: string;
    status?: string;
    assigneeId?: number;
    milestoneId?: number;
    overdue?: boolean;
    search?: string;
  }): Observable<ApiResponse<Issue[]>> {
    let params = new HttpParams();
    if (filters?.projectId) params = params.set('projectId', filters.projectId.toString());
    if (filters?.severity) params = params.set('severity', filters.severity);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.assigneeId) params = params.set('assigneeId', filters.assigneeId.toString());
    if (filters?.milestoneId) params = params.set('milestoneId', filters.milestoneId.toString());
    if (filters?.overdue != null) params = params.set('overdue', String(filters.overdue));
    if (filters?.search) params = params.set('search', filters.search);
    return this.http.get<ApiResponse<Issue[]>>(`${this.baseUrl}/issues`, { params });
  }

  public getIssuesByProject(projectId: number): Observable<ApiResponse<Issue[]>> {
    return this.http.get<ApiResponse<Issue[]>>(`${this.baseUrl}/issues/project/${projectId}`);
  }

  public createIssue(payload: any): Observable<ApiResponse<Issue>> {
    return this.http.post<ApiResponse<Issue>>(`${this.baseUrl}/issues`, payload);
  }

  public updateIssue(id: number, payload: any): Observable<ApiResponse<Issue>> {
    return this.http.put<ApiResponse<Issue>>(`${this.baseUrl}/issues/${id}`, payload);
  }

  public patchIssue(id: number, payload: any): Observable<ApiResponse<Issue>> {
    return this.http.patch<ApiResponse<Issue>>(`${this.baseUrl}/issues/${id}`, payload);
  }

  public deleteIssue(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/issues/${id}`);
  }

  public getIssueComments(issueId: number): Observable<ApiResponse<IssueComment[]>> {
    return this.http.get<ApiResponse<IssueComment[]>>(`${this.baseUrl}/issues/${issueId}/comments`);
  }

  public addIssueComment(issueId: number, payload: { authorId: number; content: string }): Observable<ApiResponse<IssueComment>> {
    const params = new HttpParams().set('authorId', payload.authorId.toString()).set('content', payload.content);
    return this.http.post<ApiResponse<IssueComment>>(`${this.baseUrl}/issues/${issueId}/comments`, null, { params });
  }

  public getIssueHistory(issueId: number): Observable<ApiResponse<IssueHistory[]>> {
    return this.http.get<ApiResponse<IssueHistory[]>>(`${this.baseUrl}/issues/${issueId}/history`);
  }

  public getIssueWatchers(issueId: number): Observable<ApiResponse<IssueWatcher[]>> {
    return this.http.get<ApiResponse<IssueWatcher[]>>(`${this.baseUrl}/issues/${issueId}/watchers`);
  }

  public addIssueWatcher(issueId: number, userId: number): Observable<ApiResponse<IssueWatcher>> {
    const params = new HttpParams().set('userId', userId.toString());
    return this.http.post<ApiResponse<IssueWatcher>>(`${this.baseUrl}/issues/${issueId}/watchers`, null, { params });
  }

  public removeIssueWatcher(issueId: number, userId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/issues/${issueId}/watchers/${userId}`);
  }
}
