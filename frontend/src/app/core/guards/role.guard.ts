import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router      = inject(Router);
  const snackBar    = inject(MatSnackBar);

  const allowedRoles: string[]          = route.data?.['roles'] ?? [];
  const denyMessage: string | undefined = route.data?.['denyMessage'];
  const userRole = authService.getRole();

  if (userRole && allowedRoles.includes(userRole)) {
    return true;
  }

  if (denyMessage) {
    snackBar.open(denyMessage, 'Fermer', { duration: 4000 });
  }
  return router.createUrlTree(['/dashboard']);
};
