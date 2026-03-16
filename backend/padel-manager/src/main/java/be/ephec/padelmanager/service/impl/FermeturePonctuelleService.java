package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.FermeturePonctuelleDTO;
import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.FermeturePonctuelle;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.repository.FermeturePonctuelleRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.service.IFermeturePonctuelleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FermeturePonctuelleService implements IFermeturePonctuelleService {

    private final FermeturePonctuelleRepo fermetureRepo;
    private final SiteRepo siteRepo;

    @Override
    public List<FermeturePonctuelleDTO> findBySite(Integer idSite) {
        return fermetureRepo.findBySiteIdSite(idSite).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public FermeturePonctuelleDTO create(Integer idSite, FermeturePonctuelleDTO dto) {
        Site site = siteRepo.findById(idSite)
                .orElseThrow(() -> new NotFoundException("Site introuvable : " + idSite));
        FermeturePonctuelle fermeture = new FermeturePonctuelle();
        fermeture.setSite(site);
        fermeture.setDateFermeture(dto.getDateFermeture());
        fermeture.setMotif(dto.getMotif());
        return toDTO(fermetureRepo.save(fermeture));
    }

    private FermeturePonctuelleDTO toDTO(FermeturePonctuelle f) {
        FermeturePonctuelleDTO dto = new FermeturePonctuelleDTO();
        dto.setIdFermeturePonctuelle(f.getIdFermeturePonctuelle());
        dto.setDateFermeture(f.getDateFermeture());
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
