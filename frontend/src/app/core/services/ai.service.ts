import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { CoachMatchingRequest, CoachMatchingResult, ProductRecommendation, SalesForecast } from '../models/ai.model';
import { Coach } from '../models/coach.model';

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private apiUrl = '/api/ai';

  private fallbackCoaches: Coach[] = [
    {
      id: 1,
      fullName: 'Marc Vauthier',
      specialty: 'Musculation, Prise de masse & Hypertrophie',
      sport: 'Musculation',
      targetLevel: 'Débutant',
      availability: 'Soir',
      rating: 4.95,
      hourlyRate: 45,
      bio: 'Coach certifié BPJEPS Haltérophilie et Musculation. Expert en prise de masse propre pour débutants et correction posturale.',
      imageUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=500&q=80',
      email: 'marc.vauthier@sportvision.com',
      phone: '+33 6 55 44 33 22'
    },
    {
      id: 2,
      fullName: 'Sarah Benali',
      specialty: 'Running, Marathon & Cardio',
      sport: 'Running',
      targetLevel: 'Tous niveaux',
      availability: 'Matin',
      rating: 5.0,
      hourlyRate: 50,
      bio: 'Ancienne coureuse d\'élite de demi-fond. Accompagne les athlètes sur 10km, semi et marathon avec analyse vidéo de la foulée.',
      imageUrl: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=500&q=80',
      email: 'sarah.benali@sportvision.com',
      phone: '+33 6 44 33 22 11'
    },
    {
      id: 3,
      fullName: 'Thomas Morel',
      specialty: 'Force Athlétique & Powerlifting',
      sport: 'Musculation',
      targetLevel: 'Intermédiaire',
      availability: 'Weekend',
      rating: 4.85,
      hourlyRate: 55,
      bio: 'Compétiteur de Powerlifting. Optimisation des trois mouvements fondamentaux (Squat, Bench, Deadlift) en sécurité.',
      imageUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=500&q=80',
      email: 'thomas.morel@sportvision.com',
      phone: '+33 6 33 22 11 00'
    },
    {
      id: 4,
      fullName: 'Julie Laurent',
      specialty: 'Fitness, Remise en forme & Perte de poids',
      sport: 'Fitness',
      targetLevel: 'Débutant',
      availability: 'Flexible',
      rating: 4.9,
      hourlyRate: 40,
      bio: 'Instructrice Pilates et fitness fonctionnel. Programmation ciblée pour perte de masse grasse et silhouette affinée.',
      imageUrl: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=500&q=80',
      email: 'julie.laurent@sportvision.com',
      phone: '+33 6 22 11 00 99'
    }
  ];

  constructor(private http: HttpClient) {}

  public getProductRecommendations(goal: string = 'Musculation', userEmail: string = 'user@sportvision.com'): Observable<ProductRecommendation[]> {
    return this.http.get<{ success: boolean; data: ProductRecommendation[] }>(
      `${this.apiUrl}/recommendations?goal=${encodeURIComponent(goal)}&userEmail=${encodeURIComponent(userEmail)}`
    ).pipe(
      map(res => res.data),
      catchError(() => of([]))
    );
  }

  public getSalesForecast(): Observable<SalesForecast[]> {
    return this.http.get<{ success: boolean; data: SalesForecast[] }>(
      `${this.apiUrl}/sales-forecast`
    ).pipe(
      map(res => res.data),
      catchError(() => of<SalesForecast[]>([
        {
          productId: 1,
          productName: 'Bandes élastiques de résistance',
          categoryName: 'Musculation & Force',
          currentStock: 45,
          predictedSalesNext30Days: 32,
          recommendedReorderQuantity: 0,
          stockHealthStatus: 'OPTIMAL',
          confidenceScore: 0.94,
          trend: 'HAUSSE',
          reason: 'Forte demande soutenue par la rentrée sportive. Stock sain.'
        },
        {
          productId: 5,
          productName: 'Haltères hexagonaux 20kg',
          categoryName: 'Musculation & Force',
          currentStock: 3,
          predictedSalesNext30Days: 18,
          recommendedReorderQuantity: 22,
          stockHealthStatus: 'CRITIQUE',
          confidenceScore: 0.91,
          trend: 'HAUSSE',
          reason: 'Rupture imminente dans moins de 5 jours ouvrés ! Commander d\'urgence.'
        },
        {
          productId: 7,
          productName: 'Montre GPS Forerunner Cardio',
          categoryName: 'Accessoires & Électronique',
          currentStock: 6,
          predictedSalesNext30Days: 14,
          recommendedReorderQuantity: 15,
          stockHealthStatus: 'RISQUE_RUPTURE',
          confidenceScore: 0.89,
          trend: 'HAUSSE',
          reason: 'Vitesse de vente supérieure au réapprovisionnement moyen.'
        }
      ]))
    );
  }

  public matchCoaches(request: CoachMatchingRequest): Observable<CoachMatchingResult[]> {
    return this.http.post<{ success: boolean; data: CoachMatchingResult[] }>(
      `${this.apiUrl}/coach-matching`, request
    ).pipe(
      map(res => res.data),
      catchError(() => {
        // Fallback local heuristic
        const results: CoachMatchingResult[] = this.fallbackCoaches.map(c => {
          let score = 75;
          const reasons = [];
          if (c.sport.toLowerCase().includes(request.sport.toLowerCase())) {
            score += 15;
            reasons.push(`Spécialiste en ${c.sport}`);
          }
          if (c.specialty.toLowerCase().includes(request.goal.toLowerCase())) {
            score += 8;
            reasons.push(`Expert en ${request.goal}`);
          }
          score = Math.min(98, score);
          return {
            coach: c,
            matchScore: score,
            compatibilityBadge: score >= 90 ? `Match Parfait (${score}%)` : `Excellente Compatibilité (${score}%)`,
            matchReasons: reasons.length ? reasons : ['Coach certifié et hautement recommandé']
          };
        });
        results.sort((a, b) => b.matchScore - a.matchScore);
        return of(results);
      })
    );
  }

  public getAllCoaches(): Observable<Coach[]> {
    return this.http.get<{ success: boolean; data: Coach[] }>(`${this.apiUrl}/coaches`).pipe(
      map(res => res.data),
      catchError(() => of(this.fallbackCoaches))
    );
  }
}
