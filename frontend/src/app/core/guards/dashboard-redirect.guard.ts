import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const isMemberUser: CanMatchFn = () => {
  const role = inject(AuthService).role();
  return role !== null && role !== 'ADMIN_GLOBAL' && role !== 'ADMIN_SITE';
};

export const isAdminUser: CanMatchFn = () => {
  const role = inject(AuthService).role();
  return role === 'ADMIN_GLOBAL' || role === 'ADMIN_SITE';
};
