package be.ephec.padelmanager.service;

import org.springframework.security.core.Authentication;

public interface IDisponibiliteService {
    int genererCreneaux(Integer siteId, Integer annee, Authentication authentication);
    int regenererCreneaux(Integer siteId, Integer annee, Authentication authentication);
}
