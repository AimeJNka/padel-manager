package be.ephec.padelmanager.dto;

import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.MatchType;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.PaiementStatus;
import be.ephec.padelmanager.model.Participation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaiementDTOTest {

    private static MatchPadel matchWithDispo(int id, LocalDateTime debut, String type) {
        Disponibilite dispo = new Disponibilite();
        dispo.setDateHeureDebut(debut);

        MatchPadel match = new MatchPadel();
        match.setIdMatch(id);
        match.setDisponibilite(dispo);
        match.setTypeMatch(type);
        return match;
    }

    @Test
    void from_whenPersonneIsNull_returnsFallbackName() {
        Membre membre = new Membre();
        membre.setMatricule("G0001");
        membre.setPersonne(null);

        LocalDateTime debut = LocalDateTime.now().plusDays(2);
        MatchPadel match = matchWithDispo(1, debut, MatchType.PUBLIC);

        Participation participation = new Participation();
        participation.setIdParticipation(10);
        participation.setMembre(membre);
        participation.setMatchPadel(match);

        Paiement paiement = new Paiement();
        paiement.setIdPaiement(42);
        paiement.setParticipation(participation);
        paiement.setMontant(BigDecimal.valueOf(15));
        paiement.setSoldeInclus(BigDecimal.ZERO);
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setStatut(PaiementStatus.PAYE);

        PaiementDTO dto = PaiementDTO.from(paiement);

        assertThat(dto.nomJoueur()).isEqualTo("—");
        assertThat(dto.matricule()).isEqualTo("G0001");
        assertThat(dto.matchDateHeureDebut()).isEqualTo(debut);
        assertThat(dto.matchType()).isEqualTo(MatchType.PUBLIC);
    }

    @Test
    void from_whenPersonneIsPresent_returnsFullName() {
        be.ephec.padelmanager.model.Personne personne = new be.ephec.padelmanager.model.Personne();
        personne.setPrenom("Alice");
        personne.setNom("Dupont");

        Membre membre = new Membre();
        membre.setMatricule("G0002");
        membre.setPersonne(personne);

        LocalDateTime debut = LocalDateTime.now().plusHours(12);
        MatchPadel match = matchWithDispo(2, debut, MatchType.PRIVE);

        Participation participation = new Participation();
        participation.setIdParticipation(11);
        participation.setMembre(membre);
        participation.setMatchPadel(match);

        Paiement paiement = new Paiement();
        paiement.setIdPaiement(43);
        paiement.setParticipation(participation);
        paiement.setMontant(BigDecimal.valueOf(15));
        paiement.setSoldeInclus(BigDecimal.ZERO);
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setStatut(PaiementStatus.PAYE);

        PaiementDTO dto = PaiementDTO.from(paiement);

        assertThat(dto.nomJoueur()).isEqualTo("Alice Dupont");
        assertThat(dto.matchDateHeureDebut()).isEqualTo(debut);
        assertThat(dto.matchType()).isEqualTo(MatchType.PRIVE);
    }
}
