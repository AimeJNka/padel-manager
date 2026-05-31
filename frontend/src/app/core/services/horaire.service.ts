import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { HoraireAnnuelDTO, UpdateHoraireRequest } from '../models/site-config.model';

@Injectable({ providedIn: 'root' })
export class HoraireService {
  private readonly http = inject(HttpClient);

  findBySite(idSite: number): Observable<HoraireAnnuelDTO[]> {
    return this.http.get<HoraireAnnuelDTO[]>(`/api/sites/${idSite}/horaires`);
  }

  create(idSite: number, dto: HoraireAnnuelDTO): Observable<HoraireAnnuelDTO> {
    return this.http.post<HoraireAnnuelDTO>(`/api/sites/${idSite}/horaires`, dto);
  }

  update(idSite: number, idHoraire: number, request: UpdateHoraireRequest): Observable<HoraireAnnuelDTO> {
    return this.http.put<HoraireAnnuelDTO>(`/api/sites/${idSite}/horaires/${idHoraire}`, request);
  }
}
