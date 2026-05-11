import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const isGlobalMembre: CanMatchFn = () => inject(AuthService).role() === 'GLOBAL';
export const isSiteMembre: CanMatchFn = () => inject(AuthService).role() === 'SITE';
export const isLibreMembre: CanMatchFn = () => inject(AuthService).role() === 'LIBRE';
export const isAdminUser: CanMatchFn = () => {
  const role = inject(AuthService).role();
  return role === 'ADMIN_GLOBAL' || role === 'ADMIN_SITE';
};
