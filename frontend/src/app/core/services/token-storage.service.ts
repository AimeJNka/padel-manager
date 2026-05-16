import { Injectable } from '@angular/core';

const ACCESS_TOKEN_KEY = 'pm_access_token';
const REFRESH_TOKEN_KEY = 'pm_refresh_token';
const ROLE_KEY = 'pm_role';
const ID_SITE_KEY = 'pm_id_site';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  setAccessToken(token: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, token);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  }

  getRole(): string | null {
    return localStorage.getItem(ROLE_KEY);
  }

  setRole(role: string): void {
    localStorage.setItem(ROLE_KEY, role);
  }

  getIdSite(): number | null {
    const raw = localStorage.getItem(ID_SITE_KEY);
    if (raw === null) return null;
    const parsed = parseInt(raw, 10);
    return isNaN(parsed) ? null : parsed;
  }

  setIdSite(idSite: number | null): void {
    if (idSite === null) {
      localStorage.removeItem(ID_SITE_KEY);
    } else {
      localStorage.setItem(ID_SITE_KEY, String(idSite));
    }
  }

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(ID_SITE_KEY);
  }
}
