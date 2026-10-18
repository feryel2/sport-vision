import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FoodService } from '../../../core/services/food.service';
import { Food } from '../../../core/models/food.model';

@Component({
  selector: 'app-food-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="admin-food-page">
      <!-- En-tête -->
      <div class="page-header">
        <div>
          <div class="header-badge-row">
            <span class="badge-sv badge-blue">GESTION DES ALIMENTS</span>
          </div>
          <h1 class="page-title">Référentiel Nutrition & Aliments</h1>
          <p class="page-subtitle">Ajouter, modifier ou supprimer les aliments et valeurs nutritionnelles du module sportif.</p>
        </div>
        <button (click)="openAddModal()" class="btn-sv btn-sv-primary">
          <span>+ Nouvel Aliment</span>
        </button>
      </div>

      <!-- Filtres -->
      <div class="sv-card filter-toolbar">
        <div class="search-input-wrap">
          <input type="text" [(ngModel)]="searchQuery" (input)="filterFoods()" placeholder="Rechercher par aliment ou bienfaits..." class="form-control-sv">
        </div>
        <div class="cat-select-wrap">
          <select [(ngModel)]="selectedCategory" (change)="filterFoods()" class="form-control-sv">
            <option value="Tous">Toutes les catégories</option>
            <option *ngFor="let cat of categories" [value]="cat">{{ cat }}</option>
          </select>
        </div>
      </div>

      <!-- Table des aliments -->
      <div class="sv-card table-card">
        <table class="sv-table">
          <thead>
            <tr>
              <th>Aliment</th>
              <th>Catégorie</th>
              <th>Portion</th>
              <th class="text-center">Calories</th>
              <th class="text-center">Protéines</th>
              <th class="text-center">Glucides</th>
              <th class="text-center">Lipides</th>
              <th>Bienfaits Sportifs</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let food of filteredFoods">
              <td>
                <strong class="food-name">{{ food.name }}</strong>
              </td>
              <td>
                <span class="badge-sv badge-blue">{{ food.category }}</span>
              </td>
              <td>{{ food.portion }}</td>
              <td class="text-center font-bold text-blue">{{ food.calories }} kcal</td>
              <td class="text-center font-bold text-success">{{ food.proteins }} g</td>
              <td class="text-center font-bold text-warning">{{ food.carbs }} g</td>
              <td class="text-center font-bold text-danger">{{ food.fats }} g</td>
              <td>
                <span class="food-benefits-text">{{ food.benefits }}</span>
              </td>
              <td class="text-right">
                <div class="table-actions">
                  <button (click)="openEditModal(food)" class="btn-action edit" title="Modifier">✏️</button>
                  <button (click)="deleteFood(food.id!)" class="btn-action delete" title="Supprimer">🗑️</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- MODALE AJOUT / ÉDITION -->
      <div class="sv-modal-backdrop" *ngIf="showModal" (click)="showModal = false">
        <div class="sv-modal-dialog" (click)="$event.stopPropagation()">
          <div class="sv-modal-header">
            <h3>{{ isEditing ? 'Modifier l\'aliment' : 'Ajouter un aliment' }}</h3>
            <button (click)="showModal = false" class="btn-close-modal">✕</button>
          </div>
          <form (ngSubmit)="saveFood()">
            <div class="sv-modal-body">
              <div class="form-group-sv">
                <label class="form-label-sv">Nom de l'aliment *</label>
                <input type="text" [(ngModel)]="currentFood.name" name="name" required class="form-control-sv" placeholder="Ex: Blanc de poulet fermier">
              </div>

              <div class="form-row-2">
                <div class="form-group-sv">
                  <label class="form-label-sv">Catégorie *</label>
                  <select [(ngModel)]="currentFood.category" name="category" required class="form-control-sv">
                    <option value="Protéines maigres">Protéines maigres</option>
                    <option value="Glucides complexes">Glucides complexes</option>
                    <option value="Glucides faciles">Glucides faciles</option>
                    <option value="Lipides sains">Lipides sains</option>
                    <option value="Compléments">Compléments alimentaires</option>
                    <option value="Fruits énergétiques">Fruits énergétiques</option>
                  </select>
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Portion usuelle *</label>
                  <input type="text" [(ngModel)]="currentFood.portion" name="portion" required class="form-control-sv" placeholder="Ex: 100g, 1 dose">
                </div>
              </div>

              <div class="form-row-4">
                <div class="form-group-sv">
                  <label class="form-label-sv">Calories (kcal)</label>
                  <input type="number" [(ngModel)]="currentFood.calories" name="calories" required class="form-control-sv">
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Protéines (g)</label>
                  <input type="number" step="0.1" [(ngModel)]="currentFood.proteins" name="proteins" required class="form-control-sv">
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Glucides (g)</label>
                  <input type="number" step="0.1" [(ngModel)]="currentFood.carbs" name="carbs" required class="form-control-sv">
                </div>
                <div class="form-group-sv">
                  <label class="form-label-sv">Lipides (g)</label>
                  <input type="number" step="0.1" [(ngModel)]="currentFood.fats" name="fats" required class="form-control-sv">
                </div>
              </div>

              <div class="form-group-sv">
                <label class="form-label-sv">Bienfaits sportifs</label>
                <textarea [(ngModel)]="currentFood.benefits" name="benefits" rows="2" class="form-control-sv" placeholder="Propriétés pour les sportifs..."></textarea>
              </div>
            </div>
            <div class="sv-modal-footer">
              <button type="button" (click)="showModal = false" class="btn-sv btn-sv-outline">Annuler</button>
              <button type="submit" class="btn-sv btn-sv-primary">Enregistrer</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-food-page {
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
      font-weight: 800;
      color: #0f172a;
      margin: 0;
    }
    .page-subtitle {
      font-size: 0.9rem;
      color: #64748b;
      margin-top: 4px;
    }

    .filter-toolbar {
      display: flex;
      gap: 16px;
      padding: 16px 20px;
      margin-bottom: 24px;
      align-items: center;
    }
    .search-input-wrap {
      flex: 1;
      min-width: 250px;
    }
    .cat-select-wrap {
      width: 220px;
    }

    .table-card {
      padding: 0;
      overflow: hidden;
    }
    .food-name {
      font-size: 0.95rem;
      color: #0f172a;
    }
    .food-benefits-text {
      font-size: 0.82rem;
      color: #64748b;
      max-width: 300px;
      display: block;
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

    /* Modal */
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
      max-width: 600px;
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
    .form-row-4 {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 10px;
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
export class FoodAdminComponent implements OnInit {
  foods: Food[] = [];
  filteredFoods: Food[] = [];
  searchQuery = '';
  selectedCategory = 'Tous';
  categories = ['Protéines maigres', 'Glucides complexes', 'Glucides faciles', 'Lipides sains', 'Compléments', 'Fruits énergétiques'];

  showModal = false;
  isEditing = false;
  currentFood: Food = this.getEmptyFood();

  constructor(private foodService: FoodService) {}

  ngOnInit(): void {
    this.foodService.foods$.subscribe(list => {
      this.foods = list;
      this.filterFoods();
    });
  }

  getEmptyFood(): Food {
    return {
      name: '',
      category: 'Protéines maigres',
      calories: 150,
      proteins: 20,
      carbs: 0,
      fats: 2,
      portion: '100g',
      benefits: ''
    };
  }

  filterFoods(): void {
    let res = this.foods;
    if (this.selectedCategory !== 'Tous') {
      res = res.filter(f => f.category.toLowerCase().includes(this.selectedCategory.toLowerCase()));
    }
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      res = res.filter(f =>
        f.name.toLowerCase().includes(q) ||
        f.category.toLowerCase().includes(q) ||
        (f.benefits && f.benefits.toLowerCase().includes(q))
      );
    }
    this.filteredFoods = res;
  }

  openAddModal(): void {
    this.isEditing = false;
    this.currentFood = this.getEmptyFood();
    this.showModal = true;
  }

  openEditModal(food: Food): void {
    this.isEditing = true;
    this.currentFood = { ...food };
    this.showModal = true;
  }

  saveFood(): void {
    if (this.isEditing && this.currentFood.id) {
      this.foodService.updateFood(this.currentFood.id, this.currentFood).subscribe();
    } else {
      this.foodService.createFood(this.currentFood).subscribe();
    }
    this.showModal = false;
  }

  deleteFood(id: number): void {
    if (confirm('Voulez-vous supprimer cet aliment ?')) {
      this.foodService.deleteFood(id).subscribe();
    }
  }
}
