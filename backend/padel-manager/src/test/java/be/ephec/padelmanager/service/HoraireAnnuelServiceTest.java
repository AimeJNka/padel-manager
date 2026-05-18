package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.HoraireAnnuelDTO;
import be.ephec.padelmanager.dto.UpdateHoraireRequest;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.HoraireAnnuel;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.repository.HoraireAnnuelRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.service.impl.HoraireAnnuelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HoraireAnnuelService.
 *
 * Known latent issue (Sprint T-A diagnostic R3): HoraireAnnuelService.update reads
 * horaire.getSite().getIdSite() without a null guard on horaire.getSite(). If the
 * entity has a null site (theoretically impossible due to FK constraint, but not
 * enforced at the Java level), the service throws a raw NullPointerException
 * rather than NotFoundException. This is documented here but NOT locked in tests,
 * since a future refactor may turn this into a proper NotFoundException — and
 * locking the current NPE behavior in a test would prevent that improvement.
 */
@ExtendWith(MockitoExtension.class)
class HoraireAnnuelServiceTest {

    @Mock HoraireAnnuelRepo horaireRepo;
    @Mock SiteRepo siteRepo;
    @Mock SiteAccessChecker siteAccessChecker;
    @Mock Authentication auth;

    HoraireAnnuelService service;

    private static final Integer SITE_ID    = 1;
    private static final Integer ID_HORAIRE = 10;

    @BeforeEach
    void setUp() {
        service = new HoraireAnnuelService(horaireRepo, siteRepo, siteAccessChecker);
    }

    // ════════════════════════════════════════════════════════════
    // findBySite
    // ════════════════════════════════════════════════════════════

    @Test
    void findBySite_returnsMappedDTOs() {
        Site site = buildSite(SITE_ID);
        HoraireAnnuel entity = buildHoraireAnnuel(ID_HORAIRE, 2026,
                LocalTime.of(8, 0), LocalTime.of(22, 0), site);
        when(horaireRepo.findBySiteIdSite(SITE_ID)).thenReturn(List.of(entity));

        List<HoraireAnnuelDTO> result = service.findBySite(SITE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdHoraire()).isEqualTo(ID_HORAIRE);
        assertThat(result.get(0).getAnnee()).isEqualTo(2026);
    }

    // ════════════════════════════════════════════════════════════
    // create
    // ════════════════════════════════════════════════════════════

    @Test
    void create_accessDenied_throwsForbidden() {
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(Authentication.class), eq(SITE_ID));
        HoraireAnnuelDTO dto = new HoraireAnnuelDTO();
        dto.setAnnee(2026);
        dto.setHeureOuverture(LocalTime.of(8, 0));
        dto.setHeureFermeture(LocalTime.of(22, 0));

