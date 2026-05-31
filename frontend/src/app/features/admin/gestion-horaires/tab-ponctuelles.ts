import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { DatePipe } from '@angular/common';

import { FermeturePonctuelleService } from '../../../core/services/fermeture-ponctuelle.service';
import { FermeturePonctuelleDTO } from '../../../core/models/site-config.model';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/dialogs/confirm-dialog';

@Component({
  selector: 'app-tab-ponctuelles',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, DatePipe],
  providers: [DatePipe],
  templateUrl: './tab-ponctuelles.html',
})
export class TabPonctuelles {
  readonly siteId = input.required<number>();

  private readonly service = inject(FermeturePonctuelleService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly datePipe = inject(DatePipe);

  protected readonly fermetures = signal<FermeturePonctuelleDTO[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly mode = signal<'idle' | 'create'>('idle');

  protected readonly todayStr = computed(() => {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  });

  protected readonly sortedFermetures = computed(() =>
    [...this.fermetures()].sort((a, b) => a.dateFermeture.localeCompare(b.dateFermeture))
  );

  readonly form = this.fb.group({
    dateFermeture: this.fb.control<string | null>(null, Validators.required),
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
      error: () => { this.errorMessage.set('Impossible de charger les fermetures ponctuelles.'); this.isLoading.set(false); },
    });
  }

  protected startCreate(): void {
    this.mode.set('create');
    this.form.reset({ dateFermeture: null, motif: '' });
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

    const { dateFermeture, motif } = this.form.getRawValue();
    const dto: FermeturePonctuelleDTO = { dateFermeture: dateFermeture!, motif: motif! };

    this.service.create(this.siteId(), dto).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set('Fermeture ponctuelle ajoutée.');
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

  protected onDelete(f: FermeturePonctuelleDTO): void {
    if (f.idFermeturePonctuelle == null) return;

    const dateLabel = this.datePipe.transform(f.dateFermeture, 'dd/MM/yyyy') ?? f.dateFermeture;
    const data: ConfirmDialogData = {
      title: 'Supprimer la fermeture ponctuelle ?',
      message: `Supprimer la fermeture du ${dateLabel} ? Cette action est irréversible.`,
      confirmLabel: 'Supprimer',
      cancelLabel: 'Annuler',
      destructive: true,
    };

    this.dialog.open(ConfirmDialog, { data, width: '480px' })
      .afterClosed()
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.service.delete(this.siteId(), f.idFermeturePonctuelle!).subscribe({
          next: () => {
            this.successMessage.set('Fermeture ponctuelle supprimée.');
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
