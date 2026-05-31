import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';

import { FermetureRecurrenteService } from '../../../core/services/fermeture-recurrente.service';
import { FermetureRecurrenteDTO } from '../../../core/models/site-config.model';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/dialogs/confirm-dialog';

const JOURS_SEMAINE = [
  { value: 0, label: 'Lundi' },
  { value: 1, label: 'Mardi' },
  { value: 2, label: 'Mercredi' },
  { value: 3, label: 'Jeudi' },
  { value: 4, label: 'Vendredi' },
  { value: 5, label: 'Samedi' },
  { value: 6, label: 'Dimanche' },
];

@Component({
  selector: 'app-tab-recurrentes',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './tab-recurrentes.html',
})
export class TabRecurrentes {
  readonly siteId = input.required<number>();

  private readonly service = inject(FermetureRecurrenteService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);

  protected readonly fermetures = signal<FermetureRecurrenteDTO[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly mode = signal<'idle' | 'create'>('idle');

  readonly joursSemaine = JOURS_SEMAINE;

  protected readonly sortedFermetures = computed(() =>
    [...this.fermetures()].sort((a, b) => a.jourSemaine - b.jourSemaine)
  );

  readonly form = this.fb.group({
    jourSemaine: this.fb.control<number | null>(null, Validators.required),
    motif: this.fb.control<string>('', [Validators.required, Validators.maxLength(255)]),
  });

  constructor() {
    effect(() => {
      const id = this.siteId();
      if (id != null) this.loadFermetures(id);
    });
  }

  private loadFermetures(idSite: number): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.service.findBySite(idSite).subscribe({
      next: (rows) => { this.fermetures.set(rows); this.isLoading.set(false); },
      error: () => { this.errorMessage.set('Impossible de charger les fermetures récurrentes.'); this.isLoading.set(false); },
    });
  }

  protected getJourLabel(value: number): string {
    return JOURS_SEMAINE.find(j => j.value === value)?.label ?? '—';
  }

  protected startCreate(): void {
    this.mode.set('create');
    this.form.reset({ jourSemaine: null, motif: '' });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  protected cancel(): void {
    this.mode.set('idle');
    this.form.reset();
    this.errorMessage.set('');
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const { jourSemaine, motif } = this.form.getRawValue();
    const dto: FermetureRecurrenteDTO = { jourSemaine: jourSemaine!, motif: motif! };

    this.service.create(this.siteId(), dto).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set('Fermeture récurrente ajoutée.');
        this.mode.set('idle');
        this.form.reset();
        this.loadFermetures(this.siteId());
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(this.mapError(err));
      },
    });
  }

  protected onDelete(f: FermetureRecurrenteDTO): void {
    if (f.idFermetureRecurrente == null) return;

    const jourLabel = this.getJourLabel(f.jourSemaine);
    const data: ConfirmDialogData = {
      title: 'Supprimer la fermeture récurrente ?',
      message: `Supprimer la fermeture récurrente du ${jourLabel} ? Cette action est irréversible.`,
      confirmLabel: 'Supprimer',
      cancelLabel: 'Annuler',
      destructive: true,
    };

    this.dialog.open(ConfirmDialog, { data, width: '480px' })
      .afterClosed()
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.service.delete(this.siteId(), f.idFermetureRecurrente!).subscribe({
          next: () => {
            this.successMessage.set('Fermeture récurrente supprimée.');
            this.errorMessage.set('');
            this.loadFermetures(this.siteId());
          },
          error: (err: HttpErrorResponse) => {
            this.errorMessage.set(this.mapError(err));
          },
        });
      });
  }

  private mapError(err: HttpErrorResponse): string {
    if (err.status === 403) return "Vous n'avez pas les droits pour gérer ce site.";
    return 'Une erreur est survenue. Réessayez.';
  }
}
