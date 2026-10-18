import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card sv-card">
        <!-- Logo -->
        <div class="auth-logo-center">
          <div class="auth-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <path d="m9 12 2 2 4-4"/>
            </svg>
          </div>
          <h2>Espace Sport Vision</h2>
          <p class="auth-sub">Connectez-vous à votre espace membre ou console d'administration</p>
        </div>

        <!-- Boutons Démo 1-Clic -->
        <div class="demo-buttons-box">
          <span class="demo-box-title">COMPTES DE DÉMONSTRATION :</span>
          <div class="demo-btns-row">
            <button (click)="fillAdmin()" class="btn-demo btn-demo-admin">
              Compte Administrateur
            </button>
            <button (click)="fillClient()" class="btn-demo btn-demo-client">
              Compte Client Démo
            </button>
          </div>
        </div>

        <!-- Onglets Connexion / Inscription -->
        <div class="auth-tabs">
          <button (click)="isRegister = false" class="auth-tab" [class.active]="!isRegister">Connexion</button>
          <button (click)="isRegister = true" class="auth-tab" [class.active]="isRegister">Créer un compte</button>
        </div>

        <!-- Formulaire de Connexion -->
        <form *ngIf="!isRegister" (ngSubmit)="submitLogin()">
          <div class="form-group-sv">
            <label class="form-label-sv">Adresse Email</label>
            <input type="email" [(ngModel)]="email" name="email" required class="form-control-sv" placeholder="admin@sportvision.com">
          </div>

          <div class="form-group-sv">
            <label class="form-label-sv">Mot de passe</label>
            <input type="password" [(ngModel)]="password" name="password" required class="form-control-sv" placeholder="••••••••">
          </div>

          <button type="submit" class="btn-sv btn-sv-primary btn-sv-lg btn-block">
            Se Connecter &rarr;
          </button>
        </form>

        <!-- Formulaire d'Inscription -->
        <form *ngIf="isRegister" (ngSubmit)="submitRegister()">
          <div class="form-group-sv">
            <label class="form-label-sv">Nom complet</label>
            <input type="text" [(ngModel)]="fullName" name="fullName" required class="form-control-sv" placeholder="Sophie Martin">
          </div>

          <div class="form-group-sv">
            <label class="form-label-sv">Adresse Email</label>
            <input type="email" [(ngModel)]="email" name="email" required class="form-control-sv" placeholder="athlete@sportvision.com">
          </div>

          <div class="form-group-sv">
            <label class="form-label-sv">Mot de passe</label>
            <input type="password" [(ngModel)]="password" name="password" required class="form-control-sv" placeholder="Minimum 6 caractères">
          </div>

          <button type="submit" class="btn-sv btn-sv-primary btn-sv-lg btn-block">
            Créer mon compte Athlète &rarr;
          </button>
        </form>

        <!-- Message d'erreur -->
        <div class="alert-danger" *ngIf="errorMessage">
          ⚠️ {{ errorMessage }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      min-height: calc(100vh - 200px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px 20px;
      background: radial-gradient(circle at 50% 20%, #eff6ff 0%, #f8fafc 100%);
    }
    .auth-card {
      width: 100%;
      max-width: 480px;
      padding: 36px;
      box-shadow: 0 20px 45px -10px rgba(29, 78, 216, 0.15);
    }
    .auth-logo-center {
      text-align: center;
      margin-bottom: 24px;
    }
    .auth-icon {
      width: 50px;
      height: 50px;
      background: linear-gradient(135deg, #1d4ed8 0%, #0284c7 100%);
      color: white;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 12px;
      box-shadow: 0 6px 16px rgba(29, 78, 216, 0.3);
    }
    .auth-logo-center h2 {
      font-size: 1.5rem;
      color: #0f172a;
    }
    .auth-sub {
      font-size: 0.85rem;
      color: #64748b;
      margin-top: 4px;
    }

    /* Box démo */
    .demo-buttons-box {
      background: #eff6ff;
      border: 1.5px dashed #93c5fd;
      border-radius: 12px;
      padding: 14px;
      margin-bottom: 24px;
      text-align: center;
    }
    .demo-box-title {
      display: block;
      font-size: 0.72rem;
      font-weight: 800;
      letter-spacing: 0.5px;
      color: #1e40af;
      margin-bottom: 10px;
    }
    .demo-btns-row {
      display: flex;
      gap: 10px;
    }
    .btn-demo {
      flex: 1;
      padding: 8px;
      border-radius: 8px;
      font-size: 0.8rem;
      font-weight: 700;
      cursor: pointer;
      border: 1px solid transparent;
      transition: all 0.2s;
    }
    .btn-demo-admin {
      background: #1e293b;
      color: #38bdf8;
    }
    .btn-demo-admin:hover {
      background: #0f172a;
    }
    .btn-demo-client {
      background: #1d4ed8;
      color: white;
    }
    .btn-demo-client:hover {
      background: #1e40af;
    }

    /* Tabs */
    .auth-tabs {
      display: flex;
      background: #f1f5f9;
      border-radius: 10px;
      padding: 4px;
      margin-bottom: 20px;
    }
    .auth-tab {
      flex: 1;
      padding: 8px;
      border: none;
      background: transparent;
      border-radius: 8px;
      font-family: inherit;
      font-size: 0.88rem;
      font-weight: 600;
      color: #64748b;
      cursor: pointer;
      transition: all 0.2s;
    }
    .auth-tab.active {
      background: white;
      color: #1d4ed8;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }
    .btn-block {
      width: 100%;
      margin-top: 10px;
    }
    .alert-danger {
      background: #fee2e2;
      color: #b91c1c;
      padding: 12px;
      border-radius: 8px;
      font-size: 0.85rem;
      margin-top: 16px;
    }
  `]
})
export class LoginComponent {
  isRegister = false;
  email = 'admin@sportvision.com';
  password = '••••••••';
  fullName = '';
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  private openAdminDestination(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    this.router.navigateByUrl(returnUrl?.startsWith('/admin') ? returnUrl : '/admin');
  }

  fillAdmin(): void {
    this.email = 'admin@sportvision.com';
    this.password = 'admin123';
    this.authService.loginAsAdmin();
    this.openAdminDestination();
  }

  fillClient(): void {
    this.email = 'client@sportvision.com';
    this.password = 'client123';
    this.authService.loginAsClient();
    this.router.navigate(['/catalogue']);
  }

  submitLogin(): void {
    if (this.email.includes('admin')) {
      this.authService.loginAsAdmin();
      this.openAdminDestination();
    } else {
      this.authService.loginAsClient();
      this.router.navigate(['/catalogue']);
    }
  }

  submitRegister(): void {
    this.authService.login(this.email, 'ROLE_USER');
    this.router.navigate(['/catalogue']);
  }
}
