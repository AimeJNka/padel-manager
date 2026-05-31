import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { PaiementService } from './paiement.service';

describe('PaiementService', () => {
  let service: PaiementService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PaiementService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getMesPaiements() GETs /api/paiements/me', () => {
    service.getMesPaiements().subscribe();

    const req = httpMock.expectOne('/api/paiements/me');
    expect(req.request.method).toBe('GET');

    req.flush([]);
  });

  it('payer(id) POSTs to /api/paiements/{id}/payer with an empty body', () => {
    service.payer(7).subscribe();

    const req = httpMock.expectOne('/api/paiements/7/payer');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});

    req.flush({ id: 7, statut: 'PAYE' });
  });

  it('rembourser(id) POSTs to /api/paiements/{id}/rembourser with an empty body', () => {
    service.rembourser(7).subscribe();

    const req = httpMock.expectOne('/api/paiements/7/rembourser');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});

    req.flush({ id: 7, statut: 'REMBOURSE' });
  });

  it('annulerParticipation(idMatch) DELETEs /api/matchs/{idMatch}/participation', () => {
    service.annulerParticipation(99).subscribe();

    const req = httpMock.expectOne('/api/matchs/99/participation');
    expect(req.request.method).toBe('DELETE');

    req.flush(null);
  });
});
