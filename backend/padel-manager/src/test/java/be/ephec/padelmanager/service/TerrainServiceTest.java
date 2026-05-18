package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.TerrainDTO;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.model.Terrain;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.repository.TerrainRepo;
import be.ephec.padelmanager.service.impl.TerrainService;
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
class TerrainServiceTest {

    @Mock TerrainRepo terrainRepo;
    @Mock SiteRepo siteRepo;
    @Mock SiteAccessChecker siteAccessChecker;
    @Mock Authentication auth;

    TerrainService service;

    private static final Integer SITE_ID    = 1;
    private static final Integer ID_TERRAIN = 10;

    @BeforeEach
    void setUp() {
        service = new TerrainService(terrainRepo, siteRepo, siteAccessChecker);
    }

    // ════════════════════════════════════════════════════════════
    // findBySite
    // ════════════════════════════════════════════════════════════

    @Test
    void findBySite_returnsMappedDTOs() {
        Site site = buildSite(SITE_ID);
        Terrain entity = buildTerrain(ID_TERRAIN, site);
        when(terrainRepo.findBySiteIdSite(SITE_ID)).thenReturn(List.of(entity));

        List<TerrainDTO> result = service.findBySite(SITE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdTerrain()).isEqualTo(ID_TERRAIN);
        assertThat(result.get(0).getNumero()).isEqualTo(1);
    }

    // ════════════════════════════════════════════════════════════
    // create
    // ════════════════════════════════════════════════════════════

    @Test
    void create_accessDenied_throwsForbidden() {
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(Authentication.class), eq(SITE_ID));
        TerrainDTO dto = new TerrainDTO();
        dto.setNumero(1);
        dto.setStatut("LIBRE");

