import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { BrandService } from '../../../core/services/brand.service';
import { SupplierService } from '../../../core/services/supplier.service';
import { Product, ProductFormData } from '../../../core/models/product.model';
import { Category } from '../../../core/models/category.model';
import { Brand } from '../../../core/models/brand.model';
import { Supplier } from '../../../core/models/supplier.model';
import { ImageUploadService } from '../../../core/services/image-upload.service';

@Component({
  selector: 'app-products-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="admin-products-page">
      <!-- En-tête -->
      <div class="page-header">
        <div>
          <div class="header-badge-row">
            <span class="badge-sv badge-blue">GESTION 3</span>
          </div>
          <h1 class="page-title">Boutique & Produits Sportifs</h1>
          <p class="page-subtitle">
            Consultez, ajoutez, modifiez ou supprimez vos équipements. Modifiez les prix directement et surveillez les niveaux de stock en temps réel.
          </p>
        </div>
        <button (click)="openAddModal()" class="btn-sv btn-sv-primary">
          <span>+ Ajouter un Produit</span>
        </button>
      </div>

      <!-- Filtres et Recherche -->
      <div class="sv-card filter-toolbar">
        <div class="search-input-wrap">
          <input type="text" [(ngModel)]="searchQuery" (input)="filterProducts()" placeholder="Rechercher par nom, description..." class="form-control-sv">
        </div>
        <div class="cat-select-wrap">
          <select [(ngModel)]="selectedCategory" (change)="filterProducts()" class="form-control-sv">
            <option [value]="null">Toutes les catégories</option>
            <option *ngFor="let cat of categories" [value]="cat.id">{{ cat.name }}</option>
          </select>
        </div>
        <div class="cat-select-wrap">
          <select [(ngModel)]="selectedGoal" (change)="filterProducts()" class="form-control-sv">
            <option value="Tous">Tous les objectifs sportifs</option>
            <option value="Musculation">💪 Musculation</option>
            <option value="Running">🏃 Running & Trail</option>
            <option value="Fitness">🧘 Fitness</option>
          </select>
        </div>
        <div class="stock-filter-wrap">
          <label class="checkbox-label">
            <input type="checkbox" [(ngModel)]="onlyLowStock" (change)="filterProducts()">
            <span class="badge-alert-stock">🚨 Alertes Stock Bas (&le; 5)</span>
          </label>
        </div>
      </div>

      <!-- TABLEAU DES PRODUITS -->
      <div class="sv-table-container sv-card">
        <table class="sv-table">
          <thead>
            <tr>
              <th>Visuel</th>
              <th>Produit & Objectif</th>
              <th>Catégorie</th>
              <th>Prix Unitaire (Modifiable)</th>
              <th>Gestion du Stock</th>
              <th>Note</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let prod of filteredProducts">
              <td>
                <img [src]="prod.imageUrl" [alt]="prod.name" class="table-thumb">
              </td>
              <td>
                <strong class="prod-name">{{ prod.name }}</strong>
                <div class="prod-submeta">
                  <span class="badge-sv badge-blue" *ngIf="prod.sportGoal">🎯 {{ prod.sportGoal }}</span>
                  <span class="badge-sv badge-secondary" *ngIf="prod.featured">★ Vedette</span>
                </div>
              </td>
              <td>
                <span class="cat-pill">{{ prod.category?.name || 'Général' }}</span>
              </td>

              <!-- PRIX MODIFIABLE DIRECTEMENT -->
              <td>
                <div class="price-edit-cell" *ngIf="editingPriceId !== prod.id">
                  <strong class="prod-price">{{ prod.price | number:'1.2-2' }} €</strong>
                  <button (click)="startEditPrice(prod)" class="btn-quick-edit-price" title="Modifier directement le prix">
                    ✏️
                  </button>
                </div>

                <div class="price-input-row" *ngIf="editingPriceId === prod.id">
                  <input type="number" step="0.01" [(ngModel)]="tempPrice" class="price-input" (keyup.enter)="savePrice(prod)">
                  <button (click)="savePrice(prod)" class="btn-save-price" title="Valider">✓</button>
                  <button (click)="cancelEditPrice()" class="btn-cancel-price" title="Annuler">✕</button>
                </div>
              </td>

              <!-- GESTION DU STOCK DIRECTE -->
              <td>
                <div class="stock-control">
                  <button (click)="decrementStock(prod)" class="btn-stock-sub" title="Diminuer stock">-</button>
                  <span class="badge-stock" [class.stock-danger]="prod.stock <= 5" [class.stock-ok]="prod.stock > 5">
                    {{ prod.stock }} {{ prod.stock <= 5 ? '⚠️ Alerte' : 'en stock' }}
                  </span>
                  <button (click)="incrementStock(prod)" class="btn-stock-add" title="Augmenter stock">+</button>
                </div>
              </td>

              <td>
                <span class="rating-stars">⭐ {{ prod.rating }} ({{ prod.reviewsCount }})</span>
              </td>

              <td class="text-right">
                <div class="table-actions">
                  <button (click)="openEditModal(prod)" class="btn-action edit" title="Modifier le produit">✏️</button>
                  <button (click)="openDeleteConfirm(prod)" class="btn-action delete" title="Supprimer le produit">🗑️</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="empty-table" *ngIf="filteredProducts.length === 0">
          <p>Aucun équipement ne correspond aux critères de filtre.</p>
        </div>
      </div>

      <!-- MODALE AJOUT / ÉDITION DE PRODUIT -->
      <div class="sv-modal-backdrop" *ngIf="showModal" (click)="closeModal()">
        <div class="sv-modal-dialog" (click)="$event.stopPropagation()">
          <div class="sv-modal-header">
            <h3>{{ isEditing ? 'Modifier l\'équipement' : 'Ajouter un nouvel équipement sportif' }}</h3>
            <button (click)="closeModal()" class="btn-close-modal">✕</button>
          </div>

          <form (ngSubmit)="saveProduct()">
            <div class="sv-modal-body">
              <div class="form-group-sv">
                <label class="form-label-sv">Nom de l'équipement *</label>
                <input type="text" [(ngModel)]="formData.name" name="name" required class="form-control-sv" placeholder="Ex: Bandes élastiques de résistance">
              </div>

              <div class="form-row-2">
                <div class="form-group-sv">
                  <label class="form-label-sv">Catégorie *</label>
                  <select [(ngModel)]="formData.categoryId" name="categoryId" required class="form-control-sv">
                    <option *ngFor="let cat of categories" [value]="cat.id">{{ cat.name }}</option>
                  </select>
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Programme / Discipline sportive</label>
                  <select [(ngModel)]="formData.sportGoal" name="sportGoal" class="form-control-sv">
                    <option value="Musculation">💪 Musculation & Force</option>
                    <option value="Running">🏃 Running & Trail</option>
                    <option value="Fitness">🧘 Fitness & Perte de poids</option>
                    <option value="Cyclisme">🚴 Cyclisme sur route</option>
                  </select>
                </div>
              </div>

              <div class="form-row-2">
                <div class="form-group-sv">
                  <label class="form-label-sv">Marque (Brand)</label>
                  <select [(ngModel)]="formData.brandId" name="brandId" class="form-control-sv">
                    <option [value]="null">Aucune marque</option>
                    <option *ngFor="let b of brands" [value]="b.id">{{ b.name }} ({{ b.country }})</option>
                  </select>
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Fournisseur (Supplier)</label>
                  <select [(ngModel)]="formData.supplierId" name="supplierId" class="form-control-sv">
                    <option [value]="null">Aucun fournisseur</option>
                    <option *ngFor="let s of suppliers" [value]="s.id">{{ s.name }}</option>
                  </select>
                </div>
              </div>

              <div class="form-row-2">
                <div class="form-group-sv">
                  <label class="form-label-sv">Prix unitaire (€) *</label>
                  <input type="number" step="0.01" [(ngModel)]="formData.price" name="price" required class="form-control-sv">
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Quantité en stock *</label>
                  <input type="number" [(ngModel)]="formData.stock" name="stock" required class="form-control-sv">
                </div>
              </div>

              <div class="form-group-sv">
                <label class="form-label-sv">Description technique *</label>
                <textarea [(ngModel)]="formData.description" name="description" rows="3" required class="form-control-sv" placeholder="Caractéristiques techniques, matériaux, conseils d'utilisation..."></textarea>
              </div>

              <div class="form-group-sv">
                <label class="form-label-sv">Image du produit</label>
                <input type="file" accept="image/*" name="productImage" class="form-control-sv" (change)="onImageSelected($event)">
                <small *ngIf="imageError" class="image-upload-error">{{ imageError }}</small>
                <div *ngIf="formData.imageUrl" class="image-upload-preview">
                  <img [src]="formData.imageUrl" [alt]="formData.name || 'Aperçu du produit'">
                  <span>Aperçu de l’image sélectionnée</span>
                </div>
              </div>

              <div class="form-group-sv">
                <label class="form-label-sv">Spécifications techniques</label>
                <input type="text" [(ngModel)]="formData.specifications" name="specifications" class="form-control-sv" placeholder="Ex: Résistance 5-30kg • Latex naturel">
              </div>

              <div class="checkbox-option">
                <label>
                  <input type="checkbox" [(ngModel)]="formData.featured" name="featured">
                  <span>Mettre en avant sur la boutique (Best-Seller)</span>
                </label>
              </div>
            </div>

            <div class="sv-modal-footer">
              <button type="button" (click)="closeModal()" class="btn-sv btn-sv-outline">Annuler</button>
              <button type="submit" class="btn-sv btn-sv-primary">
                {{ isEditing ? 'Enregistrer les modifications' : 'Créer le produit' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- MODALE CONFIRMATION DE SUPPRESSION -->
      <div class="sv-modal-backdrop" *ngIf="productToDelete" (click)="productToDelete = null">
        <div class="sv-modal-dialog delete-dialog" (click)="$event.stopPropagation()">
          <div class="sv-modal-header">
            <h3>Confirmer la suppression</h3>
            <button (click)="productToDelete = null" class="btn-close-modal">✕</button>
          </div>
          <div class="sv-modal-body">
            <p>Êtes-vous sûr de vouloir supprimer définitivement l'équipement <strong>{{ productToDelete.name }}</strong> ?</p>
          </div>
          <div class="sv-modal-footer">
            <button (click)="productToDelete = null" class="btn-sv btn-sv-outline">Annuler</button>
            <button (click)="confirmDelete()" class="btn-sv btn-sv-danger">Supprimer définitivement</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .image-upload-preview { display: flex; align-items: center; gap: 12px; margin-top: 10px; color: #64748b; font-size: 0.82rem; }
    .image-upload-preview img { width: 76px; height: 64px; object-fit: cover; border-radius: 8px; border: 1px solid #e2e8f0; }
    .image-upload-error { display: block; color: #dc2626; margin-top: 6px; }
    .admin-products-page {
      max-width: 1350px;
      margin: 0 auto;
    }
    .header-badge-row {
      display: flex;
      gap: 10px;
      margin-bottom: 8px;
    }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 16px;
    }
    .page-title {
      font-size: 1.85rem;
      color: #0f172a;
      font-weight: 800;
      margin: 0;
    }
    .page-subtitle {
      font-size: 0.9rem;
      color: #64748b;
      margin-top: 4px;
    }

    /* Barre filtres */
    .filter-toolbar {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px 20px;
      margin-bottom: 24px;
      flex-wrap: wrap;
    }
    .search-input-wrap {
      flex: 1;
      min-width: 220px;
    }
    .cat-select-wrap {
      width: 200px;
    }
    .stock-filter-wrap {
      display: flex;
      align-items: center;
    }
    .badge-alert-stock {
      color: #dc2626;
      font-weight: 700;
      font-size: 0.85rem;
    }

    .sv-table-container {
      padding: 0;
      overflow: hidden;
    }
    .sv-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9rem;
    }
    .sv-table th {
      background: #f8fafc;
      padding: 14px 18px;
      text-align: left;
      font-weight: 700;
      color: #475569;
      border-bottom: 1px solid #e2e8f0;
      font-size: 0.82rem;
      text-transform: uppercase;
    }
    .sv-table td {
      padding: 14px 18px;
      border-bottom: 1px solid #f1f5f9;
      color: #334155;
    }

    .table-thumb {
      width: 52px;
      height: 52px;
      border-radius: 8px;
      object-fit: cover;
      border: 1px solid #e2e8f0;
    }
    .prod-name {
      font-size: 0.95rem;
      color: #0f172a;
      display: block;
      margin-bottom: 4px;
    }
    .prod-submeta {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
    .cat-pill {
      background: #f1f5f9;
      padding: 3px 8px;
      border-radius: 6px;
      font-size: 0.8rem;
      font-weight: 600;
      color: #475569;
    }

    /* PRIX MODIFIABLE */
    .price-edit-cell {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .prod-price {
      font-size: 1.05rem;
      font-weight: 800;
      color: #1e3a8a;
    }
    .btn-quick-edit-price {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 0.85rem;
      opacity: 0.6;
      transition: opacity 0.15s;
    }
    .btn-quick-edit-price:hover {
      opacity: 1;
    }
    .price-input-row {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .price-input {
      width: 75px;
      padding: 4px 6px;
      border: 1px solid #0284c7;
      border-radius: 4px;
      font-weight: 700;
    }
    .btn-save-price {
      background: #16a34a;
      color: white;
      border: none;
      border-radius: 4px;
      padding: 4px 8px;
      cursor: pointer;
    }
    .btn-cancel-price {
      background: #94a3b8;
      color: white;
      border: none;
      border-radius: 4px;
      padding: 4px 8px;
      cursor: pointer;
    }

    /* GESTION STOCK */
    .stock-control {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .btn-stock-sub, .btn-stock-add {
      width: 26px;
      height: 26px;
      background: #f1f5f9;
      border: 1px solid #cbd5e1;
      border-radius: 4px;
      font-weight: 800;
      cursor: pointer;
    }
    .badge-stock {
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 0.8rem;
      font-weight: 700;
    }
    .stock-danger {
      background: #fee2e2;
      color: #dc2626;
      border: 1px solid #fca5a5;
    }
    .stock-ok {
      background: #dcfce7;
      color: #166534;
      border: 1px solid #bbf7d0;
    }

    .rating-stars {
      color: #eab308;
      font-weight: 700;
      font-size: 0.85rem;
    }

    .table-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
    .btn-action {
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      padding: 5px 8px;
      cursor: pointer;
    }
    .btn-action.edit:hover { background: #e0f2fe; }
    .btn-action.delete:hover { background: #fee2e2; }

    /* Modale */
    .sv-modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.65);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .sv-modal-dialog {
      background: white;
      border-radius: 16px;
      width: 90%;
      max-width: 650px;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
    }
    .sv-modal-header {
      padding: 20px 24px;
      border-bottom: 1px solid #e2e8f0;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .sv-modal-header h3 {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 800;
      color: #0f172a;
    }
    .btn-close-modal {
      background: none;
      border: none;
      font-size: 1.2rem;
      cursor: pointer;
    }
    .sv-modal-body {
      padding: 20px 24px;
    }
    .form-row-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
    }
    .sv-modal-footer {
      padding: 16px 24px;
      border-top: 1px solid #e2e8f0;
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      background: #f8fafc;
    }
  `]
})
export class ProductsAdminComponent implements OnInit {
  allProducts: Product[] = [];
  filteredProducts: Product[] = [];
  categories: Category[] = [];
  brands: Brand[] = [];
  suppliers: Supplier[] = [];

  searchQuery = '';
  selectedCategory: any = null;
  selectedGoal = 'Tous';
  onlyLowStock = false;

  // Direct price edit
  editingPriceId: number | null = null;
  tempPrice: number = 0;

  // Modal
  showModal = false;
  isEditing = false;
  currentProductId: number | null = null;
  formData: ProductFormData = this.getEmptyFormData();
  imageError = '';

  productToDelete: Product | null = null;

  constructor(
    private productService: ProductService,
    private brandService: BrandService,
    private supplierService: SupplierService,
    private imageUploadService: ImageUploadService
  ) {}

  ngOnInit(): void {
    this.productService.products$.subscribe(prods => {
      this.allProducts = prods;
      this.filterProducts();
    });

    this.productService.getCategories().subscribe(cats => {
      this.categories = cats;
    });

    this.brandService.getBrands().subscribe(b => {
      this.brands = b;
    });

    this.supplierService.getSuppliers().subscribe(s => {
      this.suppliers = s;
    });
  }

  getEmptyFormData(): ProductFormData {
    return {
      name: '',
      description: '',
      price: 29.99,
      stock: 20,
      categoryId: this.categories[0]?.id || 1,
      brandId: this.brands[0]?.id,
      supplierId: this.suppliers[0]?.id,
      sportGoal: 'Musculation',
      imageUrl: 'https://images.unsplash.com/photo-1598971639058-fab3c3109a00?auto=format&fit=crop&w=800&q=80',
      featured: false,
      specifications: 'Qualité certifiée Sport Vision'
    };
  }

  filterProducts(): void {
    let result = [...this.allProducts];

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q)
      );
    }

    if (this.selectedCategory) {
      result = result.filter(p => p.category && p.category.id === Number(this.selectedCategory));
    }

    if (this.selectedGoal !== 'Tous') {
      const g = this.selectedGoal.toLowerCase();
      result = result.filter(p => p.sportGoal && p.sportGoal.toLowerCase().includes(g));
    }

    if (this.onlyLowStock) {
      result = result.filter(p => p.stock <= 5);
    }

    this.filteredProducts = result;
  }

  // Direct price edit
  startEditPrice(product: Product): void {
    this.editingPriceId = product.id;
    this.tempPrice = product.price;
  }

  savePrice(product: Product): void {
    if (this.tempPrice <= 0) return;
    this.productService.updatePrice(product.id, this.tempPrice).subscribe(() => {
      product.price = this.tempPrice;
      this.editingPriceId = null;
    });
  }

  cancelEditPrice(): void {
    this.editingPriceId = null;
  }

  // Stock management
  incrementStock(product: Product): void {
    this.productService.adjustStock(product.id, 1).subscribe(p => {
      product.stock = p.stock;
    });
  }

  decrementStock(product: Product): void {
    if (product.stock > 0) {
      this.productService.adjustStock(product.id, -1).subscribe(p => {
        product.stock = p.stock;
      });
    }
  }

  openAddModal(): void {
    this.isEditing = false;
    this.currentProductId = null;
    this.formData = this.getEmptyFormData();
    this.showModal = true;
  }

  openEditModal(product: Product): void {
    this.isEditing = true;
    this.currentProductId = product.id;
    this.formData = {
      name: product.name,
      description: product.description,
      price: product.price,
      stock: product.stock,
      categoryId: product.category ? product.category.id : 1,
      brandId: product.brand?.id,
      supplierId: product.supplier?.id,
      sportGoal: product.sportGoal || 'Musculation',
      imageUrl: product.imageUrl,
      featured: product.featured,
      specifications: product.specifications || ''
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  async onImageSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.imageError = '';
    try {
      this.formData.imageUrl = await this.imageUploadService.convert(file);
    } catch (error) {
      this.imageError = error instanceof Error ? error.message : 'Impossible de charger cette image.';
    } finally {
      input.value = '';
    }
  }

  saveProduct(): void {
    if (this.isEditing && this.currentProductId) {
      this.productService.updateProduct(this.currentProductId, this.formData).subscribe(() => {
        this.closeModal();
      });
    } else {
      this.productService.createProduct(this.formData).subscribe(() => {
        this.closeModal();
      });
    }
  }

  openDeleteConfirm(product: Product): void {
    this.productToDelete = product;
  }

  confirmDelete(): void {
    if (this.productToDelete) {
      this.productService.deleteProduct(this.productToDelete.id).subscribe(() => {
        this.productToDelete = null;
      });
    }
  }
}
