import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Location } from '@angular/common';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  template: `
    <div class="unauthorized-container">
      <div class="card">
        <div class="icon-container">
          <span class="lock-icon">🔒</span>
        </div>
        <h1>403 — Access Denied</h1>
        <p>You don't have permission to access this page. Contact your administrator if you believe this is an error.</p>
        <div class="button-group">
          <button class="btn btn-primary" (click)="goToDashboard()">Go to Dashboard</button>
          <button class="btn btn-secondary" (click)="goBack()">Go Back</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .unauthorized-container {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      background-color: #0f172a;
      color: #e2e8f0;
      font-family: 'Inter', sans-serif;
    }
    .card {
      background-color: #1e293b;
      border: 1px solid #334155;
      border-radius: 8px;
      padding: 40px;
      max-width: 500px;
      text-align: center;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    }
    .icon-container {
      margin-bottom: 24px;
    }
    .lock-icon {
      font-size: 48px;
    }
    h1 {
      margin: 0 0 16px 0;
      font-size: 24px;
      font-weight: 600;
      color: #f8fafc;
    }
    p {
      margin: 0 0 32px 0;
      color: #94a3b8;
      line-height: 1.5;
    }
    .button-group {
      display: flex;
      gap: 16px;
      justify-content: center;
    }
    .btn {
      padding: 10px 20px;
      border-radius: 6px;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: background-color 0.2s;
    }
    .btn-primary {
      background-color: #4f46e5;
      color: white;
    }
    .btn-primary:hover {
      background-color: #4338ca;
    }
    .btn-secondary {
      background-color: #334155;
      color: white;
    }
    .btn-secondary:hover {
      background-color: #475569;
    }
  `]
})
export class UnauthorizedComponent {
  private router = inject(Router);
  private location = inject(Location);

  goToDashboard(): void {
    this.router.navigate(['/dashboard/executive']);
  }

  goBack(): void {
    this.location.back();
  }
}
