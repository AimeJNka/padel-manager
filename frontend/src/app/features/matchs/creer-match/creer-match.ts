import {
  ChangeDetectionStrategy, Component, computed, inject, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';
import { PenaliteService } from '../../../core/services/penalite.service';
import { SiteService, Site } from '../../../core/services/site.service';
import { MatchService } from '../../../core/services/match.service';
import { Penalite } from '../../../core/models/penalite.model';
import { DisponibiliteDTO } from '../../../core/models/match.model';
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

  protected readonly dateMin = computed(() => {
    const today = new Date();
    const todayStr = this.toDateStr(today);
    const penalty = this.penaliteActive();
    if (penalty) {
      const [y, m, d] = penalty.dateFin.split('T')[0].split('-').map(Number);
      const dayAfterFin = new Date(y, m - 1, d + 1); // local midnight, day after penalty end
      const dayAfterStr = this.toDateStr(dayAfterFin);
      return dayAfterStr > todayStr ? dayAfterStr : todayStr;
    }
    return todayStr;
  });
  protected readonly dateMax = computed(() => {
    const days = this.auth.role() === 'GLOBAL' ? 21 : 14;
    const d = new Date();
    d.setDate(d.getDate() + days);
    return this.toDateStr(d);
  });
  protected readonly canSelectDate = computed(
    () => this.dateMin() <= this.dateMax()
  );

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
    && this.canSelectDate()
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
        if (result?.type) this.submitMatch(slot.idDispo, result.type);
      });
  }

  private submitMatch(dispoId: number, type: MatchType): void {
    this.isSubmitting.set(true);
    const obs = type === 'PRIVE'
      ? this.matchService.creerPrive({ dispoId })
      : this.matchService.creerPublic({ dispoId });

    obs.subscribe({
      next: (match) => {
        this.isSubmitting.set(false);
        if (type === 'PRIVE') {
          const snackRef = this.snackBar.open('Match créé avec succès', 'Inviter', { duration: 6000 });
          snackRef.onAction().subscribe(() => {
            this.router.navigate(['/matchs', match.idMatch, 'inviter']);
          });
          this.router.navigate(['/dashboard']);
        } else {
          this.snackBar.open('Match créé et visible dans Matchs publics', 'OK', { duration: 4000 });
          this.router.navigate(['/matchs']);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        const message = err.error?.message ?? 'Erreur lors de la création du match';
        this.snackBar.open(message, 'OK', { duration: 5000 });
      },
    });
  }

  private toDateStr(d: Date): string {
    // 'sv' locale reliably outputs YYYY-MM-DD without UTC conversion
    return d.toLocaleDateString('sv');
  }
}
