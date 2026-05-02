package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Disponibilite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface DisponibiliteRepo extends JpaRepository<Disponibilite, Integer> {

    List<Disponibilite> findByTerrainSiteIdSiteAndDateHeureDebutBetween(
            Integer siteId, LocalDateTime start, LocalDateTime end);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Disponibilite d WHERE d.terrain.site.idSite = :siteId " +
           "AND d.statut = 'LIBRE' " +
           "AND d.dateHeureDebut >= :start AND d.dateHeureDebut <= :end")
    void deleteLibreByTerrainSiteAndYearRange(
            @Param("siteId") Integer siteId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
