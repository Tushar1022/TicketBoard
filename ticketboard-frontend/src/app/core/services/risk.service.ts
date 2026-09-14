import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Issue, Risk } from '../models/api.models';

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

  public getIssuesByProject(projectId: number): Observable<ApiResponse<Issue[]>> {
    return this.http.get<ApiResponse<Issue[]>>(`${this.baseUrl}/issues/project/${projectId}`);
  }

  public createIssue(payload: any): Observable<ApiResponse<Issue>> {
    return this.http.post<ApiResponse<Issue>>(`${this.baseUrl}/issues`, payload);
  }

  public updateIssue(id: number, payload: any): Observable<ApiResponse<Issue>> {
    return this.http.put<ApiResponse<Issue>>(`${this.baseUrl}/issues/${id}`, payload);
  }
}
