import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Paiement } from '../models/paiement.model';
import { PageResponse } from '../models/page.model';

export interface PaiementQueryParams {
  matricule?: string | null;
  statut?: string | null;
  matchId?: number | null;
  siteId?: number | null;
  page?: number | null;
  size?: number | null;
}

@Injectable({ providedIn: 'root' })
export class PaiementService {

  private readonly http = inject(HttpClient);

  getMesPaiements(): Observable<Paiement[]> {
    return this.http.get<Paiement[]>('/api/paiements/me');
  }

  payer(id: number): Observable<Paiement> {
    return this.http.post<Paiement>(`/api/paiements/${id}/payer`, {});
  }

  rembourser(id: number): Observable<Paiement> {
    return this.http.post<Paiement>(`/api/paiements/${id}/rembourser`, {});
  }

  getAllPaiements(params: PaiementQueryParams): Observable<PageResponse<Paiement>> {
    let httpParams = new HttpParams();
    const entries: Array<[string, unknown]> = [
      ['matricule', params.matricule],
      ['statut', params.statut],
      ['matchId', params.matchId],
      ['siteId', params.siteId],
      ['page', params.page],
      ['size', params.size],
    ];

    for (const [key, value] of entries) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }

    return this.http.get<PageResponse<Paiement>>('/api/paiements', { params: httpParams });
  }
}
