import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-store-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule],
  template: `
    <div class="topbar">Frete e entrega combinados pelo WhatsApp • Semijoias com garantia</div>
    <header>
      <div class="container bar">
        <a routerLink="/" class="logo">Lira <span>Semijoias</span></a>
        <nav>
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Início</a>
          <a routerLink="/produtos" routerLinkActive="active">Produtos</a>
        </nav>
        <form class="search" (ngSubmit)="search()">
          <input type="search" name="q" [(ngModel)]="query" placeholder="Buscar semijoias..." aria-label="Buscar" />
        </form>
        <a routerLink="/carrinho" class="cart" aria-label="Sacola">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M6 7h12l-1 13H7L6 7z" />
            <path d="M9 7a3 3 0 0 1 6 0" />
          </svg>
          @if (cart.count() > 0) { <span class="count">{{ cart.count() }}</span> }
        </a>
      </div>
    </header>

    <main>
      <router-outlet />
    </main>

    <footer>
      <div class="container foot">
        <div>
          <div class="logo">Lira <span>Semijoias</span></div>
          <p class="muted small">Peças delicadas para todos os momentos.</p>
        </div>
        <a routerLink="/admin" class="small muted">Área administrativa</a>
      </div>
    </footer>
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .topbar {
      background: var(--ink);
      color: #f3ead8;
      font-size: 0.78rem;
      text-align: center;
      padding: 0.4rem 16px;
    }
    header {
      background: var(--surface);
      border-bottom: 1px solid var(--line);
      position: sticky;
      top: 0;
      z-index: 50;
    }
    .bar { display: flex; align-items: center; gap: 1.5rem; height: 68px; }
    .logo {
      font-family: var(--serif);
      font-size: 1.6rem;
      font-weight: 700;
      color: var(--ink);
      letter-spacing: 0.02em;
    }
    .logo span { color: var(--gold); font-weight: 500; }
    nav { display: flex; gap: 1.2rem; }
    nav a { color: var(--ink); font-size: 0.9rem; padding: 0.3rem 0; border-bottom: 2px solid transparent; }
    nav a.active { border-color: var(--gold); }
    .search { flex: 1; max-width: 360px; margin-left: auto; }
    .search input { border-radius: 999px; background: var(--bg); }
    .cart { position: relative; color: var(--ink); display: flex; }
    .count {
      position: absolute;
      top: -6px;
      right: -9px;
      background: var(--gold);
      color: #fff;
      font-size: 0.7rem;
      min-width: 18px;
      height: 18px;
      border-radius: 999px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 4px;
    }
    main { flex: 1; padding: 2rem 0 3rem; }
    footer { border-top: 1px solid var(--line); background: var(--surface); padding: 1.5rem 0; }
    .foot { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; }
    .foot .logo { font-size: 1.3rem; }
    @media (max-width: 720px) {
      .bar { flex-wrap: wrap; height: auto; padding-top: 0.75rem; padding-bottom: 0.75rem; gap: 0.75rem 1rem; }
      .search { order: 5; max-width: none; flex-basis: 100%; }
      nav { margin-left: auto; }
    }
  `,
})
export class StoreLayoutComponent {
  protected cart = inject(CartService);
  private router = inject(Router);
  protected query = '';

  search(): void {
    const q = this.query.trim();
    this.router.navigate(['/produtos'], { queryParams: q ? { busca: q } : {} });
  }
}
