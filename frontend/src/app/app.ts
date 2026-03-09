import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

interface Membre {
  matricule: string;
  idPersonne: number;
  idType: number;
  idSite: number | null;
  dateInscription: string;
  soldeDu: number;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.html'
})
export class App {

  private readonly http = inject(HttpClient);

  readonly membres = signal<Membre[]>([]);
  readonly erreur = signal('');

  constructor() {
    this.loadMembres();
  }

  private loadMembres(): void {
    this.http.get<Membre[]>('/api/membres').subscribe({
      next: data => this.membres.set(data),
      error: () => this.erreur.set("Erreur de connexion à l'API")
    });
  }
}
