import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const allowedRoles: string[] = route.data?.['roles'] ?? [];
  const userRole = authService.getRole();

  if (userRole && allowedRoles.includes(userRole)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
