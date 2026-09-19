import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

export interface DocumentPreviewData {
  title: string;
  subtitle?: string;
  html?: string;
  pdfBlob?: Blob;
  downloadLabel?: string;
  onDownload?: () => void;
}

@Component({
  selector: 'app-document-preview-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatIconModule, MatButtonModule],
  templateUrl: './document-preview-dialog.component.html',
  styleUrls: ['./document-preview-dialog.component.scss']
})
export class DocumentPreviewDialogComponent implements OnInit, OnDestroy {
  public pdfUrl: SafeResourceUrl | null = null;
  public pdfObjectUrl: string | null = null;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: DocumentPreviewData,
    public dialogRef: MatDialogRef<DocumentPreviewDialogComponent>,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    if (this.data.pdfBlob) {
      this.pdfObjectUrl = URL.createObjectURL(this.data.pdfBlob);
      this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl);
    } else if (this.data.html) {
      const blob = new Blob([this.data.html], { type: 'text/html;charset=utf-8' });
      this.pdfObjectUrl = URL.createObjectURL(blob);
      this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl);
    }
  }

  ngOnDestroy(): void {
    if (this.pdfObjectUrl) {
      URL.revokeObjectURL(this.pdfObjectUrl);
    }
  }

  public print(): void {
    const iframe = document.getElementById('preview-frame') as HTMLIFrameElement | null;
    const win = iframe?.contentWindow;
    if (win) {
      win.focus();
      win.print();
    }
  }

  public download(): void {
    if (this.data.onDownload) {
      this.data.onDownload();
    }
    this.dialogRef.close();
  }
}