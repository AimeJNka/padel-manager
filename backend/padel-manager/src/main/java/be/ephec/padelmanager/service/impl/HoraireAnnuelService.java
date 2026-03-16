package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.HoraireAnnuelDTO;
import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.HoraireAnnuel;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.repository.HoraireAnnuelRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.service.IHoraireAnnuelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HoraireAnnuelService implements IHoraireAnnuelService {

    private final HoraireAnnuelRepo horaireRepo;
    private final SiteRepo siteRepo;

    @Override
    public List<HoraireAnnuelDTO> findBySite(Integer idSite) {
        return horaireRepo.findBySiteIdSite(idSite).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public HoraireAnnuelDTO create(Integer idSite, HoraireAnnuelDTO dto) {
        Site site = siteRepo.findById(idSite)
                .orElseThrow(() -> new NotFoundException("Site introuvable : " + idSite));
        HoraireAnnuel horaire = new HoraireAnnuel();
        horaire.setSite(site);
        horaire.setAnnee(dto.getAnnee());
        horaire.setHeureOuverture(dto.getHeureOuverture());
        horaire.setHeureFermeture(dto.getHeureFermeture());
        return toDTO(horaireRepo.save(horaire));
    }

    private HoraireAnnuelDTO toDTO(HoraireAnnuel h) {
        HoraireAnnuelDTO dto = new HoraireAnnuelDTO();
        dto.setIdHoraire(h.getIdHoraire());
        dto.setAnnee(h.getAnnee());
        dto.setHeureOuverture(h.getHeureOuverture());
        dto.setHeureFermeture(h.getHeureFermeture());
        if (h.getSite() != null) {
            SiteDTO s = new SiteDTO();
            s.setIdSite(h.getSite().getIdSite());
            s.setNom(h.getSite().getNom());
            s.setAdresse(h.getSite().getAdresse());
            s.setVille(h.getSite().getVille());
            s.setActif(h.getSite().getActif());
            dto.setSite(s);
        }
        return dto;
    }
}
