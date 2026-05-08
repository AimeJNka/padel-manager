import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { PenaliteService } from '../../../core/services/penalite.service';
import { Penalite } from '../../../core/models/penalite.model';

@Component({
  selector: 'app-mes-penalites',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatProgressBarModule,
    MatSnackBarModule,
  ],
  templateUrl: './mes-penalites.html',
})
export class MesPenalites implements OnInit {

  private readonly penaliteService = inject(PenaliteService);
  private readonly snackBar = inject(MatSnackBar);

  readonly penalites = signal<Penalite[]>([]);
  readonly isLoading = signal(false);
  readonly displayedColumns: string[] = ['dateDebut', 'dateFin', 'motif', 'statut'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.penaliteService.getMesPenalites().subscribe({
      next: (data) => {
        this.penalites.set(data);
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
}
