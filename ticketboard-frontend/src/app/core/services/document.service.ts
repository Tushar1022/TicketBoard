import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, ProjectDocument, TaskDocument } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly workItemBase = 'http://localhost:8080/api/v1/work-items';
  private readonly projectBase = 'http://localhost:8080/api/v1/projects/documents';

  constructor(private http: HttpClient) {}

  public uploadWorkItemDocument(workItemId: number, file: File): Observable<ApiResponse<TaskDocument>> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ApiResponse<TaskDocument>>(`${this.workItemBase}/${workItemId}/documents/upload`, form);
  }

  public uploadProjectDocument(projectId: number, file: File, description?: string): Observable<ApiResponse<ProjectDocument>> {
    const form = new FormData();
    form.append('file', file, file.name);
    if (description) form.append('description', description);
    return this.http.post<ApiResponse<ProjectDocument>>(`${this.projectBase}/project/${projectId}/upload`, form);
  }

  public getProjectDocuments(projectId: number): Observable<ApiResponse<ProjectDocument[]>> {
    return this.http.get<ApiResponse<ProjectDocument[]>>(`${this.projectBase}/project/${projectId}`);
  }

  public deleteProjectDocument(docId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.projectBase}/${docId}`);
  }

  public downloadDocument(downloadUrl: string): Observable<Blob> {
    return this.http.get(downloadUrl, {
      responseType: 'blob',
      headers: new HttpHeaders({ Accept: 'application/octet-stream' })
    });
  }

  public openDownload(downloadUrl: string): void {
    this.downloadDocument(downloadUrl).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.target = '_blank';
        a.rel = 'noopener';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => URL.revokeObjectURL(url), 5000);
      }
    });
  }
}