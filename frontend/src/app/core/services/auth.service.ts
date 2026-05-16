// Refresh is reactive only — triggered by 401 in the interceptor or by hydrate() at startup.
// There is no proactive setInterval auto-refresh: this avoids background activity during
// idle sessions and keeps the service stateless between user interactions.

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, firstValueFrom, catchError, EMPTY, tap } from 'rxjs';

import { LoginResponse, AdminLoginResponse, RegisterRequest, RefreshResponse } from '../models/auth.models';
import { TokenStorageService } from './token-storage.service';

// 30s buffer: guards against clock drift between client and server, and ensures a token
// that would expire before the next request can complete is treated as already expired.
const ACCESS_TOKEN_EXPIRY_BUFFER_MS = 30_000;

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storage = inject(TokenStorageService);

  private readonly _accessToken = signal<string | null>(null);
  private readonly _refreshToken = signal<string | null>(null);
  private readonly _role = signal<string | null>(null);
  private readonly _idSite = signal<number | null>(null);
  private readonly _matricule = signal<string | null>(null);

  readonly isAuthenticated = computed(() => this._accessToken() !== null);
  readonly role = this._role.asReadonly();
  readonly idSite = this._idSite.asReadonly();
  readonly matricule = this._matricule.asReadonly();

  login(matricule: string, motDePasse: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { matricule, motDePasse })
      .pipe(tap(response => this.hydrateFromToken(response.accessToken, response.refreshToken)));
  }

  adminLogin(email: string, motDePasse: string): Observable<AdminLoginResponse> {
    return this.http
      .post<AdminLoginResponse>('/api/auth/admin/login', { email, motDePasse })
      .pipe(tap(response => this.hydrateFromToken(response.accessToken)));
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
      catchError(() => EMPTY)
    ).subscribe();
    this.clearSession();
    this.router.navigate(['/login']);
  }

  /**
   * Exchanges the stored refresh token for a new access token.
   *
   * CONTRACT: never throws. All network and server errors are caught internally.
   * Returns true on success, false on any failure (expired token, revoked, network error).
   * Callers must not wrap this in try/catch.
   */
  async refresh(): Promise<boolean> {
    const refreshToken = this.storage.getRefreshToken();
    if (!refreshToken) return false;

    try {
      const response = await firstValueFrom(
        this.http.post<RefreshResponse>('/api/auth/refresh', { refreshToken })
      );
      this.storage.setAccessToken(response.accessToken);
      this.storage.setRole(response.role.toUpperCase());
      this._accessToken.set(response.accessToken);
      this._role.set(response.role.toUpperCase());
      return true;
    } catch {
      return false;
    }
  }

  hasValidAccessToken(): boolean {
    const token = this._accessToken();
    if (!token) return false;
    const payload = this.decodeJwtPayload(token);
    if (!payload) return false;
    const expMs = (payload['exp'] as number) * 1000;
    return expMs > Date.now() + ACCESS_TOKEN_EXPIRY_BUFFER_MS;
  }

  async hydrate(): Promise<void> {
    const accessToken = this.storage.getAccessToken();
    if (!accessToken) return;

    const payload = this.decodeJwtPayload(accessToken);
    const expMs = payload ? (payload['exp'] as number) * 1000 : 0;

    // Both a decode failure and an expired token mean the access token is unusable;
    // refresh is the recovery path in both cases, not silent eviction.
    const needsRefresh = !payload || expMs <= Date.now() + ACCESS_TOKEN_EXPIRY_BUFFER_MS;

    if (needsRefresh) {
      if (!this.storage.getRefreshToken()) {
        this.storage.clear();
        return;
      }
      const ok = await this.refresh();
      if (!ok) {
        this.storage.clear();
        return;
      }
      // refresh() updated _accessToken and _role; populate remaining signals
      const newToken = this.storage.getAccessToken()!;
      const newPayload = this.decodeJwtPayload(newToken);
      if (newPayload) {
        this._idSite.set((newPayload['idSite'] as number) ?? null);
        this._matricule.set((newPayload['sub'] as string) ?? null);
        this._refreshToken.set(this.storage.getRefreshToken());
      }
      return;
    }

    this.hydrateFromToken(accessToken, this.storage.getRefreshToken() ?? undefined);
  }

  getToken(): string | null {
    return this._accessToken();
  }

  getRole(): string | null {
    return this._role();
  }

  private hydrateFromToken(accessToken: string, refreshToken?: string): void {
    const payload = this.decodeJwtPayload(accessToken);
    if (!payload) return;

    const role = (payload['role'] as string)?.toUpperCase() ?? null;
    const idSite = (payload['idSite'] as number) ?? null;
    const matricule = (payload['sub'] as string) ?? null;

    this._accessToken.set(accessToken);
    this._refreshToken.set(refreshToken ?? null);
    this._role.set(role);
    this._idSite.set(idSite);
    this._matricule.set(matricule);

    this.storage.setAccessToken(accessToken);
    if (refreshToken) this.storage.setRefreshToken(refreshToken);
    if (role) this.storage.setRole(role);
    this.storage.setIdSite(idSite);
  }

  private clearSession(): void {
    this.storage.clear();
    this._accessToken.set(null);
    this._refreshToken.set(null);
    this._role.set(null);
    this._idSite.set(null);
    this._matricule.set(null);
  }

  private decodeJwtPayload(token: string): Record<string, unknown> | null {
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch {
      return null;
    }
  }
}
