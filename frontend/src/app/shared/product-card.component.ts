import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Product } from '../core/models';
import { CartService } from '../core/services/cart.service';
import { productImage } from '../core/utils';
import { ToastService } from './toast.component';

@Component({
  selector: 'app-product-card',
  imports: [RouterLink, CurrencyPipe],
  template: `
    <article class="product">
      <a [routerLink]="['/produto', product().slug]" class="image">
        @if (image(); as src) {
          <img [src]="src" [alt]="product().name" loading="lazy" />
        } @else {
          <div class="img-placeholder">L</div>
        }
        @if (onSale()) { <span class="tag">Promoção</span> }
        @if (product().stock === 0) { <span class="tag out">Esgotado</span> }
      </a>
      <div class="info">
        <a [routerLink]="['/produto', product().slug]" class="name">{{ product().name }}</a>
        <div class="price">
          @if (onSale()) {
            <span class="old">{{ product().price | currency }}</span>
            {{ product().promotionalPrice | currency }}
          } @else {
            {{ product().price | currency }}
          }
        </div>
        <button class="btn sm outline" [disabled]="product().stock === 0" (click)="addToCart()">
          Adicionar à sacola
        </button>
      </div>
    </article>
  `,
  styles: `
    .product {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      transition: box-shadow 0.2s, transform 0.2s;
    }
    .product:hover { box-shadow: var(--shadow); transform: translateY(-2px); }
    .image { position: relative; aspect-ratio: 1; display: block; }
    .image img, .image .img-placeholder { width: 100%; height: 100%; object-fit: cover; }
    .tag {
      position: absolute;
      top: 10px;
      left: 10px;
      background: var(--rose);
      color: #fff;
      font-size: 0.72rem;
      padding: 0.15rem 0.55rem;
      border-radius: 999px;
    }
    .tag.out { background: var(--ink); left: auto; right: 10px; }
    .info { padding: 0.9rem; display: flex; flex-direction: column; gap: 0.45rem; flex: 1; }
    .name { color: var(--ink); font-weight: 500; }
    .info .btn { margin-top: auto; }
  `,
})
export class ProductCardComponent {
  readonly product = input.required<Product>();

  private cart = inject(CartService);
  private toast = inject(ToastService);

  protected image = computed(() => productImage(this.product()));
  protected onSale = computed(() => {
    const p = this.product();
    return !!p.promotionalPrice && p.promotionalPrice < p.price;
  });

  addToCart(): void {
    this.cart.add(this.product());
    this.toast.success(`${this.product().name} adicionado à sacola.`);
  }
}
