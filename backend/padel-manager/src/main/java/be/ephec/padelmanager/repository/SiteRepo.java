package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepo extends JpaRepository<Site, Integer> {
}
