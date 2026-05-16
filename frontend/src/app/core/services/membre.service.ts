import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Membre } from '../models/membre.model';

@Injectable({ providedIn: 'root' })
export class MembreService {

  private readonly http = inject(HttpClient);

  getMonProfil(): Observable<Membre> {
    return this.http.get<Membre>('/api/membres/me');
  }
}
