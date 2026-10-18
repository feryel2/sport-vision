import { Component, OnInit, DestroyRef, ElementRef, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { FavoriteService } from '../../core/services/favorite.service';

@Component({
  selector: 'app-front-office-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <!-- Barre d'annonce supérieure -->
    <div class="top-banner">
      <div class="container banner-content">
        <div class="banner-text">
          <span class="badge-pulse">SPORT VISION</span>
        </div>
        <div class="banner-actions">
          <span class="demo-tag">Profil Démo :</span>
          <button (click)="toggleRole()" class="btn-role-switch" [class.is-admin]="authService.isAdmin">
            {{ authService.isAdmin ? '👑 Administrateur' : '👤 Client' }} &bull; Basculer
          </button>
          <a *ngIf="authService.isAdmin" routerLink="/admin" class="admin-quick-link">
            ⚙️ Console Back-Office
          </a>
        </div>
      </div>
    </div>

    <!-- Navigation Principale -->
    <header class="main-header" (keydown.escape)="closeMobileMenu(true)">
      <div class="container navbar-container">
        <!-- Logo Sport Vision -->
        <a routerLink="/" class="logo">
          <div class="logo-icon">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <path d="m9 12 2 2 4-4"/>
            </svg>
          </div>
          <div class="logo-text">
            <span class="logo-brand">SPORT<span class="text-accent">VISION</span></span>
            <span class="logo-sub">BOUTIQUE & PRODUITS SPORTIFS</span>
          </div>
        </a>

        <!-- Menu de Navigation -->
        <button #menuToggle type="button" class="mobile-menu-toggle"
          [attr.aria-expanded]="mobileMenuOpen" aria-controls="public-navigation"
          [attr.aria-label]="mobileMenuOpen ? 'Fermer le menu principal' : 'Ouvrir le menu principal'"
          (click)="mobileMenuOpen = !mobileMenuOpen">Menu</button>
        <nav id="public-navigation" class="nav-links" [class.is-open]="mobileMenuOpen"
          aria-label="Navigation principale" (click)="closeMobileMenu(true)">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Accueil</a>
          <a routerLink="/catalogue" routerLinkActive="active">Boutique & Produits</a>
          <a routerLink="/entrainement" routerLinkActive="active">Entraînement</a>
          <a routerLink="/coach-matching" routerLinkActive="active">Trouver un Coach</a>
          <a routerLink="/aliments" routerLinkActive="active">Aliments & Nutrition</a>
          <a routerLink="/contact" routerLinkActive="active">Contact & Support</a>
        </nav>

        <!-- Outils, Favoris & Panier -->
        <div class="nav-actions">
          <!-- Recherche rapide -->
          <div class="search-box">
            <input type="text" placeholder="Rechercher équipement..." [(ngModel)]="searchQuery" (keyup.enter)="onSearch()">
            <button (click)="onSearch()" class="btn-search-icon" aria-label="Rechercher">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </button>
          </div>

          <!-- Favoris -->
          <a routerLink="/favoris" class="fav-btn" title="Mes Favoris" aria-label="Favoris">
            <span class="fav-icon">❤️</span>
            <span class="fav-badge" *ngIf="favCount > 0">{{ favCount }}</span>
          </a>

          <!-- Panier -->
          <a routerLink="/panier" class="cart-btn" aria-label="Panier">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="9" cy="21" r="1"></circle>
              <circle cx="20" cy="21" r="1"></circle>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
            </svg>
            <span class="cart-badge" *ngIf="cartService.totalCount > 0">{{ cartService.totalCount }}</span>
          </a>

          <!-- Espace Client / Connexion -->
          <a routerLink="/login" class="btn-sv btn-sv-primary btn-sv-sm">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <span>{{ authService.currentUserValue ? 'Mon Compte' : 'Connexion' }}</span>
          </a>
        </div>
      </div>
    </header>

    <!-- Contenu Principal -->
    <main class="front-content">
      <router-outlet></router-outlet>
    </main>

    <!-- Footer Sport Vision -->
    <footer class="main-footer">
      <div class="footer-top">
        <div class="container footer-grid">
          <!-- Col 1 : Identité & Responsable -->
          <div class="footer-col">
            <div class="footer-brand">
              <div class="logo-icon footer-logo-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <circle cx="12" cy="12" r="4"></circle>
                </svg>
              </div>
              <span class="logo-brand text-white">SPORT<span class="text-accent">VISION</span></span>
            </div>
            <p class="footer-desc">Plateforme sportive haute pr&eacute;cision.</p>
          </div>

          <!-- Col 2 : Rayons -->
          <div class="footer-col">
            <h4 class="footer-title">Boutique & Équipements</h4>
            <ul class="footer-links">
              <li><a routerLink="/catalogue" [queryParams]="{cat: 1}">Musculation & Force (Bandes, Gants, Ceinture)</a></li>
              <li><a routerLink="/catalogue" [queryParams]="{cat: 2}">Running & Trail Performance</a></li>
              <li><a routerLink="/catalogue" [queryParams]="{cat: 3}">Nutrition & Compléments (Whey, Isolate)</a></li>
              <li><a routerLink="/catalogue" [queryParams]="{cat: 4}">Textiles Techniques & Compression</a></li>
              <li><a routerLink="/catalogue" [queryParams]="{cat: 5}">Accessoires & Montres GPS</a></li>
            </ul>
          </div>

          <!-- Col 3 : Performance & Coaching -->
          <div class="footer-col">
            <h4 class="footer-title">Coaching & Nutrition</h4>
            <ul class="footer-links">
              <li><a routerLink="/entrainement">Entraînement</a></li>
              <li><a routerLink="/coach-matching">Matching Sportif & Coachs</a></li>
              <li><a routerLink="/aliments">Base Aliments & Macro-Nutriments</a></li>
              <li><a routerLink="/favoris">Ma Liste de Favoris</a></li>
              <li><a routerLink="/panier">Panier & Historique Commandes</a></li>
              <li><a routerLink="/admin">Console d'Administration</a></li>
            </ul>
          </div>
        </div>
      </div>

      <div class="footer-bottom">
        <div class="container bottom-content">
          <p>&copy; 2026 Sport Vision</p>
          <div class="bottom-links">
            <a routerLink="/contact">Conditions Générales</a>
            <a routerLink="/admin" class="text-accent">Accès Staff Back-Office</a>
          </div>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .container {
      max-width: 1280px;
      margin: 0 auto;
      padding: 0 20px;
    }

    /* Bannière supérieure */
    .top-banner {
      background: linear-gradient(90deg, #0f172a 0%, #1e3a8a 100%);
      color: #ffffff;
      font-size: 0.82rem;
      padding: 8px 0;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }
    .banner-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px;
    }
    .badge-pulse {
      background: #0284c7;
      color: white;
      font-weight: 800;
      padding: 2px 8px;
      border-radius: 4px;
      margin-right: 6px;
    }
    .banner-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .demo-tag {
      color: #94a3b8;
    }
    .btn-role-switch {
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.25);
      color: white;
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 0.78rem;
      cursor: pointer;
    }
    .btn-role-switch.is-admin {
      background: #0284c7;
      border-color: #38bdf8;
    }
    .admin-quick-link {
      color: #38bdf8;
      font-weight: 700;
      text-decoration: underline;
    }

    /* HEADER */
    .main-header {
      background: #ffffff;
      border-bottom: 2px solid #e2e8f0;
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
    }
    .navbar-container {
      display: flex;
      justify-content: space-between;
      align-items: center;
      min-height: 75px;
      flex-wrap: wrap;
      gap: 12px;
      padding-top: 12px;
      padding-bottom: 12px;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
    }
    .logo-icon {
      background: linear-gradient(135deg, #1d4ed8 0%, #0284c7 100%);
      color: white;
      width: 44px;
      height: 44px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .logo-brand {
      font-size: 1.25rem;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: -0.5px;
      display: block;
    }
    .logo-sub {
      font-size: 0.65rem;
      font-weight: 700;
      color: #0284c7;
      letter-spacing: 0.8px;
      display: block;
    }

    .nav-links {
      display: flex;
      flex: 1 1 480px;
      flex-wrap: wrap;
      justify-content: center;
      gap: 8px;
    }
    .nav-links a {
      color: #334155;
      font-weight: 700;
      font-size: 0.92rem;
      text-decoration: none;
      padding: 6px 10px;
      border-radius: 8px;
      transition: color 0.15s ease, background 0.15s ease;
      display: flex;
      align-items: center;
      gap: 5px;
    }
    .nav-links a:hover, .nav-links a.active {
      color: #1d4ed8;
      background: #eff6ff;
    }

    .nav-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .search-box {
      display: flex;
      align-items: center;
      background: #f8fafc;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      padding: 4px 10px;
    }
    .search-box input {
      border: none;
      background: transparent;
      font-size: 0.85rem;
      outline: none;
      width: 140px;
    }
    .btn-search-icon {
      background: none;
      border: none;
      cursor: pointer;
      color: #64748b;
      display: flex;
    }

    .fav-btn, .cart-btn {
      position: relative;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      text-decoration: none;
      color: #0f172a;
    }
    .fav-icon {
      font-size: 1.1rem;
    }
    .fav-badge, .cart-badge {
      position: absolute;
      top: -5px;
      right: -5px;
      background: #0284c7;
      color: white;
      font-size: 0.7rem;
      font-weight: 800;
      width: 18px;
      height: 18px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .fav-badge {
      background: #dc2626;
    }

    /* FOOTER */
    .main-footer {
      background: #0f172a;
      color: #cbd5e1;
      border-top: 4px solid #1d4ed8;
    }
    .footer-top {
      padding: 60px 0 45px;
    }
    .footer-grid {
      display: grid;
      grid-template-columns: 1.5fr 1fr 1fr;
      gap: 35px;
    }
    .footer-logo-icon {
      margin-bottom: 10px;
    }
    .footer-desc {
      font-size: 0.88rem;
      line-height: 1.55;
      color: #94a3b8;
      margin-bottom: 16px;
    }
    .resp-badge-card {
      background: rgba(29, 78, 216, 0.2);
      border: 1px solid #1d4ed8;
      border-radius: 10px;
      padding: 10px 14px;
    }
    .resp-badge-title {
      font-size: 0.72rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #38bdf8;
      display: block;
    }
    .resp-badge-name {
      color: white;
      font-size: 1.05rem;
      display: block;
      margin: 2px 0;
    }
    .resp-badge-sub {
      font-size: 0.75rem;
      color: #94a3b8;
    }

    .footer-title {
      color: white;
      font-size: 1rem;
      font-weight: 800;
      margin: 0 0 16px;
    }
    .footer-links {
      list-style: none;
      padding: 0;
      margin: 0;
    }
    .footer-links li {
      margin-bottom: 10px;
    }
    .footer-links a {
      color: #94a3b8;
      text-decoration: none;
      font-size: 0.85rem;
      transition: color 0.15s ease;
    }
    .footer-links a:hover {
      color: #38bdf8;
    }

    .chart-colors {
      display: flex;
      gap: 8px;
      margin-top: 12px;
    }
    .color-dot {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 1px solid rgba(255,255,255,0.2);
    }
    .c-blue { background: #1d4ed8; }
    .c-sky { background: #0284c7; }
    .c-dark { background: #0f172a; }
    .c-white { background: #ffffff; }

    .footer-bottom {
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      padding: 20px 0;
      font-size: 0.82rem;
      color: #64748b;
    }
    .bottom-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 15px;
    }
    .bottom-links {
      display: flex;
      gap: 16px;
    }
    .bottom-links a {
      color: #94a3b8;
      text-decoration: none;
    }

    .mobile-menu-toggle { display: none; }
    .mobile-menu-toggle:focus-visible, .nav-links a:focus-visible {
      outline: 3px solid #0284c7;
      outline-offset: 3px;
    }
    @media (max-width: 992px) {
      .footer-grid {
        grid-template-columns: 1fr 1fr;
      }
      .logo { flex: 1; }
      .mobile-menu-toggle {
        display: block;
        padding: 8px 12px;
        border: 1px solid #cbd5e1;
        border-radius: 8px;
        background: white;
        color: #1d4ed8;
        font: inherit;
        font-weight: 700;
        cursor: pointer;
      }
      .nav-links { display: none; order: 2; flex-basis: 100%; }
      .nav-links.is-open { display: flex; flex-direction: column; align-items: stretch; }
      .nav-actions { order: 3; flex-basis: 100%; justify-content: flex-end; flex-wrap: wrap; }
    }
    @media (max-width: 576px) {
      .footer-grid {
        grid-template-columns: 1fr;
      }
      .search-box {
        display: none;
      }
    }
  `]
})
export class FrontOfficeLayoutComponent implements OnInit {
  mobileMenuOpen = false;
  @ViewChild('menuToggle') private menuToggle?: ElementRef<HTMLButtonElement>;
  private readonly destroyRef = inject(DestroyRef);
  searchQuery: string = '';
  favCount: number = 0;

  constructor(
    public cartService: CartService,
    public authService: AuthService,
    private favoriteService: FavoriteService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.closeMobileMenu());
    this.favoriteService.favorites$.subscribe(favs => {
      this.favCount = favs.length;
    });
  }

  closeMobileMenu(restoreFocus = false): void {
    if (!this.mobileMenuOpen) return;
    this.mobileMenuOpen = false;
    if (restoreFocus) this.menuToggle?.nativeElement.focus();
  }

  toggleRole(): void {
    if (this.authService.isAdmin) {
      this.authService.loginAsClient();
    } else {
      this.authService.loginAsAdmin();
    }
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/catalogue'], { queryParams: { q: this.searchQuery } });
    }
  }
}
