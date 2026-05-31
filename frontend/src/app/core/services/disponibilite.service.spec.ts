import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { DisponibiliteService } from './disponibilite.service';

describe('DisponibiliteService', () => {
  let service: DisponibiliteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DisponibiliteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lister() omits null, undefined and empty-string query params', () => {
    service
      .lister({ siteId: 1, terrainId: null, date: '', statut: 'LIBRE' })
      .subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/disponibilites');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('siteId')).toBe('1');
    expect(req.request.params.get('statut')).toBe('LIBRE');
    expect(req.request.params.has('terrainId')).toBe(false);
    expect(req.request.params.has('date')).toBe(false);
    expect(req.request.params.has('page')).toBe(false);

    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('generer() POSTs to /api/admin/creneaux/generer with siteId and annee', () => {
    service.generer({ siteId: 1, annee: 2026 }).subscribe();

    const req = httpMock.expectOne('/api/admin/creneaux/generer');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ siteId: 1, annee: 2026 });

    req.flush({ generated: 1234 });
  });

  it('regenerer() POSTs to /api/admin/creneaux/regenerer with siteId and annee', () => {
    service.regenerer({ siteId: 2, annee: 2026 }).subscribe();

    const req = httpMock.expectOne('/api/admin/creneaux/regenerer');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ siteId: 2, annee: 2026 });

    req.flush({ generated: 567 });
  });
});
