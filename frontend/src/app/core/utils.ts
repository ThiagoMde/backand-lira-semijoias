import { HttpErrorResponse } from '@angular/common/http';
import { DeliveryType, OrderStatus, Product, StockMovementType } from './models';

export function productPrice(p: Product): number {
  return p.promotionalPrice && p.promotionalPrice < p.price ? p.promotionalPrice : p.price;
}

export function productImage(p: Product): string | null {
  return (p.images?.find((i) => i.main) ?? p.images?.[0])?.url ?? null;
}

/** Extrai a mensagem do ApiError do back end (GlobalExceptionHandler). */
export function errorMessage(err: unknown, fallback = 'Ocorreu um erro. Tente novamente.'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'Não foi possível conectar ao servidor.';
    const body = err.error;
    if (body && typeof body === 'object') {
      const fields = body.fields as Record<string, string> | undefined;
      if (fields && Object.keys(fields).length) {
        return Object.entries(fields)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(' • ');
      }
      if (body.message) return body.message;
    }
    if (err.status === 403) return 'Você não tem permissão para esta ação.';
    if (err.status === 401) return 'Sessão expirada. Faça login novamente.';
  }
  return fallback;
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pendente',
  CONFIRMED: 'Confirmado',
  PAID: 'Pago',
  PREPARING: 'Em preparação',
  READY: 'Pronto',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregue',
  CANCELLED: 'Cancelado',
};

export const DELIVERY_TYPE_LABELS: Record<DeliveryType, string> = {
  DELIVERY: 'Entrega no endereço',
  PICKUP: 'Retirada',
  ARRANGE_ON_WHATSAPP: 'Combinar pelo WhatsApp',
};

export const STOCK_MOVEMENT_LABELS: Record<StockMovementType, string> = {
  ENTRY: 'Entrada',
  SALE: 'Venda',
  EXIT: 'Saída',
  RETURN: 'Devolução',
  DAMAGE: 'Avaria',
  ADJUSTMENT: 'Ajuste',
};

export const ORDER_STATUSES = Object.keys(ORDER_STATUS_LABELS) as OrderStatus[];
