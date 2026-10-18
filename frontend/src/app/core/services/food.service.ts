import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, map, catchError, of } from 'rxjs';
import { Food } from '../models/food.model';

@Injectable({
  providedIn: 'root'
})
export class FoodService {
  private apiUrl = '/api/foods';

  private defaultFoods: Food[] = [
    { id: 1, name: 'Blanc de poulet fermier grillé', category: 'Protéines maigres', calories: 165, proteins: 31.0, carbs: 0.0, fats: 3.6, portion: '100g', benefits: 'Idéal pour la prise de masse sèche et la réparation tissulaire.' },
    { id: 2, name: 'Flocons d\'avoine complets bio', category: 'Glucides complexes', calories: 375, proteins: 13.5, carbs: 60.0, fats: 7.0, portion: '100g', benefits: 'Énergie longue durée à index glycémique modéré, riche en bêta-glucanes.' },
    { id: 3, name: 'Whey Isolate 90% Native', category: 'Compléments', calories: 110, proteins: 27.0, carbs: 0.5, fats: 0.3, portion: '1 dose (30g)', benefits: 'Assimilation ultra-rapide post-entraînement pour maximiser l\'anabolisme.' },
    { id: 4, name: 'Banane mûre', category: 'Fruits énergétiques', calories: 105, proteins: 1.3, carbs: 27.0, fats: 0.3, portion: '1 pièce (120g)', benefits: 'Recharge glycogénique rapide pré-effort, excellente teneur en potassium.' },
    { id: 5, name: 'Amandes entières non grillées', category: 'Lipides sains', calories: 175, proteins: 6.0, carbs: 3.0, fats: 15.0, portion: '1 poignée (30g)', benefits: 'Acides gras mono-insaturés, magnésium et vitamine E anti-oxydante.' },
    { id: 6, name: 'Pavé de saumon atlantique', category: 'Protéines & Oméga-3', calories: 310, proteins: 30.0, carbs: 0.0, fats: 20.0, portion: '1 portion (150g)', benefits: 'Oméga-3 EPA/DHA anti-inflammatoires puissants pour les tendons et le cœur.' },
    { id: 7, name: 'Riz basmati blanc cuit', category: 'Glucides faciles', calories: 195, proteins: 4.2, carbs: 43.0, fats: 0.5, portion: '1 portion (150g)', benefits: 'Glucides faciles à digérer sans lourdeur digestive avant l\'effort.' },
    { id: 8, name: 'Oeufs entiers plein air', category: 'Protéines complètes', calories: 145, proteins: 12.6, carbs: 1.1, fats: 10.0, portion: '2 gros oeufs', benefits: 'Score d\'acides aminés de référence (100) et choline pour le système nerveux.' }
  ];

  private foodsSubject = new BehaviorSubject<Food[]>(this.loadFoods());
  public foods$ = this.foodsSubject.asObservable();

  constructor(private http: HttpClient) {
    this.refresh();
  }

  private loadFoods(): Food[] {
    const saved = localStorage.getItem('sport_vision_foods');
    return saved ? JSON.parse(saved) : this.defaultFoods;
  }

  private saveFoods(foods: Food[]): void {
    localStorage.setItem('sport_vision_foods', JSON.stringify(foods));
    this.foodsSubject.next(foods);
  }

  public refresh(): void {
    this.http.get<{ success: boolean; data: Food[] }>(this.apiUrl)
      .pipe(
        map(res => res.data),
        catchError(() => of(this.loadFoods()))
      )
      .subscribe(foods => {
        if (foods && foods.length > 0) {
          this.saveFoods(foods);
        }
      });
  }

  public getFoods(category?: string, query?: string): Observable<Food[]> {
    return this.foods$.pipe(
      map(foods => {
        return foods.filter(f => {
          const matchCat = category ? f.category.toLowerCase() === category.toLowerCase() : true;
          const matchQuery = query ?
            f.name.toLowerCase().includes(query.toLowerCase()) ||
            f.category.toLowerCase().includes(query.toLowerCase()) ||
            (f.benefits && f.benefits.toLowerCase().includes(query.toLowerCase())) : true;
          return matchCat && matchQuery;
        });
      })
    );
  }

  public getFoodById(id: number): Observable<Food | undefined> {
    return this.foods$.pipe(
      map(foods => foods.find(f => f.id === id))
    );
  }

  public createFood(food: Food): Observable<Food> {
    const newFood: Food = {
      ...food,
      id: Date.now()
    };
    this.http.post<any>(this.apiUrl, food).subscribe({ error: () => {} });
    const updated = [newFood, ...this.foodsSubject.value];
    this.saveFoods(updated);
    return of(newFood);
  }

  public updateFood(id: number, food: Food): Observable<Food> {
    const current = [...this.foodsSubject.value];
    const index = current.findIndex(f => f.id === id);
    if (index > -1) {
      current[index] = { ...food, id };
      this.saveFoods(current);
      this.http.put<any>(`${this.apiUrl}/${id}`, food).subscribe({ error: () => {} });
      return of(current[index]);
    }
    return of(food);
  }

  public deleteFood(id: number): Observable<boolean> {
    const updated = this.foodsSubject.value.filter(f => f.id !== id);
    this.saveFoods(updated);
    this.http.delete<any>(`${this.apiUrl}/${id}`).subscribe({ error: () => {} });
    return of(true);
  }
}
