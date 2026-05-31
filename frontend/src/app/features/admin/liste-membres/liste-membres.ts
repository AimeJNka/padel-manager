import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';

import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { MembreService } from '../../../core/services/membre.service';
import { MembreDTO } from '../../../core/models/membre.model';

type TypeFilter = 'ALL' | 'G' | 'S' | 'L';

@Component({
  selector: 'app-liste-membres',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, ReactiveFormsModule, DatePipe, DecimalPipe],
  templateUrl: './liste-membres.html',
})
export class ListeMembres implements OnInit {
  private readonly membreService = inject(MembreService);
  private readonly fb = inject(FormBuilder);

  protected readonly allMembres = signal<MembreDTO[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly searchControl = this.fb.nonNullable.control('');
  protected readonly typeFilterControl = this.fb.nonNullable.control<TypeFilter>('ALL');

  private readonly searchValue = toSignal(this.searchControl.valueChanges, { initialValue: '' });
  private readonly typeFilterValue = toSignal(this.typeFilterControl.valueChanges, { initialValue: 'ALL' as TypeFilter });

  protected readonly filteredMembres = computed(() => {
    const all = this.allMembres();
    const search = this.searchValue().toLowerCase().trim();
    const typeFilter = this.typeFilterValue();

    return all.filter(m => {
      if (typeFilter !== 'ALL' && m.typeMembre?.prefixe !== typeFilter) return false;
      if (!search) return true;
      const hay = `${m.matricule} ${m.personne?.nom ?? ''} ${m.personne?.prenom ?? ''}`.toLowerCase();
      return hay.includes(search);
    });
  });

  ngOnInit(): void {
    this.membreService.listAll().subscribe({
      next: (rows) => {
        this.allMembres.set(rows);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger la liste des membres.');
        this.isLoading.set(false);
      },
    });
  }
}
