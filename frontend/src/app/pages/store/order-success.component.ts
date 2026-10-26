import { CurrencyPipe } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CheckoutResponse } from '../../core/models';
import { LAST_ORDER_KEY } from './checkout.component';

@Component({
  selector: 'app-order-success',
  imports: [CurrencyPipe, RouterLink],
  template: `
    <div class="container narrow">
      <div class="card center">
        @if (order; as o) {
          <div class="check">✓</div>
          <h1>Pedido realizado!</h1>
          <p>Número do pedido: <strong>{{ o.orderNumber }}</strong></p>
          <p>Total: <strong>{{ o.total | currency }}</strong></p>
          <p class="muted">Envie a mensagem pelo WhatsApp para combinarmos o pagamento e a entrega.</p>
          <a [href]="o.whatsappUrl" target="_blank" rel="noopener" class="btn success">Enviar pedido pelo WhatsApp</a>
          <div class="mt"><a routerLink="/produtos">Continuar comprando</a></div>
        } @else {
          <p>Nenhum pedido recente encontrado.</p>
          <a routerLink="/" class="btn">Voltar à loja</a>
        }
      </div>
    </div>
  `,
  styles: `
    .narrow { max-width: 560px; }
    .card { padding: 2.5rem 1.5rem; }
    .check {
      width: 64px;
      height: 64px;
      margin: 0 auto 1rem;
      border-radius: 50%;
      background: var(--success-soft);
      color: var(--success);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 2rem;
    }
  `,
})
export class OrderSuccessComponent {
  protected order: CheckoutResponse | null = read();
}

function read(): CheckoutResponse | null {
  try {
    return JSON.parse(sessionStorage.getItem(LAST_ORDER_KEY) ?? 'null');
  } catch {
    return null;
  }
}
