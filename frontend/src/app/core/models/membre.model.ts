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
