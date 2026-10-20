import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { AUTH_STYLES, apiErrorMessage } from '../../../core/auth-ui';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card sv-card">
        <div class="auth-logo-center">
          <div class="auth-icon">🔑</div>
          <h2>Mot de passe oublié</h2>
          <p class="auth-sub">Saisissez votre email : nous vous enverrons un lien de réinitialisation</p>
        </div>

        <form (ngSubmit)="submit()" #f="ngForm" novalidate>
          <div class="form-group-sv">
            <label class="form-label-sv" for="email">Adresse email</label>
            <input id="email" type="email" [(ngModel)]="email" name="email" required email
                   autocomplete="email" class="form-control-sv" placeholder="vous@exemple.com">
          </div>
          <button type="submit" class="btn-sv btn-sv-primary btn-sv-lg btn-block" [disabled]="loading || f.invalid">
            {{ loading ? 'Envoi...' : 'Envoyer le lien' }}
          </button>
        </form>

        <div class="alert-success" *ngIf="message">✔ {{ message }}</div>
        <div class="alert-danger" *ngIf="errorMessage">⚠️ {{ errorMessage }}</div>

        <div class="auth-links">
          <a routerLink="/login">← Retour à la connexion</a>
        </div>
      </div>
    </div>
  `,
  styles: [AUTH_STYLES]
})
export class ForgotPasswordComponent {
  email = '';
  message = '';
  errorMessage = '';
  loading = false;

  constructor(private authService: AuthService) {}

  submit(): void {
    if (this.loading || !this.email) {
      return;
    }
    this.loading = true;
    this.message = '';
    this.errorMessage = '';
    this.authService.forgotPassword(this.email.trim()).subscribe({
      next: msg => {
        this.loading = false;
        this.message = msg;
      },
      error: err => {
        this.loading = false;
        this.errorMessage = apiErrorMessage(err);
      }
    });
  }
}
