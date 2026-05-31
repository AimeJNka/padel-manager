package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Membre;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembreRepo extends JpaRepository<Membre, String> {

    @Query("SELECT MAX(m.matricule) FROM Membre m WHERE m.matricule LIKE :prefixe%")
    Optional<String> findLastMatriculeByPrefixe(String prefixe);

    @Query("""
        SELECT m FROM Membre m
        LEFT JOIN m.personne p
        LEFT JOIN m.site s
        JOIN m.typeMembre tm
        WHERE (LOWER(p.nom)       LIKE :pattern
            OR LOWER(p.prenom)    LIKE :pattern
            OR LOWER(m.matricule) LIKE :pattern)
          AND (:siteIdMatch IS NULL
               OR tm.libelle IN ('Global', 'Libre')
               OR s.idSite = :siteIdMatch)
        ORDER BY p.nom, p.prenom
        """)
    List<Membre> searchByPattern(
            @Param("pattern")     String  pattern,
            @Param("siteIdMatch") Integer siteIdMatch,
            Pageable pageable);

    @Query("""
        SELECT m FROM Membre m
        INNER JOIN m.site s
        LEFT JOIN m.personne p
        WHERE (LOWER(p.nom)       LIKE :pattern
            OR LOWER(p.prenom)    LIKE :pattern
            OR LOWER(m.matricule) LIKE :pattern)
          AND s.idSite = :siteId
          AND (:siteIdMatch IS NULL OR s.idSite = :siteIdMatch)
        ORDER BY p.nom, p.prenom
        """)
    List<Membre> searchByPatternAndSite(
            @Param("pattern")     String  pattern,
            @Param("siteId")      Integer siteId,
            @Param("siteIdMatch") Integer siteIdMatch,
            Pageable pageable);
}
