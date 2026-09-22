import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, ERPAsset, ExpenseClaim, LeaveRequestApi, OKRGoalApi } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class ErpService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/erp';

  constructor(private readonly http: HttpClient) {}

  public getAssets(): Observable<ApiResponse<ERPAsset[]>> {
    return this.http.get<ApiResponse<ERPAsset[]>>(`${this.baseUrl}/assets`);
  }

  public createAssetRequest(payload: { name: string; category: string }): Observable<ApiResponse<ERPAsset>> {
    return this.http.post<ApiResponse<ERPAsset>>(`${this.baseUrl}/assets`, payload);
  }

  public getExpenseClaims(): Observable<ApiResponse<ExpenseClaim[]>> {
    return this.http.get<ApiResponse<ExpenseClaim[]>>(`${this.baseUrl}/expenses`);
  }

  public createExpenseClaim(payload: { category: string; amount: number; currency: string; description: string }): Observable<ApiResponse<ExpenseClaim>> {
    return this.http.post<ApiResponse<ExpenseClaim>>(`${this.baseUrl}/expenses`, payload);
  }

  public getLeaveRequests(): Observable<ApiResponse<LeaveRequestApi[]>> {
    return this.http.get<ApiResponse<LeaveRequestApi[]>>(`${this.baseUrl}/leaves`);
  }

  public createLeaveRequest(payload: { leaveType: string; startDate: string; endDate: string; reason: string }): Observable<ApiResponse<LeaveRequestApi>> {
    return this.http.post<ApiResponse<LeaveRequestApi>>(`${this.baseUrl}/leaves`, payload);
  }

  public getOkrGoals(): Observable<ApiResponse<OKRGoalApi[]>> {
    return this.http.get<ApiResponse<OKRGoalApi[]>>(`${this.baseUrl}/okrs`);
  }
}