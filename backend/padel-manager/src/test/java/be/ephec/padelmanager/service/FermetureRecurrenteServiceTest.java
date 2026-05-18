package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.FermetureRecurrenteDTO;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.FermetureRecurrente;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.repository.FermetureRecurrenteRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.service.impl.FermetureRecurrenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

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
class FermetureRecurrenteServiceTest {

    @Mock FermetureRecurrenteRepo fermetureRepo;
    @Mock SiteRepo siteRepo;
    @Mock SiteAccessChecker siteAccessChecker;
    @Mock Authentication auth;

    FermetureRecurrenteService service;

    private static final Integer SITE_ID      = 1;
    private static final Integer ID_FERMETURE = 10;

    @BeforeEach
    void setUp() {
        service = new FermetureRecurrenteService(fermetureRepo, siteRepo, siteAccessChecker);
    }

    // ════════════════════════════════════════════════════════════
    // findBySite
    // ════════════════════════════════════════════════════════════

    @Test
    void findBySite_returnsMappedDTOs() {
        Site site = buildSite(SITE_ID);
        FermetureRecurrente entity = buildFermetureRecurrente(ID_FERMETURE, 3, site);
        when(fermetureRepo.findBySiteIdSite(SITE_ID)).thenReturn(List.of(entity));

        List<FermetureRecurrenteDTO> result = service.findBySite(SITE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdFermetureRecurrente()).isEqualTo(ID_FERMETURE);
        assertThat(result.get(0).getJourSemaine()).isEqualTo(3);
    }

    // ════════════════════════════════════════════════════════════
    // create
    // ════════════════════════════════════════════════════════════

    @Test
    void create_accessDenied_throwsForbidden() {
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(Authentication.class), eq(SITE_ID));
        FermetureRecurrenteDTO dto = new FermetureRecurrenteDTO();
        dto.setJourSemaine(3);

        assertThatThrownBy(() -> service.create(SITE_ID, dto, auth))
                .isInstanceOf(ForbiddenException.class);
        verify(siteRepo, never()).findById(any());
    }

    @Test
    void create_siteNotFound_throwsNotFoundException() {
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.empty());
        FermetureRecurrenteDTO dto = new FermetureRecurrenteDTO();
        dto.setJourSemaine(3);

        assertThatThrownBy(() -> service.create(SITE_ID, dto, auth))
                .isInstanceOf(NotFoundException.class);
        verify(fermetureRepo, never()).save(any());
    }

    @Test
    void create_validRequest_savesAndReturnsDTO() {
        Site site = buildSite(SITE_ID);
        FermetureRecurrenteDTO dto = new FermetureRecurrenteDTO();
        dto.setJourSemaine(3);
        dto.setMotif("Fermeture test");
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.of(site));
        when(fermetureRepo.save(any(FermetureRecurrente.class))).thenAnswer(i -> i.getArgument(0));

        FermetureRecurrenteDTO result = service.create(SITE_ID, dto, auth);

        ArgumentCaptor<FermetureRecurrente> captor = ArgumentCaptor.forClass(FermetureRecurrente.class);
        verify(fermetureRepo).save(captor.capture());
        assertThat(captor.getValue().getSite()).isEqualTo(site);
        assertThat(captor.getValue().getJourSemaine()).isEqualTo(3);
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
        FermetureRecurrente fermeture = buildFermetureRecurrente(ID_FERMETURE, 3, otherSite);
        when(fermetureRepo.findById(ID_FERMETURE)).thenReturn(Optional.of(fermeture));

        assertThatThrownBy(() -> service.delete(SITE_ID, ID_FERMETURE, auth))
                .isInstanceOf(NotFoundException.class);
        verify(fermetureRepo, never()).deleteById(any());
    }

    @Test
    void delete_validRequest_callsDeleteById() {
        Site site = buildSite(SITE_ID);
        FermetureRecurrente fermeture = buildFermetureRecurrente(ID_FERMETURE, 3, site);
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

    private static FermetureRecurrente buildFermetureRecurrente(Integer id, Integer jourSemaine, Site site) {
        FermetureRecurrente f = new FermetureRecurrente();
        f.setIdFermetureRecurrente(id);
        f.setJourSemaine(jourSemaine);
        f.setMotif("test motif");
        f.setSite(site);
        return f;
    }
}
