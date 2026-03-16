package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.TerrainDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ITerrainService {
    List<TerrainDTO> findBySite(Integer idSite);
    TerrainDTO create(Integer idSite, TerrainDTO dto, Authentication authentication);
    TerrainDTO update(Integer idTerrain, TerrainDTO dto, Authentication authentication);
}
