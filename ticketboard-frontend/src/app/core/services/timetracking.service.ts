import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, EffortVariance, TimeEntry, Timesheet, TimesheetStatus } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class TimeTrackingService {
  private readonly timeEntryUrl = 'http://localhost:8080/api/v1/time-entries';
  private readonly timesheetUrl = 'http://localhost:8080/api/v1/timesheets';

  constructor(private http: HttpClient) {}

  public getMyTimeEntries(): Observable<ApiResponse<TimeEntry[]>> {
    return this.http.get<ApiResponse<TimeEntry[]>>(`${this.timeEntryUrl}/my`);
  }

  public getTimeEntriesByUser(userId: number): Observable<ApiResponse<TimeEntry[]>> {
    return this.http.get<ApiResponse<TimeEntry[]>>(`${this.timeEntryUrl}/user/${userId}`);
  }

  public getTimeEntriesByProject(projectId: number): Observable<ApiResponse<TimeEntry[]>> {
    return this.http.get<ApiResponse<TimeEntry[]>>(`${this.timeEntryUrl}/project/${projectId}`);
  }

  public logTime(payload: {
    projectId: number;
    requirementId?: number;
    workItemId?: number;
    workDate: string;
    startTime?: string;
    endTime?: string;
    breakMinutes?: number;
    totalHours?: number;
    description: string;
  }): Observable<ApiResponse<TimeEntry>> {
    return this.http.post<ApiResponse<TimeEntry>>(this.timeEntryUrl, payload);
  }

  public getEffortVariances(): Observable<ApiResponse<EffortVariance[]>> {
    return this.http.get<ApiResponse<EffortVariance[]>>(`${this.timeEntryUrl}/variance`);
  }

  public deleteTimeEntry(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.timeEntryUrl}/${id}`);
  }

  public getWeeklyTimesheet(userId?: number, dateInWeek?: string): Observable<ApiResponse<Timesheet>> {
    let params = new HttpParams();
    if (userId) params = params.set('userId', userId.toString());
    if (dateInWeek) params = params.set('dateInWeek', dateInWeek);
    return this.http.get<ApiResponse<Timesheet>>(`${this.timesheetUrl}/week`, { params });
  }

  public getPendingTimesheets(): Observable<ApiResponse<Timesheet[]>> {
    return this.http.get<ApiResponse<Timesheet[]>>(`${this.timesheetUrl}/pending`);
  }

  public submitTimesheet(id: number): Observable<ApiResponse<Timesheet>> {
    return this.http.post<ApiResponse<Timesheet>>(`${this.timesheetUrl}/${id}/submit`, {});
  }

  public reviewTimesheet(id: number, status: TimesheetStatus, rejectionReason?: string): Observable<ApiResponse<Timesheet>> {
    return this.http.post<ApiResponse<Timesheet>>(`${this.timesheetUrl}/${id}/review`, { status, rejectionReason });
  }
}
