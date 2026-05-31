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
}
