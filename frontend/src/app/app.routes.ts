import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { isGlobalMembre, isSiteMembre, isLibreMembre, isAdminUser } from './core/guards/dashboard-redirect.guard';

import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },

  {
    path: 'dashboard',
    canActivate: [authGuard],
    canMatch: [isGlobalMembre],
    loadComponent: () =>
      import('./features/dashboard/dashboards/member-dashboard/member-dashboard').then(m => m.MemberDashboard),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    canMatch: [isSiteMembre],
    loadComponent: () =>
      import('./features/dashboard/dashboards/member-dashboard/member-dashboard').then(m => m.MemberDashboard),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    canMatch: [isLibreMembre],
    loadComponent: () =>
      import('./features/dashboard/dashboards/libre-member-dashboard/libre-member-dashboard').then(m => m.LibreMemberDashboard),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    canMatch: [isAdminUser],
    loadComponent: () =>
      import('./features/dashboard/dashboards/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard),
  },

  {
    path: 'mes-paiements',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/paiements/mes-paiements/mes-paiements').then(m => m.MesPaiements),
  },
  {
    path: 'mes-penalites',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/penalites/mes-penalites/mes-penalites').then(m => m.MesPenalites),
  },
  {
    path: 'admin/paiements',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN_GLOBAL', 'ADMIN_SITE'] },
    loadComponent: () =>
      import('./features/paiements/admin-paiements/admin-paiements').then(m => m.AdminPaiements),
  },
  {
    path: 'admin/penalites',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN_GLOBAL', 'ADMIN_SITE'] },
    loadComponent: () =>
      import('./features/penalites/admin-penalites/admin-penalites').then(m => m.AdminPenalites),
  },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
