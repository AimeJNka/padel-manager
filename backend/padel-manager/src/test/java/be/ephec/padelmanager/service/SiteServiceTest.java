package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.dto.UpdateSiteRequest;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.service.impl.SiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock SiteRepo siteRepo;

    SiteService service;

    private static final Integer SITE_ID = 1;

    @BeforeEach
    void setUp() {
        service = new SiteService(siteRepo);
    }

    // ════════════════════════════════════════════════════════════
    // findAll
    // ════════════════════════════════════════════════════════════

    @Test
    void findAll_emptyRepo_returnsEmptyList() {
        when(siteRepo.findAll()).thenReturn(Collections.emptyList());

        List<SiteDTO> result = service.findAll();

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void findAll_twoSites_returnsMappedDTOs() {
        Site s1 = buildSite(1, "Site A", "1 rue test", "Bruxelles", true);
        Site s2 = buildSite(2, "Site B", "2 rue test", "Liège", false);
        when(siteRepo.findAll()).thenReturn(List.of(s1, s2));

        List<SiteDTO> result = service.findAll();

        assertThat(result).hasSize(2);
        SiteDTO dto = result.get(0);
        assertThat(dto.getIdSite()).isEqualTo(1);
        assertThat(dto.getNom()).isEqualTo("Site A");
        assertThat(dto.getAdresse()).isEqualTo("1 rue test");
        assertThat(dto.getVille()).isEqualTo("Bruxelles");
        assertThat(dto.getActif()).isTrue();
    }

    // ════════════════════════════════════════════════════════════
    // create
    // ════════════════════════════════════════════════════════════

    @Test
    void create_actifNull_defaultsToTrue() {
        SiteDTO dto = new SiteDTO();
        dto.setNom("Site A");
        dto.setAdresse("1 rue test");
        dto.setVille("Bruxelles");
        dto.setActif(null);
        when(siteRepo.save(any(Site.class))).thenAnswer(i -> i.getArgument(0));

        service.create(dto);

        ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepo).save(captor.capture());
        assertThat(captor.getValue().getActif()).isTrue();
    }

    @Test
    void create_actifFalse_respectsValue() {
        SiteDTO dto = new SiteDTO();
        dto.setNom("Site B");
        dto.setAdresse("2 rue test");
        dto.setVille("Liège");
        dto.setActif(false);
        when(siteRepo.save(any(Site.class))).thenAnswer(i -> i.getArgument(0));

        service.create(dto);

        ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepo).save(captor.capture());
        assertThat(captor.getValue().getActif()).isFalse();
    }

    // ════════════════════════════════════════════════════════════
    // update
    // ════════════════════════════════════════════════════════════

    @Test
    void update_siteNotFound_throwsNotFoundException() {
        when(siteRepo.findById(999)).thenReturn(Optional.empty());
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setNom("X");
        request.setAdresse("Y");
        request.setVille("Z");

        assertThatThrownBy(() -> service.update(999, request))
                .isInstanceOf(NotFoundException.class);
        verify(siteRepo, never()).save(any());
    }

    @Test
    void update_actifNull_doesNotOverwrite() {
        Site existing = buildSite(SITE_ID, "OldName", "OldAddr", "OldVille", true);
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.of(existing));
        when(siteRepo.save(any(Site.class))).thenAnswer(i -> i.getArgument(0));
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setNom("NewName");
        request.setAdresse("NewAddr");
        request.setVille("NewVille");
        request.setActif(null);

        service.update(SITE_ID, request);

        ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepo).save(captor.capture());
        assertThat(captor.getValue().getActif()).isTrue();
        assertThat(captor.getValue().getNom()).isEqualTo("NewName");
    }

    @Test
    void update_allFieldsProvided_savesAndReturnsDTO() {
        Site existing = buildSite(SITE_ID, "OldName", "OldAddr", "OldVille", true);
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.of(existing));
        when(siteRepo.save(any(Site.class))).thenAnswer(i -> i.getArgument(0));
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setNom("NewName");
        request.setAdresse("NewAddr");
        request.setVille("NewVille");
        request.setActif(false);

        SiteDTO result = service.update(SITE_ID, request);

        assertThat(result).isNotNull();
        assertThat(result.getNom()).isEqualTo("NewName");
        assertThat(result.getAdresse()).isEqualTo("NewAddr");
        assertThat(result.getVille()).isEqualTo("NewVille");
        assertThat(result.getActif()).isFalse();
        verify(siteRepo, times(1)).save(any());
    }

    // ════ helpers ═════════════════════════════════════════════════

    private static Site buildSite(Integer idSite, String nom, String adresse,
                                  String ville, Boolean actif) {
        Site s = new Site();
        s.setIdSite(idSite);
        s.setNom(nom);
        s.setAdresse(adresse);
        s.setVille(ville);
        s.setActif(actif);
        return s;
    }
}
