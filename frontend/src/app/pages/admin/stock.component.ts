import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product, StockMovement } from '../../core/models';
import { ProductApi, StockApi } from '../../core/services/api.services';
import { STOCK_MOVEMENT_LABELS, errorMessage } from '../../core/utils';
import { ToastService } from '../../shared/toast.component';

@Component({
  selector: 'app-stock',
  imports: [ReactiveFormsModule, DatePipe, RouterLink],
  template: `
    <div class="page-header"><h1>Estoque</h1></div>

    <div class="layout">
      <form class="card" [formGroup]="form" (ngSubmit)="submit()">
        <h3>Movimentar estoque</h3>
        <div class="tabs">
          <button type="button" [class.active]="mode() === 'entry'" (click)="mode.set('entry')">Entrada</button>
          <button type="button" [class.active]="mode() === 'exit'" (click)="mode.set('exit')">Saída</button>
        </div>
        <div class="field">
          <label for="product">Produto *</label>
          <select id="product" formControlName="productId" (change)="loadHistory()">
            <option value="" disabled>Selecione...</option>
            @for (p of products(); track p.id) {
              <option [value]="p.id">{{ p.name }} ({{ p.sku }}) — {{ p.stock }} un.</option>
            }
          </select>
        </div>
        <div class="field mt">
          <label for="qty">Quantidade *</label>
          <input id="qty" type="number" min="1" step="1" formControlName="quantity" />
        </div>
        <div class="field mt">
          <label for="reason">Motivo *</label>
          <input id="reason" formControlName="reason" [placeholder]="mode() === 'entry' ? 'Ex.: Reposição do fornecedor' : 'Ex.: Peça com defeito'" />
        </div>
        <button class="btn block mt" type="submit" [disabled]="saving()">
          Registrar {{ mode() === 'entry' ? 'entrada' : 'saída' }}
        </button>
      </form>

      <div class="col">
        <section>
          <h3>Estoque baixo</h3>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Produto</th><th>SKU</th><th>Atual</th><th>Mínimo</th><th></th></tr></thead>
              <tbody>
                @for (p of low(); track p.id) {
                  <tr>
                    <td><a [routerLink]="['/admin/produtos', p.id]">{{ p.name }}</a></td>
                    <td class="muted">{{ p.sku }}</td>
                    <td><span class="badge" [class.danger]="p.stock === 0" [class.warning]="p.stock > 0">{{ p.stock }}</span></td>
                    <td>{{ p.minimumStock }}</td>
                    <td class="actions"><button class="btn sm outline" (click)="select(p)">Repor</button></td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="muted center">Nenhum produto com estoque baixo.</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>

        <section>
          <div class="page-header">
            <h3>Movimentações {{ selectedName() ? '— ' + selectedName() : '' }}</h3>
            @if (form.controls.productId.value) {
              <button class="btn sm ghost" (click)="clearSelection()">Ver todas</button>
            }
          </div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Data</th><th>Produto</th><th>Tipo</th><th>Qtd.</th><th>Antes → Depois</th><th>Motivo</th></tr></thead>
              <tbody>
                @for (m of movements(); track m.id) {
                  <tr>
                    <td>{{ m.createdAt | date: 'short' }}</td>
                    <td>{{ productName(m.productId) }}</td>
                    <td><span class="badge" [class.success]="m.type === 'ENTRY' || m.type === 'RETURN'" [class.danger]="m.type === 'EXIT' || m.type === 'DAMAGE'">{{ typeLabels[m.type] }}</span></td>
                    <td>{{ m.quantity }}</td>
                    <td>{{ m.stockBefore }} → {{ m.stockAfter }}</td>
                    <td class="small">
                      {{ m.reason }}
                      @if (m.orderId) { <a [routerLink]="['/admin/pedidos', m.orderId]">(pedido)</a> }
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="6" class="muted center">Nenhuma movimentação.</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 320px 1fr; gap: 1.5rem; align-items: start; }
    .col { display: flex; flex-direction: column; gap: 1.5rem; min-width: 0; }
    .col .page-header { margin-bottom: 0.5rem; }
    .tabs { display: flex; border: 1px solid var(--line); border-radius: 999px; padding: 3px; margin-bottom: 1rem; }
    .tabs button { flex: 1; border: none; background: none; padding: 0.45rem; border-radius: 999px; cursor: pointer; font: inherit; }
    .tabs button.active { background: var(--gold); color: #fff; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class StockComponent implements OnInit {
  /** Query param ?produto=<id> (vindo da tela de produto). */
  readonly produto = input<string>();

  private stockApi = inject(StockApi);
  private productApi = inject(ProductApi);
  private toast = inject(ToastService);

  protected products = signal<Product[]>([]);
  protected low = signal<Product[]>([]);
  protected movements = signal<StockMovement[]>([]);
  protected mode = signal<'entry' | 'exit'>('entry');
  protected saving = signal(false);
  protected selectedId = signal('');
  protected typeLabels = STOCK_MOVEMENT_LABELS;

  protected form = inject(FormBuilder).nonNullable.group({
    productId: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    reason: ['', Validators.required],
  });

  protected selectedName = computed(() => this.products().find((p) => p.id === this.selectedId())?.name ?? '');

  ngOnInit(): void {
    this.productApi.adminList().subscribe({
      next: (list) => this.products.set(list.sort((a, b) => a.name.localeCompare(b.name))),
    });
    this.loadLow();
    const id = this.produto();
    if (id) this.form.controls.productId.setValue(id);
    this.loadHistory();
  }

  select(p: Product): void {
    this.mode.set('entry');
    this.form.patchValue({ productId: p.id, reason: 'Reposição de estoque' });
    this.loadHistory();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  clearSelection(): void {
    this.form.controls.productId.setValue('');
    this.loadHistory();
  }

  loadHistory(): void {
    const id = this.form.controls.productId.value;
    this.selectedId.set(id);
    const request = id ? this.stockApi.movementsByProduct(id) : this.stockApi.movements();
    request.subscribe({
      next: (list) => this.movements.set([...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 100)),
      error: (err) => this.toast.error(errorMessage(err)),
    });
  }

  productName(id: string): string {
    return this.products().find((p) => p.id === id)?.name ?? id;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = this.form.getRawValue();
    this.saving.set(true);
    const request = this.mode() === 'entry' ? this.stockApi.entry(body) : this.stockApi.exit(body);
    request.subscribe({
      next: (updated) => {
        this.products.update((list) => list.map((p) => (p.id === updated.id ? updated : p)));
        this.toast.success(`Estoque de ${updated.name}: ${updated.stock} un.`);
        this.form.patchValue({ quantity: 1, reason: '' });
        this.form.markAsUntouched();
        this.loadLow();
        this.loadHistory();
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(errorMessage(err));
        this.saving.set(false);
      },
    });
  }

  private loadLow(): void {
    this.stockApi.low().subscribe({ next: (list) => this.low.set(list) });
  }
}
