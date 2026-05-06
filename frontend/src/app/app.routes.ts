import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { DashboardRedirect } from './features/dashboard/redirect/redirect';
import { DashboardGlobal } from './features/dashboard/global/global';
import { DashboardSite } from './features/dashboard/site/site';
import { DashboardLibre } from './features/dashboard/libre/libre';
import { DashboardAdmin } from './features/dashboard/admin/admin';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },

  {
    path: 'dashboard',
    component: DashboardRedirect,
    canActivate: [authGuard],
  },

  {
    path: 'dashboard/membre-global',
    component: DashboardGlobal,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['GLOBAL'] },
  },
  {
    path: 'dashboard/membre-site',
    component: DashboardSite,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['SITE'] },
  },
  {
    path: 'dashboard/membre-libre',
    component: DashboardLibre,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['LIBRE'] },
  },
  {
    path: 'dashboard/admin',
    component: DashboardAdmin,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN_GLOBAL', 'ADMIN_SITE'] },
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
