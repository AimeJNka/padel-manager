package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.HoraireAnnuel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoraireAnnuelRepo extends JpaRepository<HoraireAnnuel, Integer> {
    List<HoraireAnnuel> findBySiteIdSite(Integer idSite);
    Optional<HoraireAnnuel> findBySiteIdSiteAndAnnee(Integer idSite, Integer annee);
}
