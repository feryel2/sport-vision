import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { FavoriteService } from '../../../core/services/favorite.service';
import { Product } from '../../../core/models/product.model';
import { Category } from '../../../core/models/category.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <!-- HERO BANNER ATHLETIQUE BLEU & BLANC -->
    <section class="hero-section">
      <div class="hero-bg-overlay"></div>
      <div class="container hero-container">
        <div class="hero-content">
          <div class="hero-badge-group">
          </div>

          <h1 class="hero-title">
            BOUTIQUE OFFICIELLE & ÉQUIPEMENTS DE HAUTE PERFORMANCE SPORTIVE
          </h1>
          <p class="hero-subtitle">
            Équipements de musculation, running, optique et nutrition sportive. Découvrez nos sélections personnalisées basées sur vos objectifs d'entraînement et notre mise en relation athlète &harr; coach.
          </p>
          <div class="hero-cta-group">
            <a routerLink="/catalogue" class="btn-sv btn-sv-primary btn-sv-lg">
              <span>Explorer la Boutique</span>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="5" y1="12" x2="19" y2="12"></line>
                <polyline points="12 5 19 12 12 19"></polyline>
              </svg>
            </a>
            <a routerLink="/coach-matching" class="btn-sv btn-sv-white btn-sv-lg">
              <span>Trouver un Coach</span>
            </a>
            <a routerLink="/aliments" class="btn-sv btn-sv-outline-white btn-sv-lg">
              <span>Nutrition & Aliments</span>
            </a>
          </div>

          <!-- Métriques Clés -->
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-val">100%</span>
              <span class="stat-lbl">Bleu & Blanc Officiel</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-val">Programme Pro</span>
              <span class="stat-lbl">Recommandations & Matching</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-val">4.9 / 5</span>
              <span class="stat-lbl">+1 200 avis athlètes</span>
            </div>
          </div>
        </div>

        <div class="hero-visual">
          <div class="visual-card">
            <img src="https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&q=80" alt="Entraînement Musculation Sport Vision" class="visual-img">
            <div class="visual-floating-badge">
              <div class="badge-avatar">🏆</div>
              <div class="badge-text">
                <strong>Pack Musculation</strong>
                <span>Bandes + Gants + Ceinture</span>
              </div>
              <span class="badge-price">Pack Essentiel</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- TEASER 1 : MATCHING SPORTIF ↔ COACH -->
    <section class="coach-teaser-section">
      <div class="container">
        <div class="coach-teaser-card sv-card">
          <div class="teaser-text">
            <span class="badge-sv badge-blue">PROGRAMME ENCADREMENT ATHLÈTE</span>
            <h2 class="teaser-title">Trouvez le Coach Idéal pour vos Objectifs</h2>
            <p class="teaser-desc">
              Objectif, niveau, sport préféré et disponibilité horaire sont comparés en temps réel avec les spécialités de nos coachs certifiés.
            </p>
            <div class="example-tag-row">
              <span class="example-badge">🎯 Musculation + Prise de masse + Débutant &rarr; Coachs certifiés</span>
            </div>
            <div class="teaser-actions">
              <a routerLink="/coach-matching" class="btn-sv btn-sv-primary btn-sv-lg">
                Trouver mon Coach Partenaire &rarr;
              </a>
            </div>
          </div>

          <div class="teaser-visual">
            <div class="coach-mini-preview">
              <div class="coach-bubble">
                <img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80" alt="Coach Marc" class="bubble-avatar">
                <div>
                  <strong>Marc Vauthier (98% Match)</strong>
                  <span>Spécialiste Musculation & Masse</span>
                </div>
              </div>
              <div class="coach-bubble offset">
                <img src="https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=200&q=80" alt="Coach Sarah" class="bubble-avatar">
                <div>
                  <strong>Sarah Benali (100% Match)</strong>
                  <span>Préparation Marathon & Cardio</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- RAYONS DU CATALOGUE -->
    <section class="categories-section">
      <div class="container">
        <div class="section-header">
          <div>
            <span class="badge-sv badge-blue">UNIVERS SPORTIFS</span>
            <h2 class="section-title">Catalogue de la Boutique Sportive</h2>
          </div>
          <a routerLink="/catalogue" class="btn-sv btn-sv-outline btn-sv-sm">Voir tout le catalogue &rarr;</a>
        </div>

        <div class="categories-grid">
          <div *ngFor="let cat of categories" class="category-card" [routerLink]="['/catalogue']" [queryParams]="{cat: cat.id}">
            <div class="cat-img-wrapper">
              <img [src]="cat.imageUrl" [alt]="cat.name" class="cat-img">
              <div class="cat-overlay"></div>
            </div>
            <div class="cat-details">
              <span class="cat-icon-badge">🏅</span>
              <h3 class="cat-name">{{ cat.name }}</h3>
              <p class="cat-desc">{{ cat.description }}</p>
              <span class="cat-link">Explorer ce rayon &rarr;</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- PRODUITS BEST-SELLERS & VEDETTES -->
    <section class="featured-section">
      <div class="container">
        <div class="section-header">
          <div>
            <span class="badge-sv badge-blue">SÉLECTION ÉLITE</span>
            <h2 class="section-title">Équipements Phares de la Boutique</h2>
          </div>
          <div class="filter-pills">
            <span class="pill-active">Tous les Best-Sellers</span>
          </div>
        </div>

        <div class="products-grid">
          <div *ngFor="let product of featuredProducts" class="sv-card product-card">
            <div class="product-media">
              <img [src]="product.imageUrl" [alt]="product.name" class="product-img">
              <span class="product-badge-featured" *ngIf="product.featured">Best-Seller</span>
              <span class="product-badge-stock" [class.low]="product.stock <= 5">
                {{ product.stock > 5 ? 'En stock' : 'Stock limité (' + product.stock + ')' }}
              </span>
              <button (click)="toggleFav(product, $event)" class="btn-home-fav" [title]="isFav(product.id) ? 'Retirer' : 'Ajouter'">
                {{ isFav(product.id) ? '❤️' : '🤍' }}
              </button>
            </div>

            <div class="product-info">
              <div class="product-category-row">
                <span class="product-category">{{ product.category?.name || 'Sport' }}</span>
                <span class="product-rating">⭐ {{ product.rating }}</span>
              </div>

              <h3 class="product-title" (click)="openQuickView(product)">{{ product.name }}</h3>
              <p class="product-desc">{{ product.description }}</p>

              <div class="product-footer">
                <div class="product-price">
                  <span class="amount">{{ product.price | number:'1.2-2' }} €</span>
                </div>
                <button (click)="addToCart(product)" class="btn-sv btn-sv-primary btn-sv-sm" [disabled]="product.stock === 0">
                  Ajouter
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- MODAL APERÇU RAPIDE -->
    <div class="modal-overlay" *ngIf="selectedProduct">
      <div class="modal-backdrop" (click)="closeQuickView()"></div>
      <div class="modal-content sv-card">
        <button (click)="closeQuickView()" class="btn-close-modal">✕</button>
        <div class="modal-preview-grid">
          <img [src]="selectedProduct.imageUrl" [alt]="selectedProduct.name" class="modal-img">
          <div>
            <span class="badge-sv badge-blue">{{ selectedProduct.category?.name }}</span>
            <h3 class="modal-title">{{ selectedProduct.name }}</h3>
            <div class="modal-price">{{ selectedProduct.price | number:'1.2-2' }} €</div>
            <p class="modal-desc">{{ selectedProduct.description }}</p>
            <div class="modal-specs" *ngIf="selectedProduct.specifications">
              <strong>Spécifications :</strong> {{ selectedProduct.specifications }}
            </div>
            <button (click)="addToCart(selectedProduct); closeQuickView()" class="btn-sv btn-sv-primary btn-block">
              Ajouter au panier
            </button>
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

    /* HERO */
    .hero-section {
      position: relative;
      background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0284c7 100%);
      color: white;
      padding: 60px 0 70px;
      overflow: hidden;
      border-bottom: 4px solid #38bdf8;
    }
    .hero-container {
      position: relative;
      display: grid;
      grid-template-columns: 1.2fr 0.8fr;
      gap: 40px;
      align-items: center;
      z-index: 2;
    }
    .hero-badge-group {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      margin-bottom: 18px;
    }
    .badge-resp {
      background: #0284c7;
      color: white;
      font-weight: 800;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 0.82rem;
      border: 1px solid #38bdf8;
    }
    .badge-theme {
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.25);
      color: white;
      font-weight: 700;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 0.82rem;
    }
    .hero-title {
      font-size: 2.7rem;
      font-weight: 900;
      line-height: 1.15;
      margin: 0 0 16px;
      letter-spacing: -0.5px;
    }
    .highlight-cyan {
      color: #38bdf8;
    }
    .hero-subtitle {
      font-size: 1.05rem;
      color: #cbd5e1;
      line-height: 1.55;
      margin-bottom: 28px;
      max-width: 600px;
    }
    .hero-cta-group {
      display: flex;
      gap: 14px;
      flex-wrap: wrap;
      margin-bottom: 35px;
    }
    .btn-sv-white {
      background: white;
      color: #1e3a8a;
      font-weight: 700;
    }
    .btn-sv-white:hover {
      background: #f1f5f9;
    }
    .btn-sv-outline-white {
      background: transparent;
      border: 2px solid white;
      color: white;
      font-weight: 700;
    }
    .btn-sv-outline-white:hover {
      background: rgba(255, 255, 255, 0.15);
    }
    .hero-stats {
      display: flex;
      gap: 20px;
      align-items: center;
      border-top: 1px solid rgba(255, 255, 255, 0.15);
      padding-top: 20px;
    }
    .stat-val {
      font-size: 1.4rem;
      font-weight: 900;
      color: #38bdf8;
      display: block;
    }
    .stat-lbl {
      font-size: 0.78rem;
      color: #94a3b8;
    }
    .stat-divider {
      width: 1px;
      height: 30px;
      background: rgba(255, 255, 255, 0.2);
    }

    .hero-visual {
      position: relative;
    }
    .visual-card {
      position: relative;
      border-radius: 20px;
      overflow: hidden;
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.35);
      border: 3px solid rgba(56, 189, 248, 0.3);
    }
    .visual-img {
      width: 100%;
      height: 380px;
      object-fit: cover;
      display: block;
    }
    .visual-floating-badge {
      position: absolute;
      bottom: 20px;
      left: 20px;
      right: 20px;
      background: rgba(15, 23, 42, 0.85);
      backdrop-filter: blur(10px);
      padding: 14px 18px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      gap: 14px;
      border: 1px solid rgba(56, 189, 248, 0.4);
    }
    .badge-avatar {
      font-size: 1.6rem;
    }
    .badge-text strong {
      display: block;
      color: white;
      font-size: 0.95rem;
    }
    .badge-text span {
      color: #38bdf8;
      font-size: 0.8rem;
    }
    .badge-price {
      margin-left: auto;
      background: #0284c7;
      color: white;
      font-weight: 800;
      padding: 4px 10px;
      border-radius: 8px;
      font-size: 0.85rem;
    }

    /* COACH TEASER */
    .coach-teaser-section {
      padding: 40px 0;
    }
    .coach-teaser-card {
      background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
      border: 2px solid #bfdbfe;
      border-radius: 20px;
      padding: 40px;
      display: grid;
      grid-template-columns: 1.2fr 0.8fr;
      gap: 30px;
      align-items: center;
    }
    .teaser-title {
      font-size: 1.9rem;
      font-weight: 900;
      color: #1e3a8a;
      margin: 10px 0 10px;
    }
    .teaser-desc {
      font-size: 0.95rem;
      color: #334155;
      line-height: 1.5;
      margin-bottom: 16px;
    }
    .example-tag-row {
      margin-bottom: 20px;
    }
    .example-badge {
      background: white;
      border: 1px solid #93c5fd;
      color: #1d4ed8;
      font-weight: 700;
      font-size: 0.85rem;
      padding: 5px 12px;
      border-radius: 8px;
    }
    .coach-mini-preview {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .coach-bubble {
      background: white;
      border: 1px solid #cbd5e1;
      padding: 12px 16px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 4px 10px rgba(0,0,0,0.06);
    }
    .coach-bubble.offset {
      margin-left: 30px;
    }
    .bubble-avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      object-fit: cover;
    }
    .coach-bubble strong {
      display: block;
      color: #0f172a;
      font-size: 0.9rem;
    }
    .coach-bubble span {
      font-size: 0.78rem;
      color: #0284c7;
      font-weight: 600;
    }

    /* RAYONS */
    .categories-section {
      padding: 40px 0;
    }
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      margin-bottom: 25px;
    }
    .section-title {
      font-size: 1.8rem;
      font-weight: 900;
      color: #0f172a;
      margin: 6px 0 0;
    }
    .categories-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 20px;
    }
    .category-card {
      position: relative;
      border-radius: 14px;
      overflow: hidden;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(0,0,0,0.06);
    }
    .cat-img-wrapper {
      position: relative;
      height: 180px;
    }
    .cat-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .cat-overlay {
      position: absolute;
      inset: 0;
      background: linear-gradient(180deg, rgba(15,23,42,0.1) 0%, rgba(15,23,42,0.85) 100%);
    }
    .cat-details {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      padding: 16px;
      color: white;
    }
    .cat-icon-badge {
      font-size: 1.2rem;
      margin-bottom: 4px;
      display: block;
    }
    .cat-name {
      font-size: 1.05rem;
      font-weight: 800;
      margin: 0 0 4px;
    }
    .cat-desc {
      font-size: 0.78rem;
      color: #cbd5e1;
      margin: 0 0 8px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .cat-link {
      font-size: 0.78rem;
      font-weight: 700;
      color: #38bdf8;
    }

    /* PRODUITS VEDETTES */
    .featured-section {
      padding: 40px 0 80px;
    }
    .filter-pills {
      font-size: 0.85rem;
      font-weight: 700;
      color: #1e3a8a;
    }
    .products-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 25px;
    }
    .product-card {
      display: flex;
      flex-direction: column;
      overflow: hidden;
      border-radius: 16px;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }
    .product-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 25px -10px rgba(29, 78, 216, 0.15);
    }
    .product-media {
      position: relative;
      height: 220px;
      background: #f8fafc;
    }
    .product-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .product-badge-featured {
      position: absolute;
      top: 10px;
      left: 10px;
      background: #0284c7;
      color: white;
      font-size: 0.72rem;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
    }
    .product-badge-stock {
      position: absolute;
      bottom: 10px;
      left: 10px;
      background: rgba(15, 23, 42, 0.85);
      color: #38bdf8;
      font-size: 0.72rem;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
    }
    .product-badge-stock.low {
      background: #dc2626;
      color: white;
    }
    .btn-home-fav {
      position: absolute;
      top: 10px;
      right: 10px;
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 50%;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
    }
    .product-info {
      padding: 18px;
      display: flex;
      flex-direction: column;
      flex-grow: 1;
    }
    .product-category-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 6px;
    }
    .product-category {
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      color: #0284c7;
    }
    .product-rating {
      font-size: 0.8rem;
      color: #eab308;
      font-weight: 700;
    }
    .product-title {
      font-size: 1.05rem;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 6px;
      cursor: pointer;
    }
    .product-desc {
      font-size: 0.84rem;
      color: #64748b;
      margin-bottom: 14px;
      flex-grow: 1;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .product-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 10px;
      border-top: 1px solid #f1f5f9;
    }
    .product-price .amount {
      font-size: 1.25rem;
      font-weight: 800;
      color: #1e3a8a;
    }

    /* Modal QuickView */
    .modal-overlay {
      position: fixed;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .modal-backdrop {
      position: absolute;
      inset: 0;
      background: rgba(15, 23, 42, 0.65);
      backdrop-filter: blur(4px);
    }
    .modal-content {
      position: relative;
      background: white;
      border-radius: 16px;
      max-width: 650px;
      width: 90%;
      padding: 24px;
      z-index: 10;
    }
    .btn-close-modal {
      position: absolute;
      top: 14px;
      right: 14px;
      background: none;
      border: none;
      font-size: 1.25rem;
      cursor: pointer;
    }
    .modal-preview-grid {
      display: grid;
      grid-template-columns: 1fr 1.2fr;
      gap: 20px;
    }
    .modal-img {
      width: 100%;
      height: 220px;
      object-fit: cover;
      border-radius: 10px;
    }
    .modal-title {
      font-size: 1.2rem;
      font-weight: 800;
      color: #0f172a;
      margin: 8px 0;
    }
    .modal-price {
      font-size: 1.4rem;
      font-weight: 800;
      color: #1e3a8a;
      margin-bottom: 8px;
    }
    .modal-desc {
      font-size: 0.85rem;
      color: #475569;
      line-height: 1.45;
      margin-bottom: 12px;
    }
    .modal-specs {
      background: #f8fafc;
      padding: 10px;
      border-radius: 8px;
      font-size: 0.8rem;
      margin-bottom: 14px;
    }

    @media (max-width: 992px) {
      .hero-container, .coach-teaser-card {
        grid-template-columns: 1fr;
      }
      .hero-title {
        font-size: 2.2rem;
      }
    }
  `]
})
export class HomeComponent implements OnInit {
  categories: Category[] = [];
  featuredProducts: Product[] = [];
  selectedProduct: Product | null = null;


  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private favoriteService: FavoriteService
  ) {}

  ngOnInit(): void {
    this.productService.getCategories().subscribe(cats => {
      this.categories = cats;
    });

    this.productService.getFeaturedProducts().subscribe(prods => {
      this.featuredProducts = prods;
    });

  }


  addToCart(product: Product): void {
    this.cartService.addToCart(product);
  }

  isFav(productId: number): boolean {
    return this.favoriteService.isFavorite(productId);
  }

  toggleFav(product: Product, event: Event): void {
    event.stopPropagation();
    this.favoriteService.toggleFavorite(product).subscribe();
  }

  openQuickView(product: Product): void {
    this.selectedProduct = product;
  }

  closeQuickView(): void {
    this.selectedProduct = null;
  }
}
