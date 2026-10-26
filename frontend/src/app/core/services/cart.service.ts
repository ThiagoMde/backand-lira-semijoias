import { Injectable, computed, effect, signal } from '@angular/core';
import { Product } from '../models';
import { productImage, productPrice } from '../utils';

export interface CartItem {
  productId: string;
  slug: string;
  name: string;
  image: string | null;
  unitPrice: number;
  stock: number;
  quantity: number;
}

const STORAGE_KEY = 'lira.cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly state = signal<CartItem[]>(this.restore());

  readonly items = this.state.asReadonly();
  readonly count = computed(() => this.state().reduce((sum, i) => sum + i.quantity, 0));
  readonly total = computed(() => this.state().reduce((sum, i) => sum + i.unitPrice * i.quantity, 0));

  constructor() {
    effect(() => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(this.state()));
      } catch {
        // armazenamento indisponível: o carrinho continua em memória
      }
    });
  }

  add(product: Product, quantity = 1): void {
    this.state.update((items) => {
      const existing = items.find((i) => i.productId === product.id);
      if (existing) {
        return items.map((i) =>
          i.productId === product.id
            ? { ...i, quantity: Math.min(i.quantity + quantity, product.stock), stock: product.stock }
            : i,
        );
      }
      return [
        ...items,
        {
          productId: product.id,
          slug: product.slug,
          name: product.name,
          image: productImage(product),
          unitPrice: productPrice(product),
          stock: product.stock,
          quantity: Math.min(quantity, product.stock),
        },
      ];
    });
  }

  setQuantity(productId: string, quantity: number): void {
    if (quantity <= 0) return this.remove(productId);
    this.state.update((items) =>
      items.map((i) => (i.productId === productId ? { ...i, quantity: Math.min(quantity, i.stock) } : i)),
    );
  }

  remove(productId: string): void {
    this.state.update((items) => items.filter((i) => i.productId !== productId));
  }

  clear(): void {
    this.state.set([]);
  }

  private restore(): CartItem[] {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    } catch {
      return [];
    }
  }
}
