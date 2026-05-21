import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasValidAccessToken()) {
    return true;
  }

  const ok = await authService.refresh();
  if (ok) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
