package be.ephec.padelmanager.dto;

import be.ephec.padelmanager.model.Paiement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * soldeInclus: montant de solde_du absorbé lors de ce paiement.
 * Visible au membre propriétaire par conformité avec UC-06 du CDC
 * (affichage du solde dû au membre).
 */
public record PaiementDTO(
        Integer idPaiement,
        Integer idParticipation,
        Integer idMatch,
        String matricule,
        String nomJoueur,
        BigDecimal montant,
        BigDecimal soldeInclus,
        LocalDateTime datePaiement,
        String statut,
        LocalDateTime matchDateHeureDebut,
        String matchType
) {
    public static PaiementDTO from(Paiement p) {
        var participation = p.getParticipation();
        var match = participation.getMatchPadel();
        var membre = participation.getMembre();
        var personne = membre.getPersonne();
        String nomComplet = (personne != null) ? personne.getPrenom() + " " + personne.getNom() : "—";
        return new PaiementDTO(
                p.getIdPaiement(),
                participation.getIdParticipation(),
                match.getIdMatch(),
                membre.getMatricule(),
                nomComplet,
                p.getMontant(),
                p.getSoldeInclus(),
                p.getDatePaiement(),
                p.getStatut(),
                match.getDisponibilite().getDateHeureDebut(),
                match.getTypeMatch()
        );
    }
}
