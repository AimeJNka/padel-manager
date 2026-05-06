package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.Participation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaiementRepo
        extends JpaRepository<Paiement, Integer>, JpaSpecificationExecutor<Paiement> {

    Optional<Paiement> findByParticipation(Participation participation);

    Optional<Paiement> findByParticipationIdParticipation(Integer idParticipation);

    List<Paiement> findByParticipationMembreMatriculeOrderByDatePaiementDesc(String matricule);

    /**
     * Pessimistic write lock — prevents concurrent double-payment on the same row.
     * Prefer over optimistic locking: simpler, correct for financial state transitions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Paiement p WHERE p.idPaiement = :id")
    Optional<Paiement> findByIdForUpdate(@Param("id") Integer id);
}
