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
  template: `
    <div style="padding: 2rem; font-family: Arial;">
      <h1>Padel Manager — Test BDD</h1>

      <h2>Membres en base</h2>

      <table style="border-collapse: collapse;">
        <thead>
        <tr>
          <th>Matricule</th>
          <th>id_personne</th>
          <th>id_type</th>
          <th>id_site</th>
          <th>Date inscription</th>
          <th>Solde dû</th>
        </tr>
        </thead>
        <tbody>
          @for (m of membres(); track m.matricule) {
            <tr>
              <td>{{ m.matricule }}</td>
              <td>{{ m.idPersonne }}</td>
              <td>{{ m.idType }}</td>
              <td>{{ m.idSite ?? 'NULL' }}</td>
              <td>{{ m.dateInscription }}</td>
              <td>{{ m.soldeDu }}€</td>
            </tr>
          }
        </tbody>
      </table>

      @if (membres().length === 0) {
        <p>Aucun membre trouvé.</p>
      }

      @if (erreur()) {
        <p style="color:red">{{ erreur() }}</p>
      }
    </div>
  `
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
