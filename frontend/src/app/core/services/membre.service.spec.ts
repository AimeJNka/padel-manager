import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { MembreService } from './membre.service';

describe('MembreService', () => {
  let service: MembreService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MembreService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getMonProfil() GETs /api/membres/me', () => {
    service.getMonProfil().subscribe();

    const req = httpMock.expectOne('/api/membres/me');
    expect(req.request.method).toBe('GET');

    req.flush({ matricule: 'G0001', nom: 'Dupont', prenom: 'Jean' });
  });

  // The service short-circuits on very short queries to avoid spamming the
  // backend while the user is still typing. The contract: emit [] synchronously
  // and never reach the network.
  it('search(q) emits an empty array and does NOT call the backend when q.trim().length < 2', () => {
    let received: unknown;
    service.search(' a ').subscribe(value => (received = value));

    expect(received).toEqual([]);
    httpMock.expectNone(r => r.url.startsWith('/api/membres/search'));
  });

  it('search(q, siteId) GETs /api/membres/search with the trimmed query and siteId', () => {
    service.search('  ali  ', 2).subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/membres/search');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('q')).toBe('ali');
    expect(req.request.params.get('siteId')).toBe('2');

    req.flush([]);
  });
});
