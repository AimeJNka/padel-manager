package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembreRepo extends JpaRepository<Membre, String> {
}
