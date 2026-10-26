import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Order, OrderStatus } from '../../core/models';
import { OrderApi } from '../../core/services/api.services';
import { DELIVERY_TYPE_LABELS, ORDER_STATUS_LABELS, errorMessage } from '../../core/utils';
import { ToastService } from '../../shared/toast.component';
import { statusClass } from './orders.component';

/** Status que podem ser definidos via PATCH /status depois que o pedido foi confirmado. */
const FLOW: OrderStatus[] = ['CONFIRMED', 'PAID', 'PREPARING', 'READY', 'SHIPPED', 'DELIVERED'];

@Component({
  selector: 'app-order-detail',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  template: `
    <a routerLink="/admin/pedidos" class="small">← Pedidos</a>

    @if (error()) { <div class="alert mt">{{ error() }}</div> }

    @if (order(); as o) {
      <div class="page-header mt">
        <div>
          <h1>{{ o.orderNumber }}</h1>
          <span class="muted small">Criado em {{ o.createdAt | date: 'short' }} • Atualizado em {{ o.updatedAt | date: 'short' }}</span>
        </div>
        <span [class]="'badge big ' + statusClass(o.status)">{{ labels[o.status] }}</span>
      </div>

      <div class="layout">
        <div class="col">
          <section class="card">
            <h3>Itens</h3>
            <div class="table-wrap flat">
              <table>
                <thead><tr><th>Produto</th><th>SKU</th><th>Qtd.</th><th>Unitário</th><th>Subtotal</th></tr></thead>
                <tbody>
                  @for (i of o.items; track i.productId) {
                    <tr>
                      <td><a [routerLink]="['/admin/produtos', i.productId]">{{ i.name }}</a></td>
                      <td class="muted">{{ i.sku }}</td>
                      <td>{{ i.quantity }}</td>
                      <td>{{ i.unitPrice | currency }}</td>
                      <td>{{ i.subtotal | currency }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <div class="totals">
              <div><span>Subtotal</span><span>{{ o.subtotal | currency }}</span></div>
              <div><span>Entrega</span><span>{{ o.deliveryFee | currency }}</span></div>
              <div class="total"><span>Total</span><span>{{ o.total | currency }}</span></div>
            </div>
          </section>

          @if (o.notes) {
            <section class="card"><h3>Observações</h3><p>{{ o.notes }}</p></section>
          }
        </div>

        <div class="col">
          <section class="card">
            <h3>Ações</h3>
            @if (o.status === 'PENDING') {
              <p class="small muted">Confirmar dá baixa no estoque dos itens.</p>
              <button class="btn success block" [disabled]="busy()" (click)="run(api.confirm(o.id), 'Pedido confirmado.')">Confirmar pedido</button>
            } @else if (o.status !== 'CANCELLED') {
              <div class="field">
                <label for="status">Alterar status</label>
                <select id="status" #sel>
                  @for (s of flow; track s) { <option [value]="s" [selected]="s === o.status">{{ labels[s] }}</option> }
                </select>
              </div>
              <button class="btn block mt" [disabled]="busy()" (click)="changeStatus(sel.value)">Atualizar status</button>
            }
            @if (canCancel()) {
              <button class="btn danger block mt" [disabled]="busy()" (click)="cancel()">Cancelar pedido</button>
            }
            @if (o.status === 'CANCELLED') {
              <p class="muted small">Pedido cancelado. Nenhuma ação disponível.</p>
            }
          </section>

          <section class="card">
            <h3>Cliente</h3>
            <p><strong>{{ o.customer.name }}</strong></p>
            <p>{{ o.customer.phone }}</p>
            <a class="btn outline sm" [href]="whatsapp()" target="_blank" rel="noopener">Conversar no WhatsApp</a>
          </section>

          <section class="card">
            <h3>Entrega</h3>
            <p>{{ deliveryLabels[o.delivery.type] }}</p>
            @if (o.delivery.address; as a) {
              <p class="small">
                {{ a.street }}, {{ a.number }}@if (a.complement) { — {{ a.complement }} }<br />
                {{ a.district }} — {{ a.city }}/{{ a.state }}<br />
                CEP {{ a.zipCode }}
              </p>
            }
          </section>
        </div>
      </div>
    } @else if (!error()) {
      <div class="loading">Carregando...</div>
    }
  `,
  styles: `
    .badge.big { font-size: 0.9rem; padding: 0.35rem 0.9rem; }
    .layout { display: grid; grid-template-columns: 1fr 320px; gap: 1.5rem; align-items: start; }
    .col { display: flex; flex-direction: column; gap: 1.25rem; }
    .flat { border: none; }
    .totals { margin-top: 1rem; display: flex; flex-direction: column; gap: 0.35rem; align-items: flex-end; }
    .totals div { display: flex; gap: 2rem; min-width: 220px; justify-content: space-between; }
    .totals .total { font-weight: 600; font-size: 1.1rem; border-top: 1px solid var(--line); padding-top: 0.4rem; }
    p { margin: 0 0 0.4rem; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class OrderDetailComponent implements OnInit {
  readonly id = input.required<string>();

  protected api = inject(OrderApi);
  private toast = inject(ToastService);

  protected order = signal<Order | null>(null);
  protected error = signal('');
  protected busy = signal(false);
  protected labels = ORDER_STATUS_LABELS;
  protected deliveryLabels = DELIVERY_TYPE_LABELS;
  protected flow = FLOW;
  protected statusClass = statusClass;

  protected canCancel = computed(() => {
    const s = this.order()?.status;
    return !!s && !['CANCELLED', 'SHIPPED', 'DELIVERED'].includes(s);
  });

  protected whatsapp = computed(() => {
    const phone = (this.order()?.customer.phone ?? '').replace(/\D/g, '');
    const full = phone.length <= 11 ? `55${phone}` : phone;
    return `https://wa.me/${full}`;
  });

  ngOnInit(): void {
    this.api.getById(this.id()).subscribe({
      next: (o) => this.order.set(o),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  changeStatus(status: string): void {
    const o = this.order();
    if (!o || status === o.status) return;
    this.run(this.api.updateStatus(o.id, status as OrderStatus), 'Status atualizado.');
  }

  cancel(): void {
    const o = this.order();
    if (!o || !confirm('Cancelar este pedido? Itens já baixados voltam ao estoque.')) return;
    this.run(this.api.cancel(o.id), 'Pedido cancelado.');
  }

  run(request: Observable<Order>, message: string): void {
    this.busy.set(true);
    request.subscribe({
      next: (o) => {
        this.order.set(o);
        this.toast.success(message);
        this.busy.set(false);
      },
      error: (err) => {
        this.toast.error(errorMessage(err));
        this.busy.set(false);
      },
    });
  }
}
