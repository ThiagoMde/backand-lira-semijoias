import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Category } from '../../core/models';
import { CategoryApi } from '../../core/services/api.services';
import { errorMessage } from '../../core/utils';
import { ToastService } from '../../shared/toast.component';

@Component({
  selector: 'app-admin-categories',
  imports: [ReactiveFormsModule],
  template: `
    <div class="page-header"><h1>Categorias</h1></div>

    <div class="layout">
      <form class="card" [formGroup]="form" (ngSubmit)="save()">
        <h3>{{ editing() ? 'Editar categoria' : 'Nova categoria' }}</h3>
        <div class="field">
          <label for="name">Nome *</label>
          <input id="name" formControlName="name" placeholder="Ex.: Brincos" />
        </div>
        <div class="field mt">
          <label for="description">Descrição</label>
          <textarea id="description" formControlName="description"></textarea>
        </div>
        <div class="row mt">
          <button class="btn" type="submit" [disabled]="saving()">{{ editing() ? 'Salvar' : 'Criar categoria' }}</button>
          @if (editing()) { <button class="btn ghost" type="button" (click)="cancel()">Cancelar</button> }
        </div>
      </form>

      <div class="table-wrap">
        <table>
          <thead><tr><th>Nome</th><th>Slug</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (c of categories(); track c.id) {
              <tr>
                <td><strong>{{ c.name }}</strong>@if (c.description) { <div class="muted small">{{ c.description }}</div> }</td>
                <td class="muted">{{ c.slug }}</td>
                <td><span class="badge" [class.success]="c.active" [class.neutral]="!c.active">{{ c.active ? 'Ativa' : 'Inativa' }}</span></td>
                <td class="actions">
                  <button class="btn sm outline" (click)="edit(c)">Editar</button>
                  <button class="btn sm ghost" (click)="toggle(c)">{{ c.active ? 'Desativar' : 'Ativar' }}</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="empty">Nenhuma categoria cadastrada.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 320px 1fr; gap: 1.5rem; align-items: start; }
    @media (max-width: 860px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class AdminCategoriesComponent {
  private api = inject(CategoryApi);
  private toast = inject(ToastService);

  protected categories = signal<Category[]>([]);
  protected editing = signal<Category | null>(null);
  protected saving = signal(false);
  protected form = inject(FormBuilder).nonNullable.group({
    name: ['', Validators.required],
    description: [''],
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.api.adminList().subscribe({
      next: (list) => this.categories.set(list.sort((a, b) => a.name.localeCompare(b.name))),
      error: (err) => this.toast.error(errorMessage(err)),
    });
  }

  edit(c: Category): void {
    this.editing.set(c);
    this.form.setValue({ name: c.name, description: c.description ?? '' });
  }

  cancel(): void {
    this.editing.set(null);
    this.form.reset();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = this.form.getRawValue();
    const current = this.editing();
    this.saving.set(true);
    (current ? this.api.update(current.id, body) : this.api.create(body)).subscribe({
      next: () => {
        this.toast.success(current ? 'Categoria atualizada.' : 'Categoria criada.');
        this.cancel();
        this.load();
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(errorMessage(err));
        this.saving.set(false);
      },
    });
  }

  toggle(c: Category): void {
    this.api.setActive(c.id, !c.active).subscribe({
      next: (updated) => this.categories.update((list) => list.map((x) => (x.id === updated.id ? updated : x))),
      error: (err) => this.toast.error(errorMessage(err)),
    });
  }
}
