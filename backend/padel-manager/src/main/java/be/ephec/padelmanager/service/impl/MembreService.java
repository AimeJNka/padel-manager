package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.config.Role;
import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.dto.MembreProfilDTO;
import be.ephec.padelmanager.dto.PersonneDTO;
import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.dto.TypeMembreDTO;
import be.ephec.padelmanager.dto.UpdateMembreRequest;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Personne;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PersonneRepo;
import be.ephec.padelmanager.service.IMembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembreService implements IMembreService {

    private final MembreRepo membreRepo;
    private final PersonneRepo personneRepo;

    @Override
    public MembreProfilDTO getProfil(String matricule) {
        Membre membre = membreRepo.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Membre introuvable : " + matricule));
        return toProfilDTO(membre);
    }

    @Override
    public List<MembreDTO> findAll() {
        return membreRepo.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public MembreProfilDTO getOne(String matricule) {
        return getProfil(matricule);
    }

    @Override
    @Transactional
    public MembreProfilDTO updateMembre(String matricule, UpdateMembreRequest request, Authentication auth) {
        boolean isAdminGlobal = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ADMIN_GLOBAL.authority()));
        if (!isAdminGlobal && !matricule.equals(auth.getName())) {
            throw new ForbiddenException("Accès interdit");
        }
        Membre membre = membreRepo.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Membre introuvable : " + matricule));
        Personne personne = membre.getPersonne();
        if (personne != null) {
            if (request.getEmail() != null) personne.setEmail(request.getEmail());
            if (request.getTelephone() != null) personne.setTelephone(request.getTelephone());
            personneRepo.save(personne);
        }
        return toProfilDTO(membre);
    }

    private MembreProfilDTO toProfilDTO(Membre m) {
        MembreProfilDTO dto = new MembreProfilDTO();
        dto.setMatricule(m.getMatricule());
        dto.setDateInscription(m.getDateInscription());
        dto.setSoldeDu(m.getSoldeDu());
        if (m.getPersonne() != null) {
            dto.setNom(m.getPersonne().getNom());
            dto.setPrenom(m.getPersonne().getPrenom());
            dto.setEmail(m.getPersonne().getEmail());
            dto.setTelephone(m.getPersonne().getTelephone());
        }
        if (m.getTypeMembre() != null) {
            dto.setTypeMembre(m.getTypeMembre().getLibelle());
        }
        if (m.getSite() != null) {
            dto.setSiteNom(m.getSite().getNom());
        }
        return dto;
    }

    private MembreDTO toDTO(Membre m) {
        MembreDTO dto = new MembreDTO();
        dto.setMatricule(m.getMatricule());
        dto.setDateInscription(m.getDateInscription());
        dto.setSoldeDu(m.getSoldeDu());
        if (m.getPersonne() != null) {
            PersonneDTO p = new PersonneDTO();
            p.setIdPersonne(m.getPersonne().getIdPersonne());
            p.setNom(m.getPersonne().getNom());
            p.setPrenom(m.getPersonne().getPrenom());
            p.setEmail(m.getPersonne().getEmail());
            p.setTelephone(m.getPersonne().getTelephone());
            dto.setPersonne(p);
        }
        if (m.getTypeMembre() != null) {
            TypeMembreDTO t = new TypeMembreDTO();
            t.setIdType(m.getTypeMembre().getIdType());
            t.setPrefixe(m.getTypeMembre().getPrefixe());
            t.setLibelle(m.getTypeMembre().getLibelle());
            t.setDelaiReservationJours(m.getTypeMembre().getDelaiReservationJours());
            t.setPeutCreerMatch(m.getTypeMembre().getPeutCreerMatch());
            dto.setTypeMembre(t);
        }
        if (m.getSite() != null) {
            SiteDTO s = new SiteDTO();
            s.setIdSite(m.getSite().getIdSite());
            s.setNom(m.getSite().getNom());
            s.setAdresse(m.getSite().getAdresse());
            s.setVille(m.getSite().getVille());
            s.setActif(m.getSite().getActif());
            dto.setSite(s);
        }
        return dto;
    }
}
