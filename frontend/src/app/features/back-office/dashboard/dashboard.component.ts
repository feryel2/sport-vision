import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { StatsService } from '../../../core/services/stats.service';
import { OrderService } from '../../../core/services/order.service';
import { ProductService } from '../../../core/services/product.service';
import { AiService } from '../../../core/services/ai.service';
import { DashboardStats } from '../../../core/models/stats.model';
import { Order, OrderStatus } from '../../../core/models/order.model';
import { Product } from '../../../core/models/product.model';
import { SalesForecast } from '../../../core/models/ai.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="dashboard-container">
      <!-- En-tête Dashboard -->
      <div class="dash-header">
        <div>
          <div class="header-badge-row">
            <span class="badge-sv badge-blue">GESTION 3 &bull; BOUTIQUE & PRODUITS</span>
          </div>
          <h1 class="dash-title">Tableau de Bord &bull; Boutique Sport Vision</h1>
          <p class="dash-subtitle">Pilotage des stocks, des ventes et projections prédictives de la demande.</p>
        </div>
        <div class="dash-actions">
          <a routerLink="/admin/produits" class="btn-sv btn-sv-primary">
            <span>+ Nouvel Équipement</span>
          </a>
          <button (click)="refreshStats()" class="btn-sv btn-sv-outline" title="Actualiser">
            🔄 Actualiser
          </button>
        </div>
      </div>

      <!-- 4 KPI CARDS -->
      <div class="kpi-grid" *ngIf="stats">
        <!-- KPI 1 : CA -->
        <div class="sv-card kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Chiffre d'Affaires</span>
            <div class="kpi-icon-box bg-blue">💶</div>
          </div>
          <div class="kpi-val">{{ stats.totalRevenue | number:'1.2-2' }} €</div>
          <div class="kpi-trend positive">
            <span>↑ +14.2%</span>
            <span class="trend-sub">vs mois précédent</span>
          </div>
        </div>

        <!-- KPI 2 : Commandes -->
        <div class="sv-card kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Commandes Totales</span>
            <div class="kpi-icon-box bg-cyan">🛒</div>
          </div>
          <div class="kpi-val">{{ stats.totalOrders }}</div>
          <div class="kpi-trend positive">
            <span>↑ +8.5%</span>
            <span class="trend-sub">{{ stats.pendingOrdersCount }} à préparer</span>
          </div>
        </div>

        <!-- KPI 3 : Équipements & Stocks -->
        <div class="sv-card kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Catalogue Équipements</span>
            <div class="kpi-icon-box bg-purple">📦</div>
          </div>
          <div class="kpi-val">{{ stats.totalProducts }} articles</div>
          <div class="kpi-trend" [class.negative]="stats.lowStockCount > 0">
            <span *ngIf="stats.lowStockCount > 0">⚠️ {{ stats.lowStockCount }} stocks critiques</span>
            <span *ngIf="stats.lowStockCount === 0" class="positive">✓ Stocks équilibrés</span>
          </div>
        </div>

        <!-- KPI 4 : Athlètes Inscrits -->
        <div class="sv-card kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Athlètes & Clients</span>
            <div class="kpi-icon-box bg-emerald">👥</div>
          </div>
          <div class="kpi-val">{{ stats.totalCustomers }}</div>
          <div class="kpi-trend positive">
            <span>+3 cette semaine</span>
          </div>
        </div>
      </div>

      <!-- SECTION PRÉVISION DES VENTES & GESTION DES STOCKS -->
      <div class="sv-card ai-forecast-card">
        <div class="ai-forecast-header">
          <div>
            <div class="ai-tag">
              <span>ANALYSE PRÉDICTIVE & GESTION DES STOCKS</span>
            </div>
            <h2 class="ai-forecast-title">Prévision des Ventes pour Mieux Gérer le Stock</h2>
            <p class="ai-forecast-desc">
              Projection de la demande à 30 jours, détection des risques d'épuisement et préconisation des volumes de réapprovisionnement optimaux.
            </p>
          </div>
          <button (click)="loadForecast()" class="btn-sv btn-sv-outline btn-sv-sm">
            Actualiser les Prévisions
          </button>
        </div>

        <div class="forecast-table-container">
          <table class="sv-table">
            <thead>
              <tr>
                <th>Produit & Rayon</th>
                <th class="text-center">Stock Actuel</th>
                <th class="text-center">Ventes Prévues (30j)</th>
                <th class="text-center">Réappro Recommandé</th>
                <th>Diagnostic Stock</th>
                <th>Confiance</th>
                <th>Tendance</th>
                <th class="text-right">Action Recommandée</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of salesForecasts">
                <td>
                  <strong class="forecast-pname">{{ item.productName }}</strong>
                  <span class="forecast-pcat">{{ item.categoryName }}</span>
                </td>
                <td class="text-center font-bold" [class.text-danger]="item.currentStock <= 5">
                  {{ item.currentStock }}
                </td>
                <td class="text-center font-bold text-blue">
                  {{ item.predictedSalesNext30Days }} unités
                </td>
                <td class="text-center font-bold" [class.text-danger]="item.recommendedReorderQuantity > 0">
                  {{ item.recommendedReorderQuantity > 0 ? '+' + item.recommendedReorderQuantity + ' unités' : '0 (OK)' }}
                </td>
                <td>
                  <span class="badge-status" [ngClass]="getHealthBadgeClass(item.stockHealthStatus)">
                    {{ getHealthLabel(item.stockHealthStatus) }}
                  </span>
                  <div class="forecast-reason">{{ item.reason }}</div>
                </td>
                <td>
                  <span class="badge-sv badge-blue">{{ (item.confidenceScore * 100) | number:'1.0-0' }}%</span>
                </td>
                <td>
                  <span class="trend-badge" [class.trend-up]="item.trend === 'HAUSSE'">
                    {{ item.trend === 'HAUSSE' ? '↑ Forte' : (item.trend === 'STABLE' ? '→ Stable' : '↓ Modérée') }}
                  </span>
                </td>
                <td class="text-right">
                  <button *ngIf="item.recommendedReorderQuantity > 0" (click)="quickReorder(item)" class="btn-sv btn-sv-primary btn-sv-sm" title="Valider le réapprovisionnement automatique">
                    +{{ item.recommendedReorderQuantity }} Réappro
                  </button>
                  <span *ngIf="item.recommendedReorderQuantity === 0" class="text-success font-bold">✓ Conforme</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- DERNIERES COMMANDES & ALERTES STOCKS -->
      <div class="tables-row">
        <!-- Dernières Commandes -->
        <div class="sv-card table-section">
          <div class="section-title-row">
            <h3>Dernières Commandes Clients</h3>
            <a routerLink="/admin/commandes" class="link-more">Voir toutes les commandes &rarr;</a>
          </div>

          <div class="sv-table-container">
            <table class="sv-table">
              <thead>
                <tr>
                  <th>N° Commande</th>
                  <th>Client</th>
                  <th>Total</th>
                  <th>Statut</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let order of recentOrders">
                  <td>
                    <strong>{{ order.orderNumber }}</strong>
                    <div class="order-date">{{ order.createdAt | slice:0:10 }}</div>
                  </td>
                  <td>{{ order.customerName }}</td>
                  <td><strong>{{ order.totalAmount | number:'1.2-2' }} €</strong></td>
                  <td>
                    <span class="badge-sv" [ngClass]="getStatusBadge(order.status)">
                      {{ getStatusLabel(order.status) }}
                    </span>
                  </td>
                  <td>
                    <select [ngModel]="order.status" (ngModelChange)="updateOrderStatus(order.id, $event)" class="status-select">
                      <option value="PENDING">En attente</option>
                      <option value="CONFIRMED">Confirmée</option>
                      <option value="SHIPPED">Expédiée</option>
                      <option value="DELIVERED">Livrée</option>
                      <option value="CANCELLED">Annulée</option>
                    </select>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Alertes Stocks Critiques -->
        <div class="sv-card table-section">
          <div class="section-title-row">
            <h3>⚠️ Alertes Stocks Critiques (&le; 5)</h3>
            <span class="badge-sv badge-warning">{{ lowStockProducts.length }} alertes</span>
          </div>

          <div class="stock-alerts-list">
            <div *ngFor="let prod of lowStockProducts" class="alert-item">
              <img [src]="prod.imageUrl" [alt]="prod.name" class="alert-thumb">
              <div class="alert-info">
                <strong>{{ prod.name }}</strong>
                <span>{{ prod.category?.name }} &bull; {{ prod.price | number:'1.2-2' }} €</span>
              </div>
              <div class="alert-stock-badge">
                <span class="badge-sv badge-danger">{{ prod.stock }} restants</span>
                <button (click)="replenishStock(prod)" class="btn-sv btn-sv-primary btn-sv-sm" title="Réapprovisionner +15 unités">
                  +15 Réappro
                </button>
              </div>
            </div>
            <div *ngIf="lowStockProducts.length === 0" class="no-alerts">
              ✓ Tous les équipements ont un stock suffisant.
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      max-width: 1350px;
      margin: 0 auto;
    }
    .header-badge-row {
      display: flex;
      gap: 10px;
      margin-bottom: 8px;
    }
    .dash-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 28px;
      flex-wrap: wrap;
      gap: 16px;
    }
    .dash-title {
      font-size: 1.85rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0;
    }
    .dash-subtitle {
      font-size: 0.9rem;
      color: #64748b;
      margin-top: 4px;
    }
    .dash-actions {
      display: flex;
      gap: 10px;
    }

    /* KPI */
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 20px;
      margin-bottom: 30px;
    }
    .kpi-card {
      padding: 22px;
    }
    .kpi-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    .kpi-label {
      font-size: 0.85rem;
      color: #64748b;
      font-weight: 600;
    }
    .kpi-icon-box {
      width: 42px;
      height: 42px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.2rem;
    }
    .bg-blue { background: #dbeafe; color: #1e3a8a; }
    .bg-cyan { background: #e0f2fe; color: #0284c7; }
    .bg-purple { background: #ede9fe; color: #6d28d9; }
    .bg-emerald { background: #dcfce7; color: #166534; }
    .kpi-val {
      font-size: 1.85rem;
      font-weight: 900;
      color: #0f172a;
      margin-bottom: 8px;
    }
    .kpi-trend {
      font-size: 0.82rem;
      font-weight: 700;
      display: flex;
      gap: 6px;
      align-items: center;
    }
    .positive { color: #16a34a; }
    .negative { color: #dc2626; }
    .trend-sub {
      color: #94a3b8;
      font-weight: 500;
    }

    /* AI FORECAST */
    .ai-forecast-card {
      margin-bottom: 30px;
      border: 2px solid #38bdf8;
      padding: 24px;
      background: white;
    }
    .ai-forecast-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 20px;
      flex-wrap: wrap;
      gap: 15px;
    }
    .ai-tag {
      display: inline-block;
      background: #e0f2fe;
      color: #0284c7;
      font-size: 0.75rem;
      font-weight: 800;
      padding: 3px 10px;
      border-radius: 20px;
      margin-bottom: 6px;
    }
    .ai-forecast-title {
      font-size: 1.35rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 4px;
    }
    .ai-forecast-desc {
      font-size: 0.88rem;
      color: #64748b;
      margin: 0;
      max-width: 700px;
    }
    .forecast-table-container {
      overflow-x: auto;
    }
    .forecast-pname {
      display: block;
      color: #0f172a;
      font-size: 0.92rem;
    }
    .forecast-pcat {
      font-size: 0.75rem;
      color: #64748b;
    }
    .forecast-reason {
      font-size: 0.78rem;
      color: #64748b;
      margin-top: 3px;
      max-width: 280px;
      line-height: 1.35;
    }

    .badge-status {
      font-size: 0.75rem;
      font-weight: 800;
      padding: 3px 8px;
      border-radius: 6px;
      display: inline-block;
    }
    .status-critique { background: #fee2e2; color: #dc2626; border: 1px solid #fca5a5; }
    .status-risque { background: #fef3c7; color: #d97706; border: 1px solid #fde68a; }
    .status-optimal { background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; }
    .status-surstock { background: #e0f2fe; color: #0284c7; border: 1px solid #bae6fd; }

    .trend-badge {
      font-size: 0.82rem;
      font-weight: 700;
      color: #64748b;
    }
    .trend-up {
      color: #16a34a;
    }

    /* TABLES ROW */
    .tables-row {
      display: grid;
      grid-template-columns: 1.2fr 0.8fr;
      gap: 25px;
    }
    .table-section {
      padding: 20px;
    }
    .section-title-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .section-title-row h3 {
      font-size: 1.15rem;
      font-weight: 800;
      margin: 0;
      color: #0f172a;
    }
    .link-more {
      font-size: 0.85rem;
      color: #1d4ed8;
      font-weight: 700;
      text-decoration: none;
    }
    .order-date {
      font-size: 0.75rem;
      color: #94a3b8;
    }
    .status-select {
      padding: 4px 6px;
      border-radius: 6px;
      border: 1px solid #cbd5e1;
      font-size: 0.8rem;
    }

    .stock-alerts-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .alert-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      background: #f8fafc;
      border-radius: 10px;
      border: 1px solid #e2e8f0;
    }
    .alert-thumb {
      width: 44px;
      height: 44px;
      border-radius: 8px;
      object-fit: cover;
    }
    .alert-info {
      flex-grow: 1;
    }
    .alert-info strong {
      display: block;
      font-size: 0.88rem;
      color: #0f172a;
    }
    .alert-info span {
      font-size: 0.78rem;
      color: #64748b;
    }
    .alert-stock-badge {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 4px;
    }
    .no-alerts {
      color: #16a34a;
      font-size: 0.9rem;
      font-weight: 600;
      padding: 20px 0;
      text-align: center;
    }

    @media (max-width: 1100px) {
      .kpi-grid { grid-template-columns: 1fr 1fr; }
      .tables-row { grid-template-columns: 1fr; }
    }
    @media (max-width: 600px) {
      .kpi-grid { grid-template-columns: 1fr; }
    }
  `]
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  recentOrders: Order[] = [];
  lowStockProducts: Product[] = [];
  salesForecasts: SalesForecast[] = [];

  constructor(
    private statsService: StatsService,
    private orderService: OrderService,
    private productService: ProductService,
    private aiService: AiService
  ) {}

  ngOnInit(): void {
    this.refreshStats();
    this.loadForecast();
  }

  refreshStats(): void {
    this.statsService.getDashboardStats().subscribe(stats => {
      this.stats = stats;
    });

    this.orderService.getOrders().subscribe(orders => {
      this.recentOrders = orders.slice(0, 5);
    });

    this.productService.products$.subscribe(products => {
      this.lowStockProducts = products.filter(p => p.stock <= 5);
    });
  }

  loadForecast(): void {
    this.aiService.getSalesForecast().subscribe(forecast => {
      this.salesForecasts = forecast;
    });
  }

  quickReorder(item: SalesForecast): void {
    this.productService.adjustStock(item.productId, item.recommendedReorderQuantity).subscribe(p => {
      item.currentStock = p.stock;
      item.recommendedReorderQuantity = 0;
      item.stockHealthStatus = 'OPTIMAL';
      item.reason = 'Réapprovisionnement validé ! Stock suffisant pour couvrir les 30 prochains jours.';
      this.refreshStats();
    });
  }

  getHealthBadgeClass(status: string): string {
    switch (status) {
      case 'CRITIQUE': return 'status-critique';
      case 'RISQUE_RUPTURE': return 'status-risque';
      case 'SURSTOCK': return 'status-surstock';
      default: return 'status-optimal';
    }
  }

  getHealthLabel(status: string): string {
    switch (status) {
      case 'CRITIQUE': return '🚨 Rupture Critique';
      case 'RISQUE_RUPTURE': return '⚠️ Risque de Rupture';
      case 'SURSTOCK': return '📦 Surstock';
      default: return '✓ Stock Optimal';
    }
  }

  getStatusBadge(status: OrderStatus): string {
    switch (status) {
      case 'DELIVERED': return 'badge-success';
      case 'SHIPPED': return 'badge-blue';
      case 'CONFIRMED': return 'badge-blue';
      case 'PENDING': return 'badge-warning';
      case 'CANCELLED': return 'badge-danger';
      default: return 'badge-blue';
    }
  }

  getStatusLabel(status: OrderStatus): string {
    switch (status) {
      case 'DELIVERED': return 'Livrée';
      case 'SHIPPED': return 'Expédiée';
      case 'CONFIRMED': return 'Confirmée';
      case 'PENDING': return 'En attente';
      case 'CANCELLED': return 'Annulée';
      default: return status;
    }
  }

  updateOrderStatus(orderId: number, status: any): void {
    this.orderService.updateStatus(orderId, status as OrderStatus).subscribe(() => {
      this.refreshStats();
    });
  }

  replenishStock(product: Product): void {
    this.productService.updateStock(product.id, product.stock + 15).subscribe(() => {
      this.refreshStats();
      this.loadForecast();
    });
  }
}
