import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, EmployeeWorkload, ProjectForecast, RequirementForecast, TeamCapacity } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class CapacityService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/capacity';

  constructor(private http: HttpClient) {}

  public getEmployeeWorkloads(): Observable<ApiResponse<EmployeeWorkload[]>> {
    return this.http.get<ApiResponse<EmployeeWorkload[]>>(`${this.baseUrl}/workload`);
  }

  public getTeamCapacities(): Observable<ApiResponse<TeamCapacity[]>> {
    return this.http.get<ApiResponse<TeamCapacity[]>>(`${this.baseUrl}/teams`);
  }

  public getProjectProjections(): Observable<ApiResponse<ProjectForecast[]>> {
    return this.http.get<ApiResponse<ProjectForecast[]>>(`${this.baseUrl}/projections`);
  }

  public getDemandForecast(): Observable<ApiResponse<RequirementForecast[]>> {
    return this.http.get<ApiResponse<RequirementForecast[]>>(`${this.baseUrl}/demand-forecast`);
  }
}
