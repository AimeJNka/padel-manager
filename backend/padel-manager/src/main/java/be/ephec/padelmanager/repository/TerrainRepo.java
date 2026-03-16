package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerrainRepo extends JpaRepository<Terrain, Integer> {
    List<Terrain> findBySiteIdSite(Integer idSite);
}
