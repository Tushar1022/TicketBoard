import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, RoleType, User, RoleDto, PermissionDto, Department, Team } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  constructor(private http: HttpClient) {}

  getUsers(params?: { role?: RoleType; status?: string }): Observable<ApiResponse<User[]>> {
    let httpParams = new HttpParams();
    if (params?.role) httpParams = httpParams.set('role', params.role);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    return this.http.get<ApiResponse<User[]>>(`${this.baseUrl}/users`, { params: httpParams });
  }

  getUserById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.baseUrl}/users/${id}`);
  }

  createUser(data: any): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.baseUrl}/users`, data);
  }

  updateUser(id: number, data: any): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.baseUrl}/users/${id}`, data);
  }

  toggleStatus(id: number, status: string): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.baseUrl}/users/${id}/status`, null, { params: new HttpParams().set('status', status) });
  }

  deleteUser(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/users/${id}`);
  }

  updateRoles(id: number, roles: RoleType[]): Observable<ApiResponse<User>> {
    return this.http.patch<ApiResponse<User>>(`${this.baseUrl}/users/${id}/roles`, roles);
  }

  getProjectOwners(): Observable<ApiResponse<User[]>> {
    return this.http.get<ApiResponse<User[]>>(`${this.baseUrl}/users/project-owners`);
  }

  getUsersByRole(role: RoleType): Observable<ApiResponse<User[]>> {
    return this.http.get<ApiResponse<User[]>>(`${this.baseUrl}/users`, { params: { role } });
  }

  // ─── Departments & Teams ──────────────────────────────────────────────
  getDepartments(): Observable<ApiResponse<Department[]>> {
    return this.http.get<ApiResponse<Department[]>>(`${this.baseUrl}/departments`);
  }

  getTeams(): Observable<ApiResponse<Team[]>> {
    return this.http.get<ApiResponse<Team[]>>(`${this.baseUrl}/teams`);
  }

  // ─── Role & Permission management ─────────────────────────────────────
  getAllRoles(): Observable<ApiResponse<RoleDto[]>> {
    return this.http.get<ApiResponse<RoleDto[]>>(`${this.baseUrl}/roles`);
  }

  getRoleById(id: number): Observable<ApiResponse<RoleDto>> {
    return this.http.get<ApiResponse<RoleDto>>(`${this.baseUrl}/roles/${id}`);
  }

  updateRolePermissions(id: number, permissions: string[]): Observable<ApiResponse<RoleDto>> {
    return this.http.put<ApiResponse<RoleDto>>(`${this.baseUrl}/roles/${id}/permissions`, { permissions });
  }

  getAllPermissions(): Observable<ApiResponse<PermissionDto[]>> {
    return this.http.get<ApiResponse<PermissionDto[]>>(`${this.baseUrl}/roles/permissions`);
  }
}