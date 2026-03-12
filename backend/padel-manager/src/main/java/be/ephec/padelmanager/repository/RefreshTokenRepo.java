package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevoqueFalse(String token);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoque = true WHERE r.membre.matricule = :matricule AND r.revoque = false")
    void revokeAllByMatricule(String matricule);
}
