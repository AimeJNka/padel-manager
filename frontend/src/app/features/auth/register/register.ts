import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { AuthService } from '../../../core/services/auth.service';

interface TypeMembre {
  idType: number;
  libelle: string;
}

interface Site {
  idSite: number;
  nom: string;
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
})
export class Register implements OnInit {

  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly form = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['', Validators.required],
    motDePasse: ['', [Validators.required, Validators.minLength(6)]],
    idType: [null as number | null, [Validators.required, Validators.min(1)]],
    idSite: [null as number | null],
  });

  readonly types = signal<TypeMembre[]>([]);
  readonly sites = signal<Site[]>([]);

  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isLoading = signal(false);
  readonly showSiteField = signal(false);
  readonly registeredMatricule = signal<string | null>(null);

  ngOnInit(): void {
    this.http.get<TypeMembre[]>('/api/types-membres').subscribe({
      next: data => this.types.set(data),
      error: () => this.errorMessage.set('Impossible de charger les types de membres.')
    });

    this.http.get<Site[]>('/api/sites').subscribe({
      next: data => this.sites.set(data),
      error: () => this.errorMessage.set('Impossible de charger les sites.')
    });

    this.form.controls.idType.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(idType => {
        const selectedType = this.types().find(t => t.idType === idType);
        const isSite = selectedType?.libelle === 'SITE';
        this.showSiteField.set(isSite);

        const idSiteCtrl = this.form.controls.idSite;
        if (isSite) {
          idSiteCtrl.setValidators(Validators.required);
        } else {
          idSiteCtrl.clearValidators();
          idSiteCtrl.setValue(null);
        }
        idSiteCtrl.updateValueAndValidity();
      });
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const v = this.form.getRawValue();

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.authService.register({
      nom: v.nom ?? '',
      prenom: v.prenom ?? '',
      email: v.email ?? '',
      telephone: v.telephone ?? '',
      motDePasse: v.motDePasse ?? '',
      idType: v.idType ?? 0,
      idSite: v.idSite ?? undefined,
    }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.registeredMatricule.set(response.matricule);
        this.successMessage.set('Inscription réussie !');
        this.errorMessage.set('');
        setTimeout(() => this.router.navigate(['/dashboard']), 2500);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set("Erreur lors de l'inscription. Vérifiez vos informations.");
      }
    });
  }
}
