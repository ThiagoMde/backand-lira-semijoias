import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../core/utils';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="wrap">
      <form class="card" [formGroup]="form" (ngSubmit)="submit()">
        <div class="logo">Lira <span>Semijoias</span></div>
        <p class="muted center">Painel administrativo</p>

        @if (error()) { <div class="alert">{{ error() }}</div> }

        <div class="field">
          <label for="email">E-mail</label>
          <input id="email" type="email" formControlName="email" autocomplete="username" />
        </div>
        <div class="field">
          <label for="password">Senha</label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password" />
        </div>
        <button class="btn block" type="submit" [disabled]="loading()">{{ loading() ? 'Entrando...' : 'Entrar' }}</button>
        <a routerLink="/" class="small center back">← Voltar para a loja</a>
      </form>
    </div>
  `,
  styles: `
    .wrap {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      background: linear-gradient(120deg, #f3ead8 0%, #fbf8f4 55%, #f6e3e0 100%);
    }
    .card { width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 1rem; padding: 2rem; }
    .logo { font-family: var(--serif); font-size: 2rem; font-weight: 700; text-align: center; }
    .logo span { color: var(--gold); font-weight: 500; }
    .muted.center { margin: -0.75rem 0 0.5rem; }
    .back { display: block; }
  `,
})
export class LoginComponent {
  readonly redirect = input<string>();

  private auth = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder).nonNullable;

  protected loading = signal(false);
  protected error = signal('');
  protected form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  constructor() {
    if (this.auth.token) this.router.navigate(['/admin']);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl(this.redirect() || '/admin'),
      error: (err) => {
        this.error.set(err.status === 401 || err.status === 403 ? 'E-mail ou senha inválidos.' : errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
