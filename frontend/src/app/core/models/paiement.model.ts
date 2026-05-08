export type PaiementStatut = 'EN_ATTENTE' | 'PAYE' | 'ANNULE' | 'REMBOURSE';
export type MatchType = 'PRIVE' | 'PUBLIC';

export interface Paiement {
  idPaiement: number;
  idParticipation: number;
  idMatch: number;
  matricule: string;
  nomJoueur: string;
  montant: number;
  soldeInclus: number;
  datePaiement: string | null;
  statut: PaiementStatut;
  matchDateHeureDebut: string;
  matchType: MatchType;
}
