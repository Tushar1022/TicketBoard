import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiResponse, LookupData } from '../models/api.models';

export interface CategoryInfo {
  code: string;
  name: string;
  icon: string;
  description: string;
}

@Injectable({
  providedIn: 'root'
})
export class LookupDataService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/lookup-data';

  // Real Application Master Data Categories used across TicketBoard
  public readonly appCategories: CategoryInfo[] = [
    { code: 'WORK_ITEM_STATUS', name: 'Work Item Statuses', icon: 'flag', description: 'Lifecycle & delivery stages for tasks, bugs, and user stories' },
    { code: 'PRIORITY', name: 'Work Priorities', icon: 'priority_high', description: 'Urgency & execution order for work items' },
    { code: 'WORK_ITEM_TYPE', name: 'Work Item Types', icon: 'layers', description: 'Classifications (Task, Bug, User Story, Feature, Change Request)' },
    { code: 'WORK_ITEM_SEVERITY', name: 'Issue Severities', icon: 'warning', description: 'Impact severity levels (Blocker, Critical, Major, Minor)' },
    { code: 'PROJECT_STATUS', name: 'Project Statuses', icon: 'account_tree', description: 'Overall enterprise project delivery stages' },
    { code: 'PROJECT_HEALTH', name: 'Project Health Flags', icon: 'favorite', description: 'KPI delivery health indicators (Green, Amber, Red)' },
    { code: 'RELEASE_ENVIRONMENT', name: 'Deployment Environments', icon: 'dns', description: 'Release target environments (DEV, SIT, UAT, Staging, PROD)' },
    { code: 'DEPARTMENT', name: 'Departments & Units', icon: 'corporate_fare', description: 'Enterprise functional organizational divisions' },
    { code: 'DESIGNATION', name: 'Employee Designations', icon: 'badge', description: 'Staff job roles & organizational titles' },
    { code: 'SKILL', name: 'Technical Skills', icon: 'psychology', description: 'Resource skill Matrix tags for project allocation' }
  ];

  // In-memory initial lookup fallback database matching backend SecurityDataInitializer
  private fallbackData: Record<string, LookupData[]> = {
    'WORK_ITEM_STATUS': [
      { id: 1, category: 'WORK_ITEM_STATUS', value: 'TODO', label: 'To Do', displayOrder: 1, colorCode: '#64748b', isActive: true, isDefault: true },
      { id: 2, category: 'WORK_ITEM_STATUS', value: 'IN_ANALYSIS', label: 'In Analysis', displayOrder: 2, colorCode: '#3b82f6', isActive: true },
      { id: 3, category: 'WORK_ITEM_STATUS', value: 'IN_PROGRESS', label: 'In Progress', displayOrder: 3, colorCode: '#6366f1', isActive: true },
      { id: 4, category: 'WORK_ITEM_STATUS', value: 'IN_REVIEW', label: 'In Review', displayOrder: 4, colorCode: '#8b5cf6', isActive: true },
      { id: 5, category: 'WORK_ITEM_STATUS', value: 'TESTING', label: 'Testing', displayOrder: 5, colorCode: '#06b6d4', isActive: true },
      { id: 6, category: 'WORK_ITEM_STATUS', value: 'SIT_EXIT', label: 'SIT Exit', displayOrder: 6, colorCode: '#0d9488', isActive: true },
      { id: 7, category: 'WORK_ITEM_STATUS', value: 'UAT_EXIT', label: 'UAT Exit', displayOrder: 7, colorCode: '#10b981', isActive: true },
      { id: 8, category: 'WORK_ITEM_STATUS', value: 'BLOCKED', label: 'Blocked', displayOrder: 8, colorCode: '#ef4444', isActive: true },
      { id: 9, category: 'WORK_ITEM_STATUS', value: 'COMPLETED', label: 'Completed', displayOrder: 9, colorCode: '#16a34a', isActive: true },
      { id: 10, category: 'WORK_ITEM_STATUS', value: 'CLOSED', label: 'Closed', displayOrder: 10, colorCode: '#475569', isActive: true }
    ],
    'PRIORITY': [
      { id: 11, category: 'PRIORITY', value: 'LOW', label: 'Low (P4)', displayOrder: 1, colorCode: '#10b981', isActive: true },
      { id: 12, category: 'PRIORITY', value: 'MEDIUM', label: 'Medium (P3)', displayOrder: 2, colorCode: '#3b82f6', isActive: true, isDefault: true },
      { id: 13, category: 'PRIORITY', value: 'HIGH', label: 'High (P2)', displayOrder: 3, colorCode: '#f59e0b', isActive: true },
      { id: 14, category: 'PRIORITY', value: 'CRITICAL', label: 'Critical (P1)', displayOrder: 4, colorCode: '#ef4444', isActive: true }
    ],
    'WORK_ITEM_TYPE': [
      { id: 21, category: 'WORK_ITEM_TYPE', value: 'TASK', label: 'Task', displayOrder: 1, colorCode: '#3b82f6', isActive: true, isDefault: true },
      { id: 22, category: 'WORK_ITEM_TYPE', value: 'BUG', label: 'Defect / Bug', displayOrder: 2, colorCode: '#ef4444', isActive: true },
      { id: 23, category: 'WORK_ITEM_TYPE', value: 'USER_STORY', label: 'User Story', displayOrder: 3, colorCode: '#10b981', isActive: true },
      { id: 24, category: 'WORK_ITEM_TYPE', value: 'FEATURE', label: 'Feature', displayOrder: 4, colorCode: '#8b5cf6', isActive: true },
      { id: 25, category: 'WORK_ITEM_TYPE', value: 'CHANGE_REQUEST', label: 'Change Request', displayOrder: 5, colorCode: '#ec4899', isActive: true }
    ],
    'WORK_ITEM_SEVERITY': [
      { id: 31, category: 'WORK_ITEM_SEVERITY', value: 'BLOCKER', label: 'Blocker (S1)', displayOrder: 1, colorCode: '#dc2626', isActive: true },
      { id: 32, category: 'WORK_ITEM_SEVERITY', value: 'CRITICAL', label: 'Critical (S2)', displayOrder: 2, colorCode: '#ef4444', isActive: true },
      { id: 33, category: 'WORK_ITEM_SEVERITY', value: 'MAJOR', label: 'Major (S3)', displayOrder: 3, colorCode: '#f59e0b', isActive: true },
      { id: 34, category: 'WORK_ITEM_SEVERITY', value: 'MINOR', label: 'Minor (S4)', displayOrder: 4, colorCode: '#10b981', isActive: true, isDefault: true }
    ],
    'PROJECT_STATUS': [
      { id: 41, category: 'PROJECT_STATUS', value: 'PROPOSED', label: 'Proposed', displayOrder: 1, colorCode: '#64748b', isActive: true },
      { id: 42, category: 'PROJECT_STATUS', value: 'APPROVED', label: 'Approved', displayOrder: 2, colorCode: '#3b82f6', isActive: true },
      { id: 43, category: 'PROJECT_STATUS', value: 'IN_PROGRESS', label: 'In Progress', displayOrder: 3, colorCode: '#6366f1', isActive: true, isDefault: true },
      { id: 44, category: 'PROJECT_STATUS', value: 'ON_HOLD', label: 'On Hold', displayOrder: 4, colorCode: '#f59e0b', isActive: true },
      { id: 45, category: 'PROJECT_STATUS', value: 'COMPLETED', label: 'Completed', displayOrder: 5, colorCode: '#10b981', isActive: true }
    ],
    'PROJECT_HEALTH': [
      { id: 51, category: 'PROJECT_HEALTH', value: 'GREEN', label: 'On Track (Green)', displayOrder: 1, colorCode: '#10b981', isActive: true, isDefault: true },
      { id: 52, category: 'PROJECT_HEALTH', value: 'AMBER', label: 'At Risk (Amber)', displayOrder: 2, colorCode: '#f59e0b', isActive: true },
      { id: 53, category: 'PROJECT_HEALTH', value: 'RED', label: 'Critical / Delayed (Red)', displayOrder: 3, colorCode: '#ef4444', isActive: true }
    ],
    'RELEASE_ENVIRONMENT': [
      { id: 61, category: 'RELEASE_ENVIRONMENT', value: 'DEV', label: 'Development (DEV)', displayOrder: 1, colorCode: '#3b82f6', isActive: true },
      { id: 62, category: 'RELEASE_ENVIRONMENT', value: 'SIT', label: 'System Integration (SIT)', displayOrder: 2, colorCode: '#8b5cf6', isActive: true },
      { id: 63, category: 'RELEASE_ENVIRONMENT', value: 'UAT', label: 'User Acceptance (UAT)', displayOrder: 3, colorCode: '#06b6d4', isActive: true },
      { id: 64, category: 'RELEASE_ENVIRONMENT', value: 'STAGING', label: 'Staging / Pre-Prod', displayOrder: 4, colorCode: '#f59e0b', isActive: true },
      { id: 65, category: 'RELEASE_ENVIRONMENT', value: 'PROD', label: 'Production (PROD)', displayOrder: 5, colorCode: '#10b981', isActive: true }
    ],
    'DEPARTMENT': [
      { id: 71, category: 'DEPARTMENT', value: 'ENGINEERING', label: 'Software Engineering', displayOrder: 1, colorCode: '#3b82f6', isActive: true },
      { id: 72, category: 'DEPARTMENT', value: 'QA_GOVERNANCE', label: 'Quality Assurance & Delivery Governance', displayOrder: 2, colorCode: '#10b981', isActive: true },
      { id: 73, category: 'DEPARTMENT', value: 'PRODUCT_MGMT', label: 'Product Management', displayOrder: 3, colorCode: '#8b5cf6', isActive: true }
    ],
    'DESIGNATION': [
      { id: 81, category: 'DESIGNATION', value: 'SR_SOFTWARE_ENGINEER', label: 'Senior Software Engineer', displayOrder: 1, colorCode: '#3b82f6', isActive: true },
      { id: 82, category: 'DESIGNATION', value: 'PROJECT_MANAGER', label: 'Project Delivery Manager', displayOrder: 2, colorCode: '#6366f1', isActive: true },
      { id: 83, category: 'DESIGNATION', value: 'LEAD_QA_ENGINEER', label: 'Lead QA Engineer', displayOrder: 3, colorCode: '#10b981', isActive: true },
      { id: 84, category: 'DESIGNATION', value: 'SOLUTION_ARCHITECT', label: 'Solution Architect', displayOrder: 4, colorCode: '#8b5cf6', isActive: true }
    ],
    'SKILL': [
      { id: 91, category: 'SKILL', value: 'JAVA_SPRINGBOOT', label: 'Java Spring Boot', displayOrder: 1, colorCode: '#16a34a', isActive: true },
      { id: 92, category: 'SKILL', value: 'ANGULAR', label: 'Angular 17 / TypeScript', displayOrder: 2, colorCode: '#dc2626', isActive: true },
      { id: 93, category: 'SKILL', value: 'MYSQL_DATABASE', label: 'MySQL Database Admin', displayOrder: 3, colorCode: '#0284c7', isActive: true },
      { id: 94, category: 'SKILL', value: 'DEVOPS_DOCKER', label: 'DevOps & Docker CI/CD', displayOrder: 4, colorCode: '#4f46e5', isActive: true }
    ]
  };

  constructor(private http: HttpClient) {}

  getCategories(): Observable<ApiResponse<string[]>> {
    return this.http.get<ApiResponse<string[]>>(`${this.baseUrl}/categories`).pipe(
      catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })
    );
  }

  getAll(category?: string): Observable<ApiResponse<LookupData[]>> {
    const url = category ? `${this.baseUrl}?category=${category}` : this.baseUrl;
    return this.http.get<ApiResponse<LookupData[]>>(url).pipe(
      catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })
    );
  }

  getByCategory(category: string): Observable<ApiResponse<LookupData[]>> {
    return this.http.get<ApiResponse<LookupData[]>>(`${this.baseUrl}/category/${category}`).pipe(
      catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })
    );
  }

  getById(id: number): Observable<ApiResponse<LookupData>> {
    return this.http.get<ApiResponse<LookupData>>(`${this.baseUrl}/${id}`).pipe(
      catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })
    );
  }

  create(data: { category: string; value: string; label: string; displayOrder?: number; colorCode?: string }): Observable<ApiResponse<LookupData>> {
    return this.http.post<ApiResponse<LookupData>>(this.baseUrl, data).pipe(
      catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })
    );
  }

  update(id: number, data: { category: string; value: string; label: string; displayOrder?: number; colorCode?: string; isActive?: boolean }): Observable<ApiResponse<LookupData>> {
    return this.http.put<ApiResponse<LookupData>>(`${this.baseUrl}/${id}`, data).pipe(
      catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })
    );
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`).pipe(
      catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })
    );
  }
}