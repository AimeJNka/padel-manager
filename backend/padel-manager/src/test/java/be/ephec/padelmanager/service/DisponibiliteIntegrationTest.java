package be.ephec.padelmanager.service;

import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.DisponibiliteStatus;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        // Placeholders in application.yml must resolve even though
        // @ServiceConnection overrides the actual datasource used
        "DB_USERNAME=test",
        "DB_PASSWORD=test",
        "JWT_SECRET=integration-test-secret-key-minimum-32-characters"
})
class DisponibiliteIntegrationTest {

    // static → one container shared across all tests in this class (Flyway runs once)
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    IDisponibiliteService disponibiliteService;

    @Autowired
    DisponibiliteRepo disponibiliteRepo;

    private static final Authentication ADMIN_GLOBAL =
            new TestingAuthenticationToken("admin", null, "ROLE_ADMIN_GLOBAL");

    @Test
    void genererCreneaux_site1_2026_correctCount() {
        int result = disponibiliteService.genererCreneaux(1, 2026, ADMIN_GLOBAL);
        assertThat(result).isEqualTo(7623);
    }

    @Test
    void genererCreneaux_site2_2026_correctCount() {
        int result = disponibiliteService.genererCreneaux(2, 2026, ADMIN_GLOBAL);
        assertThat(result).isEqualTo(3732);
    }

    @Test
    void genererCreneaux_unknownSite_throwsNotFoundException() {
        assertThatThrownBy(() -> disponibiliteService.genererCreneaux(999, 2026, ADMIN_GLOBAL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void genererCreneaux_site1_jan2_correctSlotStartTimes() {
        // Jan 1 is a global closure → first open day is Jan 2
        disponibiliteService.genererCreneaux(1, 2026, ADMIN_GLOBAL);

        List<Disponibilite> jan2Slots = disponibiliteRepo
                .findByTerrainSiteIdSiteAndDateHeureDebutBetween(1,
                        LocalDateTime.of(2026, 1, 2, 0, 0),
                        LocalDateTime.of(2026, 1, 2, 23, 59));

        // 3 terrains × 7 slots = 21 slots on Jan 2
        assertThat(jan2Slots).hasSize(21);

        // Verify the 7 start times for terrain id=1
        List<LocalTime> startTimes = jan2Slots.stream()
                .filter(d -> d.getTerrain().getIdTerrain() == 1)
                .map(d -> d.getDateHeureDebut().toLocalTime())
                .sorted()
                .collect(Collectors.toList());

        assertThat(startTimes).containsExactly(
                LocalTime.of(9, 0),
                LocalTime.of(10, 45),
                LocalTime.of(12, 30),
                LocalTime.of(14, 15),
                LocalTime.of(16, 0),
                LocalTime.of(17, 45),
                LocalTime.of(19, 30)
        );
    }

    @Test
    void regenererCreneaux_preservesReservedAndRegeneratesLibre() {
        // Step 1 — generate initial slots
        disponibiliteService.genererCreneaux(1, 2026, ADMIN_GLOBAL);

        // Step 2 — mark 3 slots on Jan 2 as RESERVE (simulating bookings)
        List<Disponibilite> jan2Slots = disponibiliteRepo
                .findByTerrainSiteIdSiteAndDateHeureDebutBetween(1,
                        LocalDateTime.of(2026, 1, 2, 0, 0),
                        LocalDateTime.of(2026, 1, 2, 23, 59));
        jan2Slots.subList(0, 3).forEach(d -> {
            d.setStatut(DisponibiliteStatus.RESERVE);
            disponibiliteRepo.save(d);
        });

        // Step 3 — regenerate
        int regenerated = disponibiliteService.regenererCreneaux(1, 2026, ADMIN_GLOBAL);

        // 7623 total - 3 reserved = 7620 new LIBRE slots
        assertThat(regenerated).isEqualTo(7620);

        // Step 4 — verify the 3 RESERVE slots survived
        long reserveCount = disponibiliteRepo
                .findByTerrainSiteIdSiteAndDateHeureDebutBetween(1,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 12, 31, 23, 59))
                .stream()
                .filter(d -> DisponibiliteStatus.RESERVE.equals(d.getStatut()))
                .count();
        assertThat(reserveCount).isEqualTo(3);
    }
}
