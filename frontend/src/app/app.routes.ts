import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { isMemberUser, isAdminUser } from './core/guards/dashboard-redirect.guard';

import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },

  {
    path: 'dashboard',
    canActivate: [authGuard],
    canMatch: [isMemberUser],
    loadComponent: () =>
      import('./features/dashboard/dashboards/member-dashboard/member-dashboard').then(m => m.MemberDashboard),
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

  {
    path: 'matchs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matchs/matchs-publiques/matchs-publiques').then(m => m.MatchsPubliques),
  },
  {
    path: 'matchs/creer',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['GLOBAL', 'SITE'], denyMessage: 'Action réservée aux membres globaux et de site' },
    loadComponent: () =>
      import('./features/matchs/creer-match/creer-match').then(m => m.CreerMatch),
  },
  {
    path: 'matchs/:id/inviter',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['GLOBAL', 'SITE'], denyMessage: 'Action réservée aux membres globaux et de site', title: 'Inviter des joueurs' },
    loadComponent: () =>
      import('./features/matchs/inviter-joueurs/inviter-joueurs').then(m => m.InviterJoueurs),
  },
  {
    path: 'matchs/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matchs/match-detail/match-detail').then(m => m.MatchDetail),
    data: { title: 'Détails du match' },
  },
  {
    path: 'historique',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shared/components/coming-soon/coming-soon').then(m => m.ComingSoon),
    data: { title: 'Historique' },
  },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
