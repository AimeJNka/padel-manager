import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { HoraireService } from '../../../core/services/horaire.service';
import { HoraireAnnuelDTO, UpdateHoraireRequest } from '../../../core/models/site-config.model';

function timeOrderValidator(group: AbstractControl): ValidationErrors | null {
  const ouv = group.get('heureOuverture')?.value;
  const fer = group.get('heureFermeture')?.value;
  if (!ouv || !fer) return null;
  return ouv < fer ? null : { timeOrder: true };
}

@Component({
  selector: 'app-tab-horaires',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './tab-horaires.html',
})
export class TabHoraires {
  readonly siteId = input.required<number>();

  private readonly horaireService = inject(HoraireService);
  private readonly fb = inject(FormBuilder);

  protected readonly horaires = signal<HoraireAnnuelDTO[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly mode = signal<'idle' | 'create' | 'edit'>('idle');
  protected readonly editingId = signal<number | null>(null);

  protected readonly isFormVisible = computed(() => this.mode() !== 'idle');

  readonly currentYear = new Date().getFullYear();
  readonly yearOptions = [this.currentYear, this.currentYear + 1, this.currentYear + 2];

  readonly form = this.fb.group({
    annee: this.fb.control<number | null>(null, Validators.required),
    heureOuverture: this.fb.control<string>('', Validators.required),
    heureFermeture: this.fb.control<string>('', Validators.required),
  }, { validators: [timeOrderValidator] });

  constructor() {
    effect(() => {
      const id = this.siteId();
      if (id != null) this.loadHoraires(id);
    });
  }

  private loadHoraires(idSite: number): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.horaireService.findBySite(idSite).subscribe({
      next: (rows) => { this.horaires.set(rows); this.isLoading.set(false); },
      error: () => { this.errorMessage.set('Impossible de charger les horaires.'); this.isLoading.set(false); },
    });
  }

  protected startCreate(): void {
    this.mode.set('create');
    this.editingId.set(null);
    this.form.reset({ annee: null, heureOuverture: '', heureFermeture: '' });
    this.form.controls.annee.enable();
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  protected startEdit(h: HoraireAnnuelDTO): void {
    this.mode.set('edit');
    this.editingId.set(h.idHoraire ?? null);
    this.form.setValue({
      annee: h.annee,
      heureOuverture: h.heureOuverture.substring(0, 5),
      heureFermeture: h.heureFermeture.substring(0, 5),
    });
    this.form.controls.annee.disable();
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  protected cancel(): void {
    this.mode.set('idle');
    this.editingId.set(null);
    this.form.reset();
    this.form.controls.annee.enable();
    this.errorMessage.set('');
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const { annee, heureOuverture, heureFermeture } = this.form.getRawValue();

    if (this.mode() === 'create') {
      const dto: HoraireAnnuelDTO = {
        annee: annee!,
        heureOuverture: heureOuverture!,
        heureFermeture: heureFermeture!,
      };
      this.horaireService.create(this.siteId(), dto).subscribe({
        next: () => this.handleSuccess('Horaire créé.'),
        error: (err: HttpErrorResponse) => this.handleError(err, true),
      });
    } else {
      const id = this.editingId()!;
      const request: UpdateHoraireRequest = {
        heureOuverture: heureOuverture!,
        heureFermeture: heureFermeture!,
      };
      this.horaireService.update(this.siteId(), id, request).subscribe({
        next: () => this.handleSuccess('Horaire mis à jour.'),
        error: (err: HttpErrorResponse) => this.handleError(err, false),
      });
    }
  }

  private handleSuccess(msg: string): void {
    this.isSubmitting.set(false);
    this.successMessage.set(msg);
    this.mode.set('idle');
    this.editingId.set(null);
    this.form.reset();
    this.form.controls.annee.enable();
    this.loadHoraires(this.siteId());
  }

  private handleError(err: HttpErrorResponse, wasCreate: boolean): void {
    this.isSubmitting.set(false);
    if (err.status === 403) {
      this.errorMessage.set("Vous n'avez pas les droits pour gérer ce site.");
    } else if (err.status >= 500 && wasCreate) {
      this.errorMessage.set("Un horaire existe déjà pour cette année sur ce site. Utilisez Modifier pour le mettre à jour.");
    } else {
      this.errorMessage.set('Une erreur est survenue. Réessayez.');
    }
  }
}
