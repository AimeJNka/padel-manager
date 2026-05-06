package be.ephec.padelmanager.dto;

import be.ephec.padelmanager.model.Penalite;

import java.time.LocalDateTime;

public record PenaliteDTO(
        Integer idPenalite,
        String matricule,
        String nomJoueur,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        String motif,
        boolean active
) {
    public static PenaliteDTO from(Penalite p) {
        var membre = p.getMembre();
        var personne = membre.getPersonne();
        String nom = personne.getPrenom() + " " + personne.getNom();
        boolean active = p.getDateFin().isAfter(LocalDateTime.now());
        return new PenaliteDTO(
                p.getIdPenalite(),
                membre.getMatricule(),
                nom,
                p.getDateDebut(),
                p.getDateFin(),
                p.getMotif(),
                active
        );
    }
}
