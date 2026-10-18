import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { AUTH_STYLES, apiErrorMessage, isPasswordValid, passwordChecks } from '../../../core/auth-ui';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card sv-card">
        <div class="auth-logo-center">
          <div class="auth-icon">🔒</div>
          <h2>Nouveau mot de passe</h2>
          <p class="auth-sub">Choisissez un nouveau mot de passe sécurisé</p>
        </div>

        <div class="alert-danger" *ngIf="!token">
          ⚠️ Lien invalide. <a routerLink="/forgot-password">Demander un nouveau lien</a>
        </div>

        <form *ngIf="token && !done" (ngSubmit)="submit()" #f="ngForm" novalidate>
          <div class="form-group-sv">
            <label class="form-label-sv" for="password">Nouveau mot de passe</label>
            <input id="password" type="password" [(ngModel)]="password" name="password" required
                   autocomplete="new-password" class="form-control-sv">
            <ul class="pwd-rules">
              <li *ngFor="let c of checks" [class.ok]="c.ok">{{ c.label }}</li>
            </ul>
          </div>

          <div class="form-group-sv">
            <label class="form-label-sv" for="confirm">Confirmer le mot de passe</label>
            <input id="confirm" type="password" [(ngModel)]="confirm" name="confirm" required
                   autocomplete="new-password" class="form-control-sv">
            <div class="field-error" *ngIf="confirm && confirm !== password">Les mots de passe ne correspondent pas</div>
          </div>

          <button type="submit" class="btn-sv btn-sv-primary btn-sv-lg btn-block" [disabled]="loading || !valid">
            {{ loading ? 'Enregistrement...' : 'Réinitialiser le mot de passe' }}
          </button>
        </form>

        <div class="alert-success" *ngIf="done">✔ {{ message }}</div>
        <div class="alert-danger" *ngIf="errorMessage">
          ⚠️ {{ errorMessage }} <a routerLink="/forgot-password">Demander un nouveau lien</a>
        </div>

        <div class="auth-links">
          <a routerLink="/login">← Aller à la connexion</a>
        </div>
      </div>
    </div>
  `,
  styles: [AUTH_STYLES]
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  password = '';
  confirm = '';
  message = '';
  errorMessage = '';
  loading = false;
  done = false;

  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    // Retire le jeton de l'URL (historique, Referer) une fois lu
    if (this.token) {
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
    }
  }

  get checks() {
    return passwordChecks(this.password);
  }

  get valid(): boolean {
    return isPasswordValid(this.password) && this.password === this.confirm;
  }

  submit(): void {
    if (this.loading || !this.valid) {
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.authService.resetPassword(this.token, this.password).subscribe({
      next: msg => {
        this.loading = false;
        this.done = true;
        this.message = msg;
        setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: err => {
        this.loading = false;
        this.errorMessage = apiErrorMessage(err);
      }
    });
  }
}
