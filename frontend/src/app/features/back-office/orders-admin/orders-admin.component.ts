import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-orders-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="admin-orders-page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Commandes & Ventes Athlètes</h1>
          <p class="page-subtitle">Suivez les expéditions, gérez les statuts et consultez le détail des commandes.</p>
        </div>
      </div>

      <!-- Filtres de Statut -->
      <div class="status-tabs-row">
        <button (click)="filterStatus('ALL')" class="status-tab" [class.active]="selectedStatus === 'ALL'">
          Toutes ({{ orders.length }})
        </button>
        <button (click)="filterStatus('PENDING')" class="status-tab" [class.active]="selectedStatus === 'PENDING'">
          En attente
        </button>
        <button (click)="filterStatus('CONFIRMED')" class="status-tab" [class.active]="selectedStatus === 'CONFIRMED'">
          Confirmées
        </button>
        <button (click)="filterStatus('SHIPPED')" class="status-tab" [class.active]="selectedStatus === 'SHIPPED'">
          Expédiées
        </button>
        <button (click)="filterStatus('DELIVERED')" class="status-tab" [class.active]="selectedStatus === 'DELIVERED'">
          Livrées
        </button>
      </div>

      <!-- Table des commandes -->
      <div class="sv-table-container">
        <table class="sv-table">
          <thead>
            <tr>
              <th>Réf. Commande</th>
              <th>Athlète / Client</th>
              <th>Articles</th>
              <th>Montant Total</th>
              <th>Moyen Paiement</th>
              <th>Statut Actuel</th>
              <th class="text-right">Détails</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let order of filteredOrders">
              <td>
                <strong class="order-ref">{{ order.orderNumber }}</strong>
                <div class="order-date">{{ order.createdAt | date:'short' }}</div>
              </td>
              <td>
                <div class="cust-name">{{ order.customerName }}</div>
                <div class="cust-email">{{ order.customerEmail }}</div>
              </td>
              <td>
                <span class="badge-sv badge-blue">{{ order.items.length }} article(s)</span>
              </td>
              <td>
                <strong class="order-amount">{{ order.totalAmount | number:'1.2-2' }} €</strong>
              </td>
              <td>
                <span class="payment-tag">{{ order.paymentMethod }}</span>
              </td>
              <td>
                <select [ngModel]="order.status" (ngModelChange)="updateStatus(order.id, $event)" class="status-dropdown" [ngClass]="'status-' + order.status.toLowerCase()">
                  <option value="PENDING">En attente</option>
                  <option value="CONFIRMED">Confirmée</option>
                  <option value="SHIPPED">Expédiée</option>
                  <option value="DELIVERED">Livrée</option>
                  <option value="CANCELLED">Annulée</option>
                </select>
              </td>
              <td class="text-right">
                <button (click)="openDetail(order)" class="btn-sv btn-sv-outline btn-sv-sm">
                  Voir Fiche &rarr;
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- MODALE DETAIL COMMANDE -->
      <div class="sv-modal-backdrop" *ngIf="selectedOrder" (click)="selectedOrder = null">
        <div class="sv-modal-dialog order-modal" (click)="$event.stopPropagation()">
          <div class="sv-modal-header">
            <div>
              <h3>Commande {{ selectedOrder.orderNumber }}</h3>
              <span class="modal-date">Passée le {{ selectedOrder.createdAt | date:'medium' }}</span>
            </div>
            <button (click)="selectedOrder = null" class="btn-sv btn-sv-outline btn-sv-sm">✕</button>
          </div>

          <div class="sv-modal-body">
            <!-- Client & Livraison -->
            <div class="client-info-box">
              <h4>Informations Client & Expédition</h4>
              <p><strong>Destinataire :</strong> {{ selectedOrder.customerName }}</p>
              <p><strong>Email :</strong> {{ selectedOrder.customerEmail }}</p>
              <p><strong>Téléphone :</strong> {{ selectedOrder.customerPhone || 'Non renseigné' }}</p>
              <p><strong>Adresse de livraison :</strong> {{ selectedOrder.shippingAddress }}</p>
            </div>

            <!-- Liste des articles -->
            <div class="order-items-detail">
              <h4>Articles commandés</h4>
              <div *ngFor="let item of selectedOrder.items" class="order-item-row">
                <img [src]="item.product.imageUrl" [alt]="item.product.name" class="item-img">
                <div class="item-info">
                  <strong>{{ item.product.name }}</strong>
                  <span class="item-price">{{ item.unitPrice | number:'1.2-2' }} € &times; {{ item.quantity }}</span>
                </div>
                <div class="item-line-total">
                  {{ (item.unitPrice * item.quantity) | number:'1.2-2' }} €
                </div>
              </div>
            </div>

            <!-- Total -->
            <div class="order-modal-total">
              <span>Total Réglé :</span>
              <strong class="total-highlight">{{ selectedOrder.totalAmount | number:'1.2-2' }} €</strong>
            </div>
          </div>

          <div class="sv-modal-footer">
            <button (click)="selectedOrder = null" class="btn-sv btn-sv-primary">Fermer la fiche</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-orders-page {
      max-width: 1350px;
      margin: 0 auto;
    }
    .page-header {
      margin-bottom: 24px;
    }
    .page-title {
      font-size: 1.85rem;
      color: #0f172a;
    }
    .page-subtitle {
      font-size: 0.9rem;
      color: #64748b;
      margin-top: 4px;
    }

    .status-tabs-row {
      display: flex;
      gap: 10px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }
    .status-tab {
      padding: 8px 16px;
      border-radius: 8px;
      border: 1px solid #cbd5e1;
      background: white;
      font-size: 0.88rem;
      font-weight: 600;
      color: #475569;
      cursor: pointer;
      transition: all 0.2s;
    }
    .status-tab:hover, .status-tab.active {
      background: #1d4ed8;
      color: white;
      border-color: #1d4ed8;
    }

    .order-ref {
      color: #1d4ed8;
      font-size: 0.92rem;
    }
    .order-date {
      font-size: 0.76rem;
      color: #94a3b8;
    }
    .cust-name {
      font-weight: 600;
      color: #0f172a;
    }
    .cust-email {
      font-size: 0.78rem;
      color: #64748b;
    }
    .order-amount {
      font-family: 'Outfit', sans-serif;
      font-size: 1.05rem;
      color: #0f172a;
    }
    .payment-tag {
      font-size: 0.75rem;
      background: #f1f5f9;
      padding: 3px 8px;
      border-radius: 6px;
    }

    .status-dropdown {
      padding: 6px 10px;
      border-radius: 8px;
      font-size: 0.82rem;
      font-weight: 600;
      outline: none;
      border: 1px solid #cbd5e1;
    }
    .status-confirmed { background: #eff6ff; color: #1d4ed8; }
    .status-shipped { background: #e0f2fe; color: #0284c7; }
    .status-delivered { background: #dcfce7; color: #15803d; }
    .status-pending { background: #fef3c7; color: #b45309; }
    .status-cancelled { background: #fee2e2; color: #dc2626; }

    /* Modal detail */
    .order-modal {
      max-width: 600px;
    }
    .client-info-box {
      background: #f8fafc;
      padding: 16px;
      border-radius: 10px;
      border: 1px solid #e2e8f0;
      margin-bottom: 20px;
      font-size: 0.88rem;
    }
    .client-info-box h4, .order-items-detail h4 {
      font-size: 0.95rem;
      margin-bottom: 10px;
      color: #1e3a8a;
    }
    .order-item-row {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 10px 0;
      border-bottom: 1px solid #f1f5f9;
    }
    .item-img {
      width: 45px;
      height: 45px;
      border-radius: 8px;
      object-fit: cover;
    }
    .item-info {
      flex: 1;
    }
    .item-info strong {
      display: block;
      font-size: 0.88rem;
    }
    .item-price {
      font-size: 0.78rem;
      color: #64748b;
    }
    .item-line-total {
      font-weight: 700;
    }
    .order-modal-total {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 18px;
      border-top: 2px solid #e2e8f0;
      margin-top: 18px;
    }
    .total-highlight {
      font-family: 'Outfit', sans-serif;
      font-size: 1.4rem;
      color: #1d4ed8;
    }
  `]
})
export class OrdersAdminComponent implements OnInit {
  orders: Order[] = [];
  filteredOrders: Order[] = [];
  selectedStatus: string = 'ALL';
  selectedOrder: Order | null = null;

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.orderService.getOrders().subscribe(orders => {
      this.orders = orders;
      this.filterStatus(this.selectedStatus);
    });
  }

  filterStatus(status: string): void {
    this.selectedStatus = status;
    if (status === 'ALL') {
      this.filteredOrders = this.orders;
    } else {
      this.filteredOrders = this.orders.filter(o => o.status === status);
    }
  }

  updateStatus(orderId: number, status: OrderStatus): void {
    this.orderService.updateStatus(orderId, status).subscribe();
  }

  openDetail(order: Order): void {
    this.selectedOrder = order;
  }
}
