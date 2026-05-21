package be.ephec.padelmanager.scheduler;

import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.MatchStatus;
import be.ephec.padelmanager.model.MatchType;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.ParticipationStatus;
import be.ephec.padelmanager.model.Penalite;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.repository.TerrainRepo;
import be.ephec.padelmanager.integration.IntegrationTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "DB_USERNAME=test",
        "DB_PASSWORD=test",
        "JWT_SECRET=integration-test-secret-key-minimum-32-characters",
        "spring.task.scheduling.enabled=false"
})
class MatchSchedulerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired MatchScheduler scheduler;
    @Autowired MatchPadelRepo matchPadelRepo;
    @Autowired ParticipationRepo participationRepo;
    @Autowired DisponibiliteRepo disponibiliteRepo;
    @Autowired TerrainRepo terrainRepo;
    @Autowired MembreRepo membreRepo;
    @Autowired PenaliteRepo penaliteRepo;

    // V3 seed membres used throughout:
    //   G0001 — type Global (peut_creer_match=true), no site, soldeDu=0
    //   S0001 — type Site   (peut_creer_match=true), site 1, soldeDu=0
    //   L0001 — type Libre  (peut_creer_match=false), no site, soldeDu=0
    // V3 seed terrains: ids 1–3 (site 1), 4–5 (site 2)

    @Test
    void job1_basculesPrivateMatchIncomplete_persistsTypeChangeAndPenalty() {
        // PRIVE match starting in 12h (inside 24h Job-1 window, outside Job-3 past-start window)
        // 2 CONFIRME participants — below the required 4
        Membre organizer = membreRepo.findById("G0001").orElseThrow();
        Disponibilite dispo = IntegrationTestFixtures.createDisponibilite(terrainRepo, disponibiliteRepo, 1, LocalDateTime.now().plusHours(12));
        MatchPadel match = IntegrationTestFixtures.createMatch(matchPadelRepo, dispo, organizer, MatchType.PRIVE, MatchStatus.EN_ATTENTE);
        IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "G0001", ParticipationStatus.CONFIRME);
        IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "S0001", ParticipationStatus.CONFIRME);

        scheduler.traiterMatchesHoraire();

        MatchPadel result = matchPadelRepo.findById(match.getIdMatch()).orElseThrow();
        assertThat(result.getTypeMatch()).isEqualTo(MatchType.PUBLIC);

        List<Penalite> penalites = penaliteRepo.findByMembreMatriculeOrderByDateDebutDesc("G0001");
        assertThat(penalites).hasSize(1);
        assertThat(penalites.get(0).getMotif())
                .contains("Match privé #")
                .contains("incomplet");
        assertThat(penalites.get(0).getDateFin())
                .isAfter(LocalDateTime.now().plusDays(6))
                .isBefore(LocalDateTime.now().plusDays(8));
    }

    @Test
    void job2_releasesUnpaidParticipations_setsStatutAnnulee() {
        // PUBLIC match starting in 2h — within Job-2's 24h threshold
        // 1 CONFIRME participant must be untouched; 2 EN_ATTENTE must become ANNULEE
        Membre organizer = membreRepo.findById("G0001").orElseThrow();
        Disponibilite dispo = IntegrationTestFixtures.createDisponibilite(terrainRepo, disponibiliteRepo, 1, LocalDateTime.now().plusHours(2));
        MatchPadel match = IntegrationTestFixtures.createMatch(matchPadelRepo, dispo, organizer, MatchType.PUBLIC, MatchStatus.EN_ATTENTE);
        Participation p1 = IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "G0001", ParticipationStatus.CONFIRME);
        Participation p2 = IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "S0001", ParticipationStatus.EN_ATTENTE);
        Participation p3 = IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "L0001", ParticipationStatus.EN_ATTENTE);

        scheduler.traiterMatchesHoraire();

        assertThat(participationRepo.findById(p1.getIdParticipation()).orElseThrow().getStatut())
                .isEqualTo(ParticipationStatus.CONFIRME);
        assertThat(participationRepo.findById(p2.getIdParticipation()).orElseThrow().getStatut())
                .isEqualTo(ParticipationStatus.ANNULEE);
        assertThat(participationRepo.findById(p3.getIdParticipation()).orElseThrow().getStatut())
                .isEqualTo(ParticipationStatus.ANNULEE);
    }

    @Test
    void job3_marksStartedMatchAsDemareAndCreditsSoldeForEmptySlots() {
        // PUBLIC match that started 30 min ago — within Job-3's past-start window
        // 2 CONFIRME → 2 empty slots → organizer soldeDu += 2 × 15 = 30
        // No EN_ATTENTE participants, so Job 2 does nothing; PUBLIC match, so Job 1 is irrelevant
        Membre organizer = membreRepo.findById("G0001").orElseThrow();
        organizer.setSoldeDu(new BigDecimal("10.00"));
        membreRepo.save(organizer);

        Disponibilite dispo = IntegrationTestFixtures.createDisponibilite(terrainRepo, disponibiliteRepo, 1, LocalDateTime.now().minusMinutes(30));
        MatchPadel match = IntegrationTestFixtures.createMatch(matchPadelRepo, dispo, organizer, MatchType.PUBLIC, MatchStatus.EN_ATTENTE);
        IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "G0001", ParticipationStatus.CONFIRME);
        IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "S0001", ParticipationStatus.CONFIRME);

        scheduler.traiterMatchesHoraire();

        // V10 CHECK constraint accepts DEMARRE — proves real DB enforcement
        MatchPadel result = matchPadelRepo.findById(match.getIdMatch()).orElseThrow();
        assertThat(result.getStatut()).isEqualTo(MatchStatus.DEMARRE);

        Membre updated = membreRepo.findById("G0001").orElseThrow();
        assertThat(updated.getSoldeDu().compareTo(new BigDecimal("40.00"))).isZero();
    }

    @Test
    void fullCycle_ordering_job2BeforeJob1BeforeJob3_producesCorrectFinalState() {
        // PRIVE match starting in 30 min: inside both Job-1 and Job-2 windows, outside Job-3's
        // 2 CONFIRME + 1 EN_ATTENTE — Job 2 cancels the EN_ATTENTE, then Job 1 sees 2 < 4 → bascule
        Membre organizer = membreRepo.findById("G0001").orElseThrow();
        organizer.setSoldeDu(BigDecimal.ZERO);
        membreRepo.save(organizer);

        Disponibilite dispo = IntegrationTestFixtures.createDisponibilite(terrainRepo, disponibiliteRepo, 2, LocalDateTime.now().plusMinutes(30));
        MatchPadel match = IntegrationTestFixtures.createMatch(matchPadelRepo, dispo, organizer, MatchType.PRIVE, MatchStatus.EN_ATTENTE);
        IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "G0001", ParticipationStatus.CONFIRME);
        IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "S0001", ParticipationStatus.CONFIRME);
        Participation waitingP = IntegrationTestFixtures.createParticipation(membreRepo, participationRepo, match, "L0001", ParticipationStatus.EN_ATTENTE);

        scheduler.traiterMatchesHoraire();

        // Job 2 ran: the only EN_ATTENTE participation must be ANNULEE
        assertThat(participationRepo.findById(waitingP.getIdParticipation()).orElseThrow().getStatut())
                .isEqualTo(ParticipationStatus.ANNULEE);

        // Job 1 ran: PRIVE → PUBLIC + penalty for organizer (saw 2 CONFIRME < 4)
        MatchPadel result = matchPadelRepo.findById(match.getIdMatch()).orElseThrow();
        assertThat(result.getTypeMatch()).isEqualTo(MatchType.PUBLIC);
        List<Penalite> penalites = penaliteRepo.findByMembreMatriculeOrderByDateDebutDesc("G0001");
        assertThat(penalites).hasSize(1);

        // Job 3 did NOT run: match starts in the future, so statut stays EN_ATTENTE and soldeDu is untouched
        assertThat(result.getStatut()).isEqualTo(MatchStatus.EN_ATTENTE);
        Membre updatedOrganizer = membreRepo.findById("G0001").orElseThrow();
        assertThat(updatedOrganizer.getSoldeDu().compareTo(BigDecimal.ZERO)).isZero();
    }
}
