package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaiementRepo extends JpaRepository<Paiement, Integer> {
}
