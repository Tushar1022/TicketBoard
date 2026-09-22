import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, DeveloperDashboard, DeveloperTelemetry, ExecutiveDashboard, QaDashboard } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/dashboards';

  constructor(private readonly http: HttpClient) {}

  public getExecutiveDashboard(): Observable<ApiResponse<ExecutiveDashboard>> {
    return this.http.get<ApiResponse<ExecutiveDashboard>>(`${this.baseUrl}/executive`);
  }

  public getDeveloperDashboard(userId?: number): Observable<ApiResponse<DeveloperDashboard>> {
    let params = new HttpParams();
    if (userId) params = params.set('userId', userId.toString());
    return this.http.get<ApiResponse<DeveloperDashboard>>(`${this.baseUrl}/developer`, { params });
  }

  public getDeveloperTelemetry(): Observable<ApiResponse<DeveloperTelemetry>> {
    return this.http.get<ApiResponse<DeveloperTelemetry>>(`${this.baseUrl}/developer/telemetry`);
  }

  public getQaDashboard(): Observable<ApiResponse<QaDashboard>> {
    return this.http.get<ApiResponse<QaDashboard>>(`${this.baseUrl}/qa`);
  }
}
