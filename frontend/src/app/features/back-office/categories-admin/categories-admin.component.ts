import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { BrandService } from '../../../core/services/brand.service';
import { SupplierService } from '../../../core/services/supplier.service';
import { Category } from '../../../core/models/category.model';
import { Brand } from '../../../core/models/brand.model';
import { Supplier } from '../../../core/models/supplier.model';
import { ImageUploadService } from '../../../core/services/image-upload.service';

@Component({
  selector: 'app-categories-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="admin-catalog-page">
      <!-- En-tête -->
      <div class="page-header">
        <div>
          <div class="header-badge-row">
            <span class="badge-sv badge-blue">CATALOGUE SPORT VISION</span>
          </div>
          <h1 class="page-title">Gestion du Catalogue : Catégories, Marques & Fournisseurs</h1>
          <p class="page-subtitle">
            Pilotez l'ensemble des référentiels du catalogue sportif. Ajoutez, modifiez et supprimez vos catégories, marques partenaires et distributeurs.
          </p>
        </div>

        <!-- Bouton d'ajout selon onglet actif -->
        <button (click)="openAddModal()" class="btn-sv btn-sv-primary">
          <span *ngIf="activeTab === 'categories'">+ Nouvelle Catégorie</span>
          <span *ngIf="activeTab === 'brands'">+ Nouvelle Marque</span>
          <span *ngIf="activeTab === 'suppliers'">+ Nouveau Fournisseur</span>
        </button>
      </div>

      <!-- ONGLETS DE NAVIGATION -->
      <div class="catalog-tabs-bar sv-card">
        <button (click)="activeTab = 'categories'" class="catalog-tab-btn" [class.active]="activeTab === 'categories'">
          🏷️ Catégories Sportives ({{ categories.length }})
        </button>
        <button (click)="activeTab = 'brands'" class="catalog-tab-btn" [class.active]="activeTab === 'brands'">
          ⭐ Marques & Équipementiers ({{ brands.length }})
        </button>
        <button (click)="activeTab = 'suppliers'" class="catalog-tab-btn" [class.active]="activeTab === 'suppliers'">
          🏭 Fournisseurs & Logistique ({{ suppliers.length }})
        </button>
      </div>

      <!-- ONGLET 1 : CATÉGORIES -->
      <div *ngIf="activeTab === 'categories'" class="tab-content-area">
        <div class="categories-grid">
          <div *ngFor="let cat of categories" class="sv-card category-admin-card">
            <img [src]="cat.imageUrl" [alt]="cat.name" class="cat-card-img">
            <div class="cat-card-body">
              <div class="cat-header-row">
                <h3 class="cat-title">{{ cat.name }}</h3>
                <span class="badge-sv badge-blue">#{{ cat.id }}</span>
              </div>
              <p class="cat-desc">{{ cat.description }}</p>
              <div class="cat-slug">Slug : <code>{{ cat.slug }}</code></div>
              <div class="cat-footer-actions">
                <button (click)="openEditCategory(cat)" class="btn-sv btn-sv-outline btn-sv-sm">✏️ Modifier</button>
                <button (click)="deleteCategory(cat.id)" class="btn-sv btn-sv-outline btn-sv-sm text-danger">🗑️ Supprimer</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ONGLET 2 : MARQUES (BRANDS) -->
      <div *ngIf="activeTab === 'brands'" class="tab-content-area">
        <div class="brands-grid">
          <div *ngFor="let brand of brands" class="sv-card brand-admin-card">
            <div class="brand-top-row">
              <div class="brand-logo-wrap">
                <img [src]="brand.logoUrl" [alt]="brand.name" class="brand-logo" (error)="onImgError($event)">
              </div>
              <div class="brand-info">
                <h3 class="brand-name">{{ brand.name }}</h3>
                <span class="brand-country" *ngIf="brand.country">📍 {{ brand.country }}</span>
              </div>
            </div>
            <p class="brand-desc">{{ brand.description || 'Aucune description' }}</p>
            <div class="brand-actions">
              <button (click)="openEditBrand(brand)" class="btn-sv btn-sv-outline btn-sv-sm">✏️ Modifier</button>
              <button (click)="deleteBrand(brand.id!)" class="btn-sv btn-sv-outline btn-sv-sm text-danger">🗑️ Supprimer</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ONGLET 3 : FOURNISSEURS (SUPPLIERS) -->
      <div *ngIf="activeTab === 'suppliers'" class="tab-content-area">
        <div class="suppliers-table-card sv-card">
          <table class="sv-table">
            <thead>
              <tr>
                <th>Raison Sociale</th>
                <th>Contact</th>
                <th>Coordonnées</th>
                <th>Adresse</th>
                <th>Spécialité Fourniture</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let sup of suppliers">
                <td>
                  <strong class="sup-name">{{ sup.name }}</strong>
                </td>
                <td>{{ sup.contactPerson || '-' }}</td>
                <td>
                  <div class="contact-sub">
                    <span *ngIf="sup.email">✉️ {{ sup.email }}</span>
                    <span *ngIf="sup.phone">📞 {{ sup.phone }}</span>
                  </div>
                </td>
                <td>{{ sup.address || '-' }}</td>
                <td>
                  <span class="badge-sv badge-blue">{{ sup.supplyCategory || 'Général' }}</span>
                </td>
                <td class="text-right">
                  <div class="table-actions">
                    <button (click)="openEditSupplier(sup)" class="btn-action edit" title="Modifier">✏️</button>
                    <button (click)="deleteSupplier(sup.id!)" class="btn-action delete" title="Supprimer">🗑️</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- MODALE AJOUT / ÉDITION CATÉGORIE -->
      <div class="sv-modal-backdrop" *ngIf="showCatModal" (click)="showCatModal = false">
        <div class="sv-modal-dialog" (click)="$event.stopPropagation()">
          <div class="sv-modal-header">
            <h3>{{ isEditing ? 'Modifier la catégorie' : 'Ajouter une catégorie' }}</h3>
            <button (click)="showCatModal = false" class="btn-close-modal">✕</button>
          </div>
          <form (ngSubmit)="saveCategory()">
            <div class="sv-modal-body">
              <div class="form-group-sv">
                <label class="form-label-sv">Nom de la catégorie *</label>
                <input type="text" [(ngModel)]="currentCat.name" name="name" required class="form-control-sv" placeholder="Ex: Sports d'Hiver & Glisse">
              </div>
              <div class="form-group-sv">
                <label class="form-label-sv">Identifiant d'URL (Slug)</label>
                <input type="text" [(ngModel)]="currentCat.slug" name="slug" class="form-control-sv" placeholder="sports-hiver-glisse">
              </div>
              <div class="form-group-sv">
                <label class="form-label-sv">Description</label>
                <textarea [(ngModel)]="currentCat.description" name="description" rows="3" class="form-control-sv"></textarea>
              </div>
              <div class="form-group-sv">
                <label class="form-label-sv">Image de couverture</label>
                <input type="file" accept="image/*" name="categoryImage" class="form-control-sv" (change)="onImageSelected($event, 'category')">
                <small *ngIf="imageError" class="image-upload-error">{{ imageError }}</small>
                <img *ngIf="currentCat.imageUrl" [src]="currentCat.imageUrl" alt="Aperçu de la catégorie" class="image-upload-preview">
              </div>
            </div>
            <div class="sv-modal-footer">
              <button type="button" (click)="showCatModal = false" class="btn-sv btn-sv-outline">Annuler</button>
              <button type="submit" class="btn-sv btn-sv-primary">Enregistrer</button>
            </div>
          </form>
        </div>
      </div>

      <!-- MODALE AJOUT / ÉDITION MARQUE -->
      <div class="sv-modal-backdrop" *ngIf="showBrandModal" (click)="showBrandModal = false">
        <div class="sv-modal-dialog" (click)="$event.stopPropagation()">
          <div class="sv-modal-header">
            <h3>{{ isEditing ? 'Modifier la marque' : 'Ajouter une nouvelle marque' }}</h3>
            <button (click)="showBrandModal = false" class="btn-close-modal">✕</button>
          </div>
          <form (ngSubmit)="saveBrand()">
            <div class="sv-modal-body">
              <div class="form-group-sv">
                <label class="form-label-sv">Nom de la marque *</label>
                <input type="text" [(ngModel)]="currentBrand.name" name="brandName" required class="form-control-sv" placeholder="Ex: Under Armour">
              </div>
              <div class="form-group-sv">
                <label class="form-label-sv">Pays d'origine</label>
                <input type="text" [(ngModel)]="currentBrand.country" name="brandCountry" class="form-control-sv" placeholder="Ex: USA, France, Allemagne">
              </div>
              <div class="form-group-sv">
                <label class="form-label-sv">Logo de la marque</label>
                <input type="file" accept="image/*" name="brandLogo" class="form-control-sv" (change)="onImageSelected($event, 'brand')">
                <small *ngIf="imageError" class="image-upload-error">{{ imageError }}</small>
                <img *ngIf="currentBrand.logoUrl" [src]="currentBrand.logoUrl" [alt]="'Aperçu du logo ' + currentBrand.name" class="image-upload-preview brand-image-preview">
              </div>
              <div class="form-group-sv">
                <label class="form-label-sv">Description</label>
                <textarea [(ngModel)]="currentBrand.description" name="brandDesc" rows="2" class="form-control-sv" placeholder="Positionnement de la marque..."></textarea>
              </div>
            </div>
            <div class="sv-modal-footer">
              <button type="button" (click)="showBrandModal = false" class="btn-sv btn-sv-outline">Annuler</button>
              <button type="submit" class="btn-sv btn-sv-primary">Enregistrer</button>
            </div>
          </form>
        </div>
      </div>

      <!-- MODALE AJOUT / ÉDITION FOURNISSEUR -->
      <div class="sv-modal-backdrop" *ngIf="showSupModal" (click)="showSupModal = false">
        <div class="sv-modal-dialog" (click)="$event.stopPropagation()">
          <div class="sv-modal-header">
            <h3>{{ isEditing ? 'Modifier le fournisseur' : 'Ajouter un nouveau fournisseur' }}</h3>
            <button (click)="showSupModal = false" class="btn-close-modal">✕</button>
          </div>
          <form (ngSubmit)="saveSupplier()">
            <div class="sv-modal-body">
              <div class="form-group-sv">
                <label class="form-label-sv">Raison sociale du fournisseur *</label>
                <input type="text" [(ngModel)]="currentSupplier.name" name="supName" required class="form-control-sv" placeholder="Ex: SportEquip Pro">
              </div>
              <div class="form-row-2">
                <div class="form-group-sv">
                  <label class="form-label-sv">Personne de contact</label>
                  <input type="text" [(ngModel)]="currentSupplier.contactPerson" name="supContact" class="form-control-sv" placeholder="Ex: Lucas Martin">
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Spécialité fourniture</label>
                  <input type="text" [(ngModel)]="currentSupplier.supplyCategory" name="supCat" class="form-control-sv" placeholder="Ex: Musculation & Force">
                </div>
              </div>
              <div class="form-row-2">
                <div class="form-group-sv">
                  <label class="form-label-sv">Email</label>
                  <input type="email" [(ngModel)]="currentSupplier.email" name="supEmail" class="form-control-sv" placeholder="contact@fournisseur.com">
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Téléphone</label>
                  <input type="tel" [(ngModel)]="currentSupplier.phone" name="supPhone" class="form-control-sv" placeholder="+33 1 ...">
                </div>
              </div>
              <div class="form-group-sv">
                <label class="form-label-sv">Adresse géographique</label>
                <input type="text" [(ngModel)]="currentSupplier.address" name="supAddress" class="form-control-sv" placeholder="Ville, Code Postal">
              </div>
            </div>
            <div class="sv-modal-footer">
              <button type="button" (click)="showSupModal = false" class="btn-sv btn-sv-outline">Annuler</button>
              <button type="submit" class="btn-sv btn-sv-primary">Enregistrer</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .image-upload-preview { display: block; width: 96px; height: 76px; margin-top: 10px; object-fit: contain; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; }
    .brand-image-preview { border-radius: 50%; }
    .image-upload-error { display: block; color: #dc2626; margin-top: 6px; }
    .admin-catalog-page {
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

    .catalog-tabs-bar {
      display: flex;
      gap: 12px;
      padding: 12px 16px;
      margin-bottom: 25px;
      background: white;
    }
    .catalog-tab-btn {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      color: #475569;
      font-weight: 700;
      font-size: 0.9rem;
      padding: 10px 18px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .catalog-tab-btn.active, .catalog-tab-btn:hover {
      background: #1e3a8a;
      color: white;
      border-color: #1e3a8a;
    }

    .tab-content-area {
      margin-bottom: 50px;
    }

    /* Catégories Grid */
    .categories-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
      gap: 22px;
    }
    .category-admin-card {
      padding: 0;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    .cat-card-img {
      width: 100%;
      height: 140px;
      object-fit: cover;
    }
    .cat-card-body {
      padding: 18px;
      display: flex;
      flex-direction: column;
      flex-grow: 1;
    }
    .cat-header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }
    .cat-title {
      font-size: 1.1rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0;
    }
    .cat-desc {
      font-size: 0.85rem;
      color: #64748b;
      line-height: 1.45;
      margin-bottom: 12px;
      flex-grow: 1;
    }
    .cat-slug {
      font-size: 0.78rem;
      color: #94a3b8;
      margin-bottom: 14px;
    }
    .cat-footer-actions {
      display: flex;
      gap: 8px;
    }

    /* Marques Grid */
    .brands-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 22px;
    }
    .brand-admin-card {
      padding: 20px;
      display: flex;
      flex-direction: column;
    }
    .brand-top-row {
      display: flex;
      gap: 14px;
      align-items: center;
      margin-bottom: 12px;
    }
    .brand-logo-wrap {
      width: 50px;
      height: 50px;
      background: #f1f5f9;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      border: 1px solid #e2e8f0;
    }
    .brand-logo {
      max-width: 80%;
      max-height: 80%;
      object-fit: contain;
    }
    .brand-name {
      font-size: 1.15rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 2px;
    }
    .brand-country {
      font-size: 0.8rem;
      color: #0284c7;
      font-weight: 600;
    }
    .brand-desc {
      font-size: 0.85rem;
      color: #64748b;
      margin-bottom: 16px;
      flex-grow: 1;
      line-height: 1.45;
    }
    .brand-actions {
      display: flex;
      gap: 8px;
    }

    /* Fournisseurs Table */
    .suppliers-table-card {
      padding: 0;
      overflow: hidden;
    }
    .sup-name {
      font-size: 0.95rem;
      color: #0f172a;
    }
    .contact-sub {
      display: flex;
      flex-direction: column;
      gap: 2px;
      font-size: 0.8rem;
      color: #64748b;
    }

    .table-actions {
      display: flex;
      justify-content: flex-end;
      gap: 6px;
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
    .text-danger { color: #dc2626 !important; }

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
      max-width: 550px;
      overflow: hidden;
      box-shadow: 0 20px 25px -5px rgba(0,0,0,0.2);
    }
    .sv-modal-header {
      padding: 18px 22px;
      border-bottom: 1px solid #e2e8f0;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .sv-modal-header h3 {
      margin: 0;
      font-size: 1.2rem;
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
      padding: 20px 22px;
    }
    .form-row-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
    .sv-modal-footer {
      padding: 14px 22px;
      border-top: 1px solid #e2e8f0;
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      background: #f8fafc;
    }
  `]
})
export class CategoriesAdminComponent implements OnInit {
  activeTab: 'categories' | 'brands' | 'suppliers' = 'categories';

  categories: Category[] = [];
  brands: Brand[] = [];
  suppliers: Supplier[] = [];

  // Modales
  showCatModal = false;
  showBrandModal = false;
  showSupModal = false;
  isEditing = false;
  imageError = '';

  currentCat: Category = { id: 0, name: '', slug: '', description: '', icon: '', imageUrl: '' };
  currentBrand: Brand = { name: '', country: '', logoUrl: '', description: '' };
  currentSupplier: Supplier = { name: '', contactPerson: '', email: '', phone: '', address: '', supplyCategory: '' };

  constructor(
    private productService: ProductService,
    private brandService: BrandService,
    private supplierService: SupplierService,
    private imageUploadService: ImageUploadService
  ) {}

  ngOnInit(): void {
    this.productService.getCategories().subscribe(c => this.categories = c);
    this.brandService.brands$.subscribe(b => this.brands = b);
    this.supplierService.suppliers$.subscribe(s => this.suppliers = s);
  }

  async onImageSelected(event: Event, target: 'category' | 'brand'): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.imageError = '';
    try {
      const image = await this.imageUploadService.convert(file);
      if (target === 'category') this.currentCat.imageUrl = image;
      else this.currentBrand.logoUrl = image;
    } catch (error) {
      this.imageError = error instanceof Error ? error.message : 'Impossible de charger cette image.';
    } finally {
      input.value = '';
    }
  }

  openAddModal(): void {
    this.isEditing = false;
    if (this.activeTab === 'categories') {
      this.currentCat = { id: 0, name: '', slug: '', description: '', icon: 'tag', imageUrl: 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&q=80' };
      this.showCatModal = true;
    } else if (this.activeTab === 'brands') {
      this.currentBrand = { name: '', country: 'France', logoUrl: 'https://logo.clearbit.com/nike.com', description: '' };
      this.showBrandModal = true;
    } else {
      this.currentSupplier = { name: '', contactPerson: '', email: '', phone: '', address: '', supplyCategory: 'Équipements sportifs' };
      this.showSupModal = true;
    }
  }

  // Categories
  openEditCategory(cat: Category): void {
    this.isEditing = true;
    this.currentCat = { ...cat };
    this.showCatModal = true;
  }
  saveCategory(): void {
    // In local catalog
    this.showCatModal = false;
  }
  deleteCategory(id: number): void {
    if (confirm('Voulez-vous supprimer cette catégorie ?')) {
      this.categories = this.categories.filter(c => c.id !== id);
    }
  }

  // Brands
  openEditBrand(brand: Brand): void {
    this.isEditing = true;
    this.currentBrand = { ...brand };
    this.showBrandModal = true;
  }
  saveBrand(): void {
    if (this.isEditing && this.currentBrand.id) {
      this.brandService.updateBrand(this.currentBrand.id, this.currentBrand).subscribe();
    } else {
      this.brandService.createBrand(this.currentBrand).subscribe();
    }
    this.showBrandModal = false;
  }
  deleteBrand(id: number): void {
    if (confirm('Voulez-vous supprimer cette marque ?')) {
      this.brandService.deleteBrand(id).subscribe();
    }
  }

  // Suppliers
  openEditSupplier(sup: Supplier): void {
    this.isEditing = true;
    this.currentSupplier = { ...sup };
    this.showSupModal = true;
  }
  saveSupplier(): void {
    if (this.isEditing && this.currentSupplier.id) {
      this.supplierService.updateSupplier(this.currentSupplier.id, this.currentSupplier).subscribe();
    } else {
      this.supplierService.createSupplier(this.currentSupplier).subscribe();
    }
    this.showSupModal = false;
  }
  deleteSupplier(id: number): void {
    if (confirm('Voulez-vous supprimer ce fournisseur ?')) {
      this.supplierService.deleteSupplier(id).subscribe();
    }
  }

  onImgError(event: any): void {
    event.target.src = 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=100&q=80';
  }
}
