import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const API_URL = 'http://localhost:8080/api';
const API_PATH = '/api';
const PUBLIC_AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/forgot-password', '/auth/reset-password'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const isAbsoluteApiRequest = req.url.startsWith(`${API_URL}/`);
  const isRelativeApiRequest = req.url === API_PATH || req.url.startsWith(`${API_PATH}/`);
  if (!isAbsoluteApiRequest && !isRelativeApiRequest) {
    return next(req);
  }

  const requestPath = isAbsoluteApiRequest ? req.url.slice(API_URL.length) : req.url.slice(API_PATH.length);

  const token = auth.token;
  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      const isPublicAuthCall = PUBLIC_AUTH_PATHS.some(p => requestPath === p || requestPath.startsWith(`${p}/`));
      // Jeton expiré ou révoqué : on ferme la session locale et on renvoie vers la connexion
      // Une requête anonyme lancée avant la connexion peut répondre après celle-ci.
      // Seul un 401 de la session qui a réellement envoyé le jeton peut la fermer.
      if (err.status === 401 && !isPublicAuthCall && !!token && auth.token === token && auth.isLoggedIn) {
        auth.clearSession();
        router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
      }
      return throwError(() => err);
    })
  );
};
