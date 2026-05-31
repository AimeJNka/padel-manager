import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';

// A minimal-but-valid 3-segment JWT whose payload decodes to JSON.
// AuthService.decodeJwtPayload calls atob(token.split('.')[1]) then JSON.parse.
function buildJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ typ: 'JWT', alg: 'HS256' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.sig`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: { navigate: jasmine.Spy };

  beforeEach(() => {
    localStorage.clear();
    routerSpy = { navigate: jasmine.createSpy('navigate') };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('login() POSTs to /api/auth/login with matricule and motDePasse and hydrates the session', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 900;
    const accessToken = buildJwt({ sub: 'G0001', role: 'MEMBRE', exp: futureExp });

    service.login('G0001', 'password').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ matricule: 'G0001', motDePasse: 'password' });

    req.flush({ accessToken, refreshToken: 'r-uuid' });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('MEMBRE');
    expect(service.matricule()).toBe('G0001');
    expect(localStorage.getItem('pm_access_token')).toBe(accessToken);
    expect(localStorage.getItem('pm_refresh_token')).toBe('r-uuid');
  });

  // LOGIN-STATE-CLEAR contract: clearSession() runs synchronously inside login(),
  // BEFORE the HTTP request is dispatched. A stale session must be wiped even if
  // the new request never completes.
  it('login() clears any existing session storage BEFORE issuing the request', () => {
    localStorage.setItem('pm_access_token', 'stale-token');
    localStorage.setItem('pm_refresh_token', 'stale-refresh');
    localStorage.setItem('pm_role', 'MEMBRE');
    localStorage.setItem('pm_id_site', '1');

    service.login('G0001', 'password').subscribe();

    expect(localStorage.getItem('pm_access_token')).toBeNull();
    expect(localStorage.getItem('pm_refresh_token')).toBeNull();
    expect(localStorage.getItem('pm_role')).toBeNull();
    expect(localStorage.getItem('pm_id_site')).toBeNull();

    // Drain the pending request so afterEach.verify() is happy.
    httpMock.expectOne('/api/auth/login').flush({
      accessToken: buildJwt({ sub: 'G0001', role: 'MEMBRE', exp: Math.floor(Date.now() / 1000) + 900 }),
      refreshToken: 'r',
    });
  });

  it('adminLogin() POSTs to /api/auth/admin/login with email and motDePasse', () => {
    const accessToken = buildJwt({
      sub: 'admin.site@padel.be',
      role: 'ADMIN_SITE',
      idSite: 1,
      exp: Math.floor(Date.now() / 1000) + 7200,
    });

    service.adminLogin('admin.site@padel.be', 'password').subscribe();

    const req = httpMock.expectOne('/api/auth/admin/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      email: 'admin.site@padel.be',
      motDePasse: 'password',
    });

    req.flush({ accessToken });

    expect(service.role()).toBe('ADMIN_SITE');
    expect(service.idSite()).toBe(1);
  });

  it('refresh() resolves to false and does NOT hit the network when no refresh token is stored', async () => {
    // localStorage is already empty per beforeEach
    await expectAsync(service.refresh()).toBeResolvedTo(false);
    httpMock.expectNone('/api/auth/refresh');
  });
});
