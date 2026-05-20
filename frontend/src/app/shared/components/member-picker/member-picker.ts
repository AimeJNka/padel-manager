import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  HostListener,
  inject,
  input,
  model,
  signal,
} from '@angular/core';
import { EMPTY } from 'rxjs';
import { debounceTime, distinctUntilChanged, finalize, switchMap, tap } from 'rxjs/operators';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';

import { MembreService } from '../../../core/services/membre.service';
import { MembreSearchDTO } from '../../../core/models/membre.model';

@Component({
  selector: 'app-member-picker',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './member-picker.html',
})
export class MemberPicker {
  // ── Injected ──────────────────────────────────────────────────────────────
  private readonly membreService = inject(MembreService);
  private readonly elementRef    = inject(ElementRef);

  // ── Inputs ────────────────────────────────────────────────────────────────
  readonly maxSelection      = input<number>(3);
  readonly placeholder       = input<string>('Rechercher un membre par nom ou matricule');
  readonly excludeMatricules = input<string[]>([]);
  readonly siteId            = input<number | null>(null);

  // ── Two-way binding ───────────────────────────────────────────────────────
  readonly selectedMembers = model<MembreSearchDTO[]>([]);

  // ── Internal state ────────────────────────────────────────────────────────
  protected readonly query        = signal('');
  protected readonly results      = signal<MembreSearchDTO[]>([]);
  protected readonly loading      = signal(false);
  protected readonly showDropdown = signal(false);

  // ── Computed ──────────────────────────────────────────────────────────────
  protected readonly canAddMore = computed(
    () => this.selectedMembers().length < this.maxSelection()
  );

  protected readonly filteredResults = computed(() => {
    const selectedIds = new Set(this.selectedMembers().map(m => m.matricule));
    const excluded    = new Set(this.excludeMatricules());
    return this.results().filter(
      m => !selectedIds.has(m.matricule) && !excluded.has(m.matricule)
    );
  });

  // ── Constructor: wire query → debounce → search ───────────────────────────
  constructor() {
    toObservable(this.query).pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(q => {
        if (q.trim().length >= 2) {
          this.loading.set(true);
          this.showDropdown.set(true);
        } else {
          this.results.set([]);
          this.loading.set(false);
          this.showDropdown.set(q.length > 0);
        }
      }),
      switchMap(q => {
        if (q.trim().length < 2) return EMPTY;
        return this.membreService.search(q, this.siteId() ?? undefined).pipe(
          finalize(() => this.loading.set(false))
        );
      }),
      takeUntilDestroyed(),
    ).subscribe(results => {
      this.results.set(results);
    });
  }

  // ── Handlers ──────────────────────────────────────────────────────────────
  protected onInputChange(value: string): void {
    this.query.set(value);
  }

  protected onResultClick(member: MembreSearchDTO): void {
    if (!this.canAddMore()) return;
    this.selectedMembers.update(prev => [...prev, member]);
    this.query.set('');
    this.results.set([]);
    this.showDropdown.set(false);
  }

  protected onRemoveChip(matricule: string): void {
    this.selectedMembers.update(prev => prev.filter(m => m.matricule !== matricule));
  }

  protected onFocus(): void {
    if (this.query().length > 0) this.showDropdown.set(true);
  }

  @HostListener('document:click', ['$event'])
  protected onClickOutside(event: Event): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.showDropdown.set(false);
    }
  }
}
