import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, BillingPreview, BillingSummary, InvoiceDto, InvoiceStatus } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/billing';

  constructor(private http: HttpClient) {}

  public getAllInvoices(params?: { projectId?: number; status?: InvoiceStatus }): Observable<ApiResponse<InvoiceDto[]>> {
    let httpParams = new HttpParams();
    if (params?.projectId) httpParams = httpParams.set('projectId', params.projectId.toString());
    if (params?.status) httpParams = httpParams.set('status', params.status);
    return this.http.get<ApiResponse<InvoiceDto[]>>(this.baseUrl, { params: httpParams });
  }

  public getInvoice(id: number): Observable<ApiResponse<InvoiceDto>> {
    return this.http.get<ApiResponse<InvoiceDto>>(`${this.baseUrl}/${id}`);
  }

  public createInvoice(payload: {
    projectId: number;
    fromDate: string;
    toDate: string;
    issuedDate?: string;
    dueDate?: string;
    taxRate?: number;
    currency?: string;
    notes?: string;
  }): Observable<ApiResponse<InvoiceDto>> {
    return this.http.post<ApiResponse<InvoiceDto>>(this.baseUrl, payload);
  }

  public updateInvoice(id: number, payload: { notes?: string; dueDate?: string; taxRate?: number }): Observable<ApiResponse<InvoiceDto>> {
    return this.http.put<ApiResponse<InvoiceDto>>(`${this.baseUrl}/${id}`, payload);
  }

  public updateStatus(id: number, status: InvoiceStatus): Observable<ApiResponse<InvoiceDto>> {
    return this.http.patch<ApiResponse<InvoiceDto>>(`${this.baseUrl}/${id}/status`, { status });
  }

  public deleteInvoice(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  public previewBilling(projectId: number, fromDate: string, toDate: string): Observable<ApiResponse<BillingPreview>> {
    const params = new HttpParams()
      .set('projectId', projectId.toString())
      .set('fromDate', fromDate)
      .set('toDate', toDate);
    return this.http.get<ApiResponse<BillingPreview>>(`${this.baseUrl}/preview`, { params });
  }

  public getSummary(): Observable<ApiResponse<BillingSummary>> {
    return this.http.get<ApiResponse<BillingSummary>>(`${this.baseUrl}/summary`);
  }

  public getInvoicePdfBlob(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/pdf`, { responseType: 'blob' });
  }

  public downloadInvoicePdf(id: number, invoiceNumber: string): void {
    this.getInvoicePdfBlob(id).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${invoiceNumber}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => URL.revokeObjectURL(url), 5000);
      }
    });
  }
}