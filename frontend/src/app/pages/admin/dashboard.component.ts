import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Dashboard, Order, Product } from '../../core/models';
import { DashboardApi, OrderApi, StockApi } from '../../core/services/api.services';
import { ORDER_STATUS_LABELS, errorMessage } from '../../core/utils';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  template: `
    <div class="page-header">
      <h1>Dashboard</h1>
      <a routerLink="/admin/produtos/novo" class="btn">+ Adicionar produto</a>
    </div>

    @if (error()) { <div class="alert">{{ error() }}</div> }

    @if (data(); as d) {
      <div class="stats">
        <a class="stat" routerLink="/admin/produtos"><span>Produtos</span><strong>{{ d.productsTotal }}</strong><small>{{ d.productsActive }} ativos</small></a>
        <a class="stat" routerLink="/admin/estoque"><span>Unidades em estoque</span><strong>{{ d.stockUnits }}</strong></a>
        <a class="stat warn" routerLink="/admin/estoque"><span>Estoque baixo</span><strong>{{ d.lowStock }}</strong></a>
        <a class="stat danger" routerLink="/admin/estoque"><span>Esgotados</span><strong>{{ d.outOfStock }}</strong></a>
        <a class="stat" routerLink="/admin/pedidos" [queryParams]="{ status: 'PENDING' }"><span>Pedidos pendentes</span><strong>{{ d.ordersPending }}</strong></a>
        <a class="stat" routerLink="/admin/pedidos" [queryParams]="{ status: 'CONFIRMED' }"><span>Pedidos confirmados</span><strong>{{ d.ordersConfirmed }}</strong></a>
        <a class="stat" routerLink="/admin/pedidos" [queryParams]="{ status: 'DELIVERED' }"><span>Pedidos entregues</span><strong>{{ d.ordersDelivered }}</strong></a>
      </div>
    } @else if (!error()) {
      <div class="loading">Carregando...</div>
    }

    <div class="two">
      <section>
        <div class="page-header"><h3>Pedidos pendentes</h3><a routerLink="/admin/pedidos">Ver todos</a></div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Pedido</th><th>Cliente</th><th>Total</th><th>Data</th></tr></thead>
            <tbody>
              @for (o of pending(); track o.id) {
                <tr>
                  <td><a [routerLink]="['/admin/pedidos', o.id]">{{ o.orderNumber }}</a></td>
                  <td>{{ o.customer.name }}</td>
                  <td>{{ o.total | currency }}</td>
                  <td>{{ o.createdAt | date: 'short' }}</td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="muted center">Nenhum pedido {{ statusLabels.PENDING.toLowerCase() }}.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      <section>
        <div class="page-header"><h3>Estoque baixo</h3><a routerLink="/admin/estoque">Gerenciar</a></div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Produto</th><th>Estoque</th><th>Mínimo</th></tr></thead>
            <tbody>
              @for (p of low(); track p.id) {
                <tr>
                  <td><a [routerLink]="['/admin/produtos', p.id]">{{ p.name }}</a></td>
                  <td><span class="badge" [class.danger]="p.stock === 0" [class.warning]="p.stock > 0">{{ p.stock }}</span></td>
                  <td>{{ p.minimumStock }}</td>
                </tr>
              } @empty {
                <tr><td colspan="3" class="muted center">Tudo em dia.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
  styles: `
    .stats { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 1rem; margin-bottom: 2rem; }
    .stat {
      background: var(--surface);
      border: 1px solid var(--line);
      border-left: 4px solid var(--gold);
      border-radius: var(--radius);
      padding: 1rem 1.1rem;
      display: flex;
      flex-direction: column;
      color: var(--ink);
    }
    .stat:hover { box-shadow: var(--shadow); }
    .stat.warn { border-left-color: var(--warning); }
    .stat.danger { border-left-color: var(--danger); }
    .stat span { font-size: 0.8rem; color: var(--muted); }
    .stat strong { font-size: 1.8rem; font-family: var(--serif); }
    .stat small { color: var(--muted); }
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
    .two .page-header { margin-bottom: 0.75rem; }
    @media (max-width: 900px) { .two { grid-template-columns: 1fr; } }
  `,
})
export class DashboardComponent {
  protected data = signal<Dashboard | null>(null);
  protected pending = signal<Order[]>([]);
  protected low = signal<Product[]>([]);
  protected error = signal('');
  protected statusLabels = ORDER_STATUS_LABELS;

  constructor() {
    inject(DashboardApi).get().subscribe({
      next: (d) => this.data.set(d),
      error: (err) => this.error.set(errorMessage(err)),
    });
    inject(OrderApi).adminList('PENDING').subscribe({ next: (list) => this.pending.set(list.slice(0, 6)) });
    inject(StockApi).low().subscribe({ next: (list) => this.low.set(list.slice(0, 6)) });
  }
}
