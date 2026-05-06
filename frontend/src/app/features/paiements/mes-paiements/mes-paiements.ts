import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { PaiementService } from '../../../core/services/paiement.service';
import { Paiement, PaiementStatut } from '../../../core/models/paiement.model';

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
  ],
  templateUrl: './mes-paiements.html',
})
export class MesPaiements implements OnInit {

  private readonly paiementService = inject(PaiementService);
  private readonly snackBar = inject(MatSnackBar);

  readonly paiements = signal<Paiement[]>([]);
  readonly isLoading = signal(false);
  readonly displayedColumns: string[] = ['idMatch', 'montant', 'soldeInclus', 'datePaiement', 'statut', 'actions'];

  ngOnInit(): void {
    this.load();
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
