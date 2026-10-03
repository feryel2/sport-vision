import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FoodService } from '../../../core/services/food.service';
import { Food } from '../../../core/models/food.model';

@Component({
  selector: 'app-food-nutrition',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="food-page">
      <!-- HEADER -->
      <section class="food-header">
        <div class="container">
          <div class="food-badge">
            <span>🥗 NUTRITION SPORTIVE & PERFORMANCE</span>
          </div>
          <h1 class="page-title">Aliments & Macro-Nutriments Sportifs</h1>
          <p class="page-subtitle">
            Base nutritionnelle pour vos programmes de musculation, sèche, prise de masse et sports d'endurance. Consultez, ajoutez, modifiez ou supprimez vos aliments de référence.
          </p>
        </div>
      </section>

      <div class="container food-body">
        <!-- BARRE DE GESTION & RECHERCHE -->
        <div class="sv-card toolbar-card">
          <div class="toolbar-search">
            <input type="text" [(ngModel)]="searchQuery" (input)="filterFoods()" placeholder="Rechercher un aliment, nutriment ou bienfait..." class="form-control-sv">
          </div>
          <div class="toolbar-filters">
            <button *ngFor="let cat of categories" (click)="selectCategory(cat)" class="cat-pill" [class.active]="selectedCategory === cat">
              {{ cat }}
            </button>
          </div>
          <button (click)="openAddModal()" class="btn-sv btn-sv-primary">
            + Ajouter un Aliment
          </button>
        </div>

        <!-- CALCULATEUR DE REPAS SPORTIF -->
        <div class="sv-card macro-calc-card" *ngIf="selectedFoodsForMeal.length > 0">
          <div class="calc-header">
            <h4>🍽️ Mon Assiette Sportive ({{ selectedFoodsForMeal.length }} aliment(s))</h4>
            <button (click)="clearMeal()" class="btn-clear">Vider</button>
          </div>
          <div class="macro-stats-row">
            <div class="macro-badge kcal">
              <strong>{{ totalKcal | number:'1.0-0' }}</strong> kcal
            </div>
            <div class="macro-badge prot">
              <strong>{{ totalProt | number:'1.1-1' }}g</strong> Protéines
            </div>
            <div class="macro-badge carbs">
              <strong>{{ totalCarbs | number:'1.1-1' }}g</strong> Glucides
            </div>
            <div class="macro-badge fats">
              <strong>{{ totalFats | number:'1.1-1' }}g</strong> Lipides
            </div>
          </div>
          <div class="meal-items-chips">
            <span *ngFor="let item of selectedFoodsForMeal" class="chip">
              {{ item.name }} ({{ item.portion }})
              <button (click)="removeFoodFromMeal(item)" class="chip-remove">✕</button>
            </span>
          </div>
        </div>

        <!-- TABLE DES ALIMENTS -->
        <div class="sv-card table-card">
          <div class="table-responsive">
            <table class="food-table">
              <thead>
                <tr>
                  <th>Aliment & Catégorie</th>
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
                    <div class="food-info">
                      <strong>{{ food.name }}</strong>
                      <span class="badge-sv badge-blue food-cat-tag">{{ food.category }}</span>
                    </div>
                  </td>
                  <td>{{ food.portion }}</td>
                  <td class="text-center font-bold text-blue">{{ food.calories }} kcal</td>
                  <td class="text-center text-success font-bold">{{ food.proteins }} g</td>
                  <td class="text-center text-warning font-bold">{{ food.carbs }} g</td>
                  <td class="text-center text-danger font-bold">{{ food.fats }} g</td>
                  <td>
                    <span class="food-benefits">{{ food.benefits }}</span>
                  </td>
                  <td class="text-right actions-cell">
                    <button (click)="toggleFoodInMeal(food)" class="btn-action-icon" [title]="isInMeal(food) ? 'Retirer du repas' : 'Ajouter au repas'">
                      {{ isInMeal(food) ? '➖' : '➕' }}
                    </button>
                    <button (click)="openEditModal(food)" class="btn-action-icon" title="Modifier">
                      ✏️
                    </button>
                    <button (click)="deleteFood(food.id!)" class="btn-action-icon btn-del" title="Supprimer">
                      🗑️
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- MODAL AJOUT / MODIFICATION D'UN ALIMENT -->
      <div class="modal-overlay" *ngIf="isModalOpen">
        <div class="modal-backdrop" (click)="closeModal()"></div>
        <div class="modal-content sv-card">
          <div class="modal-header">
            <h3>{{ isEditMode ? 'Modifier l\'aliment' : 'Ajouter un nouvel aliment' }}</h3>
            <button (click)="closeModal()" class="btn-close-modal">✕</button>
          </div>

          <form (ngSubmit)="saveFood()">
            <div class="modal-body">
              <div class="form-group-sv">
                <label class="form-label-sv">Nom de l'aliment *</label>
                <input type="text" [(ngModel)]="currentFood.name" name="name" required class="form-control-sv" placeholder="Ex: Blanc de poulet">
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
                <label class="form-label-sv">Bienfaits sportifs / Propriétés</label>
                <textarea [(ngModel)]="currentFood.benefits" name="benefits" rows="2" class="form-control-sv" placeholder="Ex: Idéal en collation post-effort pour la récupération..."></textarea>
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" (click)="closeModal()" class="btn-sv btn-sv-outline">Annuler</button>
              <button type="submit" class="btn-sv btn-sv-primary">
                {{ isEditMode ? 'Enregistrer les modifications' : 'Ajouter au catalogue nutritionnel' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1240px;
      margin: 0 auto;
      padding: 0 20px;
    }
    .food-header {
      background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0284c7 100%);
      color: white;
      padding: 50px 0 45px;
      text-align: center;
    }
    .food-badge {
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
    .page-title {
      font-size: 2.3rem;
      font-weight: 900;
      margin: 0 0 10px;
      letter-spacing: -0.5px;
    }
    .page-subtitle {
      max-width: 720px;
      margin: 0 auto;
      color: #cbd5e1;
      font-size: 1rem;
      line-height: 1.5;
    }

    .food-body {
      padding: 40px 20px 60px;
    }

    .toolbar-card {
      padding: 20px;
      display: flex;
      gap: 15px;
      align-items: center;
      flex-wrap: wrap;
      margin-bottom: 25px;
    }
    .toolbar-search {
      flex: 1;
      min-width: 250px;
    }
    .toolbar-filters {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .cat-pill {
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      color: #475569;
      font-size: 0.82rem;
      font-weight: 600;
      padding: 6px 12px;
      border-radius: 20px;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .cat-pill.active, .cat-pill:hover {
      background: #1e3a8a;
      color: white;
      border-color: #1e3a8a;
    }

    .macro-calc-card {
      padding: 20px;
      margin-bottom: 25px;
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
    }
    .calc-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    .calc-header h4 {
      margin: 0;
      color: #166534;
      font-weight: 800;
    }
    .btn-clear {
      background: none;
      border: none;
      color: #dc2626;
      font-size: 0.82rem;
      font-weight: 700;
      cursor: pointer;
    }
    .macro-stats-row {
      display: flex;
      gap: 15px;
      flex-wrap: wrap;
      margin-bottom: 12px;
    }
    .macro-badge {
      padding: 6px 14px;
      border-radius: 8px;
      font-size: 0.85rem;
      background: white;
      border: 1px solid #e2e8f0;
    }
    .macro-badge.kcal { border-color: #38bdf8; color: #0284c7; }
    .macro-badge.prot { border-color: #86efac; color: #16a34a; }
    .macro-badge.carbs { border-color: #fde047; color: #ca8a04; }
    .macro-badge.fats { border-color: #fca5a5; color: #dc2626; }

    .meal-items-chips {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .chip {
      background: white;
      border: 1px solid #bbf7d0;
      padding: 4px 10px;
      border-radius: 16px;
      font-size: 0.8rem;
      color: #166534;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .chip-remove {
      background: none;
      border: none;
      color: #991b1b;
      cursor: pointer;
      font-size: 0.75rem;
      padding: 0;
    }

    .table-card {
      padding: 0;
      overflow: hidden;
    }
    .food-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9rem;
    }
    .food-table th {
      background: #f8fafc;
      padding: 14px 18px;
      text-align: left;
      font-weight: 700;
      color: #475569;
      border-bottom: 1px solid #e2e8f0;
      font-size: 0.82rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .food-table td {
      padding: 14px 18px;
      border-bottom: 1px solid #f1f5f9;
      color: #334155;
    }
    .food-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .food-cat-tag {
      width: fit-content;
      font-size: 0.72rem;
    }
    .food-benefits {
      font-size: 0.83rem;
      color: #64748b;
      line-height: 1.4;
      display: block;
      max-width: 320px;
    }
    .actions-cell {
      white-space: nowrap;
    }
    .btn-action-icon {
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      padding: 5px 8px;
      margin-left: 4px;
      cursor: pointer;
      font-size: 0.85rem;
      transition: background 0.15s ease;
    }
    .btn-action-icon:hover {
      background: #e2e8f0;
    }
    .btn-del:hover {
      background: #fee2e2;
    }

    /* Modal */
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
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(4px);
    }
    .modal-content {
      position: relative;
      background: white;
      border-radius: 14px;
      max-width: 600px;
      width: 90%;
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
      color: #64748b;
    }
    .modal-body {
      padding: 20px 24px;
    }
    .form-row-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 15px;
    }
    .form-row-4 {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 10px;
    }
    .modal-footer {
      padding: 16px 24px;
      border-top: 1px solid #e2e8f0;
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      background: #f8fafc;
    }
  `]
})
export class FoodNutritionComponent implements OnInit {
  foods: Food[] = [];
  filteredFoods: Food[] = [];
  searchQuery = '';
  selectedCategory = 'Tous';
  categories = ['Tous', 'Protéines maigres', 'Glucides complexes', 'Lipides sains', 'Compléments', 'Fruits énergétiques'];

  selectedFoodsForMeal: Food[] = [];

  // Modal
  isModalOpen = false;
  isEditMode = false;
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

  selectCategory(cat: string): void {
    this.selectedCategory = cat;
    this.filterFoods();
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
    this.isEditMode = false;
    this.currentFood = this.getEmptyFood();
    this.isModalOpen = true;
  }

  openEditModal(food: Food): void {
    this.isEditMode = true;
    this.currentFood = { ...food };
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
  }

  saveFood(): void {
    if (this.isEditMode && this.currentFood.id) {
      this.foodService.updateFood(this.currentFood.id, this.currentFood).subscribe();
    } else {
      this.foodService.createFood(this.currentFood).subscribe();
    }
    this.closeModal();
  }

  deleteFood(id: number): void {
    if (confirm('Voulez-vous vraiment supprimer cet aliment ?')) {
      this.foodService.deleteFood(id).subscribe();
      this.selectedFoodsForMeal = this.selectedFoodsForMeal.filter(f => f.id !== id);
    }
  }

  // Meal Calculator
  toggleFoodInMeal(food: Food): void {
    const idx = this.selectedFoodsForMeal.findIndex(f => f.id === food.id);
    if (idx > -1) {
      this.selectedFoodsForMeal.splice(idx, 1);
    } else {
      this.selectedFoodsForMeal.push(food);
    }
  }

  isInMeal(food: Food): boolean {
    return this.selectedFoodsForMeal.some(f => f.id === food.id);
  }

  removeFoodFromMeal(food: Food): void {
    this.selectedFoodsForMeal = this.selectedFoodsForMeal.filter(f => f.id !== food.id);
  }

  clearMeal(): void {
    this.selectedFoodsForMeal = [];
  }

  get totalKcal(): number {
    return this.selectedFoodsForMeal.reduce((sum, f) => sum + (f.calories || 0), 0);
  }
  get totalProt(): number {
    return this.selectedFoodsForMeal.reduce((sum, f) => sum + (f.proteins || 0), 0);
  }
  get totalCarbs(): number {
    return this.selectedFoodsForMeal.reduce((sum, f) => sum + (f.carbs || 0), 0);
  }
  get totalFats(): number {
    return this.selectedFoodsForMeal.reduce((sum, f) => sum + (f.fats || 0), 0);
  }
}
