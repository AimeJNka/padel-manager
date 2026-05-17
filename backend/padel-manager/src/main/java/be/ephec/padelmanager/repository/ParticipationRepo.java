package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepo extends JpaRepository<Participation, Integer> {

    long countByMatchPadelIdMatchAndStatutNot(Integer idMatch, String statut);

    boolean existsByMatchPadelIdMatchAndMembreMatricule(Integer idMatch, String matricule);

    java.util.List<be.ephec.padelmanager.model.Participation> findByMatchPadelIdMatch(Integer idMatch);

    Optional<Participation> findByMatchPadelIdMatchAndMembreMatricule(Integer idMatch, String matricule);

    /**
     * Fetch participations with a given statut whose match slot starts on or before
     * {@code threshold}. JOIN FETCH loads match + disponibilite eagerly.
     * Used by Job 2 (unpaid slot release) to find EN_ATTENTE participations
     * at or past match start time.
     */
    @Query("""
            SELECT p FROM Participation p
            JOIN FETCH p.matchPadel m
            JOIN FETCH m.disponibilite d
            JOIN FETCH p.membre
            WHERE p.statut          = :statut
              AND d.dateHeureDebut <= :threshold
            """)
    List<Participation> findByStatutAndMatchDispoDebutBefore(
            @Param("statut")    String statut,
            @Param("threshold") LocalDateTime threshold
    );

    /**
     * Count participations for a given match by exact statut.
     * Used by scheduler jobs to determine confirmed-player count before applying penalty.
     */
    @Query("""
            SELECT COUNT(p) FROM Participation p
            WHERE p.matchPadel.idMatch = :idMatch
              AND p.statut             = :statut
            """)
    long countByMatchIdAndStatut(
            @Param("idMatch") Integer idMatch,
            @Param("statut")  String statut
    );
}
