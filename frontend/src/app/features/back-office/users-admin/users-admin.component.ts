import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { User, Role } from '../../../core/models/user.model';

@Component({
  selector: 'app-users-admin',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="admin-users-page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Gestion des Utilisateurs & Rôles</h1>
          <p class="page-subtitle">Consultez les membres enregistrés, administrateurs et athlètes du club Sport Vision.</p>
        </div>
      </div>

      <div class="sv-table-container">
        <table class="sv-table">
          <thead>
            <tr>
              <th>Utilisateur</th>
              <th>Adresse Email</th>
              <th>Téléphone</th>
              <th>Rôle Système</th>
              <th>Statut Compte</th>
              <th class="text-right">Action Rôle</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let u of users">
              <td>
                <div class="user-cell">
                  <div class="user-avatar">{{ getInitials(u.fullName) }}</div>
                  <div>
                    <strong class="user-name">{{ u.fullName }}</strong>
                    <div class="user-id">ID: #{{ u.id }}</div>
                  </div>
                </div>
              </td>
              <td>{{ u.email }}</td>
              <td>{{ u.phone || 'Non renseigné' }}</td>
              <td>
                <span class="badge-sv" [class.badge-blue]="u.role === 'ROLE_ADMIN'" [class.badge-white]="u.role === 'ROLE_USER'">
                  {{ u.role === 'ROLE_ADMIN' ? '👑 Administrateur' : '👤 Athlète Client' }}
                </span>
              </td>
              <td>
                <span class="badge-sv badge-success">● Actif</span>
              </td>
              <td class="text-right">
                <button (click)="toggleRole(u)" class="btn-sv btn-sv-outline btn-sv-sm">
                  Basculer en {{ u.role === 'ROLE_ADMIN' ? 'Client' : 'Admin' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .admin-users-page { max-width: 1350px; margin: 0 auto; }
    .page-header { margin-bottom: 24px; }
    .page-title { font-size: 1.85rem; color: #0f172a; }
    .page-subtitle { font-size: 0.9rem; color: #64748b; margin-top: 4px; }
    .user-cell { display: flex; align-items: center; gap: 12px; }
    .user-avatar {
      width: 36px; height: 36px; border-radius: 50%;
      background: #eff6ff; color: #1d4ed8; font-weight: 700;
      display: flex; align-items: center; justify-content: center;
      border: 1.5px solid #bfdbfe; font-size: 0.85rem;
    }
    .user-name { color: #0f172a; }
    .user-id { font-size: 0.75rem; color: #94a3b8; }
  `]
})
export class UsersAdminComponent {
  users: User[] = [
    {
      id: 1,
      email: 'admin@sportvision.com',
      fullName: 'Alexandre Laurent',
      phone: '+33 6 12 34 56 78',
      role: 'ROLE_ADMIN'
    },
    {
      id: 2,
      email: 'client@sportvision.com',
      fullName: 'Sophie Martin',
      phone: '+33 6 98 76 54 32',
      role: 'ROLE_USER'
    },
    {
      id: 3,
      email: 'marc.vasseur@club-cycliste.fr',
      fullName: 'Marc Vasseur',
      phone: '+33 6 44 22 11 00',
      role: 'ROLE_USER'
    },
    {
      id: 4,
      email: 'camille.dupont@orange.fr',
      fullName: 'Camille Dupont',
      phone: '+33 7 88 99 00 11',
      role: 'ROLE_USER'
    }
  ];

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  }

  toggleRole(user: User): void {
    user.role = user.role === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN';
  }
}
