package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.PaiementDTO;
import be.ephec.padelmanager.model.Participation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface IPaiementService {

    void creerPourParticipation(Participation participation);

    void annulerPourParticipation(Participation participation);

    PaiementDTO payerParMembre(Integer idPaiement, Authentication auth);

    PaiementDTO rembourserPaiement(Integer idPaiement, Authentication auth);

    List<PaiementDTO> listerPaiementsMembre(Authentication auth);

    Page<PaiementDTO> listerPaiementsAdmin(
            Integer matchId,
            String matricule,
            String statut,
            Integer siteId,
            Pageable pageable,
            Authentication auth
    );
}
