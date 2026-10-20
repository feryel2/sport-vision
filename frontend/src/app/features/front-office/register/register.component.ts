import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { AUTH_STYLES, apiErrorMessage, isPasswordValid, passwordChecks } from '../../../core/auth-ui';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card sv-card">
        <div class="auth-logo-center">
          <div class="auth-icon">👤</div>
          <h2>Créer un compte</h2>
          <p class="auth-sub">Rejoignez Sport Vision en quelques secondes</p>
        </div>

        <form (ngSubmit)="submit(f.valid)" #f="ngForm" novalidate>
          <div class="row-2">
            <div class="form-group-sv">
              <label class="form-label-sv" for="lastName">Nom</label>
              <input id="lastName" #lastNameCtl="ngModel" type="text" [(ngModel)]="lastName" name="lastName" required maxlength="60"
                     autocomplete="family-name" class="form-control-sv" placeholder="Martin">
              <div class="field-error" *ngIf="(lastNameCtl.touched || submitted) && !lastName.trim()">Le nom est requis.</div>
            </div>
            <div class="form-group-sv">
              <label class="form-label-sv" for="firstName">Prénom</label>
              <input id="firstName" #firstNameCtl="ngModel" type="text" [(ngModel)]="firstName" name="firstName" required maxlength="60"
                     autocomplete="given-name" class="form-control-sv" placeholder="Sophie">
              <div class="field-error" *ngIf="(firstNameCtl.touched || submitted) && !firstName.trim()">Le prénom est requis.</div>
            </div>
          </div>

          <div class="form-group-sv">
            <label class="form-label-sv" for="email">Adresse email</label>
            <input id="email" type="email" [(ngModel)]="email" name="email" required email #emailCtl="ngModel"
                   (blur)="normalizeEmail()" autocomplete="email" class="form-control-sv" placeholder="vous@exemple.com">
            <div class="field-error" *ngIf="(emailCtl.touched || submitted) && emailCtl.errors?.['required']">L’adresse email est requise.</div>
            <div class="field-error" *ngIf="(emailCtl.touched || submitted) && emailCtl.errors?.['email']">Entrez une adresse valide, par exemple nom&#64;domaine.com.</div>
          </div>

          <div class="form-group-sv">
            <label class="form-label-sv" for="password">Mot de passe</label>
            <input id="password" type="password" [(ngModel)]="password" name="password" required
                   autocomplete="new-password" class="form-control-sv" placeholder="Mot de passe sécurisé">
            <ul class="pwd-rules">
              <li *ngFor="let c of checks" [class.ok]="c.ok">{{ c.label }}</li>
            </ul>
            <div class="field-error" *ngIf="submitted && !isPasswordValid(password)">Le mot de passe doit respecter toutes les règles affichées.</div>
          </div>

          <div class="form-group-sv">
            <label class="form-label-sv" for="confirm">Confirmer le mot de passe</label>
            <input id="confirm" type="password" [(ngModel)]="confirm" name="confirm" required
                   autocomplete="new-password" class="form-control-sv" placeholder="Répétez le mot de passe">
            <div class="field-error" *ngIf="(confirm || submitted) && confirm !== password">Les mots de passe ne correspondent pas.</div>
          </div>

          <button type="submit" class="btn-sv btn-sv-primary btn-sv-lg btn-block" [disabled]="loading">
            {{ loading ? 'Création...' : 'Créer mon compte →' }}
          </button>
        </form>

        <div class="alert-danger" *ngIf="errorMessage">⚠️ {{ errorMessage }}</div>

        <div class="auth-links">
          <span>Déjà inscrit ?</span>
          <a routerLink="/login">Se connecter</a>
        </div>
      </div>
    </div>
  `,
  styles: [AUTH_STYLES]
})
export class RegisterComponent {
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  confirm = '';
  errorMessage = '';
  loading = false;
  submitted = false;

  constructor(private authService: AuthService, private router: Router) {}

  get checks() {
    return passwordChecks(this.password);
  }

  isPasswordValid(password: string): boolean {
    return isPasswordValid(password);
  }

  normalizeEmail(): void {
    this.email = this.email.trim();
  }

  formValid(ngValid: boolean | null): boolean {
    return !!ngValid && isPasswordValid(this.password) && this.password === this.confirm
      && !!this.firstName.trim() && !!this.lastName.trim();
  }

  submit(ngValid: boolean | null): void {
    if (this.loading) {
      return;
    }
    this.submitted = true;
    if (!this.formValid(ngValid)) return;

    this.loading = true;
    this.errorMessage = '';
    this.authService.register({
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      email: this.email.trim(),
      password: this.password
    }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/catalogue']);
      },
      error: err => {
        this.loading = false;
        this.errorMessage = apiErrorMessage(err);
      }
    });
  }
}
