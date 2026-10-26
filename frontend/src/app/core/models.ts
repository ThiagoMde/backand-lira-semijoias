// Tipos espelhando os modelos e DTOs do back end (com.lirasemijoias.projeto).

export type UserRole = 'ADMIN' | 'EMPLOYEE';
export type DeliveryType = 'DELIVERY' | 'PICKUP' | 'ARRANGE_ON_WHATSAPP';
export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PAID'
  | 'PREPARING'
  | 'READY'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';
export type StockMovementType = 'ENTRY' | 'SALE' | 'EXIT' | 'RETURN' | 'DAMAGE' | 'ADJUSTMENT';

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fields?: Record<string, string>;
}

// ---------- Auth ----------
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  name: string;
  email: string;
  role: UserRole;
}

// ---------- Categorias ----------
export interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CategoryRequest {
  name: string;
  description?: string;
}

// ---------- Produtos ----------
export interface ProductImage {
  url: string;
  publicId: string;
  main: boolean;
}

export interface Product {
  id: string;
  name: string;
  slug: string;
  sku: string;
  description: string;
  price: number;
  promotionalPrice?: number | null;
  categoryId: string;
  material?: string;
  color?: string;
  stock: number;
  minimumStock: number;
  images: ProductImage[];
  featured: boolean;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateProductRequest {
  name: string;
  sku: string;
  description: string;
  price: number;
  promotionalPrice?: number | null;
  categoryId: string;
  material?: string;
  color?: string;
  stock: number;
  minimumStock: number;
  featured: boolean;
}

export interface UpdateProductRequest {
  name: string;
  sku: string;
  description: string;
  price: number;
  promotionalPrice?: number | null;
  categoryId: string;
  material?: string;
  color?: string;
  minimumStock: number;
  featured: boolean;
  active: boolean;
}

export interface ProductImageRequest {
  url: string;
  publicId: string;
  main: boolean;
}

export interface UploadResponse {
  url: string;
  publicId: string;
}

// ---------- Pedidos ----------
export interface Customer {
  name: string;
  phone: string;
}

export interface DeliveryAddress {
  street: string;
  number: string;
  district: string;
  city: string;
  state: string;
  zipCode: string;
  complement?: string;
}

export interface Delivery {
  type: DeliveryType;
  address?: DeliveryAddress | null;
}

export interface OrderItem {
  productId: string;
  name: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  customer: Customer;
  delivery: Delivery;
  items: OrderItem[];
  subtotal: number;
  deliveryFee: number;
  total: number;
  status: OrderStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CheckoutRequest {
  customer: Customer;
  delivery: Delivery;
  items: { productId: string; quantity: number }[];
  notes?: string;
}

export interface CheckoutResponse {
  orderId: string;
  orderNumber: string;
  total: number;
  status: OrderStatus;
  whatsappMessage: string;
  whatsappUrl: string;
}

// ---------- Estoque ----------
export interface StockRequest {
  productId: string;
  quantity: number;
  reason: string;
}

export interface StockMovement {
  id: string;
  productId: string;
  type: StockMovementType;
  quantity: number;
  stockBefore: number;
  stockAfter: number;
  reason: string;
  userId?: string;
  orderId?: string;
  createdAt: string;
}

// ---------- Usuários ----------
export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: UserRole;
}

// ---------- Dashboard ----------
export interface Dashboard {
  productsTotal: number;
  productsActive: number;
  stockUnits: number;
  lowStock: number;
  outOfStock: number;
  ordersPending: number;
  ordersConfirmed: number;
  ordersDelivered: number;
}
