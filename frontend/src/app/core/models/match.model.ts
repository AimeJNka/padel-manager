export type MatchType = 'PRIVE' | 'PUBLIC';
export type MatchStatut = 'EN_ATTENTE' | 'EFFECTUE' | 'ANNULE';
export type DispoStatut = 'LIBRE' | 'RESERVE';
export type StatutParticipation = 'EN_ATTENTE' | 'CONFIRME' | 'ANNULEE';
export type StatutPaiement = 'EN_ATTENTE' | 'PAYE' | 'ANNULE' | 'REMBOURSE';

export interface SiteDTO {
  idSite: number;
  nom: string;
  adresse: string;
  ville: string;
  actif: boolean;
}

export interface TerrainDTO {
  idTerrain: number;
  site: SiteDTO;
  numero: number;
  statut: string;
}

export interface PersonneDTO {
  idPersonne: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
}

export interface TypeMembreDTO {
  idType: number;
  prefixe: string;
  libelle: string;
  delaiReservationJours: number;
  peutCreerMatch: boolean;
}

export interface MembreDTO {
  matricule: string;
  personne: PersonneDTO;
  typeMembre: TypeMembreDTO;
  site: SiteDTO;
  dateInscription: string;
  soldeDu: number;
}

export interface DisponibiliteDTO {
  idDispo: number;
  terrain: TerrainDTO;
  dateHeureDebut: string;
  dateHeureFin: string;
  statut: DispoStatut;
}

export interface ParticipationDTO {
  idParticipation: number;
  matricule: string;
  prenom: string | null;
  nom: string | null;
  statutParticipation: StatutParticipation;
  statutPaiement: StatutPaiement | null;
  montantPaiement: number | null;
}

export interface MatchPadelDTO {
  idMatch: number;
  disponibilite: DisponibiliteDTO;
  organisateur: MembreDTO;
  typeMatch: MatchType;
  statut: MatchStatut;
  montantTotal: number;
  dateCreation: string;
  participations: ParticipationDTO[];
}

export interface CreerMatchRequest {
  dispoId: number;
}

export interface AjouterJoueurRequest {
  matricule: string;
}
