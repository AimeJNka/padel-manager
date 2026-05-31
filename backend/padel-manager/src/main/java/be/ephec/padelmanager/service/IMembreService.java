package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.dto.MembreProfilDTO;
import be.ephec.padelmanager.dto.MembreSearchDTO;
import be.ephec.padelmanager.dto.UpdateMembreRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface IMembreService {
    MembreProfilDTO getProfil(String matricule);
    List<MembreDTO> findAll(Authentication auth);
    MembreProfilDTO getOne(String matricule);
    MembreProfilDTO updateMembre(String matricule, UpdateMembreRequest request, Authentication auth);
    List<MembreSearchDTO> search(String q, Integer siteIdMatch, Authentication auth);
}
