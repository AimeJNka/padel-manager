import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Site {
  idSite: number;
  nom: string;
  adresse: string;
  ville: string;
  actif: boolean;
}

@Injectable({ providedIn: 'root' })
export class SiteService {

  private readonly http = inject(HttpClient);

  getSites(): Observable<Site[]> {
    return this.http.get<Site[]>('/api/sites');
  }
}
