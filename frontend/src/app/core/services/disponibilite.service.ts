import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { DisponibiliteDTO } from '../models/match.model';
import { PageResponse } from '../models/page.model';

export interface DisponibiliteQueryParams {
  siteId?: number | null;
  terrainId?: number | null;
  date?: string | null;
  statut?: string | null;
  page?: number | null;
  size?: number | null;
}

@Injectable({ providedIn: 'root' })
export class DisponibiliteService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/disponibilites';

  lister(params: DisponibiliteQueryParams = {}): Observable<PageResponse<DisponibiliteDTO>> {
    let httpParams = new HttpParams();
    const entries: Array<[string, unknown]> = [
      ['siteId', params.siteId],
      ['terrainId', params.terrainId],
      ['date', params.date],
      ['statut', params.statut],
      ['page', params.page],
      ['size', params.size],
    ];
    for (const [key, value] of entries) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<DisponibiliteDTO>>(this.baseUrl, { params: httpParams });
  }
}
