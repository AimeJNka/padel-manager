import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { PenaliteService } from './penalite.service';

describe('PenaliteService', () => {
  let service: PenaliteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PenaliteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getMesPenalites() GETs /api/penalites/me and returns the array', () => {
    let received: unknown;
    service.getMesPenalites().subscribe(data => (received = data));

    const req = httpMock.expectOne('/api/penalites/me');
    expect(req.request.method).toBe('GET');

    const payload = [{ id: 1, matricule: 'G0001' }];
    req.flush(payload);

    expect(received).toEqual(payload as never);
  });

  // PenaliteService is the only service in the codebase that explicitly
  // skips the boolean literal "false" when serialising query params:
  //   if (value !== undefined && value !== null && value !== '' && value !== false)
  // Sending activeOnly=false would change the semantic at the backend
  // (returns ALL penalites instead of only active ones), so it must be dropped.
  it('getAllPenalites() omits the activeOnly param when its value is the boolean false', () => {
    service
      .getAllPenalites({ activeOnly: false, matricule: 'G0001', page: 0, size: 20 })
      .subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/penalites');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('activeOnly')).toBe(false);
    expect(req.request.params.get('matricule')).toBe('G0001');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');

    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('annulerPenalite(id) POSTs to /api/penalites/{id}/annuler with an empty body', () => {
    service.annulerPenalite(42).subscribe();

    const req = httpMock.expectOne('/api/penalites/42/annuler');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});

    req.flush({ id: 42, statut: 'ANNULEE' });
  });
});
