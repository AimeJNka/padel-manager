package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.HoraireAnnuelDTO;
import be.ephec.padelmanager.dto.UpdateHoraireRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface IHoraireAnnuelService {
    List<HoraireAnnuelDTO> findBySite(Integer idSite);
    HoraireAnnuelDTO create(Integer idSite, HoraireAnnuelDTO dto, Authentication authentication);
    HoraireAnnuelDTO update(Integer idSite, Integer idHoraire, UpdateHoraireRequest request, Authentication authentication);
}
