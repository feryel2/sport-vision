import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, map, catchError, of } from 'rxjs';
import { Review } from '../models/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private apiUrl = 'http://localhost:8080/api/reviews';

  private defaultReviews: Review[] = [
    { id: 1, productId: 1, authorName: 'Maxime Dupont', authorEmail: 'maxime@gmail.com', rating: 5, comment: 'Ces bandes élastiques sont excellentes pour chauffer les épaules avant mes séances de bench ! Très résistantes.', createdAt: '2026-09-28T10:00:00' },
    { id: 2, productId: 1, authorName: 'Romain Leclerc', authorEmail: 'romain@gmail.com', rating: 5, comment: 'Indispensable pour mes séances de musculation à la maison. Le lot de 5 permet une progression linéaire parfaite.', createdAt: '2026-09-30T14:30:00' },
    { id: 3, productId: 2, authorName: 'Damien Vallet', authorEmail: 'damien@gmail.com', rating: 5, comment: 'Très bon maintien des poignets. Fini les ampoules lors des tractions et soulevés lourds.', createdAt: '2026-10-01T18:15:00' },
    { id: 4, productId: 3, authorName: 'Alexandre V.', authorEmail: 'alex@gmail.com', rating: 5, comment: 'La ceinture est d\'une robustesse incroyable. Elle cale parfaitement les lombaires lors des séries lourdes au squat.', createdAt: '2026-10-02T11:20:00' },
    { id: 5, productId: 4, authorName: 'Karim B.', authorEmail: 'user@sportvision.com', rating: 5, comment: 'Très bonne miscibilité dans le shaker et goût chocolat au top, sans grumeaux.', createdAt: '2026-10-02T16:45:00' }
  ];

  private reviewsSubject = new BehaviorSubject<Review[]>(this.loadReviews());
  public reviews$ = this.reviewsSubject.asObservable();

  constructor(private http: HttpClient) {
    this.refresh();
  }

  private loadReviews(): Review[] {
    const saved = localStorage.getItem('sport_vision_reviews');
    return saved ? JSON.parse(saved) : this.defaultReviews;
  }

  private saveReviews(reviews: Review[]): void {
    localStorage.setItem('sport_vision_reviews', JSON.stringify(reviews));
    this.reviewsSubject.next(reviews);
  }

  public refresh(): void {
    this.http.get<{ success: boolean; data: Review[] }>(this.apiUrl)
      .pipe(
        map(res => res.data),
        catchError(() => of(this.loadReviews()))
      )
      .subscribe(reviews => {
        if (reviews && reviews.length > 0) {
          this.saveReviews(reviews);
        }
      });
  }

  public getReviewsByProduct(productId: number): Observable<Review[]> {
    return this.reviews$.pipe(
      map(reviews => reviews.filter(r => r.productId === productId || (r.product && r.product.id === productId)))
    );
  }

  public addReview(review: Review): Observable<Review> {
    const newRev: Review = {
      ...review,
      id: Date.now(),
      createdAt: new Date().toISOString()
    };
    this.http.post<any>(this.apiUrl, review).subscribe({ error: () => {} });
    const updated = [newRev, ...this.reviewsSubject.value];
    this.saveReviews(updated);
    return of(newRev);
  }

  public updateReview(id: number, review: Review): Observable<Review> {
    const current = [...this.reviewsSubject.value];
    const index = current.findIndex(r => r.id === id);
    if (index > -1) {
      current[index] = { ...review, id };
      this.saveReviews(current);
      this.http.put<any>(`${this.apiUrl}/${id}`, review).subscribe({ error: () => {} });
      return of(current[index]);
    }
    return of(review);
  }

  public deleteReview(id: number): Observable<boolean> {
    const updated = this.reviewsSubject.value.filter(r => r.id !== id);
    this.saveReviews(updated);
    this.http.delete<any>(`${this.apiUrl}/${id}`).subscribe({ error: () => {} });
    return of(true);
  }
}
