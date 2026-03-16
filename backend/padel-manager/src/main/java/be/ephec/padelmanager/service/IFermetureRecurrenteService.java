package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.FermetureRecurrenteDTO;

import java.util.List;

public interface IFermetureRecurrenteService {
    List<FermetureRecurrenteDTO> findBySite(Integer idSite);
    FermetureRecurrenteDTO create(Integer idSite, FermetureRecurrenteDTO dto);
}
