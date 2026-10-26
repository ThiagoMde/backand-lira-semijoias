import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Category, Product } from '../../core/models';
import { CategoryApi, ProductApi } from '../../core/services/api.services';
import { errorMessage, productPrice } from '../../core/utils';
import { ProductCardComponent } from '../../shared/product-card.component';

type Sort = 'recent' | 'price-asc' | 'price-desc' | 'name';

@Component({
  selector: 'app-catalog',
  imports: [FormsModule, ProductCardComponent],
  template: `
    <div class="container">
      <div class="page-header">
        <div>
          <h1>{{ title() }}</h1>
          <p class="muted small">{{ sorted().length }} produto(s)</p>
        </div>
        <div class="row">
          <select [ngModel]="sort()" (ngModelChange)="sort.set($event)" aria-label="Ordenar">
            <option value="recent">Mais recentes</option>
            <option value="price-asc">Menor preço</option>
            <option value="price-desc">Maior preço</option>
            <option value="name">Nome (A-Z)</option>
          </select>
        </div>
      </div>

      <div class="layout">
        <aside>
          <h3>Categorias</h3>
          <button class="filter" [class.active]="!categoria()" (click)="selectCategory(null)">Todas</button>
          @for (c of categories(); track c.id) {
            <button class="filter" [class.active]="categoria() === c.id" (click)="selectCategory(c.id)">{{ c.name }}</button>
          }
        </aside>

        <section>
          @if (busca()) {
            <div class="alert info row">
              Resultados para "<strong>{{ busca() }}</strong>"
              <span class="spacer"></span>
              <button class="btn sm ghost" (click)="clearSearch()">Limpar busca</button>
            </div>
          }
          @if (loading()) {
            <div class="loading">Carregando...</div>
          } @else if (error()) {
            <div class="alert">{{ error() }}</div>
          } @else if (sorted().length === 0) {
            <div class="empty">Nenhum produto encontrado.</div>
          } @else {
            <div class="grid">
              @for (p of sorted(); track p.id) {
                <app-product-card [product]="p" />
              }
            </div>
          }
        </section>
      </div>
    </div>
  `,
  styles: `
    select { width: auto; }
    .layout { display: grid; grid-template-columns: 200px 1fr; gap: 2rem; }
    aside { display: flex; flex-direction: column; gap: 0.25rem; }
    .filter {
      background: none;
      border: none;
      text-align: left;
      font: inherit;
      padding: 0.45rem 0.7rem;
      border-radius: 6px;
      cursor: pointer;
      color: var(--ink);
    }
    .filter:hover { background: var(--gold-soft); }
    .filter.active { background: var(--gold-soft); color: var(--gold-dark); font-weight: 600; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1.25rem; }
    @media (max-width: 720px) {
      .layout { grid-template-columns: 1fr; }
      aside { flex-direction: row; flex-wrap: wrap; }
      aside h3 { width: 100%; }
    }
  `,
})
export class CatalogComponent {
  /** Query params: ?busca=...&categoria=... */
  readonly busca = input<string>();
  readonly categoria = input<string>();

  private productApi = inject(ProductApi);
  private categoryApi = inject(CategoryApi);
  private router = inject(Router);

  protected products = signal<Product[]>([]);
  protected categories = signal<Category[]>([]);
  protected loading = signal(true);
  protected error = signal('');
  protected sort = signal<Sort>('recent');

  protected title = computed(() => {
    const id = this.categoria();
    return this.categories().find((c) => c.id === id)?.name ?? 'Todos os produtos';
  });

  protected sorted = computed(() => {
    const list = [...this.products()];
    switch (this.sort()) {
      case 'price-asc': return list.sort((a, b) => productPrice(a) - productPrice(b));
      case 'price-desc': return list.sort((a, b) => productPrice(b) - productPrice(a));
      case 'name': return list.sort((a, b) => a.name.localeCompare(b.name));
      default: return list.sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
    }
  });

  constructor() {
    this.categoryApi.list().subscribe({ next: (list) => this.categories.set(list) });

    effect(() => {
      const search = this.busca();
      const categoryId = this.categoria();
      this.loading.set(true);
      this.error.set('');
      this.productApi.list({ search, categoryId }).subscribe({
        next: (list) => {
          // O back end prioriza a busca; aplicamos a categoria também no cliente.
          this.products.set(categoryId ? list.filter((p) => p.categoryId === categoryId) : list);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(errorMessage(err));
          this.loading.set(false);
        },
      });
    });
  }

  selectCategory(id: string | null): void {
    this.router.navigate([], { queryParams: { categoria: id }, queryParamsHandling: 'merge' });
  }

  clearSearch(): void {
    this.router.navigate([], { queryParams: { busca: null }, queryParamsHandling: 'merge' });
  }
}
