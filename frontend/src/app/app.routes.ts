import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // ---------- Loja (público) ----------
  {
    path: '',
    loadComponent: () => import('./pages/store/store-layout.component').then((m) => m.StoreLayoutComponent),
    children: [
      { path: '', title: 'Lira Semijoias', loadComponent: () => import('./pages/store/home.component').then((m) => m.HomeComponent) },
      { path: 'produtos', title: 'Produtos • Lira Semijoias', loadComponent: () => import('./pages/store/catalog.component').then((m) => m.CatalogComponent) },
      { path: 'produto/:slug', loadComponent: () => import('./pages/store/product-detail.component').then((m) => m.ProductDetailComponent) },
      { path: 'carrinho', title: 'Carrinho • Lira Semijoias', loadComponent: () => import('./pages/store/cart.component').then((m) => m.CartComponent) },
      { path: 'checkout', title: 'Finalizar pedido • Lira Semijoias', loadComponent: () => import('./pages/store/checkout.component').then((m) => m.CheckoutComponent) },
      { path: 'pedido-realizado', title: 'Pedido realizado • Lira Semijoias', loadComponent: () => import('./pages/store/order-success.component').then((m) => m.OrderSuccessComponent) },
    ],
  },

  // ---------- Painel administrativo ----------
  {
    path: 'admin/login',
    title: 'Entrar • Painel Lira',
    loadComponent: () => import('./pages/admin/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/admin/admin-layout.component').then((m) => m.AdminLayoutComponent),
    children: [
      { path: '', title: 'Dashboard • Painel Lira', loadComponent: () => import('./pages/admin/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'produtos', title: 'Produtos • Painel Lira', loadComponent: () => import('./pages/admin/products.component').then((m) => m.AdminProductsComponent) },
      { path: 'produtos/novo', title: 'Novo produto • Painel Lira', loadComponent: () => import('./pages/admin/product-form.component').then((m) => m.ProductFormComponent) },
      { path: 'produtos/:id', title: 'Editar produto • Painel Lira', loadComponent: () => import('./pages/admin/product-form.component').then((m) => m.ProductFormComponent) },
      { path: 'categorias', title: 'Categorias • Painel Lira', canActivate: [roleGuard('ADMIN')], loadComponent: () => import('./pages/admin/categories.component').then((m) => m.AdminCategoriesComponent) },
      { path: 'pedidos', title: 'Pedidos • Painel Lira', loadComponent: () => import('./pages/admin/orders.component').then((m) => m.AdminOrdersComponent) },
      { path: 'pedidos/:id', title: 'Pedido • Painel Lira', loadComponent: () => import('./pages/admin/order-detail.component').then((m) => m.OrderDetailComponent) },
      { path: 'estoque', title: 'Estoque • Painel Lira', loadComponent: () => import('./pages/admin/stock.component').then((m) => m.StockComponent) },
      { path: 'usuarios', title: 'Usuários • Painel Lira', canActivate: [roleGuard('ADMIN')], loadComponent: () => import('./pages/admin/users.component').then((m) => m.AdminUsersComponent) },
    ],
  },

  { path: '**', redirectTo: '' },
];
