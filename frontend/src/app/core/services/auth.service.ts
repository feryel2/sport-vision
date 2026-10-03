import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { AuthResponse, Role, User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<User | null>(this.loadUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor() {}

  private loadUserFromStorage(): User | null {
    const raw = localStorage.getItem('sv_user');
    if (raw) {
      try {
        return JSON.parse(raw);
      } catch (e) {
        return null;
      }
    }
    // Par défaut, profil client démo pré-connecté pour une expérience immédiate sans friction
    const defaultUser: User = {
      id: 1,
      email: 'client@sportvision.com',
      fullName: 'Sophie Martin',
      role: 'ROLE_USER'
    };
    return defaultUser;
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  public get isAdmin(): boolean {
    return this.currentUserSubject.value?.role === 'ROLE_ADMIN';
  }

  public loginAsAdmin(): void {
    const admin: User = {
      id: 99,
      email: 'admin@sportvision.com',
      fullName: 'Alexandre Laurent (Directeur)',
      role: 'ROLE_ADMIN'
    };
    this.saveUser(admin);
  }

  public loginAsClient(): void {
    const client: User = {
      id: 1,
      email: 'client@sportvision.com',
      fullName: 'Sophie Martin',
      role: 'ROLE_USER'
    };
    this.saveUser(client);
  }

  public login(email: string, role: Role = 'ROLE_USER'): void {
    const user: User = {
      id: Date.now(),
      email: email,
      fullName: email.split('@')[0],
      role: role
    };
    this.saveUser(user);
  }

  public logout(): void {
    localStorage.removeItem('sv_user');
    this.currentUserSubject.next(null);
  }

  private saveUser(user: User): void {
    localStorage.setItem('sv_user', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }
}
