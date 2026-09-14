import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Requirement, RequirementHistory } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class RequirementService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/requirements';

  constructor(private http: HttpClient) {}

  public getAllRequirements(params?: { projectId?: number; status?: string; ownerId?: number }): Observable<ApiResponse<Requirement[]>> {
    let httpParams = new HttpParams();
    if (params?.projectId) httpParams = httpParams.set('projectId', params.projectId.toString());
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.ownerId) httpParams = httpParams.set('ownerId', params.ownerId.toString());
    return this.http.get<ApiResponse<Requirement[]>>(this.baseUrl, { params: httpParams });
  }

  public getRequirementById(id: number): Observable<ApiResponse<Requirement>> {
    return this.http.get<ApiResponse<Requirement>>(`${this.baseUrl}/${id}`);
  }

  public createRequirement(payload: any): Observable<ApiResponse<Requirement>> {
    return this.http.post<ApiResponse<Requirement>>(this.baseUrl, payload);
  }

  public updateRequirement(id: number, payload: any): Observable<ApiResponse<Requirement>> {
    return this.http.put<ApiResponse<Requirement>>(`${this.baseUrl}/${id}`, payload);
  }

  public updateStatus(id: number, status: string, comment?: string): Observable<ApiResponse<Requirement>> {
    return this.http.patch<ApiResponse<Requirement>>(`${this.baseUrl}/${id}/status`, { status, comment });
  }

  public getHistory(id: number): Observable<ApiResponse<RequirementHistory[]>> {
    return this.http.get<ApiResponse<RequirementHistory[]>>(`${this.baseUrl}/${id}/history`);
  }

  public deleteRequirement(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
