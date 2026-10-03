import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, map, catchError, of, tap } from 'rxjs';
import { Brand } from '../models/brand.model';

@Injectable({
  providedIn: 'root'
})
export class BrandService {
  private apiUrl = 'http://localhost:8080/api/brands';

  private defaultBrands: Brand[] = [
    { id: 1, name: 'Nike', country: 'USA', logoUrl: 'https://logo.clearbit.com/nike.com', description: 'Leader mondial des équipements sportifs.' },
    { id: 2, name: 'Adidas', country: 'Allemagne', logoUrl: 'https://logo.clearbit.com/adidas.com', description: 'Performance, innovation et durabilité sportive.' },
    { id: 3, name: 'Under Armour', country: 'USA', logoUrl: 'https://logo.clearbit.com/underarmour.com', description: 'Équipements et accessoires de pointe pour la musculation.' },
    { id: 4, name: 'Salomon', country: 'France', logoUrl: 'https://logo.clearbit.com/salomon.com', description: 'Référence mondiale en trail et running outdoor.' },
    { id: 5, name: 'Garmin', country: 'Suisse', logoUrl: 'https://logo.clearbit.com/garmin.com', description: 'Montres GPS, capteurs cardio et technologies d\'entraînement.' },
    { id: 6, name: 'Optimum Nutrition', country: 'Irlande', logoUrl: 'https://logo.clearbit.com/optimumnutrition.com', description: 'Numéro 1 mondial de la nutrition et protéines pour sportifs.' }
  ];

  private brandsSubject = new BehaviorSubject<Brand[]>(this.loadBrands());
  public brands$ = this.brandsSubject.asObservable();

  constructor(private http: HttpClient) {
    this.refresh();
  }

  private loadBrands(): Brand[] {
    const saved = localStorage.getItem('sport_vision_brands');
    return saved ? JSON.parse(saved) : this.defaultBrands;
  }

  private saveBrands(brands: Brand[]): void {
    localStorage.setItem('sport_vision_brands', JSON.stringify(brands));
    this.brandsSubject.next(brands);
  }

  public refresh(): void {
    this.http.get<{ success: boolean; data: Brand[] }>(this.apiUrl)
      .pipe(
        map(res => res.data),
        catchError(() => of(this.loadBrands()))
      )
      .subscribe(brands => {
        if (brands && brands.length > 0) {
          this.saveBrands(brands);
        }
      });
  }

  public getBrands(): Observable<Brand[]> {
    return this.brands$;
  }

  public getBrandById(id: number): Observable<Brand | undefined> {
    return this.brands$.pipe(
      map(brands => brands.find(b => b.id === id))
    );
  }

  public createBrand(brand: Brand): Observable<Brand> {
    const newBrand: Brand = {
      ...brand,
      id: Date.now()
    };
    this.http.post<any>(this.apiUrl, brand).subscribe({ error: () => {} });
    const updated = [...this.brandsSubject.value, newBrand];
    this.saveBrands(updated);
    return of(newBrand);
  }

  public updateBrand(id: number, brand: Brand): Observable<Brand> {
    const current = [...this.brandsSubject.value];
    const index = current.findIndex(b => b.id === id);
    if (index > -1) {
      current[index] = { ...brand, id };
      this.saveBrands(current);
      this.http.put<any>(`${this.apiUrl}/${id}`, brand).subscribe({ error: () => {} });
      return of(current[index]);
    }
    return of(brand);
  }

  public deleteBrand(id: number): Observable<boolean> {
    const updated = this.brandsSubject.value.filter(b => b.id !== id);
    this.saveBrands(updated);
    this.http.delete<any>(`${this.apiUrl}/${id}`).subscribe({ error: () => {} });
    return of(true);
  }
}
