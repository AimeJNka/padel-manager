import {
  ChangeDetectionStrategy, Component, computed, inject, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../core/services/auth.service';
import { PenaliteService } from '../../../core/services/penalite.service';
import { SiteService, Site } from '../../../core/services/site.service';
import { Penalite } from '../../../core/models/penalite.model';
import { DisponibiliteDTO } from '../../../core/models/match.model';
import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { SlotPicker } from './slot-picker/slot-picker';

@Component({
  selector: 'app-creer-match',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, SlotPicker, DatePipe],
  templateUrl: './creer-match.html',
})
export class CreerMatch {
  private readonly auth = inject(AuthService);

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

  private toDateStr(d: Date): string {
    // 'sv' locale reliably outputs YYYY-MM-DD without UTC conversion
    return d.toLocaleDateString('sv');
  }
}
