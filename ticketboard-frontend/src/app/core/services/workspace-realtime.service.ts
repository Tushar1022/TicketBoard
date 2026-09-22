import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';

export interface WorkspaceRealtimeMessage {
  type: string;
  topic?: string;
  data?: any;
  at?: string;
  userId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class WorkspaceRealtimeService {
  public readonly connected = signal<boolean>(false);
  public readonly message = signal<WorkspaceRealtimeMessage | null>(null);
  public readonly messages$ = new Subject<WorkspaceRealtimeMessage>();

  private socket: WebSocket | null = null;
  private manualClose = false;
  private reconnectAttempts = 0;
  private reconnectTimer: any = null;

  public connect(): void {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return;
    }
    this.manualClose = false;
    this.openSocket();
  }

  private openSocket(): void {
    let token: string | null = null;
    if (typeof window !== 'undefined' && window.localStorage) {
      token = localStorage.getItem('tb_token');
    }
    if (!token) {
      this.connected.set(false);
      return;
    }
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${protocol}://localhost:8080/ws-workspace?token=${encodeURIComponent(token)}`;
    try {
      this.socket = new WebSocket(url);
    } catch {
      this.connected.set(false);
      return;
    }

    this.socket.onopen = () => {
      this.connected.set(true);
      this.reconnectAttempts = 0;
    };

    this.socket.onmessage = (event: MessageEvent) => {
      try {
        const msg: WorkspaceRealtimeMessage = JSON.parse(event.data);
        this.message.set(msg);
        this.messages$.next(msg);
      } catch {
        // ignore non-JSON frames
      }
    };

    this.socket.onerror = () => {
      this.connected.set(false);
    };

    this.socket.onclose = () => {
      this.connected.set(false);
      this.socket = null;
      if (!this.manualClose) {
        this.scheduleReconnect();
      }
    };
  }

  private scheduleReconnect(): void {
    const delay = Math.min(2000 * Math.pow(2, this.reconnectAttempts), 30000);
    this.reconnectAttempts++;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    this.reconnectTimer = setTimeout(() => this.openSocket(), delay);
  }

  public disconnect(): void {
    this.manualClose = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.connected.set(false);
  }

  public sendPing(): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send('ping');
    }
  }
}