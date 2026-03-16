package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.HoraireAnnuelDTO;

import java.util.List;

public interface IHoraireAnnuelService {
    List<HoraireAnnuelDTO> findBySite(Integer idSite);
    HoraireAnnuelDTO create(Integer idSite, HoraireAnnuelDTO dto);
}
