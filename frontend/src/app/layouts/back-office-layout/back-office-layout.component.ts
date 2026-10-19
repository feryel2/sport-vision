import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-back-office-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="admin-wrapper" [class.sidebar-collapsed]="isCollapsed">
      <!-- SIDEBAR ADMIN -->
      <aside class="admin-sidebar">
        <!-- Logo Header -->
        <div class="sidebar-header">
          <div class="logo-box">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <path d="m9 12 2 2 4-4"/>
            </svg>
          </div>
          <div class="logo-titles" *ngIf="!isCollapsed">
            <span class="logo-main">SPORT<span class="text-accent">VISION</span></span>
            <span class="logo-role">CONSOLE GESTION 3</span>
          </div>
        </div>

        <!-- Menu de navigation -->
        <nav class="sidebar-menu">
          <div class="menu-section-label" *ngIf="!isCollapsed">BOUTIQUE & PRODUITS SPORTIFS</div>

          <a routerLink="/admin" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" class="nav-item">
            <span class="nav-icon">📊</span>
            <span class="nav-text" *ngIf="!isCollapsed">Tableau de Bord</span>
          </a>

          <a routerLink="/admin/produits" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">📦</span>
            <span class="nav-text" *ngIf="!isCollapsed">Produits & Prix / Stocks</span>
          </a>

          <a routerLink="/admin/categories" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">🏷️</span>
            <span class="nav-text" *ngIf="!isCollapsed">Catalogue (Cat/Marq/Fourn)</span>
          </a>

          <a routerLink="/admin/commandes" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">🛒</span>
            <span class="nav-text" *ngIf="!isCollapsed">Commandes & Ventes</span>
          </a>

          <a routerLink="/admin/aliments" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">🥗</span>
            <span class="nav-text" *ngIf="!isCollapsed">Aliments & Nutrition</span>
          </a>
          <a routerLink="/admin/utilisateurs" routerLinkActive="active" class="nav-item">
            <span class="nav-icon">👥</span>
            <span class="nav-text" *ngIf="!isCollapsed">Utilisateurs & rôles</span>
          </a>
          <div class="menu-section-label" *ngIf="!isCollapsed">ENTRAÎNEMENT</div>
          <a routerLink="/admin/entrainement/exercices" routerLinkActive="active" class="nav-item"
             aria-label="Gérer les exercices" title="Gérer les exercices">
            <span class="nav-icon" aria-hidden="true">🏋️</span>
            <span class="nav-text" *ngIf="!isCollapsed">Exercices & Entraînement</span>
          </a>
        </nav>

        <!-- Bas de sidebar : Profil & Switch Front-Office -->
        <div class="sidebar-footer">
          <a routerLink="/" class="btn-front-switch" title="Retour à la boutique publique">
            <span class="nav-icon">🌐</span>
            <span class="nav-text" *ngIf="!isCollapsed">Voir la boutique</span>
          </a>

          <div class="admin-profile-card" *ngIf="!isCollapsed">
            <div class="profile-avatar">FM</div>
            <div class="profile-info">
              <span class="profile-name">{{ authService.currentUserValue?.fullName || 'Administrateur' }}</span>
              <span class="profile-badge">Administrateur</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- ZONE PRINCIPALE -->
      <div class="admin-main">
        <!-- TOPBAR ADMIN -->
        <header class="admin-topbar">
          <div class="topbar-left">
            <button (click)="toggleSidebar()" class="btn-toggle" aria-label="Toggle Sidebar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="3" y1="12" x2="21" y2="12"></line>
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <line x1="3" y1="18" x2="21" y2="18"></line>
              </svg>
            </button>
            <div class="breadcrumb-trail">
              <span class="badge-sv badge-blue">Administration Sport Vision</span>
            </div>
          </div>

          <div class="topbar-right">
            <a routerLink="/catalogue" class="btn-sv btn-sv-outline btn-sv-sm">
              🛍️ Boutique Public
            </a>
            <button (click)="logout()" class="btn-logout" title="Déconnexion">
              🚪 Déconnexion
            </button>
          </div>
        </header>

        <!-- CONTENU DU DASHBOARD / VUES ADMIN -->
        <main class="admin-content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .admin-wrapper {
      display: flex;
      min-height: 100vh;
      background: #f8fafc;
    }

    /* SIDEBAR */
    .admin-sidebar {
      width: 270px;
      background: #0f172a;
      color: #ffffff;
      display: flex;
      flex-direction: column;
      flex-shrink: 0;
      transition: width 0.25s ease;
      border-right: 3px solid #1d4ed8;
      z-index: 50;
    }
    .admin-wrapper.sidebar-collapsed .admin-sidebar {
      width: 75px;
    }

    .sidebar-header {
      height: 75px;
      display: flex;
      align-items: center;
      padding: 0 20px;
      gap: 12px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }
    .logo-box {
      width: 40px;
      height: 40px;
      background: #1d4ed8;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      flex-shrink: 0;
    }
    .logo-titles {
      display: flex;
      flex-direction: column;
      overflow: hidden;
      white-space: nowrap;
    }
    .logo-main {
      font-size: 1.15rem;
      font-weight: 900;
      color: white;
    }
    .text-accent {
      color: #38bdf8;
    }
    .logo-role {
      font-size: 0.65rem;
      font-weight: 700;
      color: #94a3b8;
      letter-spacing: 0.8px;
    }

    .sidebar-menu {
      padding: 20px 14px;
      display: flex;
      flex-direction: column;
      gap: 6px;
      flex-grow: 1;
      overflow-y: auto;
    }
    .menu-section-label {
      font-size: 0.68rem;
      font-weight: 800;
      color: #64748b;
      letter-spacing: 0.8px;
      padding: 10px 10px 4px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 11px 14px;
      color: #cbd5e1;
      text-decoration: none;
      border-radius: 8px;
      font-size: 0.88rem;
      font-weight: 600;
      transition: all 0.15s ease;
      white-space: nowrap;
    }
    .nav-item:hover {
      background: rgba(255, 255, 255, 0.08);
      color: white;
    }
    .nav-item.active {
      background: #1d4ed8;
      color: white;
      box-shadow: 0 4px 12px rgba(29, 78, 216, 0.4);
    }
    .nav-icon {
      font-size: 1.1rem;
      flex-shrink: 0;
    }

    .sidebar-footer {
      padding: 16px 14px;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .btn-front-switch {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 9px 12px;
      background: rgba(255, 255, 255, 0.06);
      color: #94a3b8;
      text-decoration: none;
      border-radius: 6px;
      font-size: 0.82rem;
      transition: all 0.15s ease;
      white-space: nowrap;
    }
    .btn-front-switch:hover {
      background: rgba(255, 255, 255, 0.12);
      color: white;
    }

    .admin-profile-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      background: rgba(29, 78, 216, 0.2);
      border: 1px solid #1d4ed8;
      border-radius: 10px;
    }
    .profile-avatar {
      width: 36px;
      height: 36px;
      background: #1d4ed8;
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      font-size: 0.85rem;
      flex-shrink: 0;
    }
    .profile-info {
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    .profile-name {
      font-size: 0.88rem;
      font-weight: 700;
      color: white;
      white-space: nowrap;
    }
    .profile-badge {
      font-size: 0.7rem;
      color: #38bdf8;
      font-weight: 600;
    }

    /* ZONE PRINCIPALE */
    .admin-main {
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .admin-topbar {
      height: 75px;
      background: white;
      border-bottom: 1px solid #e2e8f0;
      padding: 0 30px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 40;
    }
    .topbar-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .btn-toggle {
      background: none;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: #64748b;
    }
    .breadcrumb-trail {
      display: flex;
      gap: 10px;
      align-items: center;
      flex-wrap: wrap;
    }
    .badge-outline-blue {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      color: #1d4ed8;
      font-weight: 700;
    }
    .topbar-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .btn-logout {
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 0.82rem;
      color: #dc2626;
      font-weight: 600;
      cursor: pointer;
    }

    .admin-content {
      padding: 30px;
      flex-grow: 1;
    }

    @media (max-width: 768px) {
      .admin-sidebar {
        position: fixed;
        left: -270px;
        top: 0;
        bottom: 0;
      }
      .admin-wrapper:not(.sidebar-collapsed) .admin-sidebar {
        left: 0;
      }
      .admin-topbar {
        padding: 0 16px;
      }
      .admin-content {
        padding: 16px;
      }
    }
  `]
})
export class BackOfficeLayoutComponent {
  isCollapsed = false;

  constructor(public authService: AuthService, private router: Router) {}

  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  logout(): void {
    this.authService.logout().subscribe(() => this.router.navigate(['/']));
  }
}
