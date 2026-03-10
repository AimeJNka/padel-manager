package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Personne;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonneRepo extends JpaRepository<Personne, Integer> {
}
