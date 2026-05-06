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
        String statut
) {
    public static PaiementDTO from(Paiement p) {
        // TODO M9: mapping null-safe pour personne.getPrenom()/getNom() si données incomplètes en base
        var participation = p.getParticipation();
        var membre = participation.getMembre();
        var personne = membre.getPersonne();
        String nomComplet = personne.getPrenom() + " " + personne.getNom();
        return new PaiementDTO(
                p.getIdPaiement(),
                participation.getIdParticipation(),
                participation.getMatchPadel().getIdMatch(),
                membre.getMatricule(),
                nomComplet,
                p.getMontant(),
                p.getSoldeInclus(),
                p.getDatePaiement(),
                p.getStatut()
        );
    }
}
