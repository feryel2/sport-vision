import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FavoriteService } from '../../../core/services/favorite.service';
import { CartService } from '../../../core/services/cart.service';
import { Favorite } from '../../../core/models/favorite.model';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="favorites-page">
      <!-- HEADER -->
      <section class="fav-header">
        <div class="container">
          <div class="header-badge">
            <span>❤️ LISTE DE SOUHAITS ATHLÈTE</span>
          </div>
          <h1 class="fav-title">Mes Équipements Favoris</h1>
          <p class="fav-subtitle">
            Retrouvez tous les articles de sport et optiques sauvegardés pour vos futurs entraînements ou compétitions.
          </p>
        </div>
      </section>

      <div class="container fav-body">
        <!-- État vide -->
        <div class="empty-fav-card sv-card" *ngIf="favorites.length === 0">
          <div class="empty-heart-icon">🤍</div>
          <h2>Aucun équipement dans vos favoris</h2>
          <p>Explorez notre catalogue de musculation, running, optique et nutrition pour ajouter vos articles favoris.</p>
          <a routerLink="/catalogue" class="btn-sv btn-sv-primary btn-sv-lg">
            <span>Explorer le Catalogue</span>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </a>
        </div>

        <!-- Grille des favoris -->
        <div class="fav-grid" *ngIf="favorites.length > 0">
          <div *ngFor="let fav of favorites" class="sv-card fav-card">
            <div class="card-media">
              <img [src]="fav.product.imageUrl" [alt]="fav.product.name" class="card-img">
              <button (click)="removeFavorite(fav.product.id)" class="btn-remove-fav" title="Retirer des favoris">
                ❤️
              </button>
              <span class="stock-badge" [class.low]="fav.product.stock <= 5">
                {{ fav.product.stock > 0 ? (fav.product.stock <= 5 ? 'Plus que ' + fav.product.stock + ' en stock' : 'En stock') : 'Rupture' }}
              </span>
            </div>

            <div class="card-content">
              <div class="card-meta">
                <span class="badge-sv badge-blue">{{ fav.product.category?.name || 'Sport' }}</span>
                <span class="card-rating">★ {{ fav.product.rating }}</span>
              </div>

              <h3 class="card-title">{{ fav.product.name }}</h3>
              <p class="card-desc">{{ fav.product.description | slice:0:110 }}...</p>

              <div class="card-footer">
                <div class="price-box">
                  <span class="price-val">{{ fav.product.price | number:'1.2-2' }} €</span>
                </div>
                <button (click)="addToCart(fav.product)" class="btn-sv btn-sv-primary btn-sv-sm" [disabled]="fav.product.stock === 0">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="9" cy="21" r="1"></circle>
                    <circle cx="20" cy="21" r="1"></circle>
                    <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                  </svg>
                  <span>Ajouter au panier</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1280px;
      margin: 0 auto;
      padding: 0 20px;
    }
    .fav-header {
      background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0284c7 100%);
      color: white;
      padding: 50px 0 45px;
      text-align: center;
    }
    .header-badge {
      display: inline-block;
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.25);
      padding: 5px 14px;
      border-radius: 20px;
      font-size: 0.8rem;
      font-weight: 700;
      letter-spacing: 0.5px;
      margin-bottom: 12px;
    }
    .fav-title {
      font-size: 2.3rem;
      font-weight: 900;
      margin: 0 0 10px;
      letter-spacing: -0.5px;
    }
    .fav-subtitle {
      max-width: 650px;
      margin: 0 auto;
      color: #cbd5e1;
      font-size: 1rem;
      line-height: 1.5;
    }

    .fav-body {
      padding: 40px 20px 60px;
    }

    .empty-fav-card {
      text-align: center;
      padding: 60px 20px;
      max-width: 550px;
      margin: 0 auto;
    }
    .empty-heart-icon {
      font-size: 4rem;
      margin-bottom: 15px;
    }

    .fav-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
      gap: 25px;
    }

    .fav-card {
      display: flex;
      flex-direction: column;
      overflow: hidden;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }
    .fav-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 24px -10px rgba(30, 58, 138, 0.18);
    }

    .card-media {
      position: relative;
      height: 200px;
      background: #f1f5f9;
      overflow: hidden;
    }
    .card-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .btn-remove-fav {
      position: absolute;
      top: 10px;
      right: 10px;
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 50%;
      width: 36px;
      height: 36px;
      font-size: 1.1rem;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 6px rgba(0,0,0,0.1);
      transition: transform 0.15s ease;
    }
    .btn-remove-fav:hover {
      transform: scale(1.15);
      background: #fef2f2;
    }
    .stock-badge {
      position: absolute;
      bottom: 10px;
      left: 10px;
      background: rgba(15, 23, 42, 0.85);
      color: #38bdf8;
      font-size: 0.75rem;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 6px;
      backdrop-filter: blur(4px);
    }
    .stock-badge.low {
      background: #dc2626;
      color: white;
    }

    .card-content {
      padding: 18px;
      display: flex;
      flex-direction: column;
      flex-grow: 1;
    }
    .card-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }
    .card-rating {
      font-size: 0.82rem;
      font-weight: 700;
      color: #eab308;
    }
    .card-title {
      font-size: 1.05rem;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 6px;
      line-height: 1.35;
    }
    .card-desc {
      color: #64748b;
      font-size: 0.85rem;
      line-height: 1.45;
      margin-bottom: 15px;
      flex-grow: 1;
    }
    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 12px;
      border-top: 1px solid #f1f5f9;
    }
    .price-val {
      font-size: 1.25rem;
      font-weight: 800;
      color: #1e3a8a;
    }
  `]
})
export class FavoritesComponent implements OnInit {
  favorites: Favorite[] = [];

  constructor(
    private favoriteService: FavoriteService,
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    this.favoriteService.favorites$.subscribe(favs => {
      this.favorites = favs;
    });
  }

  removeFavorite(productId: number): void {
    this.favoriteService.removeFavorite(productId);
  }

  addToCart(product: Product): void {
    this.cartService.addToCart(product, 1);
  }
}
