import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { PenaliteService } from '../../../core/services/penalite.service';
import { Penalite } from '../../../core/models/penalite.model';
import { AuthService } from '../../../core/services/auth.service';
import { SiteService, Site } from '../../../core/services/site.service';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/dialogs/confirm-dialog';

@Component({
  selector: 'app-admin-penalites',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    PageShell,
    StatusBadge,
  ],
  templateUrl: './admin-penalites.html',
})
export class AdminPenalites implements OnInit {

  private readonly penaliteService = inject(PenaliteService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly siteService = inject(SiteService);
  private readonly dialog = inject(MatDialog);

  readonly filterForm = this.fb.group({
    matricule: [''],
    activeOnly: [false],
    siteId: [null as number | null],
  });

  readonly penalites = signal<Penalite[]>([]);
  readonly totalElements = signal(0);
  readonly isLoading = signal(false);
  readonly sites = signal<Site[]>([]);

  isGlobalAdmin(): boolean {
    return this.authService.getRole() === 'ADMIN_GLOBAL';
  }

  emptyStateMessage(): string {
    const siteId = this.filterForm.getRawValue().siteId;
    if (this.isGlobalAdmin() && siteId !== null && siteId !== undefined) {
      return 'Aucune pénalité pour ce site.';
    }
    return 'Aucune pénalité ne correspond aux filtres appliqués.';
  }

  ngOnInit(): void {
    if (this.isGlobalAdmin()) {
      this.siteService.getSites().subscribe({
        next: (s) => this.sites.set(s),
        error: () => this.sites.set([]),
      });
    }
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    const v = this.filterForm.getRawValue();

    this.penaliteService
      .getAllPenalites({
        matricule: v.matricule,
        activeOnly: v.activeOnly,
        siteId: this.isGlobalAdmin() ? v.siteId : null,
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
    this.load();
  }

  onReset(): void {
    this.filterForm.reset({ matricule: '', activeOnly: false, siteId: null });
    this.load();
  }

  annuler(id: number): void {
    const penalite = this.penalites().find(p => p.idPenalite === id);
    const dateLabel = penalite
      ? new Date(penalite.dateFin).toLocaleDateString('fr-BE', { day: '2-digit', month: '2-digit', year: 'numeric' })
      : '—';
    const message = penalite
      ? `Confirmer l'annulation de la pénalité de ${penalite.matricule} (active jusqu'au ${dateLabel}) ?`
      : 'Confirmer l\'annulation de cette pénalité ?';

    this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
      data: {
        title: 'Annuler cette pénalité ?',
        message,
        confirmLabel: 'Annuler la pénalité',
        destructive: true,
      },
    }).afterClosed().subscribe((result) => {
      if (result !== true) {
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
    });
  }
}
