import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';

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
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  templateUrl: './register.html',
})
export class Register implements OnInit {

  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly registerForm = this.fb.group({
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

  ngOnInit(): void {
    this.http.get<TypeMembre[]>('/api/types-membres').subscribe({
      next: data => this.types.set(data),
      error: () => this.errorMessage.set('Impossible de charger les types de membres.')
    });

    this.http.get<Site[]>('/api/sites').subscribe({
      next: data => this.sites.set(data),
      error: () => this.errorMessage.set('Impossible de charger les sites.')
    });

    this.registerForm.controls.idType.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(idType => {
        const selectedType = this.types().find(t => t.idType === idType);
        this.showSiteField.set(selectedType?.libelle === 'SITE');

        if (!this.showSiteField()) {
          this.registerForm.controls.idSite.setValue(null);
        }
      });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const v = this.registerForm.getRawValue();

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
      next: () => {
        this.isLoading.set(false);
        this.successMessage.set('Inscription réussie ! Redirection vers la connexion...');
        this.errorMessage.set('');
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set("Erreur lors de l'inscription. Vérifiez vos informations.");
      }
    });
  }
}
