import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';

import { AuthService } from '../../../../core/services/auth.service';
import { PaiementService } from '../../../../core/services/paiement.service';
import { MembreService } from '../../../../core/services/membre.service';
import { Paiement } from '../../../../core/models/paiement.model';

@Component({
  selector: 'app-libre-member-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule, MatButtonModule, MatDividerModule, DatePipe, CurrencyPipe],
  template: `
    <div class="dashboard-container">

      <mat-card>
        <mat-card-header>
          <mat-card-title>Mes prochains matchs</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (prochainMatchs().length === 0) {
            <p class="empty-state">Aucun match à venir.</p>
          } @else {
            @for (p of prochainMatchs(); track p.idPaiement) {
              <div class="match-row">
                <span>{{ p.matchDateHeureDebut | date:'dd/MM/yyyy à HH:mm' }}</span>
                <span>{{ p.statut }}</span>
                <span>{{ p.montant | currency:'EUR' }}</span>
              </div>
              <mat-divider />
            }
          }
        </mat-card-content>
      </mat-card>

      @if (profil(); as m) {
        <mat-card>
          <mat-card-header>
            <mat-card-title>Mon profil</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p><strong>Matricule :</strong> {{ m.matricule }}</p>
            <p><strong>Type :</strong> {{ m.typeMembre.libelle }}</p>
            <p><strong>Solde dû :</strong> {{ m.soldeDu | currency:'EUR' }}</p>
          </mat-card-content>
        </mat-card>
      }

      <div class="dashboard-actions">
        <button mat-flat-button (click)="auth.logout()">Se déconnecter</button>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container { display: flex; flex-direction: column; gap: 1rem; max-width: 720px; margin: 2rem auto; padding: 0 1rem; }
    .match-row { display: flex; justify-content: space-between; align-items: center; padding: 0.5rem 0; }
    .empty-state { color: var(--mat-sys-on-surface-variant); }
    .dashboard-actions { display: flex; justify-content: flex-end; }
  `],
})
export class LibreMemberDashboard {
  protected readonly auth = inject(AuthService);

  protected readonly paiements = toSignal(
    inject(PaiementService).getMesPaiements(),
    { initialValue: [] as Paiement[] },
  );
  protected readonly profil = toSignal(
    inject(MembreService).getMonProfil(),
    { initialValue: null },
  );

  protected readonly prochainMatchs = computed(() =>
    this.paiements()
      .filter(p => p.statut !== 'ANNULE' && new Date(p.matchDateHeureDebut) > new Date())
      .sort((a, b) => new Date(a.matchDateHeureDebut).getTime() - new Date(b.matchDateHeureDebut).getTime())
  );
}
