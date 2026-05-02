package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.dto.TerrainDTO;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.model.Terrain;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.repository.TerrainRepo;
import be.ephec.padelmanager.service.ITerrainService;
import be.ephec.padelmanager.service.SiteAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TerrainService implements ITerrainService {

    private final TerrainRepo terrainRepo;
    private final SiteRepo siteRepo;
    private final SiteAccessChecker siteAccessChecker;

    @Override
    public List<TerrainDTO> findBySite(Integer idSite) {
        return terrainRepo.findBySiteIdSite(idSite).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public TerrainDTO create(Integer idSite, TerrainDTO dto, Authentication authentication) {
        siteAccessChecker.check(authentication, idSite);
        Site site = siteRepo.findById(idSite)
                .orElseThrow(() -> new NotFoundException("Site introuvable : " + idSite));
        Terrain terrain = new Terrain();
        terrain.setSite(site);
        terrain.setNumero(dto.getNumero());
        terrain.setStatut(dto.getStatut());
        return toDTO(terrainRepo.save(terrain));
    }

    @Override
    public TerrainDTO update(Integer idTerrain, TerrainDTO dto, Authentication authentication) {
        Terrain terrain = terrainRepo.findById(idTerrain)
                .orElseThrow(() -> new NotFoundException("Terrain introuvable : " + idTerrain));
        Integer idSite = terrain.getSite() != null ? terrain.getSite().getIdSite() : null;
        siteAccessChecker.check(authentication, idSite);
        terrain.setNumero(dto.getNumero());
        terrain.setStatut(dto.getStatut());
        return toDTO(terrainRepo.save(terrain));
    }

    private TerrainDTO toDTO(Terrain t) {
        TerrainDTO dto = new TerrainDTO();
        dto.setIdTerrain(t.getIdTerrain());
        dto.setNumero(t.getNumero());
        dto.setStatut(t.getStatut());
        if (t.getSite() != null) {
            SiteDTO s = new SiteDTO();
            s.setIdSite(t.getSite().getIdSite());
            s.setNom(t.getSite().getNom());
            s.setAdresse(t.getSite().getAdresse());
            s.setVille(t.getSite().getVille());
            s.setActif(t.getSite().getActif());
            dto.setSite(s);
        }
        return dto;
    }
}
