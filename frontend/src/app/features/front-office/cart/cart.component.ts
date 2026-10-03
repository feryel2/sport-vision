import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService, CartItem } from '../../../core/services/cart.service';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="cart-page">
      <!-- En-tête Panier -->
      <section class="cart-header">
        <div class="container">
          <div class="badge-group">
            <span class="badge-sv badge-white">PANIER & COMMANDES</span>
            <span class="badge-sv badge-outline-white">Gestion 3 — Responsable : Feryel Mansouri</span>
          </div>
          <h1 class="cart-title">Boutique : Panier, Paiement & Historique</h1>
          <p class="cart-subtitle">Gérez vos articles, validez votre commande en toute sécurité et consultez l'historique de vos achats passés.</p>

          <!-- TABS NAVIGATION -->
          <div class="cart-tabs-row">
            <button (click)="activeTab = 'cart'" class="tab-btn" [class.active]="activeTab === 'cart'">
              🛒 Mon Panier Actuel ({{ cartService.totalCount }})
            </button>
            <button (click)="activeTab = 'history'" class="tab-btn" [class.active]="activeTab === 'history'">
              📜 Historique de mes Commandes ({{ pastOrders.length }})
            </button>
          </div>
        </div>
      </section>

      <div class="container cart-body">
        <!-- TAB 1 : PANIER ACTUEL -->
        <ng-container *ngIf="activeTab === 'cart'">
          <!-- Si le panier est vide -->
          <div class="empty-cart-card sv-card" *ngIf="cartService.items.length === 0 && !confirmedOrder">
            <div class="empty-icon">🛒</div>
            <h2>Votre panier est actuellement vide</h2>
            <p>Découvrez notre sélection d'équipements de musculation, running, optique et nutrition sportive.</p>
            <a routerLink="/catalogue" class="btn-sv btn-sv-primary btn-sv-lg">Explorer le catalogue &rarr;</a>
          </div>

          <!-- Si le panier contient des articles -->
          <div class="cart-layout" *ngIf="cartService.items.length > 0 && !confirmedOrder">
            <!-- LISTE DES ARTICLES -->
            <div class="cart-items-section">
              <!-- Jauge livraison offerte -->
              <div class="shipping-bar-card sv-card">
                <div class="shipping-info">
                  <span *ngIf="cartService.subtotal >= 80" class="shipping-success">
                    🎉 Félicitations ! Vous bénéficiez de la <strong>Livraison Express Gratuite</strong> !
                  </span>
                  <span *ngIf="cartService.subtotal < 80">
                    Ajoutez encore <strong>{{ (80 - cartService.subtotal) | number:'1.2-2' }} €</strong> pour débloquer la livraison offerte.
                  </span>
                </div>
                <div class="progress-track">
                  <div class="progress-fill" [style.width.%]="getProgressPercentage()"></div>
                </div>
              </div>

              <!-- Table des articles -->
              <div class="sv-card items-card">
                <div class="cart-table-header">
                  <span>Article</span>
                  <span class="text-center">Quantité</span>
                  <span class="text-right">Total</span>
                  <span></span>
                </div>

                <div *ngFor="let item of cartService.items" class="cart-row">
                  <div class="item-product">
                    <img [src]="item.product.imageUrl" [alt]="item.product.name" class="item-thumb">
                    <div>
                      <span class="badge-sv badge-blue item-cat">{{ item.product.category?.name || 'Sport' }}</span>
                      <h4 class="item-name">{{ item.product.name }}</h4>
                      <span class="item-price-unit">{{ item.product.price | number:'1.2-2' }} € / unité</span>
                    </div>
                  </div>

                  <!-- Contrôles quantité -->
                  <div class="item-quantity">
                    <button (click)="decrement(item)" class="qty-btn" [disabled]="item.quantity <= 1">-</button>
                    <span class="qty-val">{{ item.quantity }}</span>
                    <button (click)="increment(item)" class="qty-btn" [disabled]="item.quantity >= item.product.stock">+</button>
                  </div>

                  <!-- Prix total ligne -->
                  <div class="item-total text-right">
                    <strong>{{ (item.product.price * item.quantity) | number:'1.2-2' }} €</strong>
                  </div>

                  <!-- Suppression -->
                  <div class="item-action text-right">
                    <button (click)="removeItem(item)" class="btn-delete" title="Supprimer cet article">✕</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- FORMULAIRE DE COMMANDE & TOTAL -->
            <div class="cart-summary-section">
              <div class="sv-card summary-card">
                <h3 class="summary-title">Validation de la Commande</h3>

                <div class="summary-rows">
                  <div class="summary-row">
                    <span>Sous-total articles</span>
                    <strong>{{ cartService.subtotal | number:'1.2-2' }} €</strong>
                  </div>
                  <div class="summary-row">
                    <span>Frais de livraison</span>
                    <strong class="text-success">{{ cartService.shippingCost === 0 ? 'Offerts' : (cartService.shippingCost | number:'1.2-2') + ' €' }}</strong>
                  </div>
                  <div class="summary-row total-row">
                    <span>Total à régler</span>
                    <span class="total-amount">{{ cartService.total | number:'1.2-2' }} €</span>
                  </div>
                </div>

                <!-- Coordonnées Athlète -->
                <div class="form-section">
                  <h4 class="form-section-title">Coordonnées de Livraison</h4>
                  <div class="form-group-sv">
                    <label class="form-label-sv">Nom complet</label>
                    <input type="text" [(ngModel)]="customerName" class="form-control-sv" placeholder="Votre nom complet">
                  </div>
                  <div class="form-group-sv">
                    <label class="form-label-sv">Email</label>
                    <input type="email" [(ngModel)]="customerEmail" class="form-control-sv" placeholder="votre@email.com">
                  </div>
                  <div class="form-group-sv">
                    <label class="form-label-sv">Téléphone</label>
                    <input type="tel" [(ngModel)]="customerPhone" class="form-control-sv" placeholder="+33 6 12 34 56 78">
                  </div>
                  <div class="form-group-sv">
                    <label class="form-label-sv">Adresse complète</label>
                    <textarea [(ngModel)]="shippingAddress" rows="2" class="form-control-sv" placeholder="Numéro, rue, code postal, ville"></textarea>
                  </div>
                </div>

                <!-- Mode de Paiement -->
                <div class="form-section">
                  <h4 class="form-section-title">Mode de Paiement Sécurisé</h4>
                  <div class="payment-options">
                    <label class="payment-radio" [class.selected]="paymentMethod === 'CARTE_BANCAIRE'">
                      <input type="radio" name="payment" value="CARTE_BANCAIRE" [(ngModel)]="paymentMethod">
                      <span>💳 Carte Bancaire (Visa / Mastercard)</span>
                    </label>
                    <label class="payment-radio" [class.selected]="paymentMethod === 'VIREMENT'">
                      <input type="radio" name="payment" value="VIREMENT" [(ngModel)]="paymentMethod">
                      <span>🏦 Virement Bancaire SEPA</span>
                    </label>
                    <label class="payment-radio" [class.selected]="paymentMethod === 'LIVRAISON'">
                      <input type="radio" name="payment" value="LIVRAISON" [(ngModel)]="paymentMethod">
                      <span>📦 Paiement à la Livraison</span>
                    </label>
                  </div>
                </div>

                <button (click)="checkout()" class="btn-sv btn-sv-primary btn-block btn-sv-lg" [disabled]="isSubmitting || !customerName || !shippingAddress">
                  <span *ngIf="!isSubmitting">Confirmer & Payer {{ cartService.total | number:'1.2-2' }} €</span>
                  <span *ngIf="isSubmitting">Traitement sécurisé en cours...</span>
                </button>
              </div>
            </div>
          </div>

          <!-- CONFIRMATION DE COMMANDE -->
          <div class="sv-card confirmation-card" *ngIf="confirmedOrder">
            <div class="confirm-icon">🎉</div>
            <span class="badge-sv badge-blue">COMMANDE VALIDÉE</span>
            <h2 class="confirm-title">Merci pour votre confiance sportive !</h2>
            <p class="confirm-num">Référence : <strong>{{ confirmedOrder.orderNumber }}</strong></p>
            <p class="confirm-desc">
              Votre commande d'un montant de <strong>{{ confirmedOrder.totalAmount | number:'1.2-2' }} €</strong> a été enregistrée avec le mode de paiement <strong>{{ confirmedOrder.paymentMethod }}</strong>.
            </p>
            <div class="confirm-actions">
              <button (click)="resetOrderView(); activeTab = 'history'" class="btn-sv btn-sv-primary">
                Consulter mon historique de commandes
              </button>
              <a routerLink="/catalogue" class="btn-sv btn-sv-outline">Continuer mes achats</a>
            </div>
          </div>
        </ng-container>

        <!-- TAB 2 : HISTORIQUE DES COMMANDES -->
        <ng-container *ngIf="activeTab === 'history'">
          <div class="history-section">
            <div class="history-header">
              <h2>Vos Commandes Passées</h2>
              <span class="badge-sv badge-blue">{{ pastOrders.length }} commande(s) enregistrée(s)</span>
            </div>

            <div *ngIf="pastOrders.length === 0" class="empty-cart-card sv-card">
              <div class="empty-icon">📜</div>
              <h3>Aucune commande passée pour l'instant</h3>
              <p>Vos commandes et factures apparaîtront ici après validation.</p>
              <button (click)="activeTab = 'cart'" class="btn-sv btn-sv-primary">Retour au panier</button>
            </div>

            <div class="orders-list" *ngIf="pastOrders.length > 0">
              <div *ngFor="let order of pastOrders" class="sv-card order-card">
                <div class="order-header-row">
                  <div>
                    <span class="order-ref">N° {{ order.orderNumber }}</span>
                    <span class="order-date">Passée le {{ order.createdAt | slice:0:10 }}</span>
                  </div>
                  <div class="order-status-box">
                    <span class="status-badge" [ngClass]="getStatusClass(order.status)">
                      {{ getStatusLabel(order.status) }}
                    </span>
                    <span class="order-total-badge">{{ order.totalAmount | number:'1.2-2' }} €</span>
                  </div>
                </div>

                <div class="order-details-grid">
                  <div class="detail-block">
                    <strong>Destinataire :</strong> {{ order.customerName }} ({{ order.customerEmail }})<br>
                    <strong>Adresse :</strong> {{ order.shippingAddress }}<br>
                    <strong>Paiement :</strong> {{ order.paymentMethod }}
                  </div>

                  <div class="detail-items-block">
                    <strong>Articles commandés :</strong>
                    <ul class="order-items-list">
                      <li *ngFor="let item of order.items">
                        {{ item.quantity }}x {{ item.product.name }} &bull; {{ (item.unitPrice * item.quantity) | number:'1.2-2' }} €
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </ng-container>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1280px;
      margin: 0 auto;
      padding: 0 20px;
    }

    .cart-header {
      background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0284c7 100%);
      color: white;
      padding: 45px 0 25px;
      margin-bottom: 35px;
      border-bottom: 3px solid #38bdf8;
    }
    .badge-group {
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
    .cart-title {
      color: white;
      font-size: 2.3rem;
      font-weight: 900;
      margin: 10px 0 8px;
    }
    .cart-subtitle {
      color: #e0f2fe;
      font-size: 1rem;
      max-width: 680px;
      line-height: 1.5;
      margin-bottom: 24px;
    }

    /* TABS */
    .cart-tabs-row {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }
    .tab-btn {
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.25);
      color: white;
      font-weight: 700;
      font-size: 0.9rem;
      padding: 10px 18px;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .tab-btn.active, .tab-btn:hover {
      background: white;
      color: #1e3a8a;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }

    .cart-body {
      padding-bottom: 70px;
    }

    .cart-layout {
      display: grid;
      grid-template-columns: 1fr 380px;
      gap: 30px;
      align-items: start;
    }

    .shipping-bar-card {
      padding: 16px 20px;
      margin-bottom: 20px;
      background: #eff6ff;
      border: 1px solid #bfdbfe;
    }
    .shipping-info {
      font-size: 0.88rem;
      color: #1e3a8a;
      margin-bottom: 8px;
    }
    .progress-track {
      background: #dbeafe;
      height: 8px;
      border-radius: 4px;
      overflow: hidden;
    }
    .progress-fill {
      background: #0284c7;
      height: 100%;
      transition: width 0.3s ease;
    }

    .items-card {
      padding: 0;
      overflow: hidden;
    }
    .cart-table-header {
      display: grid;
      grid-template-columns: 2fr 100px 100px 40px;
      padding: 14px 20px;
      background: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
      font-size: 0.8rem;
      font-weight: 700;
      color: #64748b;
      text-transform: uppercase;
    }
    .cart-row {
      display: grid;
      grid-template-columns: 2fr 100px 100px 40px;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid #f1f5f9;
    }
    .item-product {
      display: flex;
      gap: 14px;
      align-items: center;
    }
    .item-thumb {
      width: 60px;
      height: 60px;
      object-fit: cover;
      border-radius: 8px;
    }
    .item-cat {
      font-size: 0.7rem;
    }
    .item-name {
      font-size: 0.95rem;
      font-weight: 700;
      margin: 4px 0 2px;
      color: #0f172a;
    }
    .item-price-unit {
      font-size: 0.8rem;
      color: #64748b;
    }
    .item-quantity {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }
    .qty-btn {
      width: 28px;
      height: 28px;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      font-weight: 800;
      cursor: pointer;
    }
    .qty-val {
      font-weight: 700;
      font-size: 0.9rem;
      min-width: 20px;
      text-align: center;
    }
    .item-total {
      font-size: 1rem;
      color: #1e3a8a;
    }
    .btn-delete {
      background: none;
      border: none;
      color: #dc2626;
      font-size: 1rem;
      cursor: pointer;
    }

    /* SUMMARY */
    .summary-card {
      padding: 24px;
    }
    .summary-title {
      font-size: 1.2rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 18px;
    }
    .summary-rows {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-bottom: 20px;
      border-bottom: 1px solid #f1f5f9;
      padding-bottom: 16px;
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      font-size: 0.9rem;
      color: #475569;
    }
    .total-row {
      font-size: 1.15rem;
      font-weight: 800;
      color: #0f172a;
      border-top: 1px dashed #e2e8f0;
      padding-top: 10px;
      margin-top: 4px;
    }
    .total-amount {
      color: #1e3a8a;
    }

    .form-section {
      margin-bottom: 20px;
    }
    .form-section-title {
      font-size: 0.92rem;
      font-weight: 800;
      color: #1e3a8a;
      margin: 0 0 10px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .payment-options {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .payment-radio {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 14px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 600;
      color: #334155;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .payment-radio.selected, .payment-radio:hover {
      background: #eff6ff;
      border-color: #38bdf8;
      color: #1d4ed8;
    }

    /* CONFIRMATION */
    .confirmation-card {
      text-align: center;
      padding: 50px 30px;
      max-width: 600px;
      margin: 0 auto;
    }
    .confirm-icon {
      font-size: 3.5rem;
      margin-bottom: 12px;
    }
    .confirm-title {
      font-size: 1.6rem;
      font-weight: 900;
      color: #0f172a;
      margin: 12px 0 8px;
    }
    .confirm-num {
      font-size: 1.1rem;
      color: #1e3a8a;
    }
    .confirm-desc {
      color: #475569;
      line-height: 1.5;
      margin-bottom: 25px;
    }
    .confirm-actions {
      display: flex;
      justify-content: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    /* HISTORIQUE */
    .history-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    .history-header h2 {
      font-size: 1.5rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0;
    }
    .orders-list {
      display: flex;
      flex-direction: column;
      gap: 18px;
    }
    .order-card {
      padding: 22px;
    }
    .order-header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 14px;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 14px;
      flex-wrap: wrap;
      gap: 10px;
    }
    .order-ref {
      font-weight: 800;
      color: #1e3a8a;
      font-size: 1.05rem;
      margin-right: 12px;
    }
    .order-date {
      color: #64748b;
      font-size: 0.85rem;
    }
    .order-status-box {
      display: flex;
      gap: 12px;
      align-items: center;
    }
    .status-badge {
      font-size: 0.78rem;
      font-weight: 800;
      padding: 4px 10px;
      border-radius: 20px;
      text-transform: uppercase;
    }
    .status-delivered { background: #dcfce7; color: #166534; }
    .status-shipped { background: #e0f2fe; color: #0284c7; }
    .status-pending { background: #fef9c3; color: #854d0e; }
    .status-confirmed { background: #ede9fe; color: #5b21b6; }
    .order-total-badge {
      font-weight: 900;
      font-size: 1.15rem;
      color: #0f172a;
    }
    .order-details-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      font-size: 0.85rem;
      color: #475569;
      line-height: 1.5;
    }
    .order-items-list {
      margin: 6px 0 0;
      padding-left: 18px;
    }

    .empty-cart-card {
      text-align: center;
      padding: 60px 20px;
      max-width: 550px;
      margin: 0 auto;
    }
    .empty-icon {
      font-size: 3.5rem;
      margin-bottom: 12px;
    }

    @media (max-width: 900px) {
      .cart-layout, .order-details-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class CartComponent implements OnInit {
  activeTab: 'cart' | 'history' = 'cart';

  customerName: string = 'Karim Benzema';
  customerEmail: string = 'user@sportvision.com';
  customerPhone: string = '+33 6 98 76 54 32';
  shippingAddress: string = '12 Avenue des Champs-Élysées, 75008 Paris';
  paymentMethod: string = 'CARTE_BANCAIRE';

  isSubmitting: boolean = false;
  confirmedOrder: Order | null = null;
  pastOrders: Order[] = [];

  constructor(
    public cartService: CartService,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    this.orderService.getOrders().subscribe(orders => {
      this.pastOrders = orders;
    });
  }

  getProgressPercentage(): number {
    return Math.min(100, (this.cartService.subtotal / 80) * 100);
  }

  increment(item: CartItem): void {
    this.cartService.updateQuantity(item.product.id, item.quantity + 1);
  }

  decrement(item: CartItem): void {
    if (item.quantity > 1) {
      this.cartService.updateQuantity(item.product.id, item.quantity - 1);
    }
  }

  removeItem(item: CartItem): void {
    this.cartService.removeItem(item.product.id);
  }

  checkout(): void {
    if (this.cartService.items.length === 0) return;
    this.isSubmitting = true;

    const request = {
      customerName: this.customerName,
      customerEmail: this.customerEmail,
      customerPhone: this.customerPhone,
      shippingAddress: this.shippingAddress,
      paymentMethod: this.paymentMethod,
      items: this.cartService.items.map(i => ({ productId: i.product.id, quantity: i.quantity }))
    };

    this.orderService.createOrder(request, this.cartService.items).subscribe({
      next: (order) => {
        this.confirmedOrder = order;
        this.pastOrders.unshift(order);
        this.cartService.clearCart();
        this.isSubmitting = false;
      },
      error: () => {
        this.isSubmitting = false;
      }
    });
  }

  resetOrderView(): void {
    this.confirmedOrder = null;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'DELIVERED': return 'status-delivered';
      case 'SHIPPED': return 'status-shipped';
      case 'PENDING': return 'status-pending';
      default: return 'status-confirmed';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'DELIVERED': return 'Livrée';
      case 'SHIPPED': return 'Expédiée';
      case 'PENDING': return 'En attente';
      case 'CONFIRMED': return 'Confirmée';
      default: return status;
    }
  }
}
