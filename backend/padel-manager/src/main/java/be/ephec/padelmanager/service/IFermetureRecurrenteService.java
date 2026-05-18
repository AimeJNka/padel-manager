package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.FermetureRecurrenteDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface IFermetureRecurrenteService {
    List<FermetureRecurrenteDTO> findBySite(Integer idSite);
    FermetureRecurrenteDTO create(Integer idSite, FermetureRecurrenteDTO dto, Authentication authentication);
    void delete(Integer idSite, Integer idFermeture, Authentication authentication);
}
