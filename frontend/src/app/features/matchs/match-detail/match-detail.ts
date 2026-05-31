import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  numberAttribute,
  signal,
} from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { MatchService } from '../../../core/services/match.service';
import { AuthService } from '../../../core/services/auth.service';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/dialogs/confirm-dialog';
import { MatchPadelDTO, ParticipationDTO } from '../../../core/models/match.model';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { PageShell } from '../../../shared/components/page-shell/page-shell';

@Component({
  selector: 'app-match-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, RouterLink, StatusBadge, PageShell],
  templateUrl: './match-detail.html',
})
export class MatchDetail implements OnInit {

  private static readonly DELAI_ANNULATION_PUBLIC_H = 24;
  private static readonly DELAI_ANNULATION_PRIVE_H  = 48;

  readonly id = input.required({ transform: numberAttribute });

  private readonly matchService = inject(MatchService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly match = signal<MatchPadelDTO | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  private readonly currentMatricule = computed(() => this.auth.matricule());

  readonly isOrganizer = computed(() => {
    const cm = this.currentMatricule();
    return cm !== null && this.match()?.organisateur.matricule === cm;
  });

  readonly activeParticipations = computed(() =>
    this.match()?.participations.filter(p => p.statutParticipation !== 'ANNULEE') ?? []
  );

  readonly myParticipation = computed((): ParticipationDTO | null => {
    const cm = this.currentMatricule();
    if (!cm) return null;
    return this.match()?.participations.find(
      p => p.matricule === cm && p.statutParticipation !== 'ANNULEE'
    ) ?? null;
  });

  readonly isParticipantNotOrganizer = computed(() =>
    this.myParticipation() !== null && !this.isOrganizer()
  );

  readonly hoursBeforeMatch = computed(() => {
    const deb = this.match()?.disponibilite.dateHeureDebut;
    if (!deb) return Infinity;
    return (new Date(deb).getTime() - Date.now()) / 3_600_000;
  });

  readonly isLateCancellation = computed(() => this.hoursBeforeMatch() < 24);

  readonly cancelMatchDeadlinePassed = computed(() => {
    const m = this.match();
    if (!m) return true;
    const delai = m.typeMatch === 'PUBLIC'
      ? MatchDetail.DELAI_ANNULATION_PUBLIC_H
      : MatchDetail.DELAI_ANNULATION_PRIVE_H;
    return this.hoursBeforeMatch() < delai;
  });

  readonly canInvite = computed(() =>
    this.isOrganizer() &&
    this.match()?.typeMatch === 'PRIVE' &&
    this.match()?.statut !== 'ANNULE' && this.match()?.statut !== 'EFFECTUE' &&
    this.activeParticipations().length < 4
  );

  readonly canCancelMatch = computed(() =>
    this.isOrganizer() &&
    this.match()?.statut !== 'ANNULE' && this.match()?.statut !== 'EFFECTUE' &&
    !this.cancelMatchDeadlinePassed()
  );

  readonly canCancelMyParticipation = computed(() =>
    this.isParticipantNotOrganizer() &&
    this.match()?.statut !== 'ANNULE' && this.match()?.statut !== 'EFFECTUE' &&
    this.hoursBeforeMatch() > 0
  );

  ngOnInit(): void {
    this.fetchMatch();
  }

  fetchMatch(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.matchService.getOne(this.id()).subscribe({
      next: m => { this.match.set(m); this.isLoading.set(false); },
      error: () => { this.error.set('Impossible de charger le match.'); this.isLoading.set(false); },
    });
  }

  onCancelMatchClick(): void {
    const m = this.match();
    if (!m) return;
    const dateStr = new Date(m.disponibilite.dateHeureDebut).toLocaleString('fr-BE', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
    const count = this.activeParticipations().length;
    const base = `Annuler définitivement le match du ${dateStr} ?`;
    const warning = count > 0
      ? `\n\nAttention : ce match a ${count} joueur${count > 1 ? 's' : ''} inscrit${count > 1 ? 's' : ''}. Leurs participations seront annulées et leurs paiements remboursés.`
      : '';
    this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Annuler le match',
        message: base + warning,
        confirmLabel: 'Annuler le match',
        destructive: true,
      } as ConfirmDialogData,
      width: '460px',
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.submitCancelMatch();
    });
  }

  onCancelMyParticipationClick(): void {
    const late = this.isLateCancellation();
    const base = 'Annuler votre participation à ce match ?';
    const warning = late
      ? "\n\nAttention : moins de 24h avant le match. Une pénalité d'annulation tardive s'appliquera. Si votre paiement n'est pas réglé, 15 € seront ajoutés à votre solde dû."
      : '';
    this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Annuler ma participation',
        message: base + warning,
        confirmLabel: "Confirmer l'annulation",
        destructive: true,
      } as ConfirmDialogData,
      width: '460px',
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.submitCancelParticipation();
    });
  }

  private submitCancelMatch(): void {
    this.matchService.annuler(this.id()).subscribe({
      next: () => {
        this.snackBar.open('Match annulé avec succès.', 'OK', { duration: 4000 });
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        const msg: string = err?.error?.message ?? "Impossible d'annuler le match.";
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      },
    });
  }

  private submitCancelParticipation(): void {
    this.matchService.annulerParticipation(this.id()).subscribe({
      next: () => {
        this.snackBar.open('Participation annulée.', 'OK', { duration: 4000 });
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        const msg: string = err?.error?.message ?? "Impossible d'annuler la participation.";
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      },
    });
  }
}
