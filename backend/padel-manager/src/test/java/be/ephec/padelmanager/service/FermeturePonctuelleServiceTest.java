package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.FermeturePonctuelleDTO;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.FermeturePonctuelle;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.repository.FermeturePonctuelleRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.service.impl.FermeturePonctuelleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FermeturePonctuelleServiceTest {

    @Mock FermeturePonctuelleRepo fermetureRepo;
    @Mock SiteRepo siteRepo;
    @Mock SiteAccessChecker siteAccessChecker;
    @Mock Authentication auth;

    FermeturePonctuelleService service;

    private static final Integer SITE_ID      = 1;
    private static final Integer ID_FERMETURE = 10;

    @BeforeEach
    void setUp() {
        service = new FermeturePonctuelleService(fermetureRepo, siteRepo, siteAccessChecker);
    }

    // ════════════════════════════════════════════════════════════
    // findBySite
    // ════════════════════════════════════════════════════════════

    @Test
    void findBySite_returnsMappedDTOs() {
        Site site = buildSite(SITE_ID);
        FermeturePonctuelle entity = buildFermeturePonctuelle(ID_FERMETURE, LocalDate.of(2026, 1, 15), site);
        when(fermetureRepo.findBySiteIdSite(SITE_ID)).thenReturn(List.of(entity));

        List<FermeturePonctuelleDTO> result = service.findBySite(SITE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdFermeturePonctuelle()).isEqualTo(ID_FERMETURE);
        assertThat(result.get(0).getDateFermeture()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    // ════════════════════════════════════════════════════════════
    // create
    // ════════════════════════════════════════════════════════════

    @Test
    void create_accessDenied_throwsForbidden() {
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(Authentication.class), eq(SITE_ID));
        FermeturePonctuelleDTO dto = new FermeturePonctuelleDTO();
        dto.setDateFermeture(LocalDate.of(2026, 12, 25));

        assertThatThrownBy(() -> service.create(SITE_ID, dto, auth))
                .isInstanceOf(ForbiddenException.class);
        verify(siteRepo, never()).findById(any());
    }

    @Test
    void create_siteNotFound_throwsNotFoundException() {
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.empty());
        FermeturePonctuelleDTO dto = new FermeturePonctuelleDTO();
        dto.setDateFermeture(LocalDate.of(2026, 12, 25));

        assertThatThrownBy(() -> service.create(SITE_ID, dto, auth))
                .isInstanceOf(NotFoundException.class);
        verify(fermetureRepo, never()).save(any());
    }

    @Test
    void create_validRequest_savesAndReturnsDTO() {
        Site site = buildSite(SITE_ID);
        FermeturePonctuelleDTO dto = new FermeturePonctuelleDTO();
        dto.setDateFermeture(LocalDate.of(2026, 12, 25));
        dto.setMotif("Fermeture test");
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.of(site));
        when(fermetureRepo.save(any(FermeturePonctuelle.class))).thenAnswer(i -> i.getArgument(0));

        FermeturePonctuelleDTO result = service.create(SITE_ID, dto, auth);

        ArgumentCaptor<FermeturePonctuelle> captor = ArgumentCaptor.forClass(FermeturePonctuelle.class);
        verify(fermetureRepo).save(captor.capture());
        assertThat(captor.getValue().getSite()).isEqualTo(site);
        assertThat(captor.getValue().getDateFermeture()).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════
    // delete
    // ════════════════════════════════════════════════════════════

    @Test
    void delete_accessDenied_throwsForbidden() {
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(Authentication.class), eq(SITE_ID));

        assertThatThrownBy(() -> service.delete(SITE_ID, ID_FERMETURE, auth))
                .isInstanceOf(ForbiddenException.class);
        verify(fermetureRepo, never()).findById(any());
    }

    @Test
    void delete_fermetureNotFound_throwsNotFoundException() {
        when(fermetureRepo.findById(ID_FERMETURE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(SITE_ID, ID_FERMETURE, auth))
                .isInstanceOf(NotFoundException.class);
        verify(fermetureRepo, never()).deleteById(any());
    }

    @Test
    void delete_fermetureBelongsToDifferentSite_throwsNotFoundException() {
        Site otherSite = buildSite(99);
        FermeturePonctuelle fermeture = buildFermeturePonctuelle(ID_FERMETURE, LocalDate.of(2026, 1, 1), otherSite);
        when(fermetureRepo.findById(ID_FERMETURE)).thenReturn(Optional.of(fermeture));

        assertThatThrownBy(() -> service.delete(SITE_ID, ID_FERMETURE, auth))
                .isInstanceOf(NotFoundException.class);
        verify(fermetureRepo, never()).deleteById(any());
    }

    @Test
    void delete_validRequest_callsDeleteById() {
        Site site = buildSite(SITE_ID);
        FermeturePonctuelle fermeture = buildFermeturePonctuelle(ID_FERMETURE, LocalDate.of(2026, 1, 1), site);
        when(fermetureRepo.findById(ID_FERMETURE)).thenReturn(Optional.of(fermeture));

        service.delete(SITE_ID, ID_FERMETURE, auth);

        verify(fermetureRepo, times(1)).deleteById(ID_FERMETURE);
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

    private static FermeturePonctuelle buildFermeturePonctuelle(Integer id, LocalDate date, Site site) {
        FermeturePonctuelle f = new FermeturePonctuelle();
        f.setIdFermeturePonctuelle(id);
        f.setDateFermeture(date);
        f.setMotif("test motif");
        f.setSite(site);
        return f;
    }
}
