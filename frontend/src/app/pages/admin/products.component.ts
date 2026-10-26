import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Category, Product } from '../../core/models';
import { CategoryApi, ProductApi } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage, productImage } from '../../core/utils';
import { ToastService } from '../../shared/toast.component';

@Component({
  selector: 'app-admin-products',
  imports: [RouterLink, CurrencyPipe, FormsModule],
  template: `
    <div class="page-header">
      <h1>Produtos</h1>
      <a routerLink="/admin/produtos/novo" class="btn">+ Adicionar produto</a>
    </div>

    <div class="row mb filters">
      <input type="search" placeholder="Buscar por nome ou SKU..." [ngModel]="search()" (ngModelChange)="search.set($event)" />
      <select [ngModel]="categoryId()" (ngModelChange)="categoryId.set($event)">
        <option value="">Todas as categorias</option>
        @for (c of categories(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
      </select>
      <select [ngModel]="status()" (ngModelChange)="status.set($event)">
        <option value="">Todos</option>
        <option value="active">Ativos</option>
        <option value="inactive">Inativos</option>
      </select>
    </div>

    @if (error()) { <div class="alert">{{ error() }}</div> }

    @if (loading()) {
      <div class="loading">Carregando...</div>
    } @else {
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th></th><th>Produto</th><th>SKU</th><th>Categoria</th><th>Preço</th><th>Estoque</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            @for (p of filtered(); track p.id) {
              <tr>
                <td>
                  @if (image(p); as src) { <img class="thumb" [src]="src" [alt]="p.name" /> }
                  @else { <div class="thumb img-placeholder small">L</div> }
                </td>
                <td>
                  <a [routerLink]="['/admin/produtos', p.id]"><strong>{{ p.name }}</strong></a>
                  @if (p.featured) { <span class="badge">Destaque</span> }
                </td>
                <td class="muted">{{ p.sku }}</td>
                <td>{{ categoryName(p.categoryId) }}</td>
                <td>
                  @if (p.promotionalPrice) {
                    <span class="price"><span class="old">{{ p.price | currency }}</span>{{ p.promotionalPrice | currency }}</span>
                  } @else {
                    {{ p.price | currency }}
                  }
                </td>
                <td>
                  <span class="badge" [class.danger]="p.stock === 0" [class.warning]="p.stock > 0 && p.stock <= p.minimumStock" [class.success]="p.stock > p.minimumStock">{{ p.stock }}</span>
                </td>
                <td>
                  <span class="badge" [class.success]="p.active" [class.neutral]="!p.active">{{ p.active ? 'Ativo' : 'Inativo' }}</span>
                </td>
                <td class="actions">
                  <a class="btn sm outline" [routerLink]="['/admin/produtos', p.id]">Editar</a>
                  @if (auth.isAdmin()) {
                    <button class="btn sm ghost" (click)="toggle(p)">{{ p.active ? 'Desativar' : 'Ativar' }}</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="empty">
                Nenhum produto encontrado. <a routerLink="/admin/produtos/novo">Cadastre o primeiro produto</a>.
              </td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: `
    .filters input { max-width: 320px; }
    .filters select { width: auto; }
    .thumb.img-placeholder { font-size: 1rem; }
    td .badge { margin-left: 0.35rem; }
    td:nth-child(6) .badge, td:nth-child(7) .badge { margin-left: 0; }
  `,
})
export class AdminProductsComponent {
  private productApi = inject(ProductApi);
  private categoryApi = inject(CategoryApi);
  private toast = inject(ToastService);
  protected auth = inject(AuthService);

  protected products = signal<Product[]>([]);
  protected categories = signal<Category[]>([]);
  protected loading = signal(true);
  protected error = signal('');
  protected search = signal('');
  protected categoryId = signal('');
  protected status = signal('');

  protected filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.products()
      .filter((p) => !q || p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q))
      .filter((p) => !this.categoryId() || p.categoryId === this.categoryId())
      .filter((p) => !this.status() || (this.status() === 'active') === p.active)
      .sort((a, b) => a.name.localeCompare(b.name));
  });

  protected image = productImage;

  constructor() {
    this.productApi.adminList().subscribe({
      next: (list) => {
        this.products.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
    const categories$ = this.auth.isAdmin() ? this.categoryApi.adminList() : this.categoryApi.list();
    categories$.subscribe({ next: (list) => this.categories.set(list) });
  }

  categoryName(id: string): string {
    return this.categories().find((c) => c.id === id)?.name ?? '—';
  }

  toggle(p: Product): void {
    this.productApi.setActive(p.id, !p.active).subscribe({
      next: (updated) => {
        this.products.update((list) => list.map((x) => (x.id === updated.id ? updated : x)));
        this.toast.success(`Produto ${updated.active ? 'ativado' : 'desativado'}.`);
      },
      error: (err) => this.toast.error(errorMessage(err)),
    });
  }
}
