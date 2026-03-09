package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembreRepository extends JpaRepository<Membre, String> {
}
