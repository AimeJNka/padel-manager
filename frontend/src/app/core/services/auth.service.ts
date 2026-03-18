import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, finalize, EMPTY, catchError } from 'rxjs';

import { LoginResponse, AdminLoginResponse, RegisterRequest } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);

  private readonly tokenSignal = signal<string | null>(null);
  private readonly roleSignal = signal<string | null>(null);

  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);
  readonly role = computed(() => this.roleSignal());

  login(matricule: string, motDePasse: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { matricule, motDePasse })
      .pipe(
        tap(response => {
          this.tokenSignal.set(response.accessToken);
          this.roleSignal.set(response.role.toUpperCase());
        })
      );
  }

  adminLogin(email: string, motDePasse: string): Observable<AdminLoginResponse> {
    return this.http
      .post<AdminLoginResponse>('/api/auth/admin/login', { email, motDePasse })
      .pipe(
        tap(response => {
          this.tokenSignal.set(response.accessToken);
          this.roleSignal.set(response.role.toUpperCase());
        })
      );
  }

  register(data: {
    nom: string;
    prenom: string;
    email: string;
    telephone: string;
    motDePasse: string;
    idType: number;
    idSite?: number;
  }): Observable<unknown> {
    const body: RegisterRequest = {
      nom: data.nom,
      prenom: data.prenom,
      email: data.email,
      telephone: data.telephone,
      motDePasse: data.motDePasse,
      idType: data.idType,
    };

    if (data.idSite != null) {
      body.idSite = data.idSite;
    }

    return this.http.post('/api/auth/register', body);
  }

  logout(): void {
    this.http.post('/api/auth/logout', {}).pipe(
      catchError(() => EMPTY),
      finalize(() => {
        this.tokenSignal.set(null);
        this.roleSignal.set(null);
      })
    ).subscribe();
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  getRole(): string | null {
    return this.roleSignal();
  }
}
