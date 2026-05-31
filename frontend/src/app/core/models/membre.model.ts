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
