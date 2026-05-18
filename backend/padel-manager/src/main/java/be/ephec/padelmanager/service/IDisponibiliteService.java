package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.DisponibiliteDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

public interface IDisponibiliteService {
    int genererCreneaux(Integer siteId, Integer annee, Authentication authentication);
    int regenererCreneaux(Integer siteId, Integer annee, Authentication authentication);
    Page<DisponibiliteDTO> listerDisponibilites(Integer siteId, Integer terrainId, LocalDate date, String statut, Pageable pageable);
}
