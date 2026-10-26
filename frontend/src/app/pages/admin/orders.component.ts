import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Order, OrderStatus } from '../../core/models';
import { OrderApi } from '../../core/services/api.services';
import { DELIVERY_TYPE_LABELS, ORDER_STATUSES, ORDER_STATUS_LABELS, errorMessage } from '../../core/utils';

export function statusClass(status: OrderStatus): string {
  switch (status) {
    case 'PENDING': return 'warning';
    case 'CANCELLED': return 'danger';
    case 'DELIVERED': return 'success';
    default: return '';
  }
}

@Component({
  selector: 'app-admin-orders',
  imports: [RouterLink, CurrencyPipe, DatePipe, FormsModule],
  template: `
    <div class="page-header">
      <h1>Pedidos</h1>
      <select [ngModel]="status() ?? ''" (ngModelChange)="filter($event)" aria-label="Filtrar status">
        <option value="">Todos os status</option>
        @for (s of statuses; track s) { <option [value]="s">{{ labels[s] }}</option> }
      </select>
    </div>

    @if (error()) { <div class="alert">{{ error() }}</div> }

    @if (loading()) {
      <div class="loading">Carregando...</div>
    } @else {
      <div class="table-wrap">
        <table>
          <thead><tr><th>Pedido</th><th>Data</th><th>Cliente</th><th>Entrega</th><th>Itens</th><th>Total</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (o of orders(); track o.id) {
              <tr>
                <td><a [routerLink]="['/admin/pedidos', o.id]"><strong>{{ o.orderNumber }}</strong></a></td>
                <td>{{ o.createdAt | date: 'short' }}</td>
                <td>{{ o.customer.name }}<div class="muted small">{{ o.customer.phone }}</div></td>
                <td class="small">{{ deliveryLabels[o.delivery.type] }}</td>
                <td>{{ itemCount(o) }}</td>
                <td>{{ o.total | currency }}</td>
                <td><span [class]="'badge ' + statusClass(o.status)">{{ labels[o.status] }}</span></td>
                <td class="actions"><a class="btn sm outline" [routerLink]="['/admin/pedidos', o.id]">Abrir</a></td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="empty">Nenhum pedido encontrado.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: `select { width: auto; }`,
})
export class AdminOrdersComponent {
  /** Query param ?status=PENDING */
  readonly status = input<OrderStatus>();

  private api = inject(OrderApi);
  private router = inject(Router);

  protected orders = signal<Order[]>([]);
  protected loading = signal(true);
  protected error = signal('');
  protected statuses = ORDER_STATUSES;
  protected labels = ORDER_STATUS_LABELS;
  protected deliveryLabels = DELIVERY_TYPE_LABELS;
  protected statusClass = statusClass;

  constructor() {
    effect(() => {
      const status = this.status();
      this.loading.set(true);
      this.api.adminList(status).subscribe({
        next: (list) => {
          this.orders.set(list);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(errorMessage(err));
          this.loading.set(false);
        },
      });
    });
  }

  filter(status: string): void {
    this.router.navigate([], { queryParams: { status: status || null } });
  }

  itemCount(o: Order): number {
    return o.items.reduce((sum, i) => sum + i.quantity, 0);
  }
}
