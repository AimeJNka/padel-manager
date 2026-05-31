export interface Membre {
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  typeMembre: string;
  siteNom: string | null;
  dateInscription: string;
  soldeDu: number;
}

export interface MembreSearchDTO {
  matricule: string;
  prenom:    string | null;
  nom:       string | null;
  siteNom:   string | null;
}

export interface PersonneRef {
  idPersonne: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
}

export interface TypeMembreRef {
  idType: number;
  prefixe: 'G' | 'S' | 'L';
  libelle: string;
  delaiReservationJours: number;
  peutCreerMatch: boolean;
}

export interface SiteRef {
  idSite: number;
  nom: string;
  adresse: string;
  ville: string;
  actif: boolean;
}

export interface MembreDTO {
  matricule: string;
  personne: PersonneRef | null;
  typeMembre: TypeMembreRef | null;
  site: SiteRef | null;
  dateInscription: string;
  soldeDu: number;
}
