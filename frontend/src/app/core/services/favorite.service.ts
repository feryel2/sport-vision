import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, map, of } from 'rxjs';
import { Favorite } from '../models/favorite.model';
import { Product } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {
  private apiUrl = 'http://localhost:8080/api/favorites';
  private defaultEmail = 'user@sportvision.com';

  private favoritesSubject = new BehaviorSubject<Favorite[]>(this.loadFavorites());
  public favorites$ = this.favoritesSubject.asObservable();

  constructor(private http: HttpClient) {
    this.refresh();
  }

  private loadFavorites(): Favorite[] {
    const saved = localStorage.getItem('sport_vision_favorites');
    return saved ? JSON.parse(saved) : [];
  }

  private saveFavorites(favs: Favorite[]): void {
    localStorage.setItem('sport_vision_favorites', JSON.stringify(favs));
    this.favoritesSubject.next(favs);
  }

  public refresh(): void {
    this.http.get<{ success: boolean; data: Favorite[] }>(`${this.apiUrl}?userEmail=${this.defaultEmail}`)
      .subscribe({
        next: (res) => {
          if (res.data) {
            this.saveFavorites(res.data);
          }
        },
        error: () => {}
      });
  }

  public getFavorites(): Observable<Favorite[]> {
    return this.favorites$;
  }

  public isFavorite(productId: number): boolean {
    return this.favoritesSubject.value.some(f => f.product && f.product.id === productId);
  }

  public toggleFavorite(product: Product): Observable<boolean> {
    const current = [...this.favoritesSubject.value];
    const index = current.findIndex(f => f.product && f.product.id === product.id);

    if (index > -1) {
      // Remove
      current.splice(index, 1);
      this.saveFavorites(current);
      this.http.post<any>(`${this.apiUrl}/toggle?userEmail=${this.defaultEmail}&productId=${product.id}`, {}).subscribe({ error: () => {} });
      return of(false);
    } else {
      // Add
      const newFav: Favorite = {
        id: Date.now(),
        userEmail: this.defaultEmail,
        product: product,
        createdAt: new Date().toISOString()
      };
      const updated = [newFav, ...current];
      this.saveFavorites(updated);
      this.http.post<any>(`${this.apiUrl}/toggle?userEmail=${this.defaultEmail}&productId=${product.id}`, {}).subscribe({ error: () => {} });
      return of(true);
    }
  }

  public removeFavorite(productId: number): void {
    const current = this.favoritesSubject.value.filter(f => !f.product || f.product.id !== productId);
    this.saveFavorites(current);
    this.http.delete<any>(`${this.apiUrl}?userEmail=${this.defaultEmail}&productId=${productId}`).subscribe({ error: () => {} });
  }
}
