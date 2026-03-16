package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.FermeturePonctuelleDTO;

import java.util.List;

public interface IFermeturePonctuelleService {
    List<FermeturePonctuelleDTO> findBySite(Integer idSite);
    FermeturePonctuelleDTO create(Integer idSite, FermeturePonctuelleDTO dto);
}
