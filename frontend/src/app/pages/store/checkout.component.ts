import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CheckoutRequest, DeliveryType } from '../../core/models';
import { OrderApi } from '../../core/services/api.services';
import { CartService } from '../../core/services/cart.service';
import { DELIVERY_TYPE_LABELS, errorMessage } from '../../core/utils';

export const LAST_ORDER_KEY = 'lira.lastOrder';

@Component({
  selector: 'app-checkout',
  imports: [ReactiveFormsModule, CurrencyPipe, RouterLink],
  template: `
    <div class="container">
      <h1>Finalizar pedido</h1>

      @if (cart.items().length === 0) {
        <div class="card empty">
          <p>Sua sacola está vazia.</p>
          <a routerLink="/produtos" class="btn">Ver produtos</a>
        </div>
      } @else {
        <form class="layout" [formGroup]="form" (ngSubmit)="submit()">
          <div class="steps">
            <div class="card">
              <h3>Seus dados</h3>
              <div class="form-grid" formGroupName="customer">
                <div class="field">
                  <label for="name">Nome completo *</label>
                  <input id="name" formControlName="name" autocomplete="name" />
                </div>
                <div class="field">
                  <label for="phone">WhatsApp *</label>
                  <input id="phone" formControlName="phone" placeholder="(83) 99999-9999" autocomplete="tel" />
                </div>
              </div>
            </div>

            <div class="card">
              <h3>Entrega</h3>
              <div class="options">
                @for (type of deliveryTypes; track type) {
                  <label class="option" [class.active]="form.controls.deliveryType.value === type">
                    <input type="radio" formControlName="deliveryType" [value]="type" />
                    {{ deliveryLabels[type] }}
                  </label>
                }
              </div>

              @if (form.controls.deliveryType.value === 'DELIVERY') {
                <div class="form-grid mt" formGroupName="address">
                  <div class="field"><label>CEP *</label><input formControlName="zipCode" autocomplete="postal-code" /></div>
                  <div class="field full"><label>Rua *</label><input formControlName="street" autocomplete="address-line1" /></div>
                  <div class="field"><label>Número *</label><input formControlName="number" /></div>
                  <div class="field"><label>Complemento</label><input formControlName="complement" /></div>
                  <div class="field"><label>Bairro *</label><input formControlName="district" /></div>
                  <div class="field"><label>Cidade *</label><input formControlName="city" autocomplete="address-level2" /></div>
                  <div class="field"><label>Estado *</label><input formControlName="state" maxlength="2" placeholder="PB" /></div>
                </div>
              }
            </div>

            <div class="card">
              <h3>Observações</h3>
              <textarea formControlName="notes" maxlength="500" placeholder="Algo que devemos saber? (opcional)"></textarea>
            </div>
          </div>

          <div class="card summary">
            <h3>Seu pedido</h3>
            @for (item of cart.items(); track item.productId) {
              <div class="line small"><span>{{ item.quantity }}× {{ item.name }}</span><span>{{ item.unitPrice * item.quantity | currency }}</span></div>
            }
            <div class="line total"><span>Total</span><span>{{ cart.total() | currency }}</span></div>

            @if (error()) { <div class="alert">{{ error() }}</div> }

            <button class="btn block" type="submit" [disabled]="sending()">
              {{ sending() ? 'Enviando...' : 'Confirmar pedido' }}
            </button>
            <p class="muted small center">Após confirmar, você será direcionado ao WhatsApp para combinar pagamento e entrega.</p>
          </div>
        </form>
      }
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr 340px; gap: 1.5rem; align-items: start; }
    .steps { display: flex; flex-direction: column; gap: 1.25rem; }
    .options { display: flex; flex-direction: column; gap: 0.5rem; }
    .option { display: flex; align-items: center; gap: 0.6rem; padding: 0.75rem 1rem; border: 1px solid var(--line); border-radius: 8px; cursor: pointer; }
    .option.active { border-color: var(--gold); background: var(--gold-soft); }
    .option input { width: auto; }
    .line { display: flex; justify-content: space-between; gap: 1rem; margin-bottom: 0.5rem; }
    .total { font-size: 1.15rem; font-weight: 600; border-top: 1px solid var(--line); padding-top: 0.75rem; margin: 0.75rem 0 1.25rem; }
    .summary { position: sticky; top: 90px; }
    @media (max-width: 760px) { .layout { grid-template-columns: 1fr; } .summary { position: static; } }
  `,
})
export class CheckoutComponent {
  protected cart = inject(CartService);
  private orderApi = inject(OrderApi);
  private router = inject(Router);
  private fb = inject(FormBuilder).nonNullable;

  protected deliveryTypes: DeliveryType[] = ['ARRANGE_ON_WHATSAPP', 'PICKUP', 'DELIVERY'];
  protected deliveryLabels = DELIVERY_TYPE_LABELS;
  protected sending = signal(false);
  protected error = signal('');

  protected form = this.fb.group({
    customer: this.fb.group({
      name: ['', Validators.required],
      phone: ['', [Validators.required, Validators.minLength(10)]],
    }),
    deliveryType: this.fb.control<DeliveryType>('ARRANGE_ON_WHATSAPP'),
    address: this.fb.group({
      street: ['', Validators.required],
      number: ['', Validators.required],
      district: ['', Validators.required],
      city: ['', Validators.required],
      state: ['', Validators.required],
      zipCode: ['', Validators.required],
      complement: [''],
    }),
    notes: ['', Validators.maxLength(500)],
  });

  submit(): void {
    const value = this.form.getRawValue();
    const needsAddress = value.deliveryType === 'DELIVERY';
    const addressInvalid = needsAddress && this.form.controls.address.invalid;

    if (this.form.controls.customer.invalid || addressInvalid) {
      this.form.markAllAsTouched();
      this.error.set('Preencha os campos obrigatórios.');
      return;
    }

    const request: CheckoutRequest = {
      customer: value.customer,
      delivery: { type: value.deliveryType, address: needsAddress ? value.address : null },
      items: this.cart.items().map((i) => ({ productId: i.productId, quantity: i.quantity })),
      notes: value.notes || undefined,
    };

    this.sending.set(true);
    this.error.set('');
    this.orderApi.checkout(request).subscribe({
      next: (response) => {
        sessionStorage.setItem(LAST_ORDER_KEY, JSON.stringify(response));
        this.cart.clear();
        this.router.navigate(['/pedido-realizado']);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.sending.set(false);
      },
    });
  }
}
