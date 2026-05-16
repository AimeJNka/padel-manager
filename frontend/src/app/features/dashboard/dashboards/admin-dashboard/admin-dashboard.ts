import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';

import { AuthService } from '../../../../core/services/auth.service';

// AdminDashboard is intentionally minimal in Sprint 6 — KPIs and richer admin tooling are deferred to Phase 2 hi-fi implementation.
@Component({
  selector: 'app-admin-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule, MatButtonModule, MatListModule, RouterLink],
  template: `
    <div class="dashboard-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Bienvenue, {{ roleLabel() }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <mat-nav-list>
            <a mat-list-item routerLink="/admin/paiements">Gérer les paiements</a>
            <a mat-list-item routerLink="/admin/penalites">Gérer les pénalités</a>
          </mat-nav-list>
        </mat-card-content>
        <mat-card-actions>
          <button mat-flat-button (click)="auth.logout()">Se déconnecter</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .dashboard-container { max-width: 480px; margin: 2rem auto; padding: 0 1rem; }
  `],
})
export class AdminDashboard {
  protected readonly auth = inject(AuthService);

  protected readonly roleLabel = computed(() => {
    const role = this.auth.role();
    if (role === 'ADMIN_GLOBAL') return 'Administrateur Global';
    if (role === 'ADMIN_SITE') return 'Administrateur Site';
    return 'Administrateur';
  });
}
