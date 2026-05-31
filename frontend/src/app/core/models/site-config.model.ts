import { SiteRef } from './membre.model';

export interface HoraireAnnuelDTO {
  idHoraire?: number;
  site?: SiteRef;
  annee: number;
  heureOuverture: string;
  heureFermeture: string;
}

export interface UpdateHoraireRequest {
  heureOuverture: string;
  heureFermeture: string;
}

export interface FermetureRecurrenteDTO {
  idFermetureRecurrente?: number;
  site?: SiteRef;
  jourSemaine: number;
  motif?: string;
}

export interface FermeturePonctuelleDTO {
  idFermeturePonctuelle?: number;
  site?: SiteRef;
  dateFermeture: string;
  motif?: string;
}
