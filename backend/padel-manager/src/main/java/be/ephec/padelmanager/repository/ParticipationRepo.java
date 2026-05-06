package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipationRepo extends JpaRepository<Participation, Integer> {

    long countByMatchPadelIdMatchAndStatutNot(Integer idMatch, String statut);

    boolean existsByMatchPadelIdMatchAndMembreMatricule(Integer idMatch, String matricule);

    java.util.List<be.ephec.padelmanager.model.Participation> findByMatchPadelIdMatch(Integer idMatch);

    Optional<Participation> findByMatchPadelIdMatchAndMembreMatricule(Integer idMatch, String matricule);
}
