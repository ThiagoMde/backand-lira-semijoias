import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, UserRole } from '../models';
import { AuthApi } from './api.services';

const STORAGE_KEY = 'lira.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(AuthApi);
  private router = inject(Router);

  private readonly session = signal<LoginResponse | null>(this.restore());

  readonly user = this.session.asReadonly();
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.role === 'ADMIN');

  get token(): string | null {
    const current = this.session();
    if (current && isExpired(current.token)) {
      this.clear();
      return null;
    }
    return current?.token ?? null;
  }

  login(body: LoginRequest): Observable<LoginResponse> {
    return this.api.login(body).pipe(
      tap((response) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
        this.session.set(response);
      }),
    );
  }

  hasRole(...roles: UserRole[]): boolean {
    const role = this.session()?.role;
    return !!role && roles.includes(role);
  }

  logout(): void {
    this.clear();
    this.router.navigate(['/admin/login']);
  }

  private clear(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }

  private restore(): LoginResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as LoginResponse;
      return isExpired(parsed.token) ? null : parsed;
    } catch {
      return null;
    }
  }
}

function isExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
  } catch {
    return false;
  }
}
