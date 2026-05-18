package be.ephec.padelmanager.dto;

import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Penalite;
import be.ephec.padelmanager.config.MatchPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PenaliteDTOTest {

    @Test
    void from_whenPersonneIsNull_returnsFallbackName() {
        Membre membre = new Membre();
        membre.setMatricule("G0001");
        membre.setPersonne(null);

        Penalite penalite = new Penalite();
        penalite.setIdPenalite(1);
        penalite.setMembre(membre);
        penalite.setDateDebut(LocalDateTime.now().minusDays(1));
        penalite.setDateFin(LocalDateTime.now().plusDays(MatchPolicy.DUREE_PENALITE_JOURS));
        penalite.setMotif("Absence non justifiée");

        PenaliteDTO dto = PenaliteDTO.from(penalite);

        assertThat(dto.nomJoueur()).isEqualTo("—");
        assertThat(dto.matricule()).isEqualTo("G0001");
    }

    @Test
    void from_whenPersonneIsPresent_returnsFullName() {
        be.ephec.padelmanager.model.Personne personne = new be.ephec.padelmanager.model.Personne();
        personne.setPrenom("Bob");
        personne.setNom("Martin");

        Membre membre = new Membre();
        membre.setMatricule("G0002");
        membre.setPersonne(personne);

        Penalite penalite = new Penalite();
        penalite.setIdPenalite(2);
        penalite.setMembre(membre);
        penalite.setDateDebut(LocalDateTime.now().minusDays(1));
        penalite.setDateFin(LocalDateTime.now().plusDays(MatchPolicy.DUREE_PENALITE_JOURS));
        penalite.setMotif("Comportement");

        PenaliteDTO dto = PenaliteDTO.from(penalite);

        assertThat(dto.nomJoueur()).isEqualTo("Bob Martin");
    }
}
