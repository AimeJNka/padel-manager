import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';

import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/dialogs/confirm-dialog';
import { AuthService } from '../../../core/services/auth.service';
import { SiteService, Site } from '../../../core/services/site.service';
import { DisponibiliteService } from '../../../core/services/disponibilite.service';

@Component({
  selector: 'app-generation-creneaux',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, ReactiveFormsModule],
  templateUrl: './generation-creneaux.html',
})
export class GenerationCreneaux implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly siteService = inject(SiteService);
  private readonly dispoService = inject(DisponibiliteService);
  private readonly dialog = inject(MatDialog);

  protected readonly isGlobal = computed(() => this.auth.role() === 'ADMIN_GLOBAL');
  protected readonly sites = signal<Site[]>([]);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string>('');
  protected readonly successMessage = signal<string>('');

  protected readonly currentYear = new Date().getFullYear();
  protected readonly yearOptions = [this.currentYear, this.currentYear + 1];

  readonly form = this.fb.group({
    siteId: this.fb.control<number | null>(null, Validators.required),
    annee:  this.fb.control<number | null>(null, Validators.required),
  });

  protected readonly currentSiteName = computed(() => {
    const id = this.form.controls.siteId.value;
    const list = this.sites();
    if (id == null) return '';
    const s = list.find(x => x.idSite === id);
    return s ? s.nom : '';
  });

  ngOnInit(): void {
    this.siteService.getSites().subscribe({
      next: (sites) => {
        this.sites.set(sites);
        if (!this.isGlobal()) {
          const id = this.auth.idSite();
          if (id != null) this.form.patchValue({ siteId: id });
        }
      },
      error: () => this.errorMessage.set('Impossible de charger la liste des sites.'),
    });

    this.form.valueChanges.subscribe(() => {
      this.successMessage.set('');
      this.errorMessage.set('');
    });
  }

  onGenerer(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.callApi(false);
  }

  onRegenerer(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const annee = this.form.controls.annee.value!;
    const siteName = this.currentSiteName() || `site ${this.form.controls.siteId.value}`;
    const data: ConfirmDialogData = {
      title: 'Régénérer les créneaux ?',
      message: `Les créneaux LIBRES de ${siteName} en ${annee} seront supprimés et recréés selon les horaires actuels. Les créneaux RÉSERVÉS sont préservés.`,
      confirmLabel: 'Régénérer',
      cancelLabel: 'Annuler',
      destructive: true,
    };
    this.dialog.open(ConfirmDialog, { data, width: '480px' })
      .afterClosed()
      .subscribe(confirmed => { if (confirmed) this.callApi(true); });
  }

  private callApi(regenerate: boolean): void {
    const siteId = this.form.controls.siteId.value!;
    const annee = this.form.controls.annee.value!;
    const siteName = this.currentSiteName() || `site ${siteId}`;

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const call$ = regenerate
      ? this.dispoService.regenerer({ siteId, annee })
      : this.dispoService.generer({ siteId, annee });

    call$.subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        const action = regenerate ? 'régénérés' : 'générés';
        this.successMessage.set(`${res.generated} créneaux ${action} pour ${siteName} en ${annee}.`);
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(this.mapError(err, regenerate));
      },
    });
  }

  private mapError(err: HttpErrorResponse, wasRegenerate: boolean): string {
    if (err.status === 403) return "Vous n'avez pas les droits pour gérer ce site.";
    if (err.status === 404) {
      const msg = String(err.error?.message ?? '').toLowerCase();
      if (msg.includes('horaire')) {
        return "Aucun horaire défini pour ce site et cette année. Configurez d'abord les horaires.";
      }
      return 'Site introuvable.';
    }
    if ((err.status === 409 || err.status >= 500) && !wasRegenerate) {
      return 'Des créneaux existent déjà pour ce site et cette année. Utilisez Régénérer pour les recréer.';
    }
    return 'Une erreur est survenue. Réessayez ou contactez un administrateur.';
  }
}
