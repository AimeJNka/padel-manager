import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';
import { MatchService } from '../../../core/services/match.service';
import { SiteService, Site } from '../../../core/services/site.service';
import { MatchPadelDTO } from '../../../core/models/match.model';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/dialogs/confirm-dialog';
import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { MatchCard } from '../../../shared/components/match-card/match-card';

@Component({
  selector: 'app-matchs-publiques',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, MatchCard],
  templateUrl: './matchs-publiques.html',
})
export class MatchsPubliques {
  private readonly auth         = inject(AuthService);
  private readonly matchService = inject(MatchService);
  private readonly dialog       = inject(MatDialog);
  private readonly snackBar     = inject(MatSnackBar);

  protected readonly sites          = toSignal(inject(SiteService).getSites(), { initialValue: [] as Site[] });
  protected readonly selectedSiteId = signal<number | null>(null);
  protected readonly publicMatches  = signal<MatchPadelDTO[]>([]);
  protected readonly isLoading      = signal(false);
  protected readonly error          = signal<string | null>(null);
  private   readonly myMatchIds     = signal<Set<number>>(new Set<number>());

  protected readonly filteredPublicMatches = computed(() => {
    const ids = this.myMatchIds();
    return this.publicMatches().filter(m => !ids.has(m.idMatch));
  });

  protected readonly isSiteMember    = computed(() => this.auth.role() === 'SITE');
  protected readonly currentSiteName = computed(() => {
    const siteId = this.auth.idSite();
    const list   = this.sites();
    if (siteId == null || !list.length) return null;
    return list.find(s => s.idSite === siteId)?.nom ?? null;
  });

  constructor() {
    this.fetchPublicMatches();
    this.fetchMyMatchIds();
  }

  protected onSiteFilterChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedSiteId.set(value === '' ? null : Number(value));
    this.fetchPublicMatches();
  }

  protected onInscriptionClick(match: MatchPadelDTO): void {
    // Estimated per-player share: total / 4 (max participants).
    // Real amount is computed server-side at payment time.
    const estimatedShare = (match.montantTotal / 4).toFixed(2);
    const date = new Date(match.disponibilite.dateHeureDebut).toLocaleString('fr-BE', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
    const terrain = match.disponibilite.terrain;

    this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
      data: {
        title: "S'inscrire au match",
        message: `Vous allez rejoindre le match du ${date} sur le terrain `
          + `${terrain.numero} (${terrain.site.nom}). `
          + `Votre part estimée est de ${estimatedShare} €. Continuer ?`,
        confirmLabel: "S'inscrire",
        cancelLabel: 'Annuler',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed !== true) return;
      this.matchService.sInscrire(match.idMatch).subscribe({
        next: () => {
          this.snackBar.open('Inscription réussie !', 'Fermer', { duration: 3000 });
          this.myMatchIds.update(ids => new Set([...ids, match.idMatch]));
          this.fetchPublicMatches();
        },
        error: (err: HttpErrorResponse) => {
          const msg = err.error?.message ?? 'Inscription impossible. Veuillez réessayer.';
          this.snackBar.open(msg, 'Fermer', { duration: 4000 });
        },
      });
    });
  }

  private fetchMyMatchIds(): void {
    this.matchService.lister({ mine: true, size: 50 }).subscribe({
      next: page => this.myMatchIds.set(new Set(page.content.map(m => m.idMatch))),
      error: () => this.myMatchIds.set(new Set<number>()),
    });
  }

  private fetchPublicMatches(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.matchService.lister({
      type: 'PUBLIC',
      statut: 'EN_ATTENTE',
      siteId: this.selectedSiteId(),
      size: 50,
    }).subscribe({
      next: (page) => {
        this.publicMatches.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les matches. Veuillez réessayer.');
        this.isLoading.set(false);
      },
    });
  }
}
