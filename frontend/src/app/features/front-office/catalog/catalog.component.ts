import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { FavoriteService } from '../../../core/services/favorite.service';
import { ReviewService } from '../../../core/services/review.service';
import { Product } from '../../../core/models/product.model';
import { Category } from '../../../core/models/category.model';
import { Review } from '../../../core/models/review.model';
import { AiService } from '../../../core/services/ai.service';
import { ProductRecommendation } from '../../../core/models/ai.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="catalog-page">
      <!-- En-tête Catalogue -->
      <section class="catalog-header">
        <div class="container">
          <div class="header-badge-row">
            <span class="badge-sv badge-white">BOUTIQUE OFFICIELLE</span>
          </div>
          <h1 class="catalog-title">Boutique & Produits Sportifs</h1>
          <p class="catalog-subtitle">
            Équipements de musculation, running, optique et nutrition sélectionnés pour les athlètes exigeants.
          </p>

          <!-- BANNIÈRE PROGRAMME SPORTIF -->
          <div class="ai-goal-banner">
            <div class="ai-banner-left">
              <span class="ai-robot-icon">🎯</span>
              <div>
                <strong>Filtrer le catalogue par discipline</strong>
                <p>Choisissez les produits à afficher :</p>
              </div>
            </div>
            <div class="ai-goal-pills">
              <button *ngFor="let g of sportGoals" (click)="selectGoal(g)" class="goal-pill" [class.active]="selectedGoal === g">
                {{ g === 'Tous' ? '🌐 Tous les sports' : (g === 'Musculation' ? '💪 Musculation (Bandes, Gants, Ceinture)' : '🏃 ' + g) }}
              </button>
            </div>
          </div>
        </div>
      </section>

    <!-- SECTION RECOMMANDATIONS DE PRODUITS -->
    <section class="ai-recs-section">
      <div class="container">
        <div class="ai-box sv-card">
          <div class="ai-box-header">
            <div>
              <div class="ai-header-badge">
                CONSEILS & SÉLECTIONS SPORTIVES
              </div>
              <h2 class="ai-section-title">Recommandations selon votre Programme Sportif</h2>
              <p class="ai-section-subtitle">
                Le modele apprend des favoris et achats et compare les produits au programme sportif choisi.
              </p>
            </div>
            <!-- Sélecteur d'objectif -->
            <div class="ai-goal-selector">
              <button *ngFor="let g of availableGoals" (click)="setGoal(g.key)" class="btn-goal-tab" [class.active]="currentGoal === g.key">
                {{ g.label }}
              </button>
            </div>
          </div>

          <!-- Règle d'exemple du sujet mise en avant -->
          <div class="ai-example-banner" *ngIf="currentGoal === 'Musculation'">
            <span class="lamp-icon">💡</span>
            <div>
              <strong>Exemple du Sujet : Programme Musculation &rarr; Bandes élastiques, Gants et Ceinture de musculation</strong>
              <p>Le système regroupe les accessoires de protection lombaire, d'échauffement articulaire et de grip pour maximiser la sécurité et l'hypertrophie.</p>
            </div>
          </div>

          <!-- Grille des recommandations IA -->
          <div class="ai-cards-grid">
            <div *ngFor="let rec of aiRecommendations" class="sv-card rec-card">
              <div class="rec-media">
                <img [src]="rec.product.imageUrl" [alt]="rec.product.name" class="rec-img">
                <span class="match-score-badge">{{ rec.matchPercentage }}% de pertinence</span>
                <button (click)="toggleFav(rec.product, $event)" class="btn-rec-fav" [title]="isFav(rec.product.id) ? 'Retirer' : 'Ajouter'">
                  {{ isFav(rec.product.id) ? '❤️' : '🤍' }}
                </button>
              </div>

              <div class="rec-content">
                <div class="rec-meta">
                  <span class="badge-sv badge-blue">{{ rec.badge }}</span>
                  <span class="rec-rating">⭐ {{ rec.product.rating }}</span>
                </div>

                <h3 class="rec-title">{{ rec.product.name }}</h3>

                <!-- Rationale -->
                <div class="rec-reason-box">
                  <span class="rec-reason-text">✓ {{ rec.recommendationReason }}</span>
                </div>

                <div class="rec-footer">
                  <span class="rec-price">{{ rec.product.price | number:'1.2-2' }} €</span>
                  <button (click)="addToCart(rec.product)" class="btn-sv btn-sv-primary btn-sv-sm" [disabled]="rec.product.stock === 0">
                    Ajouter au panier
                  </button>
                </div>
              </div>
            </div>
          </div>
          <p *ngIf="aiRecommendations.length === 0" class="recommendations-empty">Aucune recommandation disponible pour ce programme pour le moment.</p>
        </div>
      </div>
    </section>

      <!-- Barre d'outils et filtres -->
      <div class="container catalog-body">
        <div class="catalog-layout">
          <!-- SIDEBAR FILTRES -->
          <aside class="filters-sidebar">
            <div class="filter-card">
              <h3 class="filter-heading">Recherche & Filtres</h3>

              <!-- Recherche mot-clé -->
              <div class="form-group-sv">
                <label class="form-label-sv">Mots-clés</label>
                <input type="text" [(ngModel)]="searchQuery" (input)="applyFilters()" placeholder="Ex: Bandes, gants, ceinture..." class="form-control-sv">
              </div>

              <!-- Catégories -->
              <div class="form-group-sv">
                <label class="form-label-sv">Rayons & Disciplines</label>
                <div class="category-radio-list">
                  <label class="radio-option" [class.selected]="selectedCategoryId === null">
                    <input type="radio" name="category" [value]="null" [checked]="selectedCategoryId === null" (change)="selectCategory(null)">
                    <span>Toutes disciplines ({{ allProducts.length }})</span>
                  </label>
                  <label *ngFor="let cat of categories" class="radio-option" [class.selected]="selectedCategoryId === cat.id">
                    <input type="radio" name="category" [value]="cat.id" [checked]="selectedCategoryId === cat.id" (change)="selectCategory(cat.id)">
                    <span>{{ cat.name }}</span>
                  </label>
                </div>
              </div>

              <!-- Tri par prix -->
              <div class="form-group-sv">
                <label class="form-label-sv">Trier par</label>
                <select [(ngModel)]="sortBy" (change)="applyFilters()" class="form-control-sv">
                  <option value="featured">Recommandés & Nouveautés</option>
                  <option value="price-asc">Prix : Moins cher au plus cher</option>
                  <option value="price-desc">Prix : Plus cher au moins cher</option>
                  <option value="rating">Meilleures notes clients</option>
                </select>
              </div>

              <!-- En stock uniquement -->
              <div class="checkbox-option">
                <label>
                  <input type="checkbox" [(ngModel)]="inStockOnly" (change)="applyFilters()">
                  <span>En stock uniquement</span>
                </label>
              </div>

              <button (click)="resetFilters()" class="btn-sv btn-sv-outline btn-sv-sm btn-block">
                Réinitialiser les filtres
              </button>

              <div class="sidebar-extra-links">
                <a routerLink="/favoris" class="extra-link">❤️ Voir mes favoris ({{ favoriteCount }})</a>
                <a routerLink="/coach-matching" class="extra-link">Trouver un coach sportif</a>
                <a routerLink="/aliments" class="extra-link">Aliments & Nutrition</a>
              </div>
            </div>
          </aside>

          <!-- GRILLE DES PRODUITS -->
          <div class="catalog-main">
            <!-- Barre de résultat -->
            <div class="results-bar">
              <div class="results-count">
                <strong>{{ filteredProducts.length }}</strong> produit(s) trouvé(s)
                <span *ngIf="selectedGoal !== 'Tous'" class="active-goal-tag">Objectif : {{ selectedGoal }}</span>
              </div>
              <div class="active-category-name" *ngIf="getActiveCategoryName()">
                Rayon : <span class="badge-sv badge-blue">{{ getActiveCategoryName() }}</span>
              </div>
            </div>

            <!-- Grille -->
            <div class="products-grid" *ngIf="filteredProducts.length > 0">
              <div *ngFor="let product of filteredProducts" class="sv-card product-card">
                <div class="product-media">
                  <img [src]="product.imageUrl" [alt]="product.name" class="product-img">
                  <span class="product-badge-featured" *ngIf="product.featured">Top Vente</span>
                  <span class="product-badge-stock" [class.low]="product.stock <= 5">
                    {{ product.stock > 0 ? (product.stock <= 5 ? 'Plus que ' + product.stock + ' en stock !' : 'En stock') : 'Rupture' }}
                  </span>

                  <!-- Bouton Favoris Coeur -->
                  <button (click)="toggleFav(product, $event)" class="btn-fav-heart" [class.is-fav]="isFav(product.id)" [title]="isFav(product.id) ? 'Retirer des favoris' : 'Ajouter aux favoris'">
                    {{ isFav(product.id) ? '❤️' : '🤍' }}
                  </button>
                </div>

                <div class="product-info">
                  <div class="cat-rating-row">
                    <span class="product-category">{{ product.category?.name || 'Sport' }}</span>
                    <!-- Clic sur la note pour ouvrir les avis -->
                    <button (click)="openReviews(product)" class="btn-reviews-link" title="Consulter et ajouter un avis">
                      ⭐ {{ product.rating }} ({{ product.reviewsCount }})
                    </button>
                  </div>

                  <h3 class="product-title" (click)="openReviews(product)">{{ product.name }}</h3>

                  <!-- Sport Goal tag si présent -->
                  <div class="goal-tag-box" *ngIf="product.sportGoal">
                    <span class="goal-tag">🎯 Idéal : {{ product.sportGoal }}</span>
                  </div>

                  <p class="product-desc">{{ product.description }}</p>

                  <div class="product-footer">
                    <div class="product-price">
                      <span class="amount">{{ product.price | number:'1.2-2' }} €</span>
                    </div>
                    <button (click)="addToCart(product)" class="btn-sv btn-sv-primary btn-sv-sm" [disabled]="product.stock === 0">
                      Ajouter au panier
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- État vide -->
            <div class="empty-state" *ngIf="filteredProducts.length === 0">
              <div class="empty-icon">🔍</div>
              <h3>Aucun équipement ne correspond à votre sélection</h3>
              <p>Essayez de changer d'objectif sportif ou d'ajuster vos mots-clés.</p>
              <button (click)="resetFilters()" class="btn-sv btn-sv-primary">Voir tous les produits</button>
            </div>
          </div>
        </div>
      </div>

      <!-- MODAL AVIS & REVIEWS CLIENTS -->
      <div class="modal-overlay" *ngIf="selectedProductForReviews">
        <div class="modal-backdrop" (click)="closeReviews()"></div>
        <div class="modal-content sv-card">
          <div class="modal-header">
            <div>
              <h3>Avis Clients &bull; {{ selectedProductForReviews.name }}</h3>
              <div class="product-review-header-rating">
                <span class="big-star">⭐ {{ selectedProductForReviews.rating }} / 5</span>
                <span>({{ currentProductReviews.length }} avis déposés)</span>
              </div>
            </div>
            <button (click)="closeReviews()" class="btn-close-modal">✕</button>
          </div>

          <div class="modal-body-scroll">
            <!-- Formulaire pour ajouter un avis -->
            <div class="add-review-box">
              <h4>💬 Laisser un avis sur cet équipement</h4>
              <div class="review-form-grid">
                <div class="form-group-sv">
                  <label class="form-label-sv">Votre nom / Pseudo *</label>
                  <input type="text" [(ngModel)]="newReviewAuthor" class="form-control-sv" placeholder="Ex: Thomas D.">
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Note attribuée *</label>
                  <select [(ngModel)]="newReviewRating" class="form-control-sv">
                    <option [value]="5">⭐⭐⭐⭐⭐ 5/5 - Exceptionnel</option>
                    <option [value]="4">⭐⭐⭐⭐ 4/5 - Très bon produit</option>
                    <option [value]="3">⭐⭐⭐ 3/5 - Correct</option>
                    <option [value]="2">⭐⭐ 2/5 - Décevant</option>
                    <option [value]="1">⭐ 1/5 - Insuffisant</option>
                  </select>
                </div>
              </div>
              <div class="form-group-sv">
                <label class="form-label-sv">Votre commentaire d'athlète *</label>
                <textarea [(ngModel)]="newReviewComment" rows="2" class="form-control-sv" placeholder="Décrivez votre expérience avec ce produit lors de vos entraînements..."></textarea>
              </div>
              <button (click)="submitReview()" class="btn-sv btn-sv-primary btn-sv-sm" [disabled]="!newReviewAuthor || !newReviewComment">
                Publier mon avis
              </button>
            </div>

            <!-- Liste des avis existants -->
            <div class="reviews-list">
              <h4>Derniers retours d'expérience</h4>
              <div *ngIf="currentProductReviews.length === 0" class="no-reviews">
                Aucun avis pour l'instant. Soyez le premier à donner votre avis !
              </div>

              <div *ngFor="let rev of currentProductReviews" class="review-item">
                <div class="review-item-header">
                  <div>
                    <strong>{{ rev.authorName }}</strong>
                    <span class="review-stars">{{ getStarsString(rev.rating) }}</span>
                  </div>
                  <div class="review-actions">
                    <button (click)="deleteReview(rev.id!)" class="btn-del-review" title="Supprimer cet avis">✕</button>
                  </div>
                </div>
                <p class="review-comment">{{ rev.comment }}</p>
                <span class="review-date" *ngIf="rev.createdAt">{{ rev.createdAt | slice:0:10 }}</span>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button (click)="closeReviews()" class="btn-sv btn-sv-outline">Fermer</button>
          </div>
        </div>
      </div>

      <!-- Toast d'ajout au panier -->
      <div class="cart-toast" *ngIf="showToast">
        <div class="toast-content">
          <span>✓</span>
          <span>Article ajouté avec succès à votre panier !</span>
        </div>
        <a routerLink="/panier" class="toast-btn">Voir le panier</a>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1280px;
      margin: 0 auto;
      padding: 0 20px;
    }

    .catalog-header {
      background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0284c7 100%);
      color: white;
      padding: 45px 0 35px;
      margin-bottom: 35px;
      border-bottom: 3px solid #38bdf8;
    }
    .header-badge-row {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      margin-bottom: 8px;
    }
    .badge-outline-white {
      background: rgba(255, 255, 255, 0.12);
      border: 1px solid rgba(255, 255, 255, 0.3);
      color: #f8fafc;
    }
    .catalog-title {
      color: white;
      font-size: 2.3rem;
      font-weight: 900;
      margin: 10px 0 8px;
      letter-spacing: -0.5px;
    }
    .catalog-subtitle {
      color: #e0f2fe;
      font-size: 1rem;
      max-width: 720px;
      line-height: 1.5;
      margin-bottom: 25px;
    }

    /* RECOMMANDATIONS IA */
    .ai-recs-section {
      padding: 0 0 28px;
      margin-top: -18px;
      position: relative;
    }
    .recommendations-empty {
      margin: 20px 0 0;
      padding: 18px;
      border-radius: 10px;
      background: #f8fafc;
      color: #64748b;
      text-align: center;
    }
    .ai-box {
      background: white;
      border: 2px solid #38bdf8;
      border-radius: 20px;
      padding: 30px;
      box-shadow: 0 15px 35px -5px rgba(2, 132, 199, 0.15);
    }
    .ai-box-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 20px;
      margin-bottom: 20px;
    }
    .ai-header-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: #e0f2fe;
      color: #0284c7;
      font-size: 0.78rem;
      font-weight: 800;
      padding: 4px 12px;
      border-radius: 20px;
      margin-bottom: 8px;
      letter-spacing: 0.5px;
    }
    .ai-section-title {
      font-size: 1.8rem;
      font-weight: 900;
      color: #0f172a;
      margin: 0 0 6px;
    }
    .ai-section-subtitle {
      font-size: 0.95rem;
      color: #64748b;
      margin: 0;
      max-width: 600px;
    }
    .ai-goal-selector {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .btn-goal-tab {
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      color: #334155;
      font-weight: 700;
      font-size: 0.85rem;
      padding: 8px 16px;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .btn-goal-tab.active, .btn-goal-tab:hover {
      background: #1e3a8a;
      color: white;
      border-color: #1e3a8a;
    }

    .ai-example-banner {
      background: #eff6ff;
      border-left: 4px solid #0284c7;
      padding: 12px 18px;
      border-radius: 8px;
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 25px;
    }
    .lamp-icon {
      font-size: 1.5rem;
    }
    .ai-example-banner strong {
      display: block;
      color: #1e3a8a;
      font-size: 0.9rem;
    }
    .ai-example-banner p {
      margin: 2px 0 0;
      font-size: 0.82rem;
      color: #475569;
    }

    .ai-cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 20px;
    }
    .rec-card {
      display: flex;
      flex-direction: column;
      border-radius: 14px;
      overflow: hidden;
      border: 1px solid #e2e8f0;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }
    .rec-card:hover {
      transform: translateY(-3px);
      box-shadow: 0 10px 20px rgba(30, 58, 138, 0.12);
    }
    .rec-media {
      position: relative;
      height: 180px;
      background: #f8fafc;
    }
    .rec-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
                <span class="match-score-badge">{{ rec.matchPercentage }}% de pertinence</span>
      position: absolute;
      bottom: 8px;
      left: 8px;
      background: #0284c7;
      color: white;
      font-weight: 800;
      font-size: 0.72rem;
      padding: 3px 8px;
      border-radius: 6px;
    }
    .btn-rec-fav {
      position: absolute;
      top: 8px;
      right: 8px;
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 50%;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .rec-content {
      padding: 16px;
      display: flex;
      flex-direction: column;
      flex-grow: 1;
    }
    .rec-meta {
      display: flex;
      justify-content: space-between;
      margin-bottom: 6px;
    }
    .rec-rating {
      font-size: 0.8rem;
      color: #eab308;
      font-weight: 700;
    }
    .rec-title {
      font-size: 0.98rem;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 8px;
      line-height: 1.35;
    }
    .rec-reason-box {
      background: #f8fafc;
      border-radius: 6px;
      padding: 8px 10px;
      margin-bottom: 12px;
      flex-grow: 1;
    }
    .rec-reason-text {
      font-size: 0.78rem;
      color: #475569;
      line-height: 1.4;
      display: block;
    }
    .rec-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 10px;
      border-top: 1px solid #f1f5f9;
    }
    .rec-price {
      font-size: 1.15rem;
      font-weight: 800;
      color: #1e3a8a;
    }


    /* BANNIERE IA OBJECTIF */
    .ai-goal-banner {
      background: rgba(15, 23, 42, 0.65);
      border: 1px solid rgba(56, 189, 248, 0.4);
      backdrop-filter: blur(8px);
      padding: 16px 20px;
      border-radius: 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 15px;
    }
    .ai-banner-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .ai-robot-icon {
      font-size: 2rem;
    }
    .ai-banner-left strong {
      font-size: 0.95rem;
      color: #38bdf8;
      display: block;
    }
    .ai-banner-left p {
      font-size: 0.8rem;
      color: #cbd5e1;
      margin: 2px 0 0;
    }
    .ai-goal-pills {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .goal-pill {
      background: rgba(255, 255, 255, 0.12);
      border: 1px solid rgba(255, 255, 255, 0.2);
      color: white;
      font-size: 0.82rem;
      font-weight: 700;
      padding: 6px 12px;
      border-radius: 20px;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .goal-pill:hover, .goal-pill.active {
      background: #0284c7;
      border-color: #38bdf8;
      color: white;
      box-shadow: 0 0 12px rgba(56, 189, 248, 0.4);
    }

    .catalog-layout {
      display: grid;
      grid-template-columns: 280px 1fr;
      gap: 32px;
      margin-bottom: 80px;
    }

    /* FILTRES */
    .filter-card {
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 16px;
      padding: 24px;
      box-shadow: 0 4px 15px rgba(29, 78, 216, 0.05);
      position: sticky;
      top: 100px;
    }
    .filter-heading {
      font-size: 1.1rem;
      margin-bottom: 20px;
      padding-bottom: 12px;
      border-bottom: 1px solid #f1f5f9;
      color: #0f172a;
    }
    .category-radio-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .radio-option {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 0.88rem;
      color: #475569;
      padding: 6px 10px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .radio-option:hover, .radio-option.selected {
      background: #eff6ff;
      color: #1d4ed8;
      font-weight: 600;
    }
    .checkbox-option {
      margin: 16px 0;
      font-size: 0.88rem;
      color: #334155;
    }
    .sidebar-extra-links {
      margin-top: 20px;
      padding-top: 16px;
      border-top: 1px solid #f1f5f9;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .extra-link {
      font-size: 0.83rem;
      font-weight: 700;
      color: #1e3a8a;
      text-decoration: none;
      padding: 6px 8px;
      border-radius: 6px;
      background: #f8fafc;
      transition: background 0.15s ease;
    }
    .extra-link:hover {
      background: #e0f2fe;
    }

    /* CATALOG MAIN */
    .results-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      padding-bottom: 12px;
      border-bottom: 1px solid #e2e8f0;
    }
    .results-count {
      font-size: 0.95rem;
      color: #64748b;
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .active-goal-tag {
      background: #e0f2fe;
      color: #0284c7;
      font-weight: 700;
      font-size: 0.8rem;
      padding: 2px 8px;
      border-radius: 12px;
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
      overflow: hidden;
    }
    .product-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }
    .product-card:hover .product-img {
      transform: scale(1.04);
    }

    .btn-fav-heart {
      position: absolute;
      top: 10px;
      right: 10px;
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 50%;
      width: 38px;
      height: 38px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.1rem;
      cursor: pointer;
      box-shadow: 0 2px 6px rgba(0,0,0,0.1);
      transition: transform 0.15s ease, background 0.15s ease;
      z-index: 2;
    }
    .btn-fav-heart:hover {
      transform: scale(1.15);
      background: #fef2f2;
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
      backdrop-filter: blur(4px);
    }
    .product-badge-stock.low {
      background: #dc2626;
      color: white;
    }

    .product-info {
      padding: 18px;
      display: flex;
      flex-direction: column;
      flex-grow: 1;
    }
    .cat-rating-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }
    .product-category {
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #0284c7;
    }
    .btn-reviews-link {
      background: none;
      border: none;
      color: #eab308;
      font-size: 0.82rem;
      font-weight: 700;
      cursor: pointer;
      padding: 0;
      text-decoration: underline;
    }

    .product-title {
      font-size: 1.05rem;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 6px;
      line-height: 1.35;
      cursor: pointer;
    }
    .product-title:hover {
      color: #1d4ed8;
    }

    .goal-tag-box {
      margin-bottom: 8px;
    }
    .goal-tag {
      background: #f1f5f9;
      color: #1e3a8a;
      font-size: 0.75rem;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
    }

    .product-desc {
      font-size: 0.84rem;
      color: #64748b;
      line-height: 1.45;
      margin-bottom: 16px;
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
      padding-top: 12px;
      border-top: 1px solid #f1f5f9;
    }
    .product-price .amount {
      font-size: 1.3rem;
      font-weight: 800;
      color: #1e3a8a;
    }

    /* Modal Avis */
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
      width: 92%;
      max-height: 85vh;
      display: flex;
      flex-direction: column;
      z-index: 10;
      overflow: hidden;
    }
    .modal-header {
      padding: 20px 24px;
      border-bottom: 1px solid #e2e8f0;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .modal-header h3 {
      font-size: 1.25rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 4px;
    }
    .product-review-header-rating {
      font-size: 0.88rem;
      color: #64748b;
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .big-star {
      color: #eab308;
      font-weight: 800;
    }
    .btn-close-modal {
      background: none;
      border: none;
      font-size: 1.25rem;
      cursor: pointer;
      color: #64748b;
    }
    .modal-body-scroll {
      padding: 24px;
      overflow-y: auto;
      flex-grow: 1;
    }

    .add-review-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 24px;
    }
    .add-review-box h4 {
      margin: 0 0 12px;
      font-size: 0.95rem;
      color: #0f172a;
    }
    .review-form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }

    .reviews-list h4 {
      margin: 0 0 14px;
      font-size: 1rem;
      color: #0f172a;
    }
    .no-reviews {
      color: #64748b;
      font-size: 0.88rem;
      font-style: italic;
    }
    .review-item {
      border-bottom: 1px solid #f1f5f9;
      padding: 12px 0;
    }
    .review-item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
    }
    .review-stars {
      color: #eab308;
      margin-left: 8px;
      font-size: 0.85rem;
    }
    .btn-del-review {
      background: none;
      border: none;
      color: #dc2626;
      cursor: pointer;
      font-size: 0.85rem;
    }
    .review-comment {
      font-size: 0.85rem;
      color: #475569;
      line-height: 1.45;
      margin: 0 0 4px;
    }
    .review-date {
      font-size: 0.75rem;
      color: #94a3b8;
    }

    .modal-footer {
      padding: 14px 24px;
      border-top: 1px solid #e2e8f0;
      display: flex;
      justify-content: flex-end;
      background: #f8fafc;
    }

    .cart-toast {
      position: fixed;
      bottom: 25px;
      right: 25px;
      background: #0f172a;
      color: white;
      padding: 14px 20px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      gap: 15px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.25);
      z-index: 1000;
      border-left: 4px solid #38bdf8;
    }
    .toast-btn {
      color: #38bdf8;
      font-weight: 700;
      font-size: 0.85rem;
      text-decoration: underline;
    }
  `]
})
export class CatalogComponent implements OnInit {
  allProducts: Product[] = [];
  filteredProducts: Product[] = [];
  categories: Category[] = [];

  selectedCategoryId: number | null = null;
  selectedGoal: string = 'Tous';
  currentGoal = 'Musculation';
  availableGoals = [
    { key: 'Musculation', label: 'Musculation et Force' },
    { key: 'Running', label: 'Running et Trail' },
    { key: 'Fitness', label: 'Fitness et Minceur' },
    { key: 'Cyclisme', label: 'Cyclisme' }
  ];
  aiRecommendations: ProductRecommendation[] = [];
  sportGoals: string[] = ['Tous', 'Musculation', 'Running', 'Trail', 'Fitness'];

  searchQuery = '';
  sortBy = 'featured';
  inStockOnly = false;
  showToast = false;

  favoriteCount = 0;

  // Modal Avis
  selectedProductForReviews: Product | null = null;
  currentProductReviews: Review[] = [];
  newReviewAuthor = '';
  newReviewRating = 5;
  newReviewComment = '';

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private favoriteService: FavoriteService,
    private reviewService: ReviewService,
    private route: ActivatedRoute,
    private aiService: AiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadRecommendations();
    this.productService.products$.subscribe(products => {
      this.allProducts = products;
      this.applyFilters();
    });

    this.productService.getCategories().subscribe(cats => {
      this.categories = cats;
    });

    this.favoriteService.favorites$.subscribe(favs => {
      this.favoriteCount = favs.length;
    });

    this.route.queryParams.subscribe(params => {
      if (params['cat']) {
        this.selectedCategoryId = Number(params['cat']);
      }
      if (params['goal']) {
        this.selectedGoal = params['goal'];
        if (this.availableGoals.some(option => option.key === params['goal'])) {
          this.currentGoal = params['goal'];
          this.loadRecommendations();
        }
      }
      this.applyFilters();
    });
  }

  setGoal(goal: string): void {
    this.currentGoal = goal;
    this.selectedGoal = goal;
    this.applyFilters();
    this.loadRecommendations();
  }

  loadRecommendations(): void {
    const userEmail = this.authService.currentUserValue?.email;
    this.aiService.getProductRecommendations(this.currentGoal, userEmail || '').subscribe(recs => {
      this.aiRecommendations = recs;
    });
  }

  selectCategory(categoryId: number | null): void {
    this.selectedCategoryId = categoryId;
    this.applyFilters();
  }

  selectGoal(goal: string): void {
    this.selectedGoal = goal;
    if (goal !== 'Tous' && this.availableGoals.some(option => option.key === goal)) {
      this.currentGoal = goal;
      this.loadRecommendations();
    }
    this.applyFilters();
  }

  applyFilters(): void {
    let result = [...this.allProducts];

    if (this.selectedCategoryId !== null) {
      result = result.filter(p => p.category && p.category.id === this.selectedCategoryId);
    }

    if (this.selectedGoal !== 'Tous') {
      const g = this.selectedGoal.toLowerCase();
      result = result.filter(p =>
        (p.sportGoal && p.sportGoal.toLowerCase().includes(g)) ||
        p.name.toLowerCase().includes(g) ||
        (p.category && p.category.name.toLowerCase().includes(g))
      );
    }

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q) ||
        (p.category && p.category.name.toLowerCase().includes(q))
      );
    }

    if (this.inStockOnly) {
      result = result.filter(p => p.stock > 0);
    }

    if (this.sortBy === 'price-asc') {
      result.sort((a, b) => a.price - b.price);
    } else if (this.sortBy === 'price-desc') {
      result.sort((a, b) => b.price - a.price);
    } else if (this.sortBy === 'rating') {
      result.sort((a, b) => b.rating - a.rating);
    } else {
      result.sort((a, b) => (b.featured ? 1 : 0) - (a.featured ? 1 : 0));
    }

    this.filteredProducts = result;
  }

  resetFilters(): void {
    this.selectedCategoryId = null;
    this.selectedGoal = 'Tous';
    this.searchQuery = '';
    this.sortBy = 'featured';
    this.inStockOnly = false;
    this.applyFilters();
  }

  getActiveCategoryName(): string | null {
    if (this.selectedCategoryId === null) return null;
    const cat = this.categories.find(c => c.id === this.selectedCategoryId);
    return cat ? cat.name : null;
  }

  addToCart(product: Product): void {
    this.cartService.addToCart(product);
    this.showToast = true;
    setTimeout(() => this.showToast = false, 3500);
  }

  // Favoris
  isFav(productId: number): boolean {
    return this.favoriteService.isFavorite(productId);
  }

  toggleFav(product: Product, event: Event): void {
    event.stopPropagation();
    this.favoriteService.toggleFavorite(product).subscribe();
  }

  // Avis (Reviews)
  openReviews(product: Product): void {
    this.selectedProductForReviews = product;
    this.newReviewAuthor = '';
    this.newReviewRating = 5;
    this.newReviewComment = '';
    this.reviewService.getReviewsByProduct(product.id).subscribe(revs => {
      this.currentProductReviews = revs;
    });
  }

  closeReviews(): void {
    this.selectedProductForReviews = null;
    this.currentProductReviews = [];
  }

  submitReview(): void {
    if (!this.selectedProductForReviews) return;
    const rev: Review = {
      productId: this.selectedProductForReviews.id,
      authorName: this.newReviewAuthor,
      rating: Number(this.newReviewRating),
      comment: this.newReviewComment
    };
    this.reviewService.addReview(rev).subscribe(newRev => {
      this.currentProductReviews.unshift(newRev);
      this.newReviewAuthor = '';
      this.newReviewComment = '';
    });
  }

  deleteReview(reviewId: number): void {
    this.reviewService.deleteReview(reviewId).subscribe(() => {
      this.currentProductReviews = this.currentProductReviews.filter(r => r.id !== reviewId);
    });
  }

  getStarsString(rating: number): string {
    return '⭐'.repeat(Math.min(5, Math.max(1, rating)));
  }
}
