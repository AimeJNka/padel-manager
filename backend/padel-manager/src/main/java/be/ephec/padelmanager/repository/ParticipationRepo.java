package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRepo extends JpaRepository<Participation, Integer> {
}
