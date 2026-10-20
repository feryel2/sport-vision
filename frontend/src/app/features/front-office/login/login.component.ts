import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { AUTH_STYLES, apiErrorMessage } from '../../../core/auth-ui';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card sv-card">
        <div class="auth-logo-center">
          <div class="auth-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <path d="m9 12 2 2 4-4"/>
            </svg>
          </div>
          <h2>Connexion</h2>
          <p class="auth-sub">Connectez-vous avec votre email et votre mot de passe</p>
        </div>

        <form (ngSubmit)="submit()" #f="ngForm" novalidate>
          <div class="form-group-sv">
            <label class="form-label-sv" for="email">Adresse email</label>
            <input id="email" #emailField="ngModel" type="email" [(ngModel)]="email" name="email" required email
                   (blur)="normalizeEmail()" autocomplete="username" class="form-control-sv" placeholder="vous@exemple.com">
            <div class="field-error" *ngIf="emailField.touched && emailField.errors?.['required']">
              L’adresse email est requise.
            </div>
            <div class="field-error" *ngIf="emailField.touched && emailField.errors?.['email']">
              Entrez une adresse valide, par exemple nom&#64;domaine.com.
            </div>
          </div>

          <div class="form-group-sv">
            <label class="form-label-sv" for="password">Mot de passe</label>
            <input id="password" type="password" [(ngModel)]="password" name="password" required
                   autocomplete="current-password" class="form-control-sv" placeholder="••••••••">
          </div>

          <button type="submit" class="btn-sv btn-sv-primary btn-sv-lg btn-block" [disabled]="loading || f.invalid">
            {{ loading ? 'Connexion...' : 'Se connecter →' }}
          </button>
        </form>

        <div class="alert-danger" *ngIf="errorMessage">⚠️ {{ errorMessage }}</div>

        <div class="auth-links">
          <a routerLink="/forgot-password">Mot de passe oublié ?</a>
          <a routerLink="/register">Créer un compte</a>
        </div>
      </div>
    </div>
  `,
  styles: [AUTH_STYLES]
})
export class LoginComponent {
  email = '';
  password = '';
  errorMessage = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  normalizeEmail(): void {
    this.email = this.email.trim();
  }

  submit(): void {
    if (this.loading || !this.email || !this.password) {
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.authService.login(this.email.trim(), this.password).subscribe({
      next: user => {
        this.loading = false;
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        // Redirection interne uniquement (évite les open-redirects) et réservée aux droits de l'utilisateur
        const returnPath = returnUrl?.split(/[?#]/, 1)[0];
        const authPages = ['/login', '/register', '/forgot-password', '/reset-password'];
        const safe = returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//')
          && !authPages.includes(returnPath!)
          && (user.role === 'ROLE_ADMIN' || !returnPath!.startsWith('/admin'));
        this.router.navigateByUrl(safe ? returnUrl! : user.role === 'ROLE_ADMIN' ? '/admin' : '/');
      },
      error: err => {
        this.loading = false;
        this.errorMessage = apiErrorMessage(err, 'Email ou mot de passe incorrect');
      }
    });
  }
}
