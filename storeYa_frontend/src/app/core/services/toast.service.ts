import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastType = 'success' | 'error' | 'info' | 'loading' | 'warning';

export interface ToastMessage {
  message: string;
  type: ToastType;
  title?: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toastSubject = new BehaviorSubject<ToastMessage | null>(null);
  toast$ = this.toastSubject.asObservable();

  show(message: string, type: ToastType = 'info') {
    this.toastSubject.next({ message, type });

    if (type !== 'loading') {
      setTimeout(() => this.clear(), 2500);
    }
  }

  clear() {
    this.toastSubject.next(null);
  }
}
