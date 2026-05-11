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
    data: {
      title: 'Tableau de bord — Membre Global',
      description: 'Bienvenue sur votre espace membre global.',
      links: [
        { label: 'Mes paiements', route: '/mes-paiements' },
        { label: 'Mes pénalités', route: '/mes-penalites' },
      ],
    },
    loadComponent: () =>
      import('./features/dashboard/components/dashboard-card/dashboard-card').then(m => m.DashboardCard),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    canMatch: [isSiteMembre],
    data: {
      title: 'Tableau de bord — Membre Site',
      description: 'Bienvenue sur votre espace membre site.',
      links: [
        { label: 'Mes paiements', route: '/mes-paiements' },
        { label: 'Mes pénalités', route: '/mes-penalites' },
      ],
    },
    loadComponent: () =>
      import('./features/dashboard/components/dashboard-card/dashboard-card').then(m => m.DashboardCard),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    canMatch: [isLibreMembre],
    data: {
      title: 'Tableau de bord — Membre Libre',
      description: 'Bienvenue sur votre espace membre libre.',
      links: [
        { label: 'Mes paiements', route: '/mes-paiements' },
        { label: 'Mes pénalités', route: '/mes-penalites' },
      ],
    },
    loadComponent: () =>
      import('./features/dashboard/components/dashboard-card/dashboard-card').then(m => m.DashboardCard),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    canMatch: [isAdminUser],
    data: {
      title: 'Tableau de bord — Administration',
      description: "Bienvenue sur l'espace administrateur.",
      links: [
        { label: 'Gestion paiements', route: '/admin/paiements' },
        { label: 'Gestion pénalités', route: '/admin/penalites' },
      ],
    },
    loadComponent: () =>
      import('./features/dashboard/components/dashboard-card/dashboard-card').then(m => m.DashboardCard),
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
