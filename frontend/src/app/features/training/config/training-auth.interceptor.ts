import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';

/** Attach the session token only to same-origin Training API requests. */
export const trainingAuthInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url !== '/api/training' && !req.url.startsWith('/api/training/')) {
    return next(req);
  }
  const token = inject(AuthService).token;
  return next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req);
};
