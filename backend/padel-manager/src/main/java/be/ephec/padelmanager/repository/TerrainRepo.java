package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Terrain;
import org.springframework.data.repository.CrudRepository;

public interface TerrainRepo extends CrudRepository<Terrain, Integer> {
}
