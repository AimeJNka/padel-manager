package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerrainRepo extends JpaRepository<Terrain, Integer> {
}
