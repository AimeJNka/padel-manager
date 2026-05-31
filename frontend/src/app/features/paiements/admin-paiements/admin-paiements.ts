import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { AuthService } from '../../../core/services/auth.service';
import { PaiementService } from '../../../core/services/paiement.service';
import { Paiement, PaiementStatut } from '../../../core/models/paiement.model';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/dialogs/confirm-dialog';

@Component({
  selector: 'app-admin-paiements',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    PageShell,
    StatusBadge,
  ],
  templateUrl: './admin-paiements.html',
})
export class AdminPaiements implements OnInit {

  private readonly paiementService = inject(PaiementService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);

  readonly filterForm = this.fb.group({
    matricule: [''],
    statut: [''],
    matchId: [null as number | null],
    siteId: [null as number | null],
  });

  readonly paiements = signal<Paiement[]>([]);
  readonly totalElements = signal(0);
  readonly isLoading = signal(false);

  readonly statutOptions: { value: PaiementStatut | ''; label: string }[] = [
    { value: '', label: 'Tous' },
    { value: 'EN_ATTENTE', label: 'En attente' },
    { value: 'PAYE', label: 'Payé' },
    { value: 'ANNULE', label: 'Annulé' },
    { value: 'REMBOURSE', label: 'Remboursé' },
  ];

  ngOnInit(): void {
    this.load();
  }

  isGlobalAdmin(): boolean {
    return this.authService.getRole() === 'ADMIN_GLOBAL';
  }

  load(): void {
    this.isLoading.set(true);
    const v = this.filterForm.getRawValue();

    this.paiementService
      .getAllPaiements({
        matricule: v.matricule,
        statut: v.statut,
        matchId: v.matchId,
        siteId: this.isGlobalAdmin() ? v.siteId : null,
      })
      .subscribe({
        next: (page) => {
          this.paiements.set(page.content);
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
    this.filterForm.reset({ matricule: '', statut: '', matchId: null, siteId: null });
    this.load();
  }

  rembourser(id: number): void {
    const paiement = this.paiements().find(p => p.idPaiement === id);
    const dateLabel = paiement
      ? new Date(paiement.matchDateHeureDebut).toLocaleDateString('fr-BE', { day: '2-digit', month: '2-digit', year: 'numeric' })
      : '—';
    const message = paiement
      ? `Confirmer le remboursement de ${paiement.montant} € au joueur ${paiement.matricule} pour le match du ${dateLabel} ?`
      : 'Confirmer le remboursement de ce paiement ?';

    this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
      data: {
        title: 'Rembourser ce paiement ?',
        message,
        confirmLabel: 'Rembourser',
        destructive: true,
      },
    }).afterClosed().subscribe((result) => {
      if (result !== true) {
        return;
      }
      this.paiementService.rembourser(id).subscribe({
        next: () => {
          this.snackBar.open('Remboursement effectué', 'Fermer', { duration: 3000 });
          this.load();
        },
        error: (err: HttpErrorResponse) => {
          this.snackBar.open(err.error?.error ?? 'Erreur', 'Fermer', { duration: 4000 });
        },
      });
    });
  }
}
