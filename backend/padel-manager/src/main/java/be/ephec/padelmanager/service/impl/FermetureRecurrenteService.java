package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.FermetureRecurrenteDTO;
import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.FermetureRecurrente;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.repository.FermetureRecurrenteRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.service.IFermetureRecurrenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FermetureRecurrenteService implements IFermetureRecurrenteService {

    private final FermetureRecurrenteRepo fermetureRepo;
    private final SiteRepo siteRepo;

    @Override
    public List<FermetureRecurrenteDTO> findBySite(Integer idSite) {
        return fermetureRepo.findBySiteIdSite(idSite).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public FermetureRecurrenteDTO create(Integer idSite, FermetureRecurrenteDTO dto) {
        Site site = siteRepo.findById(idSite)
                .orElseThrow(() -> new NotFoundException("Site introuvable : " + idSite));
        FermetureRecurrente fermeture = new FermetureRecurrente();
        fermeture.setSite(site);
        fermeture.setJourSemaine(dto.getJourSemaine());
        fermeture.setMotif(dto.getMotif());
        return toDTO(fermetureRepo.save(fermeture));
    }

    private FermetureRecurrenteDTO toDTO(FermetureRecurrente f) {
        FermetureRecurrenteDTO dto = new FermetureRecurrenteDTO();
        dto.setIdFermetureRecurrente(f.getIdFermetureRecurrente());
        dto.setJourSemaine(f.getJourSemaine());
        dto.setMotif(f.getMotif());
        if (f.getSite() != null) {
            SiteDTO s = new SiteDTO();
            s.setIdSite(f.getSite().getIdSite());
            s.setNom(f.getSite().getNom());
            s.setAdresse(f.getSite().getAdresse());
            s.setVille(f.getSite().getVille());
            s.setActif(f.getSite().getActif());
            dto.setSite(s);
        }
        return dto;
    }
}
