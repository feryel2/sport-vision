import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';
import { AuthResponse, RegisterPayload, UpdateProfilePayload, User } from '../models/user.model';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

const TOKEN_KEY = 'sv_token';
const USER_KEY = 'sv_user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private currentUserSubject = new BehaviorSubject<User | null>(this.loadUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  private loadUserFromStorage(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw || !localStorage.getItem(TOKEN_KEY)) {
      return null;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  public get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  public get isLoggedIn(): boolean {
    return !!this.token && !!this.currentUserSubject.value;
  }

  public get isAdmin(): boolean {
    return this.currentUserSubject.value?.role === 'ROLE_ADMIN';
  }

  login(email: string, password: string): Observable<User> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, { email, password })
      .pipe(map(res => this.startSession(res.data)));
  }

  register(payload: RegisterPayload): Observable<User> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/register`, payload)
      .pipe(map(res => this.startSession(res.data)));
  }

  forgotPassword(email: string): Observable<string> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/forgot-password`, { email })
      .pipe(map(res => res.message));
  }

  resetPassword(token: string, newPassword: string): Observable<string> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/reset-password`, { token, newPassword })
      .pipe(map(res => res.message));
  }

  loadProfile(): Observable<User> {
    return this.http.get<ApiResponse<User>>(`${this.apiUrl}/me`).pipe(
      map(res => res.data),
      tap(user => this.saveUser(user))
    );
  }

  updateProfile(payload: UpdateProfilePayload): Observable<User> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/me`, payload).pipe(
      map(res => res.data),
      tap(user => this.saveUser(user))
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<string> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/me/password`, { currentPassword, newPassword })
      .pipe(map(res => res.message));
  }

  /** Désactive le compte (mot de passe requis) puis ferme la session locale. */
  deactivateAccount(password: string): Observable<string> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/me/deactivate`, { password }).pipe(
      map(res => {
        this.clearSession();
        return res.message;
      })
    );
  }

  /** Révoque le jeton côté serveur puis efface la session locale, même si l'appel échoue. */
  logout(): Observable<void> {
    const request = this.token
      ? this.http.post(`${this.apiUrl}/logout`, {}).pipe(catchError(() => of(null)))
      : of(null);
    return request.pipe(
      map(() => this.clearSession())
    );
  }

  public clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUserSubject.next(null);
  }

  private startSession(auth: AuthResponse): User {
    localStorage.setItem(TOKEN_KEY, auth.token);
    const user: User = { id: auth.id, email: auth.email, fullName: auth.fullName, role: auth.role };
    this.saveUser(user);
    return user;
  }

  private saveUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }
}
