package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.MatchPadel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchPadelRepo extends JpaRepository<MatchPadel, Integer>, JpaSpecificationExecutor<MatchPadel> {

    /**
     * Fetch matches by type and statut whose slot starts on or before {@code threshold}.
     * JOIN FETCH prevents LazyInitializationException when the scheduler accesses
     * disponibilite and organisateur outside an HTTP transaction.
     * Used by Job 1 (private match bascule) and Job 2 (unpaid slot release).
     */
    @Query("""
            SELECT m FROM MatchPadel m
            JOIN FETCH m.disponibilite d
            JOIN FETCH m.organisateur
            WHERE m.typeMatch       = :typeMatch
              AND m.statut          = :statut
              AND d.dateHeureDebut <= :threshold
            """)
    List<MatchPadel> findByTypeMatchAndStatutAndDispoDebutBefore(
            @Param("typeMatch")  String typeMatch,
            @Param("statut")     String statut,
            @Param("threshold")  LocalDateTime threshold
    );

    /**
     * Fetch all matches with a given statut whose slot has already started (any type).
     * JOIN FETCH prevents LazyInitializationException from outside an HTTP transaction.
     * Used by Job 3 (organizer solde calculation at match start).
     */
    @Query("""
            SELECT m FROM MatchPadel m
            JOIN FETCH m.disponibilite d
            JOIN FETCH m.organisateur
            WHERE m.statut         = :statut
              AND d.dateHeureDebut <= :now
            """)
    List<MatchPadel> findStartedMatchesByStatut(
            @Param("statut") String statut,
            @Param("now")    LocalDateTime now
    );
}
