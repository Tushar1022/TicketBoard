import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, DependencyDto, TaskDocument, WorkItem, WorkItemStatus, WorkItemType } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class WorkItemService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/work-items';

  constructor(private http: HttpClient) {}

  public getWorkItems(params?: {
    projectId?: number;
    requirementId?: number;
    assigneeId?: number;
    status?: WorkItemStatus;
    type?: WorkItemType;
  }): Observable<ApiResponse<WorkItem[]>> {
    let httpParams = new HttpParams();
    if (params?.projectId) httpParams = httpParams.set('projectId', params.projectId.toString());
    if (params?.requirementId) httpParams = httpParams.set('requirementId', params.requirementId.toString());
    if (params?.assigneeId) httpParams = httpParams.set('assigneeId', params.assigneeId.toString());
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.type) httpParams = httpParams.set('type', params.type);
    return this.http.get<ApiResponse<WorkItem[]>>(this.baseUrl, { params: httpParams });
  }

  public getWorkItemById(id: number): Observable<ApiResponse<WorkItem>> {
    return this.http.get<ApiResponse<WorkItem>>(`${this.baseUrl}/${id}`);
  }

  public createWorkItem(payload: any): Observable<ApiResponse<WorkItem>> {
    return this.http.post<ApiResponse<WorkItem>>(this.baseUrl, payload);
  }

  public updateWorkItem(id: number, payload: any): Observable<ApiResponse<WorkItem>> {
    return this.http.put<ApiResponse<WorkItem>>(`${this.baseUrl}/${id}`, payload);
  }

  public updateStatus(id: number, status: WorkItemStatus, comment?: string): Observable<ApiResponse<WorkItem>> {
    return this.http.patch<ApiResponse<WorkItem>>(`${this.baseUrl}/${id}/status`, { status, comment });
  }

  public syncWithJira(id: number): Observable<ApiResponse<WorkItem>> {
    return this.http.post<ApiResponse<WorkItem>>(`${this.baseUrl}/${id}/jira-sync`, {});
  }

  public getDocuments(workItemId: number): Observable<ApiResponse<TaskDocument[]>> {
    return this.http.get<ApiResponse<TaskDocument[]>>(`${this.baseUrl}/${workItemId}/documents`);
  }

  public addDocument(workItemId: number, payload: { fileName: string; fileType?: string; fileSize?: number; fileUrl?: string }): Observable<ApiResponse<TaskDocument>> {
    return this.http.post<ApiResponse<TaskDocument>>(`${this.baseUrl}/${workItemId}/documents`, payload);
  }

  public uploadDocument(workItemId: number, file: File): Observable<ApiResponse<TaskDocument>> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ApiResponse<TaskDocument>>(`${this.baseUrl}/${workItemId}/documents/upload`, form);
  }

  public deleteDocument(docId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/documents/${docId}`);
  }

  public blockWorkItem(id: number, payload: { reason: string; owner: string; expectedResolutionDate?: string }): Observable<ApiResponse<WorkItem>> {
    return this.http.patch<ApiResponse<WorkItem>>(`${this.baseUrl}/${id}/status`, {
      status: 'BLOCKED',
      blockedReason: payload.reason,
      blockedOwner: payload.owner,
      expectedResolutionDate: payload.expectedResolutionDate
    });
  }

  public unblockWorkItem(id: number, payload: { reason: string }): Observable<ApiResponse<WorkItem>> {
    return this.http.patch<ApiResponse<WorkItem>>(`${this.baseUrl}/${id}/status`, {
      status: 'IN_PROGRESS',
      comment: payload.reason
    });
  }

  public addDependency(payload: DependencyDto): Observable<ApiResponse<DependencyDto>> {
    return this.http.post<ApiResponse<DependencyDto>>(`${this.baseUrl}/dependencies`, payload);
  }

  public deleteDependency(dependencyId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/dependencies/${dependencyId}`);
  }

  public deleteWorkItem(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
