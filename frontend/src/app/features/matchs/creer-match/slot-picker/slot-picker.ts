import {
  ChangeDetectionStrategy, Component, computed,
  effect, inject, input, output, signal,
} from '@angular/core';

import { DisponibiliteService } from '../../../../core/services/disponibilite.service';
import { DisponibiliteDTO } from '../../../../core/models/match.model';

interface SlotGroup {
  time: string;
  dispos: DisponibiliteDTO[];
}

@Component({
  selector: 'app-slot-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './slot-picker.html',
})
export class SlotPicker {
  // ── Inputs ────────────────────────────────────────────────────────────────
  readonly siteId = input<number | null>(null);
  readonly date   = input<string | null>(null);   // YYYY-MM-DD

  // ── Output ────────────────────────────────────────────────────────────────
  readonly slotSelected = output<DisponibiliteDTO>();

  // ── Services ──────────────────────────────────────────────────────────────
  private readonly dispoService = inject(DisponibiliteService);

  // ── State ─────────────────────────────────────────────────────────────────
  protected readonly slots      = signal<DisponibiliteDTO[]>([]);
  protected readonly selectedId = signal<number | null>(null);
  protected readonly isLoading  = signal(false);
  protected readonly error      = signal<string | null>(null);

  // ── Layout D: grouped by time slot, terrains sorted ASC within each group ─
  protected readonly groups = computed<SlotGroup[]>(() => {
    const map = new Map<string, DisponibiliteDTO[]>();
    for (const dispo of this.slots()) {
      const time = this.formatTime(dispo.dateHeureDebut);
      if (!map.has(time)) map.set(time, []);
      map.get(time)!.push(dispo);
    }
    for (const list of map.values()) {
      list.sort((a, b) => a.terrain.numero - b.terrain.numero);
    }
    return Array.from(map.entries())
      .map(([time, dispos]) => ({ time, dispos }))
      .sort((a, b) => a.time.localeCompare(b.time));
  });

  constructor() {
    effect(() => {
      const s = this.siteId();
      const d = this.date();
      if (s != null && d != null) {
        this.fetchSlots(s, d);
      } else {
        this.slots.set([]);
        this.selectedId.set(null);
      }
    });
  }

  private fetchSlots(siteId: number, date: string): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.selectedId.set(null);
    this.dispoService.lister({ siteId, date, size: 100 }).subscribe({
      next: (page) => {
        this.slots.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les créneaux.');
        this.isLoading.set(false);
      },
    });
  }

  protected onSlotClick(dispo: DisponibiliteDTO): void {
    this.selectedId.set(dispo.idDispo);
    this.slotSelected.emit(dispo);
  }

  protected formatTime(dt: string): string {
    return new Date(dt).toLocaleTimeString('fr-BE', { hour: '2-digit', minute: '2-digit' });
  }
}
