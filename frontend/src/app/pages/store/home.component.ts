import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Category, Product } from '../../core/models';
import { CategoryApi, ProductApi } from '../../core/services/api.services';
import { errorMessage } from '../../core/utils';
import { ProductCardComponent } from '../../shared/product-card.component';

@Component({
  selector: 'app-home',
  imports: [RouterLink, ProductCardComponent],
  template: `
    <section class="hero">
      <div class="container hero-inner">
        <p class="eyebrow">Nova coleção</p>
        <h1>Brilho delicado para<br />todos os seus momentos</h1>
        <p class="muted">Semijoias banhadas com acabamento premium e garantia.</p>
        <a routerLink="/produtos" class="btn">Ver todos os produtos</a>
      </div>
    </section>

    <div class="container">
      @if (categories().length) {
        <section class="section">
          <h2>Categorias</h2>
          <div class="categories">
            @for (c of categories(); track c.id) {
              <a class="category" [routerLink]="['/produtos']" [queryParams]="{ categoria: c.id }">{{ c.name }}</a>
            }
          </div>
        </section>
      }

      <section class="section">
        <div class="page-header">
          <h2>{{ featured().length ? 'Destaques' : 'Novidades' }}</h2>
          <a routerLink="/produtos">Ver tudo →</a>
        </div>
        @if (loading()) {
          <div class="loading">Carregando produtos...</div>
        } @else if (error()) {
          <div class="alert">{{ error() }}</div>
        } @else if (showcase().length === 0) {
          <div class="empty">Nenhum produto disponível no momento.</div>
        } @else {
          <div class="grid">
            @for (p of showcase(); track p.id) {
              <app-product-card [product]="p" />
            }
          </div>
        }
      </section>
    </div>
  `,
  styles: `
    :host { display: block; margin-top: -2rem; }
    .hero {
      background: linear-gradient(120deg, #f3ead8 0%, #fbf8f4 55%, #f6e3e0 100%);
      padding: 4.5rem 0;
      border-bottom: 1px solid var(--line);
    }
    .hero-inner { max-width: 1180px; }
    .hero h1 { font-size: clamp(2rem, 5vw, 3.2rem); margin-bottom: 0.75rem; }
    .hero .btn { margin-top: 1rem; }
    .eyebrow { text-transform: uppercase; letter-spacing: 0.2em; font-size: 0.75rem; color: var(--gold-dark); margin: 0 0 0.5rem; }
    .section { margin-top: 2.5rem; }
    .categories { display: flex; gap: 0.6rem; flex-wrap: wrap; }
    .category {
      padding: 0.55rem 1.2rem;
      border: 1px solid var(--line);
      border-radius: 999px;
      background: var(--surface);
      color: var(--ink);
      font-size: 0.9rem;
    }
    .category:hover { border-color: var(--gold); color: var(--gold-dark); }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 1.25rem; }
  `,
})
export class HomeComponent {
  private productApi = inject(ProductApi);
  private categoryApi = inject(CategoryApi);

  protected products = signal<Product[]>([]);
  protected categories = signal<Category[]>([]);
  protected loading = signal(true);
  protected error = signal('');

  protected featured = computed(() => this.products().filter((p) => p.featured));
  protected showcase = computed(() => (this.featured().length ? this.featured() : this.products()).slice(0, 8));

  constructor() {
    this.productApi.list().subscribe({
      next: (list) => {
        this.products.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
    this.categoryApi.list().subscribe({ next: (list) => this.categories.set(list) });
  }
}
