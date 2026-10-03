import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AiService } from '../../../core/services/ai.service';
import { CoachMatchingRequest, CoachMatchingResult } from '../../../core/models/ai.model';

@Component({
  selector: 'app-coach-matching',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="matching-page">
      <!-- HEADER -->
      <section class="matching-header">
        <div class="container">
          <div class="ai-pill">
            <span>PROGRAMME D'ENCADREMENT SPORTIF</span>
          </div>
          <h1 class="page-title">Matching Sportif &harr; Coach Personnalisé</h1>
          <p class="page-subtitle">
            Sélectionnez votre discipline, vos objectifs de progression, votre niveau et vos créneaux pour être orienté vers l'entraîneur diplômé idéal.
          </p>
        </div>
      </section>

      <div class="container matching-body">
        <!-- FORMULAIRE -->
        <div class="sv-card filter-card">
          <div class="card-header-row">
            <div>
              <h2 class="card-title">Définissez votre profil sportif</h2>
              <p class="card-subtitle">Critères analysés en direct avec notre réseau de coachs certifiés.</p>
            </div>
            <!-- Preset rapide cas du sujet -->
            <button (click)="applyExamplePreset()" class="btn-preset">
              🎯 Exemple du sujet : Musculation + Prise de masse + Débutant
            </button>
          </div>

          <div class="form-grid">
            <!-- 1. Sport -->
            <div class="form-group-sv">
              <label class="form-label-sv">Discipline sportive</label>
              <select [(ngModel)]="request.sport" class="form-control-sv">
                <option value="Musculation">Musculation & Force</option>
                <option value="Running">Running & Course à pied</option>
                <option value="Fitness">Fitness & Remise en forme</option>
                <option value="Cyclisme">Cyclisme sur route & Vélo</option>
              </select>
            </div>

            <!-- 2. Objectif -->
            <div class="form-group-sv">
              <label class="form-label-sv">Objectif principal</label>
              <select [(ngModel)]="request.goal" class="form-control-sv">
                <option value="Prise de masse">Prise de masse musculaire</option>
                <option value="Force & Hypertrophie">Force & Hypertrophie</option>
                <option value="Perte de poids">Perte de poids & Sèche</option>
                <option value="Marathon & Endurance">Marathon & Endurance</option>
                <option value="Remise en forme">Remise en forme & Mobilité</option>
              </select>
            </div>

            <!-- 3. Niveau -->
            <div class="form-group-sv">
              <label class="form-label-sv">Niveau actuel</label>
              <select [(ngModel)]="request.level" class="form-control-sv">
                <option value="Débutant">Débutant (Démarrage / Apprentissage)</option>
                <option value="Intermédiaire">Intermédiaire (Pratique régulière)</option>
                <option value="Avancé">Avancé (Compétition & Performance)</option>
              </select>
            </div>

            <!-- 4. Disponibilité -->
            <div class="form-group-sv">
              <label class="form-label-sv">Disponibilité horaire</label>
              <select [(ngModel)]="request.availability" class="form-control-sv">
                <option value="Soir">En soirée (après 18h)</option>
                <option value="Matin">Tôt le matin (6h - 9h)</option>
                <option value="Weekend">Les weekends</option>
                <option value="Flexible">Horaires flexibles</option>
              </select>
            </div>
          </div>

          <div class="form-actions">
            <button (click)="runMatching()" class="btn-sv btn-sv-primary btn-sv-lg" [disabled]="loading">
              <span *ngIf="!loading">Rechercher les Coachs Compatibles</span>
              <span *ngIf="loading">Recherche en cours...</span>
            </button>
          </div>
        </div>

        <!-- RÉSULTATS DU MATCHING -->
        <div class="results-section" *ngIf="results.length > 0">
          <div class="results-header">
            <h3>Coachs Partenaires Recommandés ({{ results.length }})</h3>
            <span class="badge-sv badge-blue">Classés par indice de compatibilité</span>
          </div>

          <div class="coaches-grid">
            <div *ngFor="let res of results" class="sv-card coach-card" [class.best-match]="res.matchScore >= 90">
              <!-- Score Badge -->
              <div class="match-badge-bar">
                <div class="score-pill" [class.perfect]="res.matchScore >= 90">
                  <span class="score-num">{{ res.matchScore }}%</span>
                  <span class="score-text">{{ res.compatibilityBadge }}</span>
                </div>
                <span class="rate-tag">{{ res.coach.hourlyRate }} € / heure</span>
              </div>

              <!-- Coach Info -->
              <div class="coach-profile">
                <img [src]="res.coach.imageUrl" [alt]="res.coach.fullName" class="coach-avatar">
                <div class="coach-meta">
                  <h4 class="coach-name">{{ res.coach.fullName }}</h4>
                  <span class="coach-spec">{{ res.coach.specialty }}</span>
                  <div class="coach-submeta">
                    <span class="coach-sport-pill">🏅 {{ res.coach.sport }}</span>
                    <span class="coach-stars">★ {{ res.coach.rating }}</span>
                    <span class="coach-dispo">🕒 {{ res.coach.availability }}</span>
                  </div>
                </div>
              </div>

              <p class="coach-bio">{{ res.coach.bio }}</p>

              <!-- Raisons du matching IA -->
              <div class="ai-reasons-box">
                <span class="reasons-title">💡 Pourquoi l'IA vous recommande ce coach :</span>
                <ul class="reasons-list">
                  <li *ngFor="let reason of res.matchReasons">
                    <span class="bullet">✓</span> {{ reason }}
                  </li>
                </ul>
              </div>

              <!-- Action -->
              <div class="coach-footer">
                <button (click)="bookSession(res.coach)" class="btn-sv btn-sv-primary btn-block">
                  Prendre Rendez-vous avec {{ res.coach.fullName.split(' ')[0] }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Notification de réservation -->
        <div class="booking-modal" *ngIf="bookedCoach">
          <div class="modal-backdrop" (click)="bookedCoach = null"></div>
          <div class="modal-box sv-card">
            <div class="modal-icon">✅</div>
            <h3>Demande transmise avec succès !</h3>
            <p>
              Votre demande de mise en relation avec <strong>{{ bookedCoach.fullName }}</strong> ({{ bookedCoach.specialty }}) a été enregistrée.
            </p>
            <p class="modal-sub">
              Le coach vous contactera sous 24h ouvrées aux coordonnées de votre compte athlète.
            </p>
            <button (click)="bookedCoach = null" class="btn-sv btn-sv-primary">Fermer</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
    }
    .matching-header {
      background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0284c7 100%);
      color: white;
      padding: 50px 0 45px;
      text-align: center;
    }
    .ai-pill {
      display: inline-block;
      background: rgba(2, 132, 199, 0.4);
      border: 1px solid #38bdf8;
      color: #e0f2fe;
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

    .matching-body {
      padding: 40px 20px 60px;
    }

    .filter-card {
      padding: 30px;
      margin-bottom: 40px;
    }
    .card-header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 15px;
      margin-bottom: 25px;
      border-bottom: 1px solid #f1f5f9;
      padding-bottom: 20px;
    }
    .card-title {
      font-size: 1.4rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 4px;
    }
    .card-subtitle {
      font-size: 0.9rem;
      color: #64748b;
      margin: 0;
    }
    .btn-preset {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      color: #1e3a8a;
      font-weight: 700;
      font-size: 0.85rem;
      padding: 8px 14px;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.15s ease;
    }
    .btn-preset:hover {
      background: #dbeafe;
    }

    .form-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 20px;
      margin-bottom: 25px;
    }

    .form-actions {
      display: flex;
      justify-content: center;
    }

    .results-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 25px;
    }
    .results-header h3 {
      font-size: 1.5rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0;
    }

    .coaches-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
      gap: 25px;
    }

    .coach-card {
      padding: 24px;
      display: flex;
      flex-direction: column;
      position: relative;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }
    .coach-card.best-match {
      border: 2px solid #38bdf8;
      box-shadow: 0 10px 25px -5px rgba(2, 132, 199, 0.2);
    }

    .match-badge-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 18px;
    }
    .score-pill {
      background: #f1f5f9;
      color: #334155;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 0.85rem;
      font-weight: 800;
      display: flex;
      gap: 6px;
      align-items: center;
    }
    .score-pill.perfect {
      background: #0284c7;
      color: white;
    }
    .rate-tag {
      font-weight: 800;
      color: #1e3a8a;
      font-size: 1.05rem;
    }

    .coach-profile {
      display: flex;
      gap: 16px;
      align-items: center;
      margin-bottom: 16px;
    }
    .coach-avatar {
      width: 68px;
      height: 68px;
      border-radius: 50%;
      object-fit: cover;
      border: 3px solid #e0f2fe;
    }
    .coach-name {
      font-size: 1.2rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 2px;
    }
    .coach-spec {
      font-size: 0.85rem;
      color: #0284c7;
      font-weight: 700;
      display: block;
      margin-bottom: 6px;
    }
    .coach-submeta {
      display: flex;
      gap: 8px;
      font-size: 0.78rem;
      color: #64748b;
      align-items: center;
      flex-wrap: wrap;
    }
    .coach-sport-pill {
      background: #f8fafc;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 600;
    }
    .coach-stars {
      color: #eab308;
      font-weight: 700;
    }

    .coach-bio {
      font-size: 0.88rem;
      line-height: 1.5;
      color: #475569;
      margin: 0 0 16px;
      flex-grow: 1;
    }

    .ai-reasons-box {
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-radius: 8px;
      padding: 12px 14px;
      margin-bottom: 18px;
    }
    .reasons-title {
      font-size: 0.8rem;
      font-weight: 800;
      color: #166534;
      display: block;
      margin-bottom: 6px;
    }
    .reasons-list {
      list-style: none;
      padding: 0;
      margin: 0;
    }
    .reasons-list li {
      font-size: 0.82rem;
      color: #14532d;
      margin-bottom: 4px;
      display: flex;
      gap: 6px;
    }
    .bullet {
      font-weight: 900;
      color: #16a34a;
    }

    .coach-footer {
      margin-top: auto;
    }

    /* Modal */
    .booking-modal {
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
    .modal-box {
      position: relative;
      background: white;
      padding: 35px 30px;
      border-radius: 16px;
      max-width: 480px;
      width: 90%;
      text-align: center;
      z-index: 10;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
    }
    .modal-icon {
      font-size: 3rem;
      margin-bottom: 12px;
    }
    .modal-box h3 {
      font-size: 1.35rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 10px;
    }
    .modal-box p {
      font-size: 0.95rem;
      color: #475569;
      line-height: 1.5;
      margin-bottom: 8px;
    }
    .modal-sub {
      font-size: 0.85rem !important;
      color: #64748b !important;
      margin-bottom: 20px !important;
    }
  `]
})
export class CoachMatchingComponent implements OnInit {
  request: CoachMatchingRequest = {
    sport: 'Musculation',
    goal: 'Prise de masse',
    level: 'Débutant',
    availability: 'Soir'
  };

  results: CoachMatchingResult[] = [];
  loading = false;
  bookedCoach: any = null;

  constructor(private aiService: AiService) {}

  ngOnInit(): void {
    this.runMatching();
  }

  applyExamplePreset(): void {
    this.request = {
      sport: 'Musculation',
      goal: 'Prise de masse',
      level: 'Débutant',
      availability: 'Soir'
    };
    this.runMatching();
  }

  runMatching(): void {
    this.loading = true;
    this.aiService.matchCoaches(this.request).subscribe({
      next: (res) => {
        this.results = res;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  bookSession(coach: any): void {
    this.bookedCoach = coach;
  }
}
