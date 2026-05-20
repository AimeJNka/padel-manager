import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Membre, MembreSearchDTO } from '../models/membre.model';

@Injectable({ providedIn: 'root' })
export class MembreService {

  private readonly http    = inject(HttpClient);
  private readonly baseUrl = '/api/membres';

  getMonProfil(): Observable<Membre> {
    return this.http.get<Membre>(`${this.baseUrl}/me`);
  }

  search(q: string, siteId?: number): Observable<MembreSearchDTO[]> {
    if (q.trim().length < 2) return of([]);
    const params: Record<string, string | number> = { q: q.trim() };
    if (siteId != null) params['siteId'] = siteId;
    return this.http.get<MembreSearchDTO[]>(`${this.baseUrl}/search`, { params });
  }
}
