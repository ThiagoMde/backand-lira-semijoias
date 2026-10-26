import { Component, Injectable, inject, signal } from '@angular/core';

export interface Toast {
  id: number;
  text: string;
  kind: 'success' | 'error' | 'info';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 1;

  success(text: string): void { this.show(text, 'success'); }
  error(text: string): void { this.show(text, 'error'); }
  info(text: string): void { this.show(text, 'info'); }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private show(text: string, kind: Toast['kind']): void {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, text, kind }]);
    setTimeout(() => this.dismiss(id), 4500);
  }
}

@Component({
  selector: 'app-toast',
  template: `
    <div class="toasts" aria-live="polite">
      @for (t of toast.toasts(); track t.id) {
        <button type="button" class="toast" [class]="t.kind" (click)="toast.dismiss(t.id)">{{ t.text }}</button>
      }
    </div>
  `,
  styles: `
    .toasts {
      position: fixed;
      right: 16px;
      bottom: 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      z-index: 1000;
      max-width: calc(100vw - 32px);
    }
    .toast {
      border: none;
      text-align: left;
      font: inherit;
      cursor: pointer;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      color: #fff;
      background: var(--ink);
      box-shadow: var(--shadow);
      max-width: 380px;
    }
    .toast.success { background: var(--success); }
    .toast.error { background: var(--danger); }
  `,
})
export class ToastComponent {
  protected toast = inject(ToastService);
}
