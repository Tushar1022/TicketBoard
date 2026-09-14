import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiResponse, AuthResponse, RoleType, User } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/auth';

  public currentUser = signal<User | null>(this.getStoredUser());
  public isAuthenticated = signal<boolean>(this.hasValidToken());
  public userPermissions = signal<Set<string>>(this.getStoredPermissions());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  public login(credentials: { email: string; password: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, credentials).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.setSession(res.data);
        }
      })
    );
  }

  public register(payload: any): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.baseUrl}/register`, payload);
  }

  public getMe(): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.baseUrl}/me`).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.currentUser.set(res.data);
          this.userPermissions.set(new Set(res.data.permissions || []));
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('tb_user', JSON.stringify(res.data));
          }
        }
      })
    );
  }

  public changePassword(payload: { oldPassword: string; newPassword: string }): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/change-password`, payload);
  }

  public logout(): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.removeItem('tb_token');
      localStorage.removeItem('tb_user');
    }
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    this.userPermissions.set(new Set());
    this.router.navigate(['/login']);
  }

  public getToken(): string | null {
    if (typeof window !== 'undefined' && window.localStorage) {
      return localStorage.getItem('tb_token');
    }
    return null;
  }

  // ─── Role checks (backward compatible) ─────────────────────────────────
  public hasRole(...roles: RoleType[]): boolean {
    const user = this.currentUser();
    if (!user || !user.roles) return false;
    return roles.some((r) => user.roles.includes(r));
  }

  public isSuperAdmin(): boolean {
    return this.hasRole('ROLE_SUPER_ADMIN');
  }

  public isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN');
  }

  public isProjectOwner(): boolean {
    return this.hasRole('ROLE_PROJECT_OWNER');
  }

  public isProjectManager(): boolean {
    return this.hasRole('ROLE_PROJECT_MANAGER', 'ROLE_SUPER_ADMIN');
  }

  public isTeamLead(): boolean {
    return this.hasRole('ROLE_TEAM_LEAD', 'ROLE_SUPER_ADMIN');
  }

  // ─── Permission checks ─────────────────────────────────────────────────
  public hasPermission(permission: string): boolean {
    return this.userPermissions().has(permission);
  }

  public hasAnyPermission(...permissions: string[]): boolean {
    const perms = this.userPermissions();
    return permissions.some((p) => perms.has(p));
  }

  public hasAllPermissions(...permissions: string[]): boolean {
    const perms = this.userPermissions();
    return permissions.every((p) => perms.has(p));
  }

  public canAccessModule(module: string): boolean {
    const perms = this.userPermissions();
    return [...perms].some((p) => p.startsWith(module.toLowerCase() + ':'));
  }

  // ─── Session management ────────────────────────────────────────────────
  private setSession(authData: AuthResponse): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('tb_token', authData.accessToken);
      localStorage.setItem('tb_user', JSON.stringify(authData.user));
    }
    this.currentUser.set(authData.user);
    this.isAuthenticated.set(true);
    this.userPermissions.set(new Set(authData.user.permissions || []));
  }

  private getStoredUser(): User | null {
    if (typeof window !== 'undefined' && window.localStorage) {
      const stored = localStorage.getItem('tb_user');
      if (stored) {
        try {
          return JSON.parse(stored);
        } catch {
          return null;
        }
      }
    }
    return null;
  }

  private getStoredPermissions(): Set<string> {
    const user = this.getStoredUser();
    return new Set(user?.permissions || []);
  }

  private hasValidToken(): boolean {
    if (typeof window !== 'undefined' && window.localStorage) {
      return !!localStorage.getItem('tb_token');
    }
    return false;
  }
}