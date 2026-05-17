package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Penalite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PenaliteRepo
        extends JpaRepository<Penalite, Integer>, JpaSpecificationExecutor<Penalite> {

    boolean existsByMembreMatriculeAndDateFinAfter(String matricule, java.time.LocalDateTime now);

    List<Penalite> findByMembreMatriculeOrderByDateDebutDesc(String matricule);
}
