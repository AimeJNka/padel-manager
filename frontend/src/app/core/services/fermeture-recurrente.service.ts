import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FermetureRecurrenteDTO } from '../models/site-config.model';

@Injectable({ providedIn: 'root' })
export class FermetureRecurrenteService {
  private readonly http = inject(HttpClient);

  findBySite(idSite: number): Observable<FermetureRecurrenteDTO[]> {
    return this.http.get<FermetureRecurrenteDTO[]>(`/api/sites/${idSite}/fermetures/recurrentes`);
  }
}
