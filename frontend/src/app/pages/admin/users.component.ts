import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { User, UserRole } from '../../core/models';
import { UserApi } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../core/utils';
import { ToastService } from '../../shared/toast.component';

@Component({
  selector: 'app-admin-users',
  imports: [ReactiveFormsModule, DatePipe],
  template: `
    <div class="page-header"><h1>Usuários</h1></div>

    <div class="layout">
      <form class="card" [formGroup]="form" (ngSubmit)="create()">
        <h3>Novo usuário</h3>
        <div class="field"><label for="name">Nome *</label><input id="name" formControlName="name" /></div>
        <div class="field mt"><label for="email">E-mail *</label><input id="email" type="email" formControlName="email" /></div>
        <div class="field mt">
          <label for="password">Senha *</label>
          <input id="password" type="password" formControlName="password" autocomplete="new-password" />
          <span class="hint">Mínimo de 8 caracteres.</span>
        </div>
        <div class="field mt">
          <label for="role">Perfil *</label>
          <select id="role" formControlName="role">
            <option value="EMPLOYEE">Funcionário (produtos, pedidos e estoque)</option>
            <option value="ADMIN">Administrador (acesso total)</option>
          </select>
        </div>
        <button class="btn block mt" type="submit" [disabled]="saving()">Criar usuário</button>
      </form>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Nome</th><th>E-mail</th><th>Perfil</th><th>Desde</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (u of users(); track u.id) {
              <tr>
                <td><strong>{{ u.name }}</strong></td>
                <td>{{ u.email }}</td>
                <td><span class="badge">{{ roleLabel(u.role) }}</span></td>
                <td class="small">{{ u.createdAt | date: 'shortDate' }}</td>
                <td><span class="badge" [class.success]="u.active" [class.neutral]="!u.active">{{ u.active ? 'Ativo' : 'Inativo' }}</span></td>
                <td class="actions">
                  @if (u.email !== auth.user()?.email) {
                    <button class="btn sm ghost" (click)="toggle(u)">{{ u.active ? 'Desativar' : 'Ativar' }}</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="empty">Nenhum usuário.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 320px 1fr; gap: 1.5rem; align-items: start; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class AdminUsersComponent {
  private api = inject(UserApi);
  private toast = inject(ToastService);
  protected auth = inject(AuthService);

  protected users = signal<User[]>([]);
  protected saving = signal(false);
  protected form = inject(FormBuilder).nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['EMPLOYEE' as UserRole],
  });

  constructor() {
    this.load();
  }

  roleLabel(role: UserRole): string {
    return role === 'ADMIN' ? 'Administrador' : 'Funcionário';
  }

  load(): void {
    this.api.list().subscribe({
      next: (list) => this.users.set(list),
      error: (err) => this.toast.error(errorMessage(err)),
    });
  }

  create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.api.create(this.form.getRawValue()).subscribe({
      next: (user) => {
        this.users.update((list) => [...list, user]);
        this.form.reset();
        this.toast.success('Usuário criado.');
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(errorMessage(err));
        this.saving.set(false);
      },
    });
  }

  toggle(u: User): void {
    this.api.setActive(u.id, !u.active).subscribe({
      next: (updated) => this.users.update((list) => list.map((x) => (x.id === updated.id ? updated : x))),
      error: (err) => this.toast.error(errorMessage(err)),
    });
  }
}
