import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';

import { AuthService } from '../../../core/services/auth.service';
import { LoginResponse, AdminLoginResponse } from '../../../core/models/auth.models';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTabsModule,
  ],
  templateUrl: './login.html',
})
export class Login {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly isAdminMode = signal(false);

  readonly memberForm = this.fb.nonNullable.group({
    matricule: ['', Validators.required],
    motDePasse: ['', Validators.required],
  });

  readonly adminForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', Validators.required],
  });

  readonly errorMessage = signal('');
  readonly isLoading = signal(false);

  switchMode(adminMode: boolean): void {
    this.isAdminMode.set(adminMode);
    this.errorMessage.set('');
    this.memberForm.reset();
    this.adminForm.reset();
  }

  onSubmit(): void {
    const form = this.isAdminMode() ? this.adminForm : this.memberForm;

    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    let request$: Observable<LoginResponse | AdminLoginResponse>;

    if (this.isAdminMode()) {
      const { email, motDePasse } = this.adminForm.getRawValue();
      request$ = this.authService.adminLogin(email, motDePasse);
    } else {
      const { matricule, motDePasse } = this.memberForm.getRawValue();
      request$ = this.authService.login(matricule, motDePasse);
    }

    request$.subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.status === 401
            ? 'Identifiants incorrects.'
            : 'Une erreur est survenue, veuillez réessayer.'
        );
      }
    });
  }
}
