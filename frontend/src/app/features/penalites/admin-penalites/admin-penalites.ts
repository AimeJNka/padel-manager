import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { PenaliteService } from '../../../core/services/penalite.service';
import { Penalite } from '../../../core/models/penalite.model';

@Component({
  selector: 'app-admin-penalites',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatTableModule,
    MatChipsModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSnackBarModule,
  ],
  templateUrl: './admin-penalites.html',
})
export class AdminPenalites implements OnInit {

  private readonly penaliteService = inject(PenaliteService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly filterForm = this.fb.group({
    matricule: [''],
    activeOnly: [false],
  });

  readonly penalites = signal<Penalite[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly isLoading = signal(false);

  readonly displayedColumns: string[] = [
    'nomJoueur',
    'matricule',
    'dateDebut',
    'dateFin',
    'motif',
    'statut',
    'actions',
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    const v = this.filterForm.getRawValue();

    this.penaliteService
      .getAllPenalites({
        matricule: v.matricule,
        activeOnly: v.activeOnly,
        page: this.pageIndex(),
        size: this.pageSize(),
      })
      .subscribe({
        next: (page) => {
          this.penalites.set(page.content);
          this.totalElements.set(page.totalElements);
          this.isLoading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.snackBar.open(err.error?.error ?? 'Erreur', 'Fermer', { duration: 4000 });
        },
      });
  }

  onFilter(): void {
    this.pageIndex.set(0);
    this.load();
  }

  onReset(): void {
    this.filterForm.reset({ matricule: '', activeOnly: false });
    this.pageIndex.set(0);
    this.load();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  annuler(id: number): void {
    if (!window.confirm('Confirmer l\'annulation de cette pénalité ?')) {
      return;
    }
    this.penaliteService.annulerPenalite(id).subscribe({
      next: () => {
        this.snackBar.open('Pénalité annulée', 'Fermer', { duration: 3000 });
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.snackBar.open(err.error?.error ?? 'Erreur', 'Fermer', { duration: 4000 });
      },
    });
  }
}
