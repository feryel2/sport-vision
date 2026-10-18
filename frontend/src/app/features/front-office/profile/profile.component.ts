import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/user.model';
import { AUTH_STYLES, apiErrorMessage, isPasswordValid, passwordChecks } from '../../../core/auth-ui';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="auth-page">
      <div class="auth-card wide sv-card">
        <div class="auth-logo-center">
          <div class="auth-icon">👤</div>
          <h2>Mon Profil</h2>
          <p class="auth-sub" *ngIf="user">{{ user.email }} &bull;
            {{ user.role === 'ROLE_ADMIN' ? 'Administrateur' : 'Utilisateur' }}
            <ng-container *ngIf="user.createdAt"> &bull; Membre depuis le {{ user.createdAt | date:'dd/MM/yyyy' }}</ng-container>
          </p>
        </div>

        <div class="alert-danger" *ngIf="loadError">⚠️ {{ loadError }}</div>

        <form *ngIf="user" (ngSubmit)="saveProfile()" #pf="ngForm" novalidate>
          <h3 class="section-title">Informations personnelles</h3>
          <div class="row-2">
            <div class="form-group-sv">
              <label class="form-label-sv" for="lastName">Nom</label>
              <input id="lastName" type="text" [(ngModel)]="lastName" name="lastName" required maxlength="60" class="form-control-sv">
            </div>
            <div class="form-group-sv">
              <label class="form-label-sv" for="firstName">Prénom</label>
              <input id="firstName" type="text" [(ngModel)]="firstName" name="firstName" required maxlength="60" class="form-control-sv">
            </div>
          </div>
          <div class="row-2">
            <div class="form-group-sv">
              <label class="form-label-sv" for="email">Adresse email</label>
              <input id="email" type="email" [value]="user.email" disabled class="form-control-sv">
            </div>
            <div class="form-group-sv">
              <label class="form-label-sv" for="phone">Téléphone</label>
              <input id="phone" type="tel" [(ngModel)]="phone" name="phone" maxlength="20"
                     pattern="^$|^[+0-9 ().\\-]{6,20}$" class="form-control-sv" placeholder="+33 6 12 34 56 78">
            </div>
          </div>
          <button type="submit" class="btn-sv btn-sv-primary btn-block" [disabled]="savingProfile || pf.invalid">
            {{ savingProfile ? 'Enregistrement...' : 'Enregistrer les modifications' }}
          </button>
          <div class="alert-success" *ngIf="profileMessage">✔ {{ profileMessage }}</div>
          <div class="alert-danger" *ngIf="profileError">⚠️ {{ profileError }}</div>
        </form>

        <form *ngIf="user" (ngSubmit)="savePassword()" class="pwd-form" novalidate>
          <h3 class="section-title">Changer le mot de passe</h3>
          <div class="form-group-sv">
            <label class="form-label-sv" for="current">Mot de passe actuel</label>
            <input id="current" type="password" [(ngModel)]="currentPassword" name="current" autocomplete="current-password" class="form-control-sv">
          </div>
          <div class="form-group-sv">
            <label class="form-label-sv" for="newPwd">Nouveau mot de passe</label>
            <input id="newPwd" type="password" [(ngModel)]="newPassword" name="newPwd" autocomplete="new-password" class="form-control-sv">
            <ul class="pwd-rules" *ngIf="newPassword">
              <li *ngFor="let c of checks" [class.ok]="c.ok">{{ c.label }}</li>
            </ul>
          </div>
          <button type="submit" class="btn-sv btn-sv-outline btn-block"
                  [disabled]="savingPwd || !currentPassword || !isValid(newPassword)">
            {{ savingPwd ? 'Modification...' : 'Modifier le mot de passe' }}
          </button>
          <div class="alert-success" *ngIf="pwdMessage">✔ {{ pwdMessage }}</div>
          <div class="alert-danger" *ngIf="pwdError">⚠️ {{ pwdError }}</div>
        </form>

        <div *ngIf="user" class="danger-zone">
          <h3 class="section-title danger">Zone de danger</h3>
          <p class="auth-sub" *ngIf="!confirmingDeactivate">
            La désactivation de votre compte vous déconnecte et empêche toute nouvelle connexion.
          </p>
          <button *ngIf="!confirmingDeactivate" type="button" class="btn-sv btn-sv-danger btn-block"
                  (click)="confirmingDeactivate = true">Désactiver mon compte</button>

          <form *ngIf="confirmingDeactivate" (ngSubmit)="deactivate()" novalidate>
            <div class="form-group-sv">
              <label class="form-label-sv" for="deactPwd">Confirmez avec votre mot de passe</label>
              <input id="deactPwd" type="password" [(ngModel)]="deactivatePassword" name="deactPwd"
                     autocomplete="current-password" class="form-control-sv">
            </div>
            <div class="row-2">
              <button type="button" class="btn-sv btn-sv-outline" (click)="cancelDeactivate()">Annuler</button>
              <button type="submit" class="btn-sv btn-sv-danger" [disabled]="deactivating || !deactivatePassword">
                {{ deactivating ? 'Désactivation...' : 'Confirmer la désactivation' }}
              </button>
            </div>
          </form>
          <div class="alert-danger" *ngIf="deactivateError">⚠️ {{ deactivateError }}</div>
        </div>
      </div>
    </div>
  `,
  styles: [AUTH_STYLES, `
    .section-title { font-size: 1.05rem; color: #0f172a; margin: 8px 0 14px; }
    .section-title.danger { color: #b91c1c; }
    .pwd-form { margin-top: 28px; padding-top: 20px; border-top: 1px solid #e2e8f0; }
    .danger-zone { margin-top: 28px; padding-top: 20px; border-top: 1px solid #fecaca; }
  `]
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  firstName = '';
  lastName = '';
  phone = '';
  currentPassword = '';
  newPassword = '';

  loadError = '';
  profileMessage = '';
  profileError = '';
  pwdMessage = '';
  pwdError = '';
  savingProfile = false;
  savingPwd = false;
  confirmingDeactivate = false;
  deactivatePassword = '';
  deactivateError = '';
  deactivating = false;

  constructor(private authService: AuthService, private router: Router) {}

  cancelDeactivate(): void {
    this.confirmingDeactivate = false;
    this.deactivatePassword = '';
    this.deactivateError = '';
  }

  deactivate(): void {
    this.deactivating = true;
    this.deactivateError = '';
    this.authService.deactivateAccount(this.deactivatePassword).subscribe({
      next: () => this.router.navigate(['/']),
      error: err => {
        this.deactivating = false;
        this.deactivateError = apiErrorMessage(err);
      }
    });
  }

  ngOnInit(): void {
    this.authService.loadProfile().subscribe({
      next: user => this.applyUser(user),
      error: err => this.loadError = apiErrorMessage(err, 'Impossible de charger votre profil')
    });
  }

  get checks() {
    return passwordChecks(this.newPassword);
  }

  isValid(pwd: string): boolean {
    return isPasswordValid(pwd);
  }

  private applyUser(user: User): void {
    this.user = user;
    this.firstName = user.firstName ?? '';
    this.lastName = user.lastName ?? '';
    this.phone = user.phone ?? '';
  }

  saveProfile(): void {
    this.savingProfile = true;
    this.profileMessage = this.profileError = '';
    this.authService.updateProfile({
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      phone: this.phone.trim()
    }).subscribe({
      next: user => {
        this.savingProfile = false;
        this.applyUser(user);
        this.profileMessage = 'Profil mis à jour';
      },
      error: err => {
        this.savingProfile = false;
        this.profileError = apiErrorMessage(err);
      }
    });
  }

  savePassword(): void {
    this.savingPwd = true;
    this.pwdMessage = this.pwdError = '';
    this.authService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: msg => {
        this.savingPwd = false;
        this.pwdMessage = msg;
        this.currentPassword = this.newPassword = '';
      },
      error: err => {
        this.savingPwd = false;
        this.pwdError = apiErrorMessage(err);
      }
    });
  }
}
