import {
  ChangeDetectionStrategy, Component, computed, inject, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';
import { PenaliteService } from '../../../core/services/penalite.service';
import { SiteService, Site } from '../../../core/services/site.service';
import { MatchService } from '../../../core/services/match.service';
import { Penalite } from '../../../core/models/penalite.model';
import { DisponibiliteDTO, MatchPadelDTO } from '../../../core/models/match.model';
import { MembreSearchDTO } from '../../../core/models/membre.model';
import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { SlotPicker } from './slot-picker/slot-picker';
import {
  CreerMatchDialog,
  CreerMatchDialogData,
  CreerMatchDialogResult,
  MatchType,
} from '../../../shared/dialogs/creer-match-dialog/creer-match-dialog';

@Component({
  selector: 'app-creer-match',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, SlotPicker, DatePipe],
  templateUrl: './creer-match.html',
})
export class CreerMatch {
  private readonly auth         = inject(AuthService);
  private readonly dialog       = inject(MatDialog);
  private readonly snackBar     = inject(MatSnackBar);
  private readonly router       = inject(Router);
  private readonly matchService = inject(MatchService);

  // ── External data ──────────────────────────────────────────────────────────
  protected readonly sites     = toSignal(inject(SiteService).getSites(),
                                   { initialValue: [] as Site[] });
  protected readonly penalites = toSignal(inject(PenaliteService).getMesPenalites(),
                                   { initialValue: [] as Penalite[] });

  // ── Penalty ────────────────────────────────────────────────────────────────
  protected readonly penaliteActive = computed(() =>
    this.penalites().find(p => p.active) ?? null);

  // ── Site ──────────────────────────────────────────────────────────────────
  protected readonly isSiteMember    = computed(() => this.auth.role() === 'SITE');
  protected readonly currentSiteName = computed(() => {
    const id   = this.auth.idSite();
    const list = this.sites();
    if (id == null || !list.length) return null;
    return list.find(s => s.idSite === id)?.nom ?? null;
  });

  // Pre-set for SITE members (from JWT), null for GLOBAL until user picks
  protected readonly selectedSiteId = signal<number | null>(this.auth.idSite());

  // ── Date ──────────────────────────────────────────────────────────────────
  protected readonly selectedDate = signal<string | null>(null);

  protected readonly dateMin = computed(() => this.toDateStr(new Date()));

  protected readonly dateMax = computed(() => {
    const days = this.auth.role() === 'GLOBAL' ? 21 : 14;
    const d = new Date();
    d.setDate(d.getDate() + days);
    return this.toDateStr(d);
  });

  protected readonly penaltyAvailableFrom = computed(() => {
    const pen = this.penaliteActive();
    if (!pen) return null;
    const [y, m, d] = pen.dateFin.split('T')[0].split('-').map(Number);
    const available = new Date(y, m - 1, d + 1);
    const dd = String(available.getDate()).padStart(2, '0');
    const mm = String(available.getMonth() + 1).padStart(2, '0');
    const yyyy = available.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
  });

  // ── Slot selection ─────────────────────────────────────────────────────────
  protected readonly selectedSlot      = signal<DisponibiliteDTO | null>(null);
  protected readonly canShowSlotPicker = computed(
    () => this.selectedSiteId() != null && this.selectedDate() != null);

  // ── Handlers ──────────────────────────────────────────────────────────────
  protected onDateChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.selectedDate.set(value || null);
    this.selectedSlot.set(null);
  }

  protected onSiteFilterChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedSiteId.set(value === '' ? null : Number(value));
    this.selectedSlot.set(null);
  }

  protected onSlotSelected(dispo: DisponibiliteDTO): void {
    this.selectedSlot.set(dispo);
  }

  // ── Submission ────────────────────────────────────────────────────────────
  protected readonly isSubmitting = signal(false);

  protected readonly canCreate = computed(() =>
    this.selectedSlot() != null
    && this.penaliteActive() == null
  );

  protected onCreerMatchClick(): void {
    const slot = this.selectedSlot();
    if (!slot) return;

    this.dialog
      .open<CreerMatchDialog, CreerMatchDialogData, CreerMatchDialogResult>(
        CreerMatchDialog,
        { data: { slot, shareEstimate: 15 }, width: '480px' }
      )
      .afterClosed()
      .subscribe(result => {
        if (result?.type) this.submitMatch(slot.idDispo, result.type, result.invites);
      });
  }

  private submitMatch(dispoId: number, type: MatchType, invites?: MembreSearchDTO[]): void {
    this.isSubmitting.set(true);
    const obs = type === 'PRIVE'
      ? this.matchService.creerPrive({ dispoId })
      : this.matchService.creerPublic({ dispoId });

    obs.subscribe({
      next: (match) => {
        if (type === 'PRIVE' && invites && invites.length > 0) {
          void this.sendInvitations(match.idMatch, invites);
        } else {
          this.handleSuccessNoInvites(match, type);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        const message = err.error?.message ?? 'Erreur lors de la création du match';
        this.snackBar.open(message, 'OK', { duration: 5000 });
      },
    });
  }

  private async sendInvitations(
    idMatch: number,
    invites: MembreSearchDTO[],
  ): Promise<void> {
    type InvitationResult = { invitee: MembreSearchDTO; success: boolean; error?: string };
    const results: InvitationResult[] = [];

    for (const invitee of invites) {
      try {
        await firstValueFrom(
          this.matchService.ajouterJoueur(idMatch, { matricule: invitee.matricule })
        );
        results.push({ invitee, success: true });
      } catch (err: any) {
        results.push({ invitee, success: false, error: err.error?.message ?? 'Erreur inconnue' });
      }
    }

    this.isSubmitting.set(false);
    this.handleInvitationResults(idMatch, results);
  }

  private handleInvitationResults(
    idMatch: number,
    results: Array<{ invitee: MembreSearchDTO; success: boolean; error?: string }>,
  ): void {
    const successes = results.filter(r => r.success).length;
    const failures  = results.filter(r => !r.success);
    const total     = results.length;

    let message: string;
    if (failures.length === 0) {
      message = `Match créé. ${total} invitation${total > 1 ? 's' : ''} envoyée${total > 1 ? 's' : ''}.`;
    } else if (successes === 0) {
      const detail = failures.map(f => `${f.invitee.prenom} ${f.invitee.nom}: ${f.error}`).join('; ');
      message = `Match créé. Aucune invitation envoyée. ${detail}`;
    } else {
      const names = failures.map(f => `${f.invitee.prenom} ${f.invitee.nom}`).join(', ');
      message = `Match créé. ${successes}/${total} invitations envoyées. Échecs: ${names}.`;
    }

    this.snackBar.open(message, 'OK', { duration: 7000 });
    this.router.navigate(['/matchs', idMatch]);
  }

  private handleSuccessNoInvites(match: MatchPadelDTO, type: MatchType): void {
    this.isSubmitting.set(false);
    if (type === 'PRIVE') {
      this.snackBar.open('Match créé avec succès', 'OK', { duration: 4000 });
      this.router.navigate(['/matchs', match.idMatch]);
    } else {
      this.snackBar.open('Match créé et visible dans Matchs publics', 'OK', { duration: 4000 });
      this.router.navigate(['/matchs']);
    }
  }

  private toDateStr(d: Date): string {
    // 'sv' locale reliably outputs YYYY-MM-DD without UTC conversion
    return d.toLocaleDateString('sv');
  }
}
