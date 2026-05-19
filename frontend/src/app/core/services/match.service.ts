import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { MatchPadelDTO, CreerMatchRequest, AjouterJoueurRequest } from '../models/match.model';
import { PageResponse } from '../models/page.model';

export interface MatchQueryParams {
  siteId?: number | null;
  statut?: string | null;
  type?: string | null;
  mine?: boolean | null;
  page?: number | null;
  size?: number | null;
}

@Injectable({ providedIn: 'root' })
export class MatchService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/matchs';

  lister(params: MatchQueryParams = {}): Observable<PageResponse<MatchPadelDTO>> {
    let httpParams = new HttpParams();
    const entries: Array<[string, unknown]> = [
      ['siteId', params.siteId],
      ['statut', params.statut],
      ['type', params.type],
      ['mine', params.mine],
      ['page', params.page],
      ['size', params.size],
    ];
    for (const [key, value] of entries) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<MatchPadelDTO>>(this.baseUrl, { params: httpParams });
  }

  getOne(idMatch: number): Observable<MatchPadelDTO> {
    return this.http.get<MatchPadelDTO>(`${this.baseUrl}/${idMatch}`);
  }

  creerPrive(request: CreerMatchRequest): Observable<MatchPadelDTO> {
    return this.http.post<MatchPadelDTO>(`${this.baseUrl}/prive`, request);
  }

  creerPublic(request: CreerMatchRequest): Observable<MatchPadelDTO> {
    return this.http.post<MatchPadelDTO>(`${this.baseUrl}/public`, request);
  }

  ajouterJoueur(idMatch: number, request: AjouterJoueurRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/${idMatch}/joueurs`, request);
  }

  sInscrire(idMatch: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/${idMatch}/inscription`, {});
  }

  annuler(idMatch: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${idMatch}`);
  }

  annulerParticipation(idMatch: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${idMatch}/participation`);
  }
}
