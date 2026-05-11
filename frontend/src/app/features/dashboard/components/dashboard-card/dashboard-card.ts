import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../../../core/services/auth.service';

interface DashboardLink { label: string; route: string; }

@Component({
  selector: 'app-dashboard-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule, MatButtonModule, RouterLink],
  template: `
    <div class="dashboard-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ title() }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>{{ description() }}</p>
        </mat-card-content>
        <mat-card-actions>
          @for (link of links(); track link.route) {
            <a mat-button [routerLink]="link.route">{{ link.label }}</a>
          }
          <button mat-flat-button (click)="authService.logout()">Se déconnecter</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
})
export class DashboardCard {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly links = input.required<DashboardLink[]>();

  readonly authService = inject(AuthService);
}
