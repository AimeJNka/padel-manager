import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FermeturePonctuelleDTO } from '../models/site-config.model';

@Injectable({ providedIn: 'root' })
export class FermeturePonctuelleService {
  private readonly http = inject(HttpClient);

  findBySite(idSite: number): Observable<FermeturePonctuelleDTO[]> {
    return this.http.get<FermeturePonctuelleDTO[]>(`/api/sites/${idSite}/fermetures/ponctuelles`);
  }

  create(idSite: number, dto: FermeturePonctuelleDTO): Observable<FermeturePonctuelleDTO> {
    return this.http.post<FermeturePonctuelleDTO>(`/api/sites/${idSite}/fermetures/ponctuelles`, dto);
  }

  delete(idSite: number, idFermeture: number): Observable<void> {
    return this.http.delete<void>(`/api/sites/${idSite}/fermetures/ponctuelles/${idFermeture}`);
  }
}
