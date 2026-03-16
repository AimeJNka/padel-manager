package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.FermeturePonctuelle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FermeturePonctuelleRepo extends JpaRepository<FermeturePonctuelle, Integer> {
    List<FermeturePonctuelle> findBySiteIdSite(Integer idSite);
}