        assertThatThrownBy(() -> service.create(SITE_ID, dto, auth))
                .isInstanceOf(ForbiddenException.class);
        verify(siteRepo, never()).findById(any());
    }

    @Test
    void create_siteNotFound_throwsNotFoundException() {
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.empty());
        HoraireAnnuelDTO dto = new HoraireAnnuelDTO();
        dto.setAnnee(2026);
        dto.setHeureOuverture(LocalTime.of(8, 0));
        dto.setHeureFermeture(LocalTime.of(22, 0));

        assertThatThrownBy(() -> service.create(SITE_ID, dto, auth))
                .isInstanceOf(NotFoundException.class);
        verify(horaireRepo, never()).save(any());
    }

    @Test
    void create_validRequest_savesAndReturnsDTO() {
        Site site = buildSite(SITE_ID);
        HoraireAnnuelDTO dto = new HoraireAnnuelDTO();
        dto.setAnnee(2026);
        dto.setHeureOuverture(LocalTime.of(8, 0));
        dto.setHeureFermeture(LocalTime.of(22, 0));
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.of(site));
        when(horaireRepo.save(any(HoraireAnnuel.class))).thenAnswer(i -> i.getArgument(0));

        HoraireAnnuelDTO result = service.create(SITE_ID, dto, auth);

        ArgumentCaptor<HoraireAnnuel> captor = ArgumentCaptor.forClass(HoraireAnnuel.class);
        verify(horaireRepo).save(captor.capture());
        assertThat(captor.getValue().getSite()).isEqualTo(site);
        assertThat(captor.getValue().getAnnee()).isEqualTo(2026);
        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════
    // update
    // ════════════════════════════════════════════════════════════

    @Test
    void update_accessDenied_throwsForbidden() {
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(Authentication.class), eq(SITE_ID));
        UpdateHoraireRequest request = new UpdateHoraireRequest();
        request.setHeureOuverture(LocalTime.of(9, 0));
        request.setHeureFermeture(LocalTime.of(21, 0));

        assertThatThrownBy(() -> service.update(SITE_ID, ID_HORAIRE, request, auth))
                .isInstanceOf(ForbiddenException.class);
        verify(horaireRepo, never()).findById(any());
    }

    @Test
    void update_horaireNotFound_throwsNotFoundException() {
        when(horaireRepo.findById(ID_HORAIRE)).thenReturn(Optional.empty());
        UpdateHoraireRequest request = new UpdateHoraireRequest();
        request.setHeureOuverture(LocalTime.of(9, 0));
        request.setHeureFermeture(LocalTime.of(21, 0));

        assertThatThrownBy(() -> service.update(SITE_ID, ID_HORAIRE, request, auth))
                .isInstanceOf(NotFoundException.class);
        verify(horaireRepo, never()).save(any());
    }

    @Test
    void update_horaireBelongsToDifferentSite_throwsNotFoundException() {
        Site otherSite = buildSite(99);
        HoraireAnnuel horaire = buildHoraireAnnuel(ID_HORAIRE, 2026,
                LocalTime.of(8, 0), LocalTime.of(22, 0), otherSite);
        when(horaireRepo.findById(ID_HORAIRE)).thenReturn(Optional.of(horaire));
        UpdateHoraireRequest request = new UpdateHoraireRequest();
        request.setHeureOuverture(LocalTime.of(9, 0));
        request.setHeureFermeture(LocalTime.of(21, 0));

        assertThatThrownBy(() -> service.update(SITE_ID, ID_HORAIRE, request, auth))
                .isInstanceOf(NotFoundException.class);
        verify(horaireRepo, never()).save(any());
    }

    /**
     * Immutability invariant: HoraireAnnuelService.update mutates only
     * heureOuverture and heureFermeture. The annee field is treated as a stable
     * key and is never written by update.
     *
     * This test locks the invariant by verifying the saved entity retains its
     * original annee value (2025) after an update request that does not target
     * the annee field. The UpdateHoraireRequest DTO itself does not expose an
     * annee setter — the invariant is reinforced structurally at the request layer
     * AND verified at the persistence layer.
     */
    @Test
    void update_validRequest_updatesHoursOnlyNotAnnee() {
        Site site = buildSite(SITE_ID);
        HoraireAnnuel horaire = buildHoraireAnnuel(ID_HORAIRE, 2025,
                LocalTime.of(8, 0), LocalTime.of(22, 0), site);
        UpdateHoraireRequest request = new UpdateHoraireRequest();
        request.setHeureOuverture(LocalTime.of(9, 0));
        request.setHeureFermeture(LocalTime.of(21, 0));
        when(horaireRepo.findById(ID_HORAIRE)).thenReturn(Optional.of(horaire));
        when(horaireRepo.save(any(HoraireAnnuel.class))).thenAnswer(i -> i.getArgument(0));

        HoraireAnnuelDTO result = service.update(SITE_ID, ID_HORAIRE, request, auth);

        ArgumentCaptor<HoraireAnnuel> captor = ArgumentCaptor.forClass(HoraireAnnuel.class);
        verify(horaireRepo).save(captor.capture());
        assertThat(captor.getValue().getAnnee()).isEqualTo(2025);                          // UNCHANGED
        assertThat(captor.getValue().getHeureOuverture()).isEqualTo(LocalTime.of(9, 0));   // UPDATED
        assertThat(captor.getValue().getHeureFermeture()).isEqualTo(LocalTime.of(21, 0));  // UPDATED
        assertThat(result).isNotNull();
    }

    // ════ helpers ═════════════════════════════════════════════════

    private static Site buildSite(Integer idSite) {
        Site s = new Site();
        s.setIdSite(idSite);
        s.setNom("Site " + idSite);
        s.setAdresse("Adresse test");
        s.setVille("Bruxelles");
        s.setActif(true);
        return s;
    }

    private static HoraireAnnuel buildHoraireAnnuel(Integer id, Integer annee,
            LocalTime open, LocalTime close, Site site) {
        HoraireAnnuel h = new HoraireAnnuel();
        h.setIdHoraire(id);
        h.setAnnee(annee);
        h.setHeureOuverture(open);
        h.setHeureFermeture(close);
        h.setSite(site);
        return h;
    }
}
