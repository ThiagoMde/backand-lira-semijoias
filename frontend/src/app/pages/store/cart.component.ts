import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe, RouterLink],
  template: `
    <div class="container">
      <h1>Sua sacola</h1>

      @if (cart.items().length === 0) {
        <div class="card empty">
          <p>Sua sacola está vazia.</p>
          <a routerLink="/produtos" class="btn">Ver produtos</a>
        </div>
      } @else {
        <div class="layout">
          <div class="card items">
            @for (item of cart.items(); track item.productId) {
              <div class="item">
                <a [routerLink]="['/produto', item.slug]">
                  @if (item.image) {
                    <img [src]="item.image" [alt]="item.name" class="img" />
                  } @else {
                    <div class="img img-placeholder">L</div>
                  }
                </a>
                <div class="grow">
                  <a [routerLink]="['/produto', item.slug]" class="name">{{ item.name }}</a>
                  <div class="muted small">{{ item.unitPrice | currency }} cada</div>
                </div>
                <div class="qty">
                  <button (click)="cart.setQuantity(item.productId, item.quantity - 1)">−</button>
                  <span>{{ item.quantity }}</span>
                  <button (click)="cart.setQuantity(item.productId, item.quantity + 1)" [disabled]="item.quantity >= item.stock">+</button>
                </div>
                <div class="subtotal">{{ item.unitPrice * item.quantity | currency }}</div>
                <button class="btn ghost sm" (click)="cart.remove(item.productId)" aria-label="Remover">✕</button>
              </div>
            }
          </div>

          <div class="card summary">
            <h3>Resumo</h3>
            <div class="line"><span>Subtotal</span><span>{{ cart.total() | currency }}</span></div>
            <div class="line muted small"><span>Entrega</span><span>Combinada no WhatsApp</span></div>
            <div class="line total"><span>Total</span><span>{{ cart.total() | currency }}</span></div>
            <a routerLink="/checkout" class="btn block">Finalizar pedido</a>
            <a routerLink="/produtos" class="btn ghost block mt">Continuar comprando</a>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr 320px; gap: 1.5rem; align-items: start; }
    .items { padding: 0.5rem 1.25rem; }
    .item { display: flex; align-items: center; gap: 1rem; padding: 1rem 0; border-bottom: 1px solid var(--line); }
    .item:last-child { border-bottom: none; }
    .img { width: 72px; height: 72px; border-radius: 8px; object-fit: cover; font-size: 1.4rem; }
    .grow { flex: 1; min-width: 0; }
    .name { color: var(--ink); font-weight: 500; }
    .qty { display: inline-flex; align-items: center; border: 1px solid var(--line); border-radius: 999px; }
    .qty button { border: none; background: none; width: 30px; height: 32px; cursor: pointer; }
    .qty button:disabled { opacity: 0.35; }
    .qty span { min-width: 22px; text-align: center; }
    .subtotal { width: 100px; text-align: right; font-weight: 600; }
    .line { display: flex; justify-content: space-between; margin-bottom: 0.6rem; }
    .total { font-size: 1.15rem; font-weight: 600; border-top: 1px solid var(--line); padding-top: 0.75rem; margin-bottom: 1.25rem; }
    @media (max-width: 760px) {
      .layout { grid-template-columns: 1fr; }
      .item { flex-wrap: wrap; }
      .subtotal { width: auto; }
    }
  `,
})
export class CartComponent {
  protected cart = inject(CartService);
}
