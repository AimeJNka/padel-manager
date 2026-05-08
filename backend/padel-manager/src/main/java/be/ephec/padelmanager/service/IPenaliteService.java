package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.PenaliteDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface IPenaliteService {

    List<PenaliteDTO> listerPenalitesMembre(Authentication auth);

    Page<PenaliteDTO> listerPenalitesAdmin(
            String matricule,
            Boolean activeOnly,
            Integer siteId,
            Pageable pageable,
            Authentication auth
    );

    PenaliteDTO annulerPenalite(Integer idPenalite, Authentication auth);
}
