import {
  ChangeDetectionStrategy, Component, OnInit,
  inject, signal,
} from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';

import { PaiementService } from '../../../core/services/paiement.service';
import { Paiement, PaiementStatut } from '../../../core/models/paiement.model';
import { MembreService } from '../../../core/services/membre.service';
import {
  AnnulationDialog,
  AnnulationDialogResult,
} from './annulation-dialog';
import {
  PaiementConfirmDialog,
  PaiementConfirmData,
} from '../../../shared/dialogs/paiement-confirm-dialog/paiement-confirm-dialog';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { PageShell } from '../../../shared/components/page-shell/page-shell';

@Component({
  selector: 'app-mes-paiements',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, RouterLink, StatusBadge, PageShell],
  templateUrl: './mes-paiements.html',
})
export class MesPaiements implements OnInit {

  private static readonly STATUT_ORDER: Record<PaiementStatut, number> = {
    EN_ATTENTE: 0,
    PAYE:       1,
    REMBOURSE:  2,
    ANNULE:     3,
  };

  private readonly paiementService = inject(PaiementService);
  private readonly membreService   = inject(MembreService);
  private readonly snackBar        = inject(MatSnackBar);
  private readonly dialog          = inject(MatDialog);

  readonly paiements = signal<Paiement[]>([]);
  readonly isLoading = signal(false);
  protected readonly soldeDu = signal<number>(0);

  ngOnInit(): void {
    this.load();
    this.membreService.getMonProfil().subscribe({
      next:  (m) => this.soldeDu.set(m.soldeDu ?? 0),
      error: ()  => this.soldeDu.set(0),
    });
  }

  matchIsUpcoming(p: Paiement): boolean {
    return new Date(p.matchDateHeureDebut).getTime() > Date.now();
  }

  private sortPaiements(list: Paiement[]): Paiement[] {
    return [...list].sort((a, b) => {
      const so = MesPaiements.STATUT_ORDER[a.statut] - MesPaiements.STATUT_ORDER[b.statut];
      if (so !== 0) return so;
      const ta = new Date(a.matchDateHeureDebut).getTime();
      const tb = new Date(b.matchDateHeureDebut).getTime();
      if (ta !== tb) return ta - tb;
      return a.idPaiement - b.idPaiement;
    });
  }

  private load(): void {
    this.isLoading.set(true);
    this.paiementService.getMesPaiements().subscribe({
      next: (list) => {
        this.paiements.set(this.sortPaiements(list));
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.snackBar.open(err.error?.error ?? 'Erreur lors du chargement.', 'Fermer', {
          duration: 4000,
        });
      },
    });
  }

  protected payer(p: Paiement): void {
    const ref = this.dialog.open<PaiementConfirmDialog, PaiementConfirmData, boolean>(
      PaiementConfirmDialog,
      {
        data: {
          montant:   p.montant,
          soldeDu:   this.soldeDu(),
          matchDate: p.matchDateHeureDebut,
        } satisfies PaiementConfirmData,
        width: '400px',
      },
    );

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.paiementService.payer(p.idPaiement).subscribe({
        next: (updated) => {
          this.paiements.update(list =>
            this.sortPaiements(list.map(x => x.idPaiement === updated.idPaiement ? updated : x))
          );
          this.soldeDu.set(0);
          this.snackBar.open('Paiement effectué', 'Fermer', { duration: 3000 });
        },
        error: (err: HttpErrorResponse) => {
          this.snackBar.open(err.error?.error ?? 'Erreur lors du paiement.', 'Fermer', { duration: 4000 });
        },
      });
    });
  }

  annulerParticipation(p: Paiement): void {
    const ref = this.dialog.open<AnnulationDialog, Paiement, AnnulationDialogResult>(
      AnnulationDialog,
      {
        data: p,
        disableClose: true,
        width: '460px',
      },
    );

    ref.afterClosed().subscribe((result) => {
      if (!result) return;
      if (result.success) {
        this.snackBar.open('Participation annulée', 'Fermer', { duration: 3000 });
        this.load();
        return;
      }
      if (result.error) {
        this.snackBar.open(this.errorMessage(result.error), 'Fermer', { duration: 4000 });
      }
    });
  }

  private errorMessage(err: HttpErrorResponse): string {
    if (err.status === 403) return "Vous n'êtes pas inscrit à ce match.";
    if (err.status === 409) return 'Ce match a déjà commencé.';
    return 'Une erreur est survenue. Veuillez réessayer.';
  }
}
