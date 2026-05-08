import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Penalite } from '../models/penalite.model';
import { PageResponse } from '../models/page.model';

export interface PenaliteQueryParams {
  matricule?: string | null;
  activeOnly?: boolean | null;
  siteId?: number | null;
  page?: number | null;
  size?: number | null;
}

@Injectable({ providedIn: 'root' })
export class PenaliteService {

  private readonly http = inject(HttpClient);

  getMesPenalites(): Observable<Penalite[]> {
    return this.http.get<Penalite[]>('/api/penalites/me');
  }

  getAllPenalites(params: PenaliteQueryParams): Observable<PageResponse<Penalite>> {
    let httpParams = new HttpParams();
    const entries: Array<[string, unknown]> = [
      ['matricule', params.matricule],
      ['activeOnly', params.activeOnly],
      ['siteId', params.siteId],
      ['page', params.page],
      ['size', params.size],
    ];

    for (const [key, value] of entries) {
      if (value !== undefined && value !== null && value !== '' && value !== false) {
        httpParams = httpParams.set(key, String(value));
      }
    }

    return this.http.get<PageResponse<Penalite>>('/api/penalites', { params: httpParams });
  }

  annulerPenalite(id: number): Observable<Penalite> {
    return this.http.post<Penalite>(`/api/penalites/${id}/annuler`, {});
  }
}
