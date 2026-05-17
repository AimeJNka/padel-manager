package be.ephec.padelmanager.service;

import be.ephec.padelmanager.exception.BadRequestException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.PaiementStatus;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.ParticipationStatus;
import be.ephec.padelmanager.model.Penalite;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PaiementRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.impl.ParticipationService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock MatchPadelRepo matchPadelRepo;
    @Mock ParticipationRepo participationRepo;
    @Mock PaiementRepo paiementRepo;
    @Mock MembreRepo membreRepo;
    @Mock PenaliteRepo penaliteRepo;

    ParticipationService service;

    private static final String MATRICULE = "G0001";
    private static final Integer ID_MATCH = 1;

    @BeforeEach
    void setUp() {
        service = new ParticipationService(
                matchPadelRepo, participationRepo, paiementRepo, membreRepo, penaliteRepo);
    }

    private Authentication authAs(String matricule) {
        return new TestingAuthenticationToken(matricule, null, "ROLE_GLOBAL");
    }

    /**
     * Builds the entity graph required by annulerParticipation.
     * matchStart controls the dateHeureDebut of the disponibilite.
     */
    private Participation buildParticipation(LocalDateTime matchStart, String paiementStatut,
                                             BigDecimal soldeDuInitial, String participationStatut) {
        Membre membre = new Membre();
        membre.setMatricule(MATRICULE);
        membre.setSoldeDu(soldeDuInitial);

        Disponibilite dispo = new Disponibilite();
        dispo.setIdDispo(10);
        dispo.setDateHeureDebut(matchStart);

        MatchPadel match = new MatchPadel();
        match.setIdMatch(ID_MATCH);
        match.setDisponibilite(dispo);
        match.setMontantTotal(new BigDecimal("60.00"));

        Participation participation = new Participation();
        participation.setIdParticipation(7);
        participation.setMembre(membre);
        participation.setMatchPadel(match);
        participation.setStatut(participationStatut);

        Paiement paiement = new Paiement();
        paiement.setIdPaiement(99);
        paiement.setParticipation(participation);
        paiement.setMontant(new BigDecimal("15.00"));
        paiement.setSoldeInclus(BigDecimal.ZERO);
        paiement.setStatut(paiementStatut);

        // Wire repository mocks for the happy path
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(participationRepo.findByMatchPadelIdMatchAndMembreMatricule(ID_MATCH, MATRICULE))
                .thenReturn(Optional.of(participation));
        // lenient: not used in guard-clause tests (alreadyCancelled, matchInPast) that throw before paiement lookup
        lenient().when(paiementRepo.findByParticipation(participation)).thenReturn(Optional.of(paiement));

        return participation;
    }

    // ── Early cancellation (>= 24h) ─────────────────

    @Test
    void annulerParticipation_earlyCancel_unpaid() {
        LocalDateTime start = LocalDateTime.now().plusHours(25);
        Participation participation = buildParticipation(start, PaiementStatus.EN_ATTENTE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);
        Paiement paiement = paiementRepo.findByParticipation(participation).orElseThrow();

        service.annulerParticipation(ID_MATCH, authAs(MATRICULE));

        assertThat(participation.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(paiement.getStatut()).isEqualTo(PaiementStatus.ANNULE);
        assertThat(participation.getMembre().getSoldeDu()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(penaliteRepo, never()).save(any());
        verify(membreRepo, never()).save(any());
    }

    @Test
    void annulerParticipation_earlyCancel_paid() {
        LocalDateTime start = LocalDateTime.now().plusHours(25);
        Participation participation = buildParticipation(start, PaiementStatus.PAYE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);
        Paiement paiement = paiementRepo.findByParticipation(participation).orElseThrow();

        service.annulerParticipation(ID_MATCH, authAs(MATRICULE));

        assertThat(participation.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(paiement.getStatut()).isEqualTo(PaiementStatus.REMBOURSE);
        assertThat(participation.getMembre().getSoldeDu()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(penaliteRepo, never()).save(any());
        verify(membreRepo, never()).save(any());
    }

    // ── Late cancellation (< 24h) ───────────────────

    @Test
    void annulerParticipation_lateCancel_unpaid() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.plusHours(10);
        Participation participation = buildParticipation(start, PaiementStatus.EN_ATTENTE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);
        Paiement paiement = paiementRepo.findByParticipation(participation).orElseThrow();

        service.annulerParticipation(ID_MATCH, authAs(MATRICULE));

        assertThat(participation.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(paiement.getStatut()).isEqualTo(PaiementStatus.ANNULE);
        assertThat(participation.getMembre().getSoldeDu()).isEqualByComparingTo(new BigDecimal("15.00"));
        verify(membreRepo, times(1)).save(participation.getMembre());

        ArgumentCaptor<Penalite> penCaptor = ArgumentCaptor.forClass(Penalite.class);
        verify(penaliteRepo, times(1)).save(penCaptor.capture());
        Penalite saved = penCaptor.getValue();
        assertThat(saved.getMotif()).isEqualTo("ANNULATION_TARDIVE");
        assertThat(saved.getMembre().getMatricule()).isEqualTo(MATRICULE);
        // dateFin within 1 second of now+7d
        long deltaSeconds = Math.abs(ChronoUnit.SECONDS.between(
                saved.getDateFin(), LocalDateTime.now().plusDays(7)));
        assertThat(deltaSeconds).isLessThanOrEqualTo(1);
    }

    @Test
    void annulerParticipation_lateCancel_paid() {
        LocalDateTime start = LocalDateTime.now().plusHours(10);
        Participation participation = buildParticipation(start, PaiementStatus.PAYE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);
        Paiement paiement = paiementRepo.findByParticipation(participation).orElseThrow();

        service.annulerParticipation(ID_MATCH, authAs(MATRICULE));

        assertThat(participation.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        // Payment absorbed as late-cancel fee → paiement moves to ANNULE, no extra debt
        assertThat(paiement.getStatut()).isEqualTo(PaiementStatus.ANNULE);
        assertThat(participation.getMembre().getSoldeDu()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(membreRepo, never()).save(participation.getMembre());
        verify(penaliteRepo, times(1)).save(any(Penalite.class));
    }

    // ── Validation errors ───────────────────────────

    @Test
    void annulerParticipation_notParticipant_throwsForbidden() {
        // Non-participant calling DELETE must receive 403, not 404 (F-10: no state leak).
        MatchPadel match = new MatchPadel();
        match.setIdMatch(ID_MATCH);
        when(matchPadelRepo.findById(ID_MATCH)).thenReturn(Optional.of(match));
        when(participationRepo.findByMatchPadelIdMatchAndMembreMatricule(ID_MATCH, MATRICULE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.annulerParticipation(ID_MATCH, authAs(MATRICULE)))
                .isInstanceOf(ForbiddenException.class);

        verify(participationRepo, never()).save(any());
        verify(paiementRepo, never()).save(any());
    }

    @Test
    void annulerParticipation_alreadyCancelled() {
        LocalDateTime start = LocalDateTime.now().plusHours(25);
        buildParticipation(start, PaiementStatus.ANNULE, BigDecimal.ZERO, ParticipationStatus.ANNULEE);

        assertThatThrownBy(() -> service.annulerParticipation(ID_MATCH, authAs(MATRICULE)))
                .isInstanceOf(BadRequestException.class);

        verify(participationRepo, never()).save(any());
        verify(paiementRepo, never()).save(any());
        verify(penaliteRepo, never()).save(any());
    }

    @Test
    void annulerParticipation_matchInPast() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        buildParticipation(start, PaiementStatus.EN_ATTENTE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);

        assertThatThrownBy(() -> service.annulerParticipation(ID_MATCH, authAs(MATRICULE)))
                .isInstanceOf(BadRequestException.class);

        verify(participationRepo, never()).save(any());
        verify(paiementRepo, never()).save(any());
        verify(penaliteRepo, never()).save(any());
    }

    // ── 24h boundary ────────────────────────────────

    @Test
    void annulerParticipation_boundary_exactly24h() {
        // +2s buffer absorbs LocalDateTime.now() drift between test and service — still rounds to 24h
        LocalDateTime start = LocalDateTime.now().plusHours(24).plusSeconds(2);
        Participation participation = buildParticipation(start, PaiementStatus.EN_ATTENTE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);
        Paiement paiement = paiementRepo.findByParticipation(participation).orElseThrow();

        service.annulerParticipation(ID_MATCH, authAs(MATRICULE));

        assertThat(participation.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(paiement.getStatut()).isEqualTo(PaiementStatus.ANNULE);
        assertThat(participation.getMembre().getSoldeDu()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(penaliteRepo, never()).save(any());
        verify(membreRepo, never()).save(any());
    }

    @Test
    void annulerParticipation_boundary_23h59m() {
        // 23h59m => truncates to 23 => late
        LocalDateTime start = LocalDateTime.now().plusHours(23).plusMinutes(59);
        Participation participation = buildParticipation(start, PaiementStatus.EN_ATTENTE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);
        Paiement paiement = paiementRepo.findByParticipation(participation).orElseThrow();

        service.annulerParticipation(ID_MATCH, authAs(MATRICULE));

        assertThat(participation.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(paiement.getStatut()).isEqualTo(PaiementStatus.ANNULE);
        assertThat(participation.getMembre().getSoldeDu()).isEqualByComparingTo(new BigDecimal("15.00"));
        verify(penaliteRepo, times(1)).save(any(Penalite.class));
        verify(membreRepo, times(1)).save(any(Membre.class));
    }

    // ── Additional crash / edge-case guards ─────────

    @Test
    void annulerParticipation_boundary_exactly24h_isPAYE() {
        // Exactly at the 24h boundary, member already PAID.
        // Spec: hoursRestantes < delaiRequis => exactly 24h is NOT late => REMBOURSE, never absorbed as fee.
        LocalDateTime start = LocalDateTime.now().plusHours(24).plusSeconds(2);
        Participation participation = buildParticipation(start, PaiementStatus.PAYE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);
        Paiement paiement = paiementRepo.findByParticipation(participation).orElseThrow();

        service.annulerParticipation(ID_MATCH, authAs(MATRICULE));

        assertThat(participation.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(paiement.getStatut()).isEqualTo(PaiementStatus.REMBOURSE);
        assertThat(participation.getMembre().getSoldeDu()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(penaliteRepo, never()).save(any());
        verify(membreRepo, never()).save(any());
    }

    @Test
    void annulerParticipation_lateCancel_paid_paymentAbsorbed() {
        // Duplicate-charge guard: late + PAYE must NOT add debt nor save the membre.
        // Penalty is issued, paiement moves to ANNULE (absorbed as late-cancel fee).
        LocalDateTime start = LocalDateTime.now().plusHours(5);
        Participation participation = buildParticipation(start, PaiementStatus.PAYE, BigDecimal.ZERO, ParticipationStatus.EN_ATTENTE);
        Paiement paiement = paiementRepo.findByParticipation(participation).orElseThrow();

        service.annulerParticipation(ID_MATCH, authAs(MATRICULE));

        assertThat(participation.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(paiement.getStatut()).isEqualTo(PaiementStatus.ANNULE);
        assertThat(participation.getMembre().getSoldeDu()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(membreRepo, never()).save(any());
        verify(penaliteRepo, times(1)).save(any(Penalite.class));
    }

    // ── libererPlacesNonPayees ───────────────────────

    @Test
    void libererPlacesNonPayees_noEligibleParticipations_returnsZero() {
        when(participationRepo.findByStatutAndMatchDispoDebutBefore(
                eq(ParticipationStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int result = service.libererPlacesNonPayees();

        assertThat(result).isZero();
    }

    @Test
    void libererPlacesNonPayees_enAttente_setToAnnulee() {
        Participation p = new Participation();
        p.setStatut(ParticipationStatus.EN_ATTENTE);
        when(participationRepo.findByStatutAndMatchDispoDebutBefore(
                eq(ParticipationStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(p));

        int result = service.libererPlacesNonPayees();

        assertThat(result).isEqualTo(1);
        assertThat(p.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        verify(participationRepo, times(1)).saveAll(List.of(p));
    }

    @Test
    void libererPlacesNonPayees_multipleParticipations_allSetToAnnulee() {
        Participation p1 = new Participation(); p1.setStatut(ParticipationStatus.EN_ATTENTE);
        Participation p2 = new Participation(); p2.setStatut(ParticipationStatus.EN_ATTENTE);
        Participation p3 = new Participation(); p3.setStatut(ParticipationStatus.EN_ATTENTE);
        when(participationRepo.findByStatutAndMatchDispoDebutBefore(
                eq(ParticipationStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(p1, p2, p3));

        int result = service.libererPlacesNonPayees();

        assertThat(result).isEqualTo(3);
        assertThat(p1.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(p2.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(p3.getStatut()).isEqualTo(ParticipationStatus.ANNULEE);
    }

    @Test
    void libererPlacesNonPayees_emptyBatch_saveAllNotCalled() {
        when(participationRepo.findByStatutAndMatchDispoDebutBefore(
                eq(ParticipationStatus.EN_ATTENTE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        service.libererPlacesNonPayees();

        verify(participationRepo, never()).saveAll(any());
    }
}
