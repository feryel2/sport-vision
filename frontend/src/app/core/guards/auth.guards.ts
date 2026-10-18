import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn
    ? true
    : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  return auth.isAdmin ? true : router.createUrlTree(['/']);
};

/** Empêche un utilisateur déjà connecté de revenir sur connexion / inscription. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn ? router.createUrlTree([auth.isAdmin ? '/admin' : '/catalogue']) : true;
};
