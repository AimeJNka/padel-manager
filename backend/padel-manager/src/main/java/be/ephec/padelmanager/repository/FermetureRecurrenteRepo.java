package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.FermetureRecurrente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FermetureRecurrenteRepo extends JpaRepository<FermetureRecurrente, Integer> {
    List<FermetureRecurrente> findBySiteIdSite(Integer idSite);
}
