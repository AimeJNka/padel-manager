package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.PaiementDTO;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.MatchStatus;
import be.ephec.padelmanager.model.MatchType;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.PaiementStatus;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.ParticipationStatus;
import be.ephec.padelmanager.model.Terrain;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PaiementRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.TerrainRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "DB_USERNAME=test",
        "DB_PASSWORD=test",
        "JWT_SECRET=integration-test-secret-key-minimum-32-characters"
})
class PaiementAdminIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired IPaiementService paiementService;
    @Autowired PaiementRepo paiementRepo;
    @Autowired ParticipationRepo participationRepo;
    @Autowired MatchPadelRepo matchPadelRepo;
    @Autowired DisponibiliteRepo disponibiliteRepo;
    @Autowired TerrainRepo terrainRepo;
    @Autowired MembreRepo membreRepo;

    // V3 seed: site 1 terrains = id 1,2,3 ; site 2 terrains = id 4,5
    // V3 seed: membre G0001 (site NULL), S0001 (site 1)
    private static final Authentication ADMIN_GLOBAL =
            new TestingAuthenticationToken("admin_global", null, "ROLE_ADMIN_GLOBAL");

    private Authentication adminSite(int siteId) {
        TestingAuthenticationToken auth =
                new TestingAuthenticationToken("admin_site_" + siteId, null, "ROLE_ADMIN_SITE");
        auth.setDetails(siteId);
        return auth;
    }

    private Paiement seedPaiement(String membreMatricule, int terrainId) {
        Terrain terrain = terrainRepo.findById(terrainId).orElseThrow();
        Membre membre = membreRepo.findById(membreMatricule).orElseThrow();

        Disponibilite dispo = new Disponibilite();
        dispo.setTerrain(terrain);
        dispo.setDateHeureDebut(LocalDateTime.now().plusDays(2));
        dispo.setDateHeureFin(LocalDateTime.now().plusDays(2).plusMinutes(90));
        dispo.setStatut("RESERVE");
        disponibiliteRepo.save(dispo);

        MatchPadel match = new MatchPadel();
        match.setDisponibilite(dispo);
        match.setOrganisateur(membre);
        match.setTypeMatch(MatchType.PUBLIC);
        match.setStatut(MatchStatus.EN_ATTENTE);
        match.setMontantTotal(new BigDecimal("60.00"));
        match.setDateCreation(LocalDateTime.now());
        matchPadelRepo.save(match);

        Participation participation = new Participation();
        participation.setMatchPadel(match);
        participation.setMembre(membre);
        participation.setStatut(ParticipationStatus.EN_ATTENTE);
        participation.setDateInscription(LocalDateTime.now());
        participationRepo.save(participation);

        Paiement paiement = new Paiement();
        paiement.setParticipation(participation);
        paiement.setMontant(new BigDecimal("15.00"));
        paiement.setSoldeInclus(BigDecimal.ZERO);
        paiement.setStatut(PaiementStatus.EN_ATTENTE);
        paiement.setDatePaiement(LocalDateTime.now());
        return paiementRepo.save(paiement);
    }

    @Test
    void listerPaiementsAdmin_globalAdmin_seesAll() {
        seedPaiement("G0001", 1); // site 1
        seedPaiement("G0001", 4); // site 2

        Page<PaiementDTO> result = paiementService.listerPaiementsAdmin(
                null, null, null, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "datePaiement")),
                ADMIN_GLOBAL);

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void listerPaiementsAdmin_siteScope_adminSiteSeesOnlyOwnSite() {
        Paiement site1Pay = seedPaiement("G0001", 1); // site 1
        seedPaiement("G0001", 4);                     // site 2

        Page<PaiementDTO> result = paiementService.listerPaiementsAdmin(
                null, null, null, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "datePaiement")),
                adminSite(1));

        assertThat(result.getContent())
                .extracting(PaiementDTO::idPaiement)
                .contains(site1Pay.getIdPaiement());
        // every returned payment must belong to site 1
        assertThat(result.getContent()).allSatisfy(dto -> {
            Paiement p = paiementRepo.findById(dto.idPaiement()).orElseThrow();
            int site = p.getParticipation().getMatchPadel()
                    .getDisponibilite().getTerrain().getSite().getIdSite();
            assertThat(site).isEqualTo(1);
        });
    }

    @Test
    void listerPaiementsAdmin_sortByDatePaiementDesc() {
        Paiement older = seedPaiement("G0001", 1);
        Paiement newer = seedPaiement("G0001", 1);
        // ensure newer has a strictly later datePaiement
        newer.setDatePaiement(older.getDatePaiement().plusSeconds(10));
        paiementRepo.save(newer);

        Page<PaiementDTO> result = paiementService.listerPaiementsAdmin(
                null, null, null, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "datePaiement")),
                ADMIN_GLOBAL);

        assertThat(result.getContent()).isNotEmpty();
        // first element must have datePaiement >= last element
        LocalDateTime first = result.getContent().get(0).datePaiement();
        LocalDateTime last = result.getContent().get(result.getContent().size() - 1).datePaiement();
        assertThat(first).isAfterOrEqualTo(last);
    }

    @Test
    void listerPaiementsAdmin_pagination_defaultPageSize() {
        for (int i = 0; i < 21; i++) {
            // alternate between 2 terrains on site 1
            seedPaiement("G0001", i % 2 == 0 ? 1 : 2);
        }

        Page<PaiementDTO> page0 = paiementService.listerPaiementsAdmin(
                null, null, null, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "datePaiement")),
                ADMIN_GLOBAL);

        assertThat(page0.getTotalElements()).isGreaterThanOrEqualTo(21);
        assertThat(page0.getContent()).hasSize(20);
        assertThat(page0.getTotalPages()).isGreaterThanOrEqualTo(2);
    }
}
