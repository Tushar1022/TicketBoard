import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Release } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class ReleaseService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/releases';

  constructor(private http: HttpClient) {}

  public getReleases(projectId?: number): Observable<ApiResponse<Release[]>> {
    let params = new HttpParams();
    if (projectId) params = params.set('projectId', projectId.toString());
    return this.http.get<ApiResponse<Release[]>>(this.baseUrl, { params });
  }

  public getReleaseById(id: number): Observable<ApiResponse<Release>> {
    return this.http.get<ApiResponse<Release>>(`${this.baseUrl}/${id}`);
  }

  public createRelease(payload: any): Observable<ApiResponse<Release>> {
    return this.http.post<ApiResponse<Release>>(this.baseUrl, payload);
  }

  public updateRelease(id: number, payload: any): Observable<ApiResponse<Release>> {
    return this.http.put<ApiResponse<Release>>(`${this.baseUrl}/${id}`, payload);
  }

  public deleteRelease(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
