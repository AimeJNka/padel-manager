package be.ephec.padelmanager.service;

import be.ephec.padelmanager.model.*;
import be.ephec.padelmanager.repository.*;
import be.ephec.padelmanager.service.SiteAccessChecker;
import be.ephec.padelmanager.service.impl.DisponibiliteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import be.ephec.padelmanager.exception.NotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// LENIENT: the @BeforeEach stubs are not used in the TODO test (empty body)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisponibiliteServiceTest {

    @Mock DisponibiliteRepo disponibiliteRepo;
    @Mock TerrainRepo terrainRepo;
    @Mock HoraireAnnuelRepo horaireRepo;
    @Mock FermeturePonctuelleRepo fermeturePonctuelleRepo;
    @Mock FermetureRecurrenteRepo fermetureRecurrenteRepo;
    @Mock SiteRepo siteRepo;
    @Mock SiteAccessChecker siteAccessChecker;
    @Mock Authentication authentication;

    @InjectMocks DisponibiliteService service;

    private static final int SITE_ID = 1;
    private static final int ANNEE = 2026;

    private Site site;
    private Terrain terrain;
    private HoraireAnnuel horaire;

    @BeforeEach
    void setUp() {
        site = new Site();
        site.setIdSite(SITE_ID);

        terrain = new Terrain();
        terrain.setIdTerrain(1);
        terrain.setSite(site);
        terrain.setNumero(1);

        horaire = new HoraireAnnuel();
        horaire.setSite(site);
        horaire.setAnnee(ANNEE);
        // 09:00–10:31 → exactly 1 slot (09:00–10:30) fits per day
        horaire.setHeureOuverture(LocalTime.of(9, 0));
        horaire.setHeureFermeture(LocalTime.of(10, 31));

        when(siteRepo.existsById(SITE_ID)).thenReturn(true);
        when(horaireRepo.findBySiteIdSiteAndAnnee(SITE_ID, ANNEE)).thenReturn(Optional.of(horaire));
        when(terrainRepo.findBySiteIdSite(SITE_ID)).thenReturn(List.of(terrain));
        when(fermeturePonctuelleRepo.findBySiteIdSiteOrSiteIsNull(SITE_ID)).thenReturn(Collections.emptyList());
        when(fermetureRecurrenteRepo.findBySiteIdSiteOrSiteIsNull(SITE_ID)).thenReturn(Collections.emptyList());
        when(disponibiliteRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void genererCreneaux_noClosures_oneSlotPerDay() {
        // 2026 = 365 days, 1 terrain, 1 slot/day → 365
        int result = service.genererCreneaux(SITE_ID, ANNEE, authentication);
        assertThat(result).isEqualTo(365);
    }

    @Test
    void genererCreneaux_ponctuelleClosure_skipsDay() {
        FermeturePonctuelle fermeture = new FermeturePonctuelle();
        fermeture.setDateFermeture(LocalDate.of(2026, 1, 1));
        when(fermeturePonctuelleRepo.findBySiteIdSiteOrSiteIsNull(SITE_ID))
                .thenReturn(List.of(fermeture));

        int result = service.genererCreneaux(SITE_ID, ANNEE, authentication);
        assertThat(result).isEqualTo(364); // 365 - 1 closed day
    }

    @Test
    void genererCreneaux_recurrenteClosure_skipsAllMondays() {
        // 2026 has 52 Mondays (Jan 1 = Thursday; Dec 31 = Thursday → 52 full weeks)
        FermetureRecurrente fermeture = new FermetureRecurrente();
        fermeture.setJourSemaine(0); // Monday — ordinal matches DayOfWeek.MONDAY.ordinal()
        when(fermetureRecurrenteRepo.findBySiteIdSiteOrSiteIsNull(SITE_ID))
                .thenReturn(List.of(fermeture));

        int result = service.genererCreneaux(SITE_ID, ANNEE, authentication);
        assertThat(result).isEqualTo(313); // 365 - 52 Mondays
    }

    @Test
    void genererCreneaux_multipleTerrains_multipliesCount() {
        Terrain terrain2 = new Terrain();
        terrain2.setIdTerrain(2);
        terrain2.setSite(site);
        when(terrainRepo.findBySiteIdSite(SITE_ID)).thenReturn(List.of(terrain, terrain2));

        int result = service.genererCreneaux(SITE_ID, ANNEE, authentication);
        assertThat(result).isEqualTo(365 * 2);
    }

    @Test
    void genererCreneaux_9to22Window_sevenSlotsPerDay() {
        // 09:00, 10:45, 12:30, 14:15, 16:00, 17:45, 19:30 → last ends 21:00 ≤ 22:00
        // 21:15 would end at 22:45 → doesn't fit
        horaire.setHeureFermeture(LocalTime.of(22, 0));

        int result = service.genererCreneaux(SITE_ID, ANNEE, authentication);
        assertThat(result).isEqualTo(365 * 7);
    }

    @Test
    void genererCreneaux_siteNotFound_throwsNotFoundException() {
        when(siteRepo.existsById(999)).thenReturn(false);
        assertThatThrownBy(() -> service.genererCreneaux(999, ANNEE, authentication))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void genererCreneaux_noHoraireForYear_throwsNotFoundException() {
        when(horaireRepo.findBySiteIdSiteAndAnnee(SITE_ID, 2099)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.genererCreneaux(SITE_ID, 2099, authentication))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void genererCreneaux_noTerrains_returnsZero() {
        when(terrainRepo.findBySiteIdSite(SITE_ID)).thenReturn(Collections.emptyList());
        int result = service.genererCreneaux(SITE_ID, ANNEE, authentication);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void genererCreneaux_slotEndsExactlyAtClose_isValid() {
        // 09:00–10:30 is exactly 90 min: the slot ends precisely at closeTime → must fit
        horaire.setHeureFermeture(LocalTime.of(10, 30));
        int result = service.genererCreneaux(SITE_ID, ANNEE, authentication);
        assertThat(result).isEqualTo(365); // 1 slot/day, no closures
    }

    @Test
    void regenererCreneaux_preservesReservedSlots() {
        Disponibilite reserved = new Disponibilite();
        reserved.setTerrain(terrain);
        reserved.setDateHeureDebut(LocalDateTime.of(2026, 3, 9, 9, 0));
        reserved.setStatut(DisponibiliteStatus.RESERVE);

        when(disponibiliteRepo.findByTerrainSiteIdSiteAndDateHeureDebutBetween(eq(SITE_ID), any(), any()))
                .thenReturn(List.of(reserved));

        int result = service.regenererCreneaux(SITE_ID, ANNEE, authentication);
        assertThat(result).isEqualTo(364);
        verify(disponibiliteRepo)
                .deleteLibreByTerrainSiteAndYearRange(eq(SITE_ID), any(), any());

    }
}