        assertThatThrownBy(() -> service.create(SITE_ID, dto, auth))
                .isInstanceOf(ForbiddenException.class);
        verify(siteRepo, never()).findById(any());
    }

    @Test
    void create_siteNotFound_throwsNotFoundException() {
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.empty());
        TerrainDTO dto = new TerrainDTO();
        dto.setNumero(1);
        dto.setStatut("LIBRE");

        assertThatThrownBy(() -> service.create(SITE_ID, dto, auth))
                .isInstanceOf(NotFoundException.class);
        verify(terrainRepo, never()).save(any());
    }

    @Test
    void create_validRequest_savesWithCorrectSiteAndFields() {
        Site site = buildSite(SITE_ID);
        TerrainDTO dto = new TerrainDTO();
        dto.setNumero(3);
        dto.setStatut("LIBRE");
        when(siteRepo.findById(SITE_ID)).thenReturn(Optional.of(site));
        when(terrainRepo.save(any(Terrain.class))).thenAnswer(i -> i.getArgument(0));

        TerrainDTO result = service.create(SITE_ID, dto, auth);

        ArgumentCaptor<Terrain> captor = ArgumentCaptor.forClass(Terrain.class);
        verify(terrainRepo).save(captor.capture());
        assertThat(captor.getValue().getSite()).isEqualTo(site);
        assertThat(captor.getValue().getNumero()).isEqualTo(3);
        assertThat(captor.getValue().getStatut()).isEqualTo("LIBRE");
        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════
    // update
    // ════════════════════════════════════════════════════════════

    @Test
    void update_terrainNotFound_throwsNotFoundException() {
        when(terrainRepo.findById(ID_TERRAIN)).thenReturn(Optional.empty());
        TerrainDTO dto = new TerrainDTO();
        dto.setNumero(5);
        dto.setStatut("OCCUPE");

        assertThatThrownBy(() -> service.update(ID_TERRAIN, dto, auth))
                .isInstanceOf(NotFoundException.class);
        verify(siteAccessChecker, never()).check(any(Authentication.class), any());
    }

    /**
     * Security invariant: TerrainService.update derives idSite from the loaded
     * entity, not from caller input. A caller cannot bypass site-scoped auth by
     * providing a "wrong" idSite — there is no idSite parameter on the update
     * signature at all.
     *
     * This test verifies the property by mocking SiteAccessChecker to throw
     * specifically on the entity's idSite (2), proving the service passes the
     * entity-derived value to the checker.
     *
     * Related concept: TOCTOU prevention (time-of-check-to-time-of-use).
     */
    @Test
    void update_accessDeniedOnEntityDerivedSiteId_throwsForbidden() {
        Site siteB = buildSite(2);
        Terrain terrain = buildTerrain(ID_TERRAIN, siteB);
        when(terrainRepo.findById(ID_TERRAIN)).thenReturn(Optional.of(terrain));
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(Authentication.class), eq(2));
        TerrainDTO dto = new TerrainDTO();

        assertThatThrownBy(() -> service.update(ID_TERRAIN, dto, auth))
                .isInstanceOf(ForbiddenException.class);
        verify(siteAccessChecker).check(any(Authentication.class), eq(2));
        verify(terrainRepo, never()).save(any());
    }

    @Test
    void update_validRequest_updatesNumeroAndStatut() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(ID_TERRAIN, site);
        TerrainDTO dto = new TerrainDTO();
        dto.setNumero(5);
        dto.setStatut("OCCUPE");
        when(terrainRepo.findById(ID_TERRAIN)).thenReturn(Optional.of(terrain));
        when(terrainRepo.save(any(Terrain.class))).thenAnswer(i -> i.getArgument(0));

        TerrainDTO result = service.update(ID_TERRAIN, dto, auth);

        ArgumentCaptor<Terrain> captor = ArgumentCaptor.forClass(Terrain.class);
        verify(terrainRepo).save(captor.capture());
        assertThat(captor.getValue().getNumero()).isEqualTo(5);
        assertThat(captor.getValue().getStatut()).isEqualTo("OCCUPE");
        assertThat(result).isNotNull();
    }

    // ════════════════════════════════════════════════════════════
    // delete
    // ════════════════════════════════════════════════════════════

    @Test
    void delete_accessDenied_throwsForbidden() {
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(Authentication.class), eq(SITE_ID));

        assertThatThrownBy(() -> service.delete(SITE_ID, ID_TERRAIN, auth))
                .isInstanceOf(ForbiddenException.class);
        verify(terrainRepo, never()).findById(any());
    }

    @Test
    void delete_terrainNotFound_throwsNotFoundException() {
        when(terrainRepo.findById(ID_TERRAIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(SITE_ID, ID_TERRAIN, auth))
                .isInstanceOf(NotFoundException.class);
        verify(terrainRepo, never()).deleteById(any());
    }

    @Test
    void delete_terrainBelongsToDifferentSite_throwsNotFoundException() {
        Site otherSite = buildSite(99);
        Terrain terrain = buildTerrain(ID_TERRAIN, otherSite);
        when(terrainRepo.findById(ID_TERRAIN)).thenReturn(Optional.of(terrain));

        assertThatThrownBy(() -> service.delete(SITE_ID, ID_TERRAIN, auth))
                .isInstanceOf(NotFoundException.class);
        verify(terrainRepo, never()).deleteById(any());
    }

    @Test
    void delete_validRequest_callsDeleteById() {
        Site site = buildSite(SITE_ID);
        Terrain terrain = buildTerrain(ID_TERRAIN, site);
        when(terrainRepo.findById(ID_TERRAIN)).thenReturn(Optional.of(terrain));

        service.delete(SITE_ID, ID_TERRAIN, auth);

        verify(terrainRepo, times(1)).deleteById(ID_TERRAIN);
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

    private static Terrain buildTerrain(Integer idTerrain, Site site) {
        Terrain t = new Terrain();
        t.setIdTerrain(idTerrain);
        t.setNumero(1);
        t.setStatut("LIBRE");
        t.setSite(site);
        return t;
    }
}
