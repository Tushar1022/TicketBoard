import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Milestone, Project, ProjectStats } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/projects';
  private readonly milestoneUrl = 'http://localhost:8080/api/v1/milestones';

  constructor(private http: HttpClient) {}

  public getProjectStats(id: number): Observable<ApiResponse<ProjectStats>> {
    return this.http.get<ApiResponse<ProjectStats>>(`${this.baseUrl}/${id}/stats`);
  }

  public getAllProjects(params?: { status?: string; managerId?: number; userId?: number }): Observable<ApiResponse<Project[]>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.managerId) httpParams = httpParams.set('managerId', params.managerId.toString());
    if (params?.userId) httpParams = httpParams.set('userId', params.userId.toString());
    
    return this.http.get<ApiResponse<Project[]>>(this.baseUrl, { params: httpParams });
  }

  public getProjectById(id: number): Observable<ApiResponse<Project>> {
    return this.http.get<ApiResponse<Project>>(`${this.baseUrl}/${id}`);
  }

  public createProject(payload: any): Observable<ApiResponse<Project>> {
    return this.http.post<ApiResponse<Project>>(this.baseUrl, payload);
  }

  public updateProject(id: number, payload: any): Observable<ApiResponse<Project>> {
    return this.http.put<ApiResponse<Project>>(`${this.baseUrl}/${id}`, payload);
  }

  public deleteProject(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  public addMember(projectId: number, payload: { userId: number; projectRole: string; allocatedHoursPerDay: number }): Observable<ApiResponse<Project>> {
    return this.http.post<ApiResponse<Project>>(`${this.baseUrl}/${projectId}/members`, payload);
  }

  public removeMember(projectId: number, userId: number): Observable<ApiResponse<Project>> {
    return this.http.delete<ApiResponse<Project>>(`${this.baseUrl}/${projectId}/members/${userId}`);
  }

  public getMilestones(projectId: number, filters?: { status?: string; flag?: string; overdue?: boolean }): Observable<ApiResponse<Milestone[]>> {
    let httpParams = new HttpParams();
    if (filters?.status) httpParams = httpParams.set('status', filters.status);
    if (filters?.flag) httpParams = httpParams.set('flag', filters.flag);
    if (filters?.overdue != null) httpParams = httpParams.set('overdue', String(filters.overdue));
    return this.http.get<ApiResponse<Milestone[]>>(`${this.milestoneUrl}/project/${projectId}`, { params: httpParams });
  }

  public createMilestone(payload: any): Observable<ApiResponse<Milestone>> {
    return this.http.post<ApiResponse<Milestone>>(this.milestoneUrl, payload);
  }

  public updateMilestone(id: number, payload: any): Observable<ApiResponse<Milestone>> {
    return this.http.put<ApiResponse<Milestone>>(`${this.milestoneUrl}/${id}`, payload);
  }

  public deleteMilestone(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.milestoneUrl}/${id}`);
  }
}
