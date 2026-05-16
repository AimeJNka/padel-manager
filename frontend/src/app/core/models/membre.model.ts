export interface Membre {
  matricule: string;
  personne: {
    nom: string;
    prenom: string;
    email: string;
    telephone: string | null;
  };
  typeMembre: {
    idType: number;
    prefixe: string;
    libelle: string;
    delaiReservationJours: number;
    peutCreerMatch: boolean;
  };
  site: {
    idSite: number;
    nom: string;
    adresse: string;
    ville: string;
    actif: boolean;
  } | null;
  dateInscription: string;
  soldeDu: number;
}
