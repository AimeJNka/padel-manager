import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { PaiementService } from '../../../core/services/paiement.service';
import { Paiement, PaiementStatut } from '../../../core/models/paiement.model';
import {
  AnnulationDialog,
  AnnulationDialogResult,
} from './annulation-dialog';

@Component({
  selector: 'app-mes-paiements',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatDialogModule,
  ],
  templateUrl: './mes-paiements.html',
})
export class MesPaiements implements OnInit {

  private readonly paiementService = inject(PaiementService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly paiements = signal<Paiement[]>([]);
  readonly isLoading = signal(false);
  readonly displayedColumns: string[] = [
    'idMatch',
    'match',
    'montant',
    'soldeInclus',
    'datePaiement',
    'statut',
    'actions',
  ];

  ngOnInit(): void {
    this.load();
  }

  matchIsUpcoming(p: Paiement): boolean {
    return new Date(p.matchDateHeureDebut).getTime() > Date.now();
  }

  load(): void {
    this.isLoading.set(true);
    this.paiementService.getMesPaiements().subscribe({
      next: (data) => {
        this.paiements.set(data);
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

  payer(id: number): void {
    this.paiementService.payer(id).subscribe({
      next: () => {
        this.snackBar.open('Paiement effectué', 'Fermer', { duration: 3000 });
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.snackBar.open(err.error?.error ?? 'Erreur', 'Fermer', { duration: 4000 });
      },
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
      if (!result) {
        return;
      }
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
    if (err.status === 403) {
      return "Vous n'êtes pas inscrit à ce match.";
    }
    if (err.status === 409) {
      return 'Ce match a déjà commencé.';
    }
    return 'Une erreur est survenue. Veuillez réessayer.';
  }

  chipColor(statut: PaiementStatut): 'primary' | 'accent' | 'warn' | undefined {
    switch (statut) {
      case 'EN_ATTENTE':
        return 'warn';
      case 'PAYE':
        return 'primary';
      case 'ANNULE':
        return 'accent';
      case 'REMBOURSE':
      default:
        return undefined;
    }
  }
}
