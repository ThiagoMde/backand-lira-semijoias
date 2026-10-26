import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Category,
  CategoryRequest,
  CheckoutRequest,
  CheckoutResponse,
  CreateProductRequest,
  CreateUserRequest,
  Dashboard,
  LoginRequest,
  LoginResponse,
  Order,
  OrderStatus,
  Product,
  ProductImageRequest,
  StockMovement,
  StockRequest,
  UpdateProductRequest,
  UploadResponse,
  User,
} from '../models';

const API = environment.apiUrl;

/** AuthController — /api/auth */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private http = inject(HttpClient);

  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API}/auth/login`, body);
  }
}

/** ProductController (público) e AdminProductController */
@Injectable({ providedIn: 'root' })
export class ProductApi {
  private http = inject(HttpClient);

  // ----- Público: /api/products -----
  list(filters: { search?: string; categoryId?: string } = {}): Observable<Product[]> {
    let params = new HttpParams();
    if (filters.search) params = params.set('search', filters.search);
    if (filters.categoryId) params = params.set('categoryId', filters.categoryId);
    return this.http.get<Product[]>(`${API}/products`, { params });
  }

  getById(id: string): Observable<Product> {
    return this.http.get<Product>(`${API}/products/${id}`);
  }

  getBySlug(slug: string): Observable<Product> {
    return this.http.get<Product>(`${API}/products/slug/${encodeURIComponent(slug)}`);
  }

  // ----- Admin: /api/admin/products -----
  adminList(): Observable<Product[]> {
    return this.http.get<Product[]>(`${API}/admin/products`);
  }

  create(body: CreateProductRequest): Observable<Product> {
    return this.http.post<Product>(`${API}/admin/products`, body);
  }

  update(id: string, body: UpdateProductRequest): Observable<Product> {
    return this.http.put<Product>(`${API}/admin/products/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<Product> {
    return this.http.patch<Product>(`${API}/admin/products/${id}/status`, null, {
      params: { active },
    });
  }

  addImage(id: string, body: ProductImageRequest): Observable<Product> {
    return this.http.post<Product>(`${API}/admin/products/${id}/images`, body);
  }

  removeImage(id: string, publicId: string): Observable<Product> {
    return this.http.delete<Product>(`${API}/admin/products/${id}/images`, {
      params: { publicId },
    });
  }
}

/** CategoryController (público) e AdminCategoryController */
@Injectable({ providedIn: 'root' })
export class CategoryApi {
  private http = inject(HttpClient);

  list(): Observable<Category[]> {
    return this.http.get<Category[]>(`${API}/categories`);
  }

  adminList(): Observable<Category[]> {
    return this.http.get<Category[]>(`${API}/admin/categories`);
  }

  create(body: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(`${API}/admin/categories`, body);
  }

  update(id: string, body: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${API}/admin/categories/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<Category> {
    return this.http.patch<Category>(`${API}/admin/categories/${id}/status`, null, {
      params: { active },
    });
  }
}

/** OrderController (checkout público) e AdminOrderController */
@Injectable({ providedIn: 'root' })
export class OrderApi {
  private http = inject(HttpClient);

  checkout(body: CheckoutRequest): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${API}/orders/checkout`, body);
  }

  adminList(status?: OrderStatus | ''): Observable<Order[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Order[]>(`${API}/admin/orders`, { params });
  }

  getById(id: string): Observable<Order> {
    return this.http.get<Order>(`${API}/admin/orders/${id}`);
  }

  confirm(id: string): Observable<Order> {
    return this.http.patch<Order>(`${API}/admin/orders/${id}/confirm`, null);
  }

  cancel(id: string): Observable<Order> {
    return this.http.patch<Order>(`${API}/admin/orders/${id}/cancel`, null);
  }

  updateStatus(id: string, status: OrderStatus): Observable<Order> {
    return this.http.patch<Order>(`${API}/admin/orders/${id}/status`, { status });
  }
}

/** AdminStockController — /api/admin/stock */
@Injectable({ providedIn: 'root' })
export class StockApi {
  private http = inject(HttpClient);

  entry(body: StockRequest): Observable<Product> {
    return this.http.post<Product>(`${API}/admin/stock/entry`, body);
  }

  exit(body: StockRequest): Observable<Product> {
    return this.http.post<Product>(`${API}/admin/stock/exit`, body);
  }

  low(): Observable<Product[]> {
    return this.http.get<Product[]>(`${API}/admin/stock/low`);
  }

  movements(): Observable<StockMovement[]> {
    return this.http.get<StockMovement[]>(`${API}/admin/stock/movements`);
  }

  movementsByProduct(productId: string): Observable<StockMovement[]> {
    return this.http.get<StockMovement[]>(`${API}/admin/stock/movements/product/${productId}`);
  }
}

/** AdminUploadController — /api/admin/uploads (Cloudinary) */
@Injectable({ providedIn: 'root' })
export class UploadApi {
  private http = inject(HttpClient);

  uploadImage(file: File): Observable<UploadResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<UploadResponse>(`${API}/admin/uploads/images`, form);
  }

  deleteImage(publicId: string): Observable<void> {
    return this.http.delete<void>(`${API}/admin/uploads/images`, { params: { publicId } });
  }
}

/** AdminUserController — /api/admin/users */
@Injectable({ providedIn: 'root' })
export class UserApi {
  private http = inject(HttpClient);

  list(): Observable<User[]> {
    return this.http.get<User[]>(`${API}/admin/users`);
  }

  create(body: CreateUserRequest): Observable<User> {
    return this.http.post<User>(`${API}/admin/users`, body);
  }

  setActive(id: string, active: boolean): Observable<User> {
    return this.http.patch<User>(`${API}/admin/users/${id}/status`, null, { params: { active } });
  }
}

/** AdminDashboardController — /api/admin/dashboard */
@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private http = inject(HttpClient);

  get(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${API}/admin/dashboard`);
  }
}
