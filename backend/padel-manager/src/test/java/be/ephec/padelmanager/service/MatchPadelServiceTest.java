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

import java.math.BigDecimal;
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

import be.ephec.padelmanager.config.MatchPolicy;
import be.ephec.padelmanager.dto.MatchPadelDTO;
import be.ephec.padelmanager.exception.ConflictException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.DisponibiliteStatus;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.model.Terrain;
import be.ephec.padelmanager.model.TypeMembre;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
    private static final Integer DISPO_ID = 10;
    private static final Integer SITE_ID = 1;
    private static final Integer TERRAIN_ID = 1;
    private static final String JOUEUR_MATRICULE = "G0002";

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

    private MatchPadel buildStartedMatch(int idMatch, BigDecimal existingSolde) {
        Membre org = new Membre();
        org.setMatricule(ORGANISATEUR);
        org.setSoldeDu(existingSolde);

        MatchPadel match = new MatchPadel();
        match.setIdMatch(idMatch);
        match.setStatut(MatchStatus.EN_ATTENTE);
        match.setOrganisateur(org);
        return match;
    }

    @Test
    void ajouterJoueur_publicMatch_throwsBadRequest() {
        // CF-M-010: organizer must not add players directly to a PUBLIC match.
        Membre organisateur = new Membre();
        organisateur.setMatricule(ORGANISATEUR);

        MatchPadel match = new MatchPadel();
        match.setIdMatch(ID_MATCH);
        match.setTypeMatch(MatchType.PUBLIC);
        match.setStatut(MatchStatus.EN_ATTENTE);
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

    // ── traiterSoldeMatchesDemarres ──────────────────

    @Test
    void traiterSoldeMatchesDemarres_noEligibleMatches_returnsZero() {
        when(matchPadelRepo.findStartedMatchesByStatut(any(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int result = service.traiterSoldeMatchesDemarres();

        assertThat(result).isZero();
        verify(membreRepo, never()).save(any());
        verify(matchPadelRepo, never()).save(any(MatchPadel.class));
    }

    @Test
    void traiterSoldeMatchesDemarres_matchWith4Confirme_noSoldeChangeButTransitionsToDemarre() {
        MatchPadel match = buildStartedMatch(10, null);
        when(matchPadelRepo.findStartedMatchesByStatut(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(4L);

        int result = service.traiterSoldeMatchesDemarres();

        assertThat(result).isEqualTo(1);
        assertThat(match.getStatut()).isEqualTo(MatchStatus.DEMARRE);
        verify(membreRepo, never()).save(any());
        verify(matchPadelRepo).save(match);
    }

    @Test
    void traiterSoldeMatchesDemarres_matchWith3Confirme_addsSoldeOf15() {
        MatchPadel match = buildStartedMatch(10, null);
        when(matchPadelRepo.findStartedMatchesByStatut(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(3L);

        service.traiterSoldeMatchesDemarres();

        assertThat(match.getOrganisateur().getSoldeDu().compareTo(BigDecimal.valueOf(15))).isZero();
    }

    @Test
    void traiterSoldeMatchesDemarres_matchWith0Confirme_addsSoldeOf60() {
        MatchPadel match = buildStartedMatch(10, null);
        when(matchPadelRepo.findStartedMatchesByStatut(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(0L);

        service.traiterSoldeMatchesDemarres();

        assertThat(match.getOrganisateur().getSoldeDu().compareTo(BigDecimal.valueOf(60))).isZero();
    }

    @Test
    void traiterSoldeMatchesDemarres_existingSoldeDu_isIncrementedNotReplaced() {
        MatchPadel match = buildStartedMatch(10, BigDecimal.valueOf(30));
        when(matchPadelRepo.findStartedMatchesByStatut(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(3L);

        service.traiterSoldeMatchesDemarres();

        assertThat(match.getOrganisateur().getSoldeDu().compareTo(BigDecimal.valueOf(45))).isZero();
    }

    @Test
    void traiterSoldeMatchesDemarres_multipleMatches_returnsCorrectCount() {
        MatchPadel match1 = buildStartedMatch(10, null);
        MatchPadel match2 = buildStartedMatch(11, null);
        MatchPadel match3 = buildStartedMatch(12, null);
        when(matchPadelRepo.findStartedMatchesByStatut(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(match1, match2, match3));
        when(participationRepo.countByMatchIdAndStatut(anyInt(), any())).thenReturn(4L);

        int result = service.traiterSoldeMatchesDemarres();

        assertThat(result).isEqualTo(3);
    }

    @Test
    void traiterSoldeMatchesDemarres_matchTransitionsFromEnAttenteToDemarre() {
        MatchPadel match = buildStartedMatch(10, null);
        assertThat(match.getStatut()).isEqualTo(MatchStatus.EN_ATTENTE);
        when(matchPadelRepo.findStartedMatchesByStatut(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(match));
        when(participationRepo.countByMatchIdAndStatut(10, ParticipationStatus.CONFIRME)).thenReturn(4L);

        service.traiterSoldeMatchesDemarres();

        assertThat(match.getStatut()).isEqualTo(MatchStatus.DEMARRE);
    }

    // ════════════════════════════════════════════════════════════
    // creerMatchPrive — creation path with guard variants
    // ════════════════════════════════════════════════════════════

    @Test
    void creerMatchPrive_validRequest_savesMatchInPriveStatusAndEN_ATTENTE() {
        TypeMembre typeMembre = buildTypeMembre(true, 14);
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().plusHours(24), terrain);
        Membre organisateur = buildMembre(ORGANISATEUR, typeMembre, site, null);

        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(organisateur));
        when(disponibiliteRepo.findById(DISPO_ID)).thenReturn(Optional.of(dispo));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);
        when(matchPadelRepo.save(any(MatchPadel.class))).thenAnswer(inv -> inv.getArgument(0));

        service.creerMatchPrive(DISPO_ID, authAs(ORGANISATEUR));

        ArgumentCaptor<MatchPadel> captor = ArgumentCaptor.forClass(MatchPadel.class);
        verify(matchPadelRepo).save(captor.capture());
        MatchPadel saved = captor.getValue();
        assertThat(saved.getTypeMatch()).isEqualTo(MatchType.PRIVE);
        assertThat(saved.getStatut()).isEqualTo(MatchStatus.EN_ATTENTE);
        assertThat(saved.getOrganisateur()).isEqualTo(organisateur);
        assertThat(saved.getMontantTotal()).isEqualByComparingTo(MatchPolicy.PRIX_TOTAL_MATCH);
        assertThat(dispo.getStatut()).isEqualTo(DisponibiliteStatus.RESERVE);
        verify(disponibiliteRepo).save(dispo);
        verify(participationRepo).save(any(Participation.class));
        verify(paiementService).creerPourParticipation(any(Participation.class));
    }

    @Test
    void creerMatchPrive_membreCannotCreateMatch_throwsForbidden() {
        TypeMembre typeMembre = buildTypeMembre(false, null);
        Disponibilite dispo = buildDispo(DISPO_ID, null, null, null);
        Membre organisateur = buildMembre(ORGANISATEUR, typeMembre, null, null);

        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(organisateur));
        when(disponibiliteRepo.findById(DISPO_ID)).thenReturn(Optional.of(dispo));

        assertThatThrownBy(() -> service.creerMatchPrive(DISPO_ID, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(matchPadelRepo, never()).save(any());
        verify(paiementService, never()).creerPourParticipation(any());
    }

    @Test
    void creerMatchPrive_membreHasActivePenalty_throwsForbidden() {
        TypeMembre typeMembre = buildTypeMembre(true, null);
        Disponibilite dispo = buildDispo(DISPO_ID, null, null, null);
        Membre organisateur = buildMembre(ORGANISATEUR, typeMembre, null, null);

        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(organisateur));
        when(disponibiliteRepo.findById(DISPO_ID)).thenReturn(Optional.of(dispo));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(true);

        assertThatThrownBy(() -> service.creerMatchPrive(DISPO_ID, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(matchPadelRepo, never()).save(any());
    }

    @Test
    void creerMatchPrive_membreHasSoldeDu_throwsForbidden() {
        TypeMembre typeMembre = buildTypeMembre(true, null);
        Disponibilite dispo = buildDispo(DISPO_ID, null, null, null);
        Membre organisateur = buildMembre(ORGANISATEUR, typeMembre, null, BigDecimal.valueOf(15));

        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(organisateur));
        when(disponibiliteRepo.findById(DISPO_ID)).thenReturn(Optional.of(dispo));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.creerMatchPrive(DISPO_ID, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(matchPadelRepo, never()).save(any());
    }

    @Test
    void creerMatchPrive_dispoNotLibre_throwsConflict() {
        TypeMembre typeMembre = buildTypeMembre(true, null);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.RESERVE, null, null);
        Membre organisateur = buildMembre(ORGANISATEUR, typeMembre, null, null);

        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(organisateur));
        when(disponibiliteRepo.findById(DISPO_ID)).thenReturn(Optional.of(dispo));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.creerMatchPrive(DISPO_ID, authAs(ORGANISATEUR)))
                .isInstanceOf(ConflictException.class);
        verify(matchPadelRepo, never()).save(any());
    }

    @Test
    void creerMatchPrive_dispoInPast_throwsBadRequest() {
        TypeMembre typeMembre = buildTypeMembre(true, null);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().minusHours(1), null);
        Membre organisateur = buildMembre(ORGANISATEUR, typeMembre, null, null);

        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(organisateur));
        when(disponibiliteRepo.findById(DISPO_ID)).thenReturn(Optional.of(dispo));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.creerMatchPrive(DISPO_ID, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(matchPadelRepo, never()).save(any());
    }

    @Test
    void creerMatchPrive_dispoBeyondReservationWindow_throwsBadRequest() {
        TypeMembre typeMembre = buildTypeMembre(true, 7);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().plusDays(10), null);
        Membre organisateur = buildMembre(ORGANISATEUR, typeMembre, null, null);

        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(organisateur));
        when(disponibiliteRepo.findById(DISPO_ID)).thenReturn(Optional.of(dispo));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.creerMatchPrive(DISPO_ID, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(matchPadelRepo, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════
    // creerMatchPublic — delegation smoke test
    // ════════════════════════════════════════════════════════════

    @Test
    void creerMatchPublic_validRequest_savesMatchInPublicStatus() {
        TypeMembre typeMembre = buildTypeMembre(true, 14);
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().plusHours(24), terrain);
        Membre organisateur = buildMembre(ORGANISATEUR, typeMembre, site, null);

        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(organisateur));
        when(disponibiliteRepo.findById(DISPO_ID)).thenReturn(Optional.of(dispo));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);
        when(matchPadelRepo.save(any(MatchPadel.class))).thenAnswer(inv -> inv.getArgument(0));

        service.creerMatchPublic(DISPO_ID, authAs(ORGANISATEUR));

        ArgumentCaptor<MatchPadel> captor = ArgumentCaptor.forClass(MatchPadel.class);
        verify(matchPadelRepo).save(captor.capture());
        assertThat(captor.getValue().getTypeMatch()).isEqualTo(MatchType.PUBLIC);
        assertThat(captor.getValue().getStatut()).isEqualTo(MatchStatus.EN_ATTENTE);
    }

    // ════════════════════════════════════════════════════════════
    // getMatch — simple lookup
    // ════════════════════════════════════════════════════════════

    @Test
    void getMatch_existingMatch_returnsDTO() {
        MatchPadel match = buildPriveMatch(ID_MATCH, ORGANISATEUR);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        MatchPadelDTO dto = service.getMatch(ID_MATCH, null);

        assertThat(dto).isNotNull();
        assertThat(dto.getIdMatch()).isEqualTo(ID_MATCH);
        assertThat(dto.getTypeMatch()).isEqualTo(MatchType.PRIVE);
        assertThat(dto.getOrganisateur().getMatricule()).isEqualTo(ORGANISATEUR);
    }

    @Test
    void getMatch_unknownId_throwsNotFoundException() {
        when(matchPadelRepo.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMatch(999, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    // ════════════════════════════════════════════════════════════
    // ajouterJoueur — action path with 9 guard scenarios
    // ════════════════════════════════════════════════════════════

    @Test
    void ajouterJoueur_matchNotFound_throwsNotFoundException() {
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs(ORGANISATEUR)))
                .isInstanceOf(NotFoundException.class);
        verify(participationRepo, never()).save(any());
        verify(paiementService, never()).creerPourParticipation(any());
    }

    @Test
    void ajouterJoueur_matchAnnule_throwsBadRequest() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.ANNULE, org, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void ajouterJoueur_callerNotOrganisateur_throwsForbidden() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs("G9999")))
                .isInstanceOf(ForbiddenException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void ajouterJoueur_joueurNotFound_throwsNotFoundException() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(JOUEUR_MATRICULE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs(ORGANISATEUR)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(JOUEUR_MATRICULE);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void ajouterJoueur_joueurHasActivePenalty_throwsForbidden() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, null);
        Membre joueur = buildMembre(JOUEUR_MATRICULE, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(JOUEUR_MATRICULE)).thenReturn(Optional.of(joueur));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(JOUEUR_MATRICULE), any(LocalDateTime.class))).thenReturn(true);

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void ajouterJoueur_joueurHasSoldeDu_throwsForbidden() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, null);
        Membre joueur = buildMembre(JOUEUR_MATRICULE, null, null, BigDecimal.valueOf(15));
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(JOUEUR_MATRICULE)).thenReturn(Optional.of(joueur));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(JOUEUR_MATRICULE), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void ajouterJoueur_joueurAlreadyInscribed_throwsConflict() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, null);
        Membre joueur = buildMembre(JOUEUR_MATRICULE, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(JOUEUR_MATRICULE)).thenReturn(Optional.of(joueur));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(JOUEUR_MATRICULE), any(LocalDateTime.class))).thenReturn(false);
        when(participationRepo.existsByMatchPadelIdMatchAndMembreMatricule(
                ID_MATCH, JOUEUR_MATRICULE)).thenReturn(true);

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs(ORGANISATEUR)))
                .isInstanceOf(ConflictException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void ajouterJoueur_matchAlreadyFull_throwsBadRequest() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, null);
        Membre joueur = buildMembre(JOUEUR_MATRICULE, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(JOUEUR_MATRICULE)).thenReturn(Optional.of(joueur));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(JOUEUR_MATRICULE), any(LocalDateTime.class))).thenReturn(false);
        when(participationRepo.existsByMatchPadelIdMatchAndMembreMatricule(
                ID_MATCH, JOUEUR_MATRICULE)).thenReturn(false);
        when(participationRepo.countByMatchPadelIdMatchAndStatutNot(
                ID_MATCH, ParticipationStatus.ANNULEE)).thenReturn(4L);

        assertThatThrownBy(() -> service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void ajouterJoueur_validRequest_savesParticipationAndCallsPaiementService() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, null);
        Membre joueur = buildMembre(JOUEUR_MATRICULE, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(JOUEUR_MATRICULE)).thenReturn(Optional.of(joueur));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(JOUEUR_MATRICULE), any(LocalDateTime.class))).thenReturn(false);
        when(participationRepo.existsByMatchPadelIdMatchAndMembreMatricule(
                ID_MATCH, JOUEUR_MATRICULE)).thenReturn(false);
        when(participationRepo.countByMatchPadelIdMatchAndStatutNot(
                ID_MATCH, ParticipationStatus.ANNULEE)).thenReturn(3L);

        service.ajouterJoueur(ID_MATCH, JOUEUR_MATRICULE, authAs(ORGANISATEUR));

        ArgumentCaptor<Participation> participationCaptor = ArgumentCaptor.forClass(Participation.class);
        verify(participationRepo).save(participationCaptor.capture());

        ArgumentCaptor<Participation> paiementCaptor = ArgumentCaptor.forClass(Participation.class);
        verify(paiementService).creerPourParticipation(paiementCaptor.capture());

        Participation savedParticipation = participationCaptor.getValue();
        assertThat(savedParticipation.getMatchPadel()).isEqualTo(match);
        assertThat(savedParticipation.getMembre()).isEqualTo(joueur);
        assertThat(savedParticipation.getStatut()).isEqualTo(ParticipationStatus.EN_ATTENTE);
        assertThat(paiementCaptor.getValue()).isSameAs(savedParticipation);
    }

    // ════════════════════════════════════════════════════════════
    // sInscrireMatchPublic — public-match self-registration with 10 guard scenarios
    // ════════════════════════════════════════════════════════════

    @Test
    void sInscrireMatchPublic_matchNotPublic_throwsBadRequest() {
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(participationRepo, never()).save(any());
        verify(paiementService, never()).creerPourParticipation(any());
    }

    @Test
    void sInscrireMatchPublic_matchAnnule_throwsBadRequest() {
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.ANNULE, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void sInscrireMatchPublic_membreHasActivePenalty_throwsForbidden() {
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, null);
        Membre membre = buildMembre(ORGANISATEUR, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(membre));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(true);

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void sInscrireMatchPublic_membreHasSoldeDu_throwsForbidden() {
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, null);
        Membre membre = buildMembre(ORGANISATEUR, null, null, BigDecimal.valueOf(15));
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(membre));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void sInscrireMatchPublic_invalidDispoOrTerrainOrSite_throwsBadRequest() {
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, null);
        Membre membre = buildMembre(ORGANISATEUR, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(membre));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void sInscrireMatchPublic_siteMismatch_throwsForbidden() {
        Site siteA = buildSite(SITE_ID);
        Site siteB = buildSite(2);
        Terrain terrain = buildTerrain(TERRAIN_ID, siteA);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE, null, terrain);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, dispo);
        Membre membre = buildMembre(ORGANISATEUR, null, siteB, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(membre));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void sInscrireMatchPublic_slotInPast_throwsBadRequest() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().minusHours(1), terrain);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, dispo);
        Membre membre = buildMembre(ORGANISATEUR, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(membre));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void sInscrireMatchPublic_slotBeyondReservationWindow_throwsBadRequest() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().plusDays(10), terrain);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, dispo);
        TypeMembre typeMembre = buildTypeMembre(false, 7);
        Membre membre = buildMembre(ORGANISATEUR, typeMembre, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(membre));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void sInscrireMatchPublic_alreadyInscribed_throwsConflict() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().plusHours(24), terrain);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, dispo);
        Membre membre = buildMembre(ORGANISATEUR, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(membre));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);
        when(participationRepo.existsByMatchPadelIdMatchAndMembreMatricule(
                ID_MATCH, ORGANISATEUR)).thenReturn(true);

        assertThatThrownBy(() -> service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(ConflictException.class);
        verify(participationRepo, never()).save(any());
    }

    @Test
    void sInscrireMatchPublic_validRequest_savesParticipationAndCallsPaiementService() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().plusHours(24), terrain);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, dispo);
        Membre membre = buildMembre(ORGANISATEUR, null, null, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(membreRepo.findById(ORGANISATEUR)).thenReturn(Optional.of(membre));
        when(penaliteRepo.existsByMembreMatriculeAndDateFinAfter(
                eq(ORGANISATEUR), any(LocalDateTime.class))).thenReturn(false);
        when(participationRepo.existsByMatchPadelIdMatchAndMembreMatricule(
                ID_MATCH, ORGANISATEUR)).thenReturn(false);
        when(participationRepo.countByMatchPadelIdMatchAndStatutNot(
                ID_MATCH, ParticipationStatus.ANNULEE)).thenReturn(3L);

        service.sInscrireMatchPublic(ID_MATCH, authAs(ORGANISATEUR));

        ArgumentCaptor<Participation> participationCaptor = ArgumentCaptor.forClass(Participation.class);
        verify(participationRepo).save(participationCaptor.capture());

        ArgumentCaptor<Participation> paiementCaptor = ArgumentCaptor.forClass(Participation.class);
        verify(paiementService).creerPourParticipation(paiementCaptor.capture());

        Participation savedParticipation = participationCaptor.getValue();
        assertThat(savedParticipation.getMatchPadel()).isEqualTo(match);
        assertThat(savedParticipation.getMembre()).isEqualTo(membre);
        assertThat(savedParticipation.getStatut()).isEqualTo(ParticipationStatus.EN_ATTENTE);
        assertThat(paiementCaptor.getValue()).isSameAs(savedParticipation);
    }

    // ════════════════════════════════════════════════════════════
    // annulerMatch — cancellation path with temporal + cascade logic
    // ════════════════════════════════════════════════════════════

    @Test
    void annulerMatch_matchNotFound_throwsNotFoundException() {
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.annulerMatch(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(NotFoundException.class);
        verify(disponibiliteRepo, never()).save(any());
        verify(matchPadelRepo, never()).save(any());
    }

    @Test
    void annulerMatch_callerNotOrganisateur_throwsForbidden() {
        Membre autreOrg = buildMembre("AUTRE", null, null, null);
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.RESERVE,
                LocalDateTime.now().plusHours(30), terrain);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, autreOrg, dispo);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.annulerMatch(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(ForbiddenException.class);
        verify(disponibiliteRepo, never()).save(any());
    }

    @Test
    void annulerMatch_matchAlreadyAnnule_throwsBadRequest() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.LIBRE,
                LocalDateTime.now().plusHours(30), terrain);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.ANNULE, org, dispo);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.annulerMatch(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(disponibiliteRepo, never()).save(any());
    }

    @Test
    void annulerMatch_invalidDispo_throwsBadRequest() {
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, org, null);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.annulerMatch(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(disponibiliteRepo, never()).save(any());
    }

    @Test
    void annulerMatch_publicMatchWithin24h_throwsBadRequest() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.RESERVE,
                LocalDateTime.now().plusHours(20), terrain);
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, org, dispo);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.annulerMatch(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(disponibiliteRepo, never()).save(any());
    }

    @Test
    void annulerMatch_priveMatchWithin48h_throwsBadRequest() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.RESERVE,
                LocalDateTime.now().plusHours(40), terrain);
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, dispo);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.annulerMatch(ID_MATCH, authAs(ORGANISATEUR)))
                .isInstanceOf(BadRequestException.class);
        verify(disponibiliteRepo, never()).save(any());
    }

    @Test
    void annulerMatch_publicMatch30hAhead_cascadeProperly() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.RESERVE,
                LocalDateTime.now().plusHours(30), terrain);
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, org, dispo);
        Participation p1 = new Participation();
        p1.setStatut(ParticipationStatus.EN_ATTENTE);
        Participation p2 = new Participation();
        p2.setStatut(ParticipationStatus.EN_ATTENTE);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(participationRepo.findByMatchPadelIdMatch(ID_MATCH)).thenReturn(List.of(p1, p2));

        service.annulerMatch(ID_MATCH, authAs(ORGANISATEUR));

        assertThat(match.getStatut()).isEqualTo(MatchStatus.ANNULE);
        assertThat(dispo.getStatut()).isEqualTo(DisponibiliteStatus.LIBRE);
        assertThat(p1.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(p2.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        verify(paiementService, times(2)).annulerPourParticipation(any());
        verify(disponibiliteRepo).save(dispo);
        verify(participationRepo).saveAll(any(List.class));
        verify(matchPadelRepo).save(match);
    }

    @Test
    void annulerMatch_priveMatch72hAhead_cascadeProperly() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(TERRAIN_ID, site);
        Disponibilite dispo = buildDispo(DISPO_ID, DisponibiliteStatus.RESERVE,
                LocalDateTime.now().plusHours(72), terrain);
        Membre org = buildMembre(ORGANISATEUR, null, null, null);
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PRIVE, MatchStatus.EN_ATTENTE, org, dispo);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(participationRepo.findByMatchPadelIdMatch(ID_MATCH)).thenReturn(List.of());

        service.annulerMatch(ID_MATCH, authAs(ORGANISATEUR));

        assertThat(match.getStatut()).isEqualTo(MatchStatus.ANNULE);
        assertThat(dispo.getStatut()).isEqualTo(DisponibiliteStatus.LIBRE);
        verify(paiementService, never()).annulerPourParticipation(any());
        verify(disponibiliteRepo).save(dispo);
        verify(participationRepo).saveAll(any(List.class));
        verify(matchPadelRepo).save(match);
    }

    /**
     * Tests for listerMatchs verify INVOCATION ONLY:
     * - matchPadelRepo.findAll(Specification, Pageable) is called
     * - Filters propagate correctly through the call
     *
     * The JPA Criteria predicates built inside the Specification lambda
     * are NOT validated at this layer — they require real EntityManager
     * context (@DataJpaTest) to verify SQL generation. This is by design.
     */
    // ════════════════════════════════════════════════════════════
    // listerMatchs — paginated listing (delegation tests only)
    // ════════════════════════════════════════════════════════════

    @Test
    void listerMatchs_allFiltersNull_callsRepoFindAllWithSpec() {
        when(matchPadelRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<MatchPadelDTO> result = service.listerMatchs(null, null, null, null,
                PageRequest.of(0, 10), null);

        verify(matchPadelRepo).findAll(any(Specification.class), any(Pageable.class));
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void listerMatchs_mineTrueWithAuth_callsRepoFindAllWithSpec() {
        MatchPadel match = buildMatch(ID_MATCH, MatchType.PUBLIC, MatchStatus.EN_ATTENTE, null, null);
        when(matchPadelRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<MatchPadelDTO> result = service.listerMatchs(null, null, null, true,
                PageRequest.of(0, 10), authAs(ORGANISATEUR));

        verify(matchPadelRepo).findAll(any(Specification.class), any(Pageable.class));
        assertThat(result.getContent()).hasSize(1);
    }

    // ════ helpers ═════════════════════════════════════════════════

    private static TypeMembre buildTypeMembre(boolean peutCreerMatch, Integer delaiReservationJours) {
        TypeMembre t = new TypeMembre();
        t.setPeutCreerMatch(peutCreerMatch);
        t.setDelaiReservationJours(delaiReservationJours);
        return t;
    }

    private static Site buildSite(Integer idSite) {
        Site s = new Site();
        s.setIdSite(idSite);
        return s;
    }

    private static Terrain buildTerrain(Integer idTerrain, Site site) {
        Terrain t = new Terrain();
        t.setIdTerrain(idTerrain);
        t.setSite(site);
        return t;
    }

    private static Disponibilite buildDispo(Integer idDispo, String status,
                                            LocalDateTime debut, Terrain terrain) {
        Disponibilite d = new Disponibilite();
        d.setIdDispo(idDispo);
        d.setStatut(status);
        d.setDateHeureDebut(debut);
        d.setTerrain(terrain);
        return d;
    }

    private static Membre buildMembre(String matricule, TypeMembre typeMembre,
                                      Site site, BigDecimal soldeDu) {
        Membre m = new Membre();
        m.setMatricule(matricule);
        m.setTypeMembre(typeMembre);
        m.setSite(site);
        m.setSoldeDu(soldeDu);
        return m;
    }

    private static MatchPadel buildMatch(Integer idMatch, String typeMatch, String statut,
                                         Membre organisateur, Disponibilite dispo) {
        MatchPadel m = new MatchPadel();
        m.setIdMatch(idMatch);
        m.setTypeMatch(typeMatch);
        m.setStatut(statut);
        m.setOrganisateur(organisateur);
        m.setDisponibilite(dispo);
        return m;
    }
}
