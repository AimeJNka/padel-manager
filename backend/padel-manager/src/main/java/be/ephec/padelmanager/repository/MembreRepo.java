package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MembreRepo extends JpaRepository<Membre, String> {

    @Query("SELECT MAX(m.matricule) FROM Membre m WHERE m.matricule LIKE :prefixe%")
    Optional<String> findLastMatriculeByPrefixe(String prefixe);
}
