import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const API_URL = 'http://localhost:8080/api';
const PUBLIC_AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/forgot-password', '/auth/reset-password'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!req.url.startsWith(API_URL)) {
    return next(req);
  }

  const token = auth.token;
  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      const isPublicAuthCall = PUBLIC_AUTH_PATHS.some(p => req.url.startsWith(API_URL + p));
      // Jeton expiré ou révoqué : on ferme la session locale et on renvoie vers la connexion
      if (err.status === 401 && !isPublicAuthCall && auth.isLoggedIn) {
        auth.clearSession();
        router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
      }
      return throwError(() => err);
    })
  );
};
