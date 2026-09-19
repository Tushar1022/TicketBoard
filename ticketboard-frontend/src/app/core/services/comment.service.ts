import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActivityLog, ApiResponse, Comment } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class CommentService {
  private readonly commentUrl = 'http://localhost:8080/api/v1/comments';
  private readonly activityUrl = 'http://localhost:8080/api/v1/activity-logs';

  constructor(private http: HttpClient) {}

  public getComments(entityType: string, entityId: number): Observable<ApiResponse<Comment[]>> {
    let params = new HttpParams()
      .set('entityType', entityType)
      .set('entityId', entityId.toString());
    return this.http.get<ApiResponse<Comment[]>>(this.commentUrl, { params });
  }

  public addComment(payload: { entityType: string; entityId: number; content: string }): Observable<ApiResponse<Comment>> {
    return this.http.post<ApiResponse<Comment>>(this.commentUrl, payload);
  }

  public updateComment(id: number, content: string): Observable<ApiResponse<Comment>> {
    return this.http.put<ApiResponse<Comment>>(`${this.commentUrl}/${id}`, { content });
  }

  public deleteComment(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.commentUrl}/${id}`);
  }

  public getAuditLogs(entityType: string, entityId: number): Observable<ApiResponse<ActivityLog[]>> {
    let params = new HttpParams()
      .set('entityType', entityType)
      .set('entityId', entityId.toString());
    return this.http.get<ApiResponse<ActivityLog[]>>(`${this.activityUrl}/timeline`, { params });
  }
}
