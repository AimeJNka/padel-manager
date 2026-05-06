export type PaiementStatut = 'EN_ATTENTE' | 'PAYE' | 'ANNULE' | 'REMBOURSE';

export interface Paiement {
  idPaiement: number;
  idParticipation: number;
  idMatch: number;
  matricule: string;
  nomJoueur: string;
  montant: number;
  soldeInclus: number;
  datePaiement: string;
  statut: PaiementStatut;
}
