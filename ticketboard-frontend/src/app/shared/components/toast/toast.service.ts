import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  title: string;
  message: string;
  duration: number;
  createdAt: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  public readonly toasts = signal<Toast[]>([]);

  private counter = 0;

  public success(message: string, title = 'Success'): void {
    this.show('success', title, message);
  }

  public error(message: string, title = 'Error'): void {
    this.show('error', title, message);
  }

  public warning(message: string, title = 'Warning'): void {
    this.show('warning', title, message);
  }

  public info(message: string, title = 'Info'): void {
    this.show('info', title, message);
  }

  public dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private show(type: ToastType, title: string, message: string, duration = 1800): void {
    const toast: Toast = { id: ++this.counter, type, title, message, duration, createdAt: Date.now() };
    this.toasts.update((list) => [...list, toast]);
    if (toast.duration > 0) {
      setTimeout(() => this.dismiss(toast.id), toast.duration);
    }
  }
}