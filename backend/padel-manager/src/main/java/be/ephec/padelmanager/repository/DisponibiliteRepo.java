package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Disponibilite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface DisponibiliteRepo extends JpaRepository<Disponibilite, Integer>, JpaSpecificationExecutor<Disponibilite> {

    List<Disponibilite> findByTerrainSiteIdSiteAndDateHeureDebutBetween(
            Integer siteId, LocalDateTime start, LocalDateTime end);

    // NOTE: 'LIBRE' here mirrors DisponibiliteStatus.LIBRE — JPQL cannot reference Java constants. Keep in sync.
    // The NOT EXISTS guards the FK from match_padel.id_dispo: a LIBRE dispo still referenced by any match
    // (e.g. an ANNULE match) cannot be deleted without violating the FK constraint. Such rows are preserved.
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Disponibilite d WHERE d.terrain.site.idSite = :siteId " +
           "AND d.statut = 'LIBRE' " +
           "AND d.dateHeureDebut >= :start AND d.dateHeureDebut <= :end " +
           "AND NOT EXISTS (SELECT 1 FROM MatchPadel m WHERE m.disponibilite = d)")
    void deleteLibreByTerrainSiteAndYearRange(
            @Param("siteId") Integer siteId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT m.disponibilite.idDispo FROM MatchPadel m " +
           "WHERE m.disponibilite.terrain.site.idSite = :siteId " +
           "  AND m.disponibilite.dateHeureDebut >= :start " +
           "  AND m.disponibilite.dateHeureDebut <= :end")
    Set<Integer> findMatchedDispoIdsForSiteInRange(
            @Param("siteId") Integer siteId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
