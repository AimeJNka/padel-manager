package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.dto.MembreProfilDTO;

import java.util.List;

public interface IMembreService {
    MembreProfilDTO getProfil(String matricule);
    List<MembreDTO> findAll();
}
