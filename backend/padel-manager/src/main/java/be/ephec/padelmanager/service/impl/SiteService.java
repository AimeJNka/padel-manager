package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.service.ISiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService implements ISiteService {

    private final SiteRepo siteRepo;

    @Override
    public List<SiteDTO> findAll() {
        return siteRepo.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public SiteDTO create(SiteDTO dto) {
        Site site = new Site();
        site.setNom(dto.getNom());
        site.setAdresse(dto.getAdresse());
        site.setVille(dto.getVille());
        site.setActif(dto.getActif() != null ? dto.getActif() : true);
        return toDTO(siteRepo.save(site));
    }

    private SiteDTO toDTO(Site s) {
        SiteDTO dto = new SiteDTO();
        dto.setIdSite(s.getIdSite());
        dto.setNom(s.getNom());
        dto.setAdresse(s.getAdresse());
        dto.setVille(s.getVille());
        dto.setActif(s.getActif());
        return dto;
    }
}
