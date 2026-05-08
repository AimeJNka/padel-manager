import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-libre',
  imports: [MatCardModule, MatButtonModule, RouterLink],
  template: `
    <div class="dashboard-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Tableau de bord — Membre Libre</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>Bienvenue sur votre espace membre libre.</p>
        </mat-card-content>
        <mat-card-actions>
          <a mat-button routerLink="/mes-paiements">Mes paiements</a>
          <a mat-button routerLink="/mes-penalites">Mes pénalités</a>
          <button mat-raised-button (click)="logout()">Se déconnecter</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
})
export class DashboardLibre {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
