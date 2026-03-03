package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembreRepository extends JpaRepository<Membre, String> {
}
