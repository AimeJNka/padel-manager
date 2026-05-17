package be.ephec.padelmanager.service;

import be.ephec.padelmanager.exception.BadRequestException;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.MatchStatus;
import be.ephec.padelmanager.model.MatchType;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.ParticipationStatus;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.impl.MatchPadelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchPadelServiceTest {

    @Mock MatchPadelRepo matchPadelRepo;
    @Mock ParticipationRepo participationRepo;
    @Mock DisponibiliteRepo disponibiliteRepo;
    @Mock MembreRepo membreRepo;
    @Mock PenaliteRepo penaliteRepo;
    @Mock IPaiementService paiementService;
    @Mock IPenaliteService penaliteService;

    MatchPadelService service;

    private static final String ORGANISATEUR = "G0001";
    private static final Integer ID_MATCH = 1;

    @BeforeEach
    void setUp() {
        service = new MatchPadelService(
                matchPadelRepo, participationRepo, disponibiliteRepo, membreRepo,
                penaliteRepo, paiementService, penaliteService);
    }

    private Authentication authAs(String matricule) {
        return new TestingAuthenticationToken(matricule, null, "ROLE_GLOBAL");
    }

    private MatchPadel buildPriveMatch(Integer idMatch, String organisateurMatricule) {
        Membre org = new Membre();
        org.setMatricule(organisateurMatricule);

        Disponibilite dispo = new Disponibilite();
        dispo.setDateHeureDebut(LocalDateTime.now().minusMinutes(30));

        MatchPadel match = new MatchPadel();
        match.setIdMatch(idMatch);
        match.setTypeMatch(MatchType.PRIVE);
        match.setStatut(MatchStatus.EN_ATTENTE);
        match.setOrganisateur(org);
        match.setDisponibilite(dispo);
        return match;
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

    // ── basculerMatchesIncomplets ────────────────────

    @Test
    void basculerMatchesIncomplets_noEligibleMatches_returnsZero() {
        when(matchPadelRepo.findByTypeMatchAndStatutAndDispoDebutBefore(
                eq(MatchType.PRIVE), eq(MatchStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int result = service.basculerMatchesIncomplets();

        assertThat(result).isZero();
        verify(penaliteService, never()).appliquerPenalite(any(), anyInt(), any());
    }

    @Test
    void basculerMatchesIncomplets_privateMatchWith3Confirme_basculesToPublicAndAppliesPenalite() {
        MatchPadel match = buildPriveMatch(10, ORGANISATEUR);
        when(matchPadelRepo.findByTypeMatchAndStatutAndDispoDebutBefore(
                eq(MatchType.PRIVE), eq(MatchStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(3L);

        int result = service.basculerMatchesIncomplets();

        assertThat(result).isEqualTo(1);
        assertThat(match.getTypeMatch()).isEqualTo(MatchType.PUBLIC);
        verify(matchPadelRepo, times(1)).save(match);
        verify(penaliteService, times(1)).appliquerPenalite(eq(match.getOrganisateur()), eq(7), any(String.class));
    }

    @Test
    void basculerMatchesIncomplets_privateMatchWith0Confirme_basculesToPublicAndAppliesPenalite() {
        MatchPadel match = buildPriveMatch(10, ORGANISATEUR);
        when(matchPadelRepo.findByTypeMatchAndStatutAndDispoDebutBefore(
                eq(MatchType.PRIVE), eq(MatchStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(0L);

        int result = service.basculerMatchesIncomplets();

        assertThat(result).isEqualTo(1);
        assertThat(match.getTypeMatch()).isEqualTo(MatchType.PUBLIC);
        verify(penaliteService, times(1)).appliquerPenalite(any(), eq(7), any(String.class));
    }

    @Test
    void basculerMatchesIncomplets_penaliteMotifMatchesExactFormat() {
        MatchPadel match = buildPriveMatch(10, ORGANISATEUR);
        when(matchPadelRepo.findByTypeMatchAndStatutAndDispoDebutBefore(
                eq(MatchType.PRIVE), eq(MatchStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(2L);

        service.basculerMatchesIncomplets();

        ArgumentCaptor<String> motifCaptor = ArgumentCaptor.forClass(String.class);
        verify(penaliteService).appliquerPenalite(any(), anyInt(), motifCaptor.capture());
        assertThat(motifCaptor.getValue()).isEqualTo("Match privé #10 incomplet — UC-03");
    }

    @Test
    void basculerMatchesIncomplets_privateMatchWith4Confirme_noBasculeNoPenalite() {
        MatchPadel match = buildPriveMatch(10, ORGANISATEUR);
        when(matchPadelRepo.findByTypeMatchAndStatutAndDispoDebutBefore(
                eq(MatchType.PRIVE), eq(MatchStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(4L);

        int result = service.basculerMatchesIncomplets();

        assertThat(result).isZero();
        assertThat(match.getTypeMatch()).isEqualTo(MatchType.PRIVE);
        verify(penaliteService, never()).appliquerPenalite(any(), anyInt(), any());
        verify(matchPadelRepo, never()).save(any());
    }
}
