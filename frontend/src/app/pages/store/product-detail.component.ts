import { CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { Category, Product } from '../../core/models';
import { CategoryApi, ProductApi } from '../../core/services/api.services';
import { CartService } from '../../core/services/cart.service';
import { errorMessage, productImage } from '../../core/utils';
import { ToastService } from '../../shared/toast.component';

@Component({
  selector: 'app-product-detail',
  imports: [CurrencyPipe, RouterLink],
  template: `
    <div class="container">
      @if (loading()) {
        <div class="loading">Carregando...</div>
      } @else if (error()) {
        <div class="alert">{{ error() }}</div>
        <a routerLink="/produtos" class="btn outline">Voltar aos produtos</a>
      } @else if (product(); as p) {
        <nav class="crumbs small muted">
          <a routerLink="/produtos">Produtos</a>
          @if (category(); as c) {
            / <a [routerLink]="['/produtos']" [queryParams]="{ categoria: c.id }">{{ c.name }}</a>
          }
          / {{ p.name }}
        </nav>

        <div class="detail">
          <div class="gallery">
            <div class="main-image">
              @if (selectedImage(); as src) {
                <img [src]="src" [alt]="p.name" />
              } @else {
                <div class="img-placeholder">L</div>
              }
            </div>
            @if (p.images.length > 1) {
              <div class="thumbs">
                @for (img of p.images; track img.publicId) {
                  <button [class.active]="selectedImage() === img.url" (click)="selected.set(img.url)">
                    <img [src]="img.url" [alt]="p.name" />
                  </button>
                }
              </div>
            }
          </div>

          <div class="info">
            <h1>{{ p.name }}</h1>
            <p class="muted small">Cód.: {{ p.sku }}</p>
            <div class="price big">
              @if (p.promotionalPrice && p.promotionalPrice < p.price) {
                <span class="old">{{ p.price | currency }}</span>
                {{ p.promotionalPrice | currency }}
              } @else {
                {{ p.price | currency }}
              }
            </div>

            <p class="description">{{ p.description }}</p>

            <dl>
              @if (p.material) { <dt>Material</dt><dd>{{ p.material }}</dd> }
              @if (p.color) { <dt>Cor</dt><dd>{{ p.color }}</dd> }
              <dt>Disponibilidade</dt>
              <dd>
                @if (p.stock > 0) {
                  <span class="badge success">{{ p.stock }} em estoque</span>
                } @else {
                  <span class="badge danger">Esgotado</span>
                }
              </dd>
            </dl>

            @if (p.stock > 0) {
              <div class="row buy">
                <div class="qty">
                  <button (click)="quantity.set(quantity() - 1)" [disabled]="quantity() <= 1">−</button>
                  <span>{{ quantity() }}</span>
                  <button (click)="quantity.set(quantity() + 1)" [disabled]="quantity() >= p.stock">+</button>
                </div>
                <button class="btn" (click)="add(false)">Adicionar à sacola</button>
                <button class="btn outline" (click)="add(true)">Comprar agora</button>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .crumbs { margin-bottom: 1.25rem; }
    .detail { display: grid; grid-template-columns: 1fr 1fr; gap: 2.5rem; }
    .main-image { aspect-ratio: 1; border-radius: var(--radius); overflow: hidden; border: 1px solid var(--line); background: #fff; }
    .main-image img, .main-image .img-placeholder { width: 100%; height: 100%; object-fit: cover; }
    .thumbs { display: flex; gap: 0.5rem; margin-top: 0.75rem; flex-wrap: wrap; }
    .thumbs button { padding: 0; border: 2px solid transparent; border-radius: 8px; overflow: hidden; cursor: pointer; width: 70px; height: 70px; background: none; }
    .thumbs button.active { border-color: var(--gold); }
    .thumbs img { width: 100%; height: 100%; object-fit: cover; }
    .price.big { font-size: 1.6rem; margin: 0.75rem 0 1rem; }
    .description { white-space: pre-line; }
    dl { display: grid; grid-template-columns: max-content 1fr; gap: 0.4rem 1rem; margin: 1.25rem 0; }
    dt { color: var(--muted); font-size: 0.85rem; }
    dd { margin: 0; }
    .buy { margin-top: 1.5rem; }
    .qty { display: inline-flex; align-items: center; border: 1px solid var(--line); border-radius: 999px; background: #fff; }
    .qty button { border: none; background: none; width: 36px; height: 38px; font-size: 1.1rem; cursor: pointer; }
    .qty button:disabled { opacity: 0.35; cursor: default; }
    .qty span { min-width: 28px; text-align: center; }
    @media (max-width: 760px) { .detail { grid-template-columns: 1fr; } }
  `,
})
export class ProductDetailComponent {
  readonly slug = input.required<string>();

  private productApi = inject(ProductApi);
  private categoryApi = inject(CategoryApi);
  private cart = inject(CartService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private title = inject(Title);

  protected product = signal<Product | null>(null);
  protected categories = signal<Category[]>([]);
  protected loading = signal(true);
  protected error = signal('');
  protected selected = signal<string | null>(null);
  protected quantity = signal(1);

  protected selectedImage = computed(() => {
    const p = this.product();
    return this.selected() ?? (p ? productImage(p) : null);
  });
  protected category = computed(() => this.categories().find((c) => c.id === this.product()?.categoryId));

  constructor() {
    this.categoryApi.list().subscribe({ next: (list) => this.categories.set(list) });

    effect(() => {
      const slug = this.slug();
      this.loading.set(true);
      this.selected.set(null);
      this.quantity.set(1);
      this.productApi.getBySlug(slug).subscribe({
        next: (p) => {
          this.product.set(p);
          this.title.setTitle(`${p.name} • Lira Semijoias`);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(errorMessage(err, 'Produto não encontrado.'));
          this.loading.set(false);
        },
      });
    });
  }

  add(goToCart: boolean): void {
    const p = this.product();
    if (!p) return;
    this.cart.add(p, this.quantity());
    if (goToCart) {
      this.router.navigate(['/carrinho']);
    } else {
      this.toast.success(`${p.name} adicionado à sacola.`);
    }
  }
}
