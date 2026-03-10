package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.MatchPadel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPadelRepo extends JpaRepository<MatchPadel, Integer> {
}
