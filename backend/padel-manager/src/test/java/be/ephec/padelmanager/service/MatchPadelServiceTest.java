package be.ephec.padelmanager.service;

import be.ephec.padelmanager.exception.BadRequestException;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.impl.MatchPadelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchPadelServiceTest {

    @Mock MatchPadelRepo matchPadelRepo;
    @Mock ParticipationRepo participationRepo;
    @Mock DisponibiliteRepo disponibiliteRepo;
    @Mock MembreRepo membreRepo;
    @Mock PenaliteRepo penaliteRepo;
    @Mock IPaiementService paiementService;

    MatchPadelService service;

    private static final String ORGANISATEUR = "G0001";
    private static final Integer ID_MATCH = 1;

    @BeforeEach
    void setUp() {
        service = new MatchPadelService(
                matchPadelRepo, participationRepo, disponibiliteRepo, membreRepo, penaliteRepo, paiementService);
    }

    private Authentication authAs(String matricule) {
        return new TestingAuthenticationToken(matricule, null, "ROLE_GLOBAL");
    }

    @Test
    void ajouterJoueur_publicMatch_throwsBadRequest() {
        // CF-M-010: organizer must not add players directly to a PUBLIC match.
        Membre organisateur = new Membre();
        organisateur.setMatricule(ORGANISATEUR);

        MatchPadel match = new MatchPadel();
        match.setIdMatch(ID_MATCH);
        match.setTypeMatch("PUBLIC");
        match.setStatut("EN_ATTENTE");
        match.setOrganisateur(organisateur);

        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, "G0002", authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CF-M-010");
    }
}
