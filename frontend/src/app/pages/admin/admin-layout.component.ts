import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell" [class.open]="menuOpen()">
      <aside>
        <div class="brand">Lira <span>Painel</span></div>
        <nav>
          <a routerLink="/admin" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Dashboard</a>
          <a routerLink="/admin/produtos" routerLinkActive="active">Produtos</a>
          @if (auth.isAdmin()) {
            <a routerLink="/admin/categorias" routerLinkActive="active">Categorias</a>
          }
          <a routerLink="/admin/pedidos" routerLinkActive="active">Pedidos</a>
          <a routerLink="/admin/estoque" routerLinkActive="active">Estoque</a>
          @if (auth.isAdmin()) {
            <a routerLink="/admin/usuarios" routerLinkActive="active">Usuários</a>
          }
        </nav>
        <div class="foot">
          <a routerLink="/" target="_blank">Ver loja ↗</a>
        </div>
      </aside>

      <div class="content">
        <header>
          <button class="btn ghost sm menu" (click)="menuOpen.set(!menuOpen())" aria-label="Menu">☰</button>
          <a routerLink="/admin/produtos/novo" class="btn sm">+ Novo produto</a>
          <span class="spacer"></span>
          <div class="user">
            <strong>{{ auth.user()?.name }}</strong>
            <span class="badge">{{ auth.user()?.role === 'ADMIN' ? 'Administrador' : 'Funcionário' }}</span>
          </div>
          <button class="btn outline sm" (click)="auth.logout()">Sair</button>
        </header>
        <main>
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: `
    .shell { display: grid; grid-template-columns: 230px 1fr; min-height: 100vh; }
    aside { background: var(--ink); color: #e9e2d6; display: flex; flex-direction: column; padding: 1.25rem 0.9rem; }
    .brand { font-family: var(--serif); font-size: 1.6rem; font-weight: 700; padding: 0 0.6rem 1.25rem; color: #fff; }
    .brand span { color: var(--gold); font-weight: 500; }
    nav { display: flex; flex-direction: column; gap: 0.2rem; }
    nav a { color: #d6cdbf; padding: 0.6rem 0.8rem; border-radius: 8px; font-size: 0.92rem; }
    nav a:hover { background: rgba(255, 255, 255, 0.06); color: #fff; }
    nav a.active { background: var(--gold); color: #fff; }
    .foot { margin-top: auto; padding: 0.6rem; }
    .foot a { color: #bfb4a4; font-size: 0.85rem; }
    .content { display: flex; flex-direction: column; min-width: 0; }
    header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1.5rem;
      background: var(--surface);
      border-bottom: 1px solid var(--line);
      position: sticky;
      top: 0;
      z-index: 20;
    }
    .user { display: flex; align-items: center; gap: 0.5rem; font-size: 0.9rem; }
    .menu { display: none; }
    main { padding: 1.75rem 1.5rem 3rem; max-width: 1200px; width: 100%; }
    @media (max-width: 860px) {
      .shell { grid-template-columns: 1fr; }
      aside { display: none; }
      .shell.open aside { display: flex; position: fixed; inset: 0 30% 0 0; z-index: 100; }
      .menu { display: inline-flex; }
      .user strong { display: none; }
      main { padding: 1.25rem 16px 3rem; }
    }
  `,
})
export class AdminLayoutComponent {
  protected auth = inject(AuthService);
  protected menuOpen = signal(false);

  constructor() {
    inject(Router)
      .events.pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.menuOpen.set(false));
  }
}
