package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.config.Role;
import be.ephec.padelmanager.dto.PenaliteDTO;
import be.ephec.padelmanager.exception.ConflictException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Penalite;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.IPenaliteService;
import be.ephec.padelmanager.service.SiteAccessChecker;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PenaliteService implements IPenaliteService {

    private final PenaliteRepo penaliteRepo;
    private final SiteAccessChecker siteAccessChecker;

    @Override
    public List<PenaliteDTO> listerPenalitesMembre(Authentication auth) {
        return penaliteRepo
                .findByMembreMatriculeOrderByDateDebutDesc(auth.getName())
                .stream()
                .map(PenaliteDTO::from)
                .toList();
    }

    @Override
    public Page<PenaliteDTO> listerPenalitesAdmin(
            String matricule, Boolean activeOnly, Integer siteId,
            Pageable pageable, Authentication auth) {

        Specification<Penalite> spec = buildSpec(matricule, activeOnly, siteId, auth);
        return penaliteRepo.findAll(spec, pageable).map(PenaliteDTO::from);
    }

    @Override
    public PenaliteDTO annulerPenalite(Integer idPenalite, Authentication auth) {
        Penalite pen = penaliteRepo.findById(idPenalite)
                .orElseThrow(() -> new NotFoundException("Pénalité introuvable"));

        checkSiteAccess(pen, auth);

        if (!pen.getDateFin().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Pénalité déjà expirée");
        }

        pen.setDateFin(LocalDateTime.now());
        penaliteRepo.save(pen);
        return PenaliteDTO.from(pen);
    }

    private Specification<Penalite> buildSpec(
            String matricule, Boolean activeOnly, Integer siteId, Authentication auth) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (matricule != null) {
                predicates.add(cb.equal(root.get("membre").get("matricule"), matricule));
            }
            if (Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.greaterThan(root.get("dateFin"), LocalDateTime.now()));
            }

            boolean isSiteAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(Role.ADMIN_SITE.authority()));

            if (isSiteAdmin) {
                // TODO M9: retourner 403 si siteId param != auth.getDetails() pour ADMIN_SITE au lieu de silently override
                Integer adminSiteId = (Integer) auth.getDetails();
                if (adminSiteId == null) {
                    throw new ForbiddenException("Accès refusé");
                }
                predicates.add(cb.equal(root.get("membre").get("site").get("idSite"), adminSiteId));
            } else if (siteId != null) {
                predicates.add(cb.equal(root.get("membre").get("site").get("idSite"), siteId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void checkSiteAccess(Penalite pen, Authentication auth) {
        Integer membreSiteId = pen.getMembre().getSite() != null
                ? pen.getMembre().getSite().getIdSite()
                : null;
        siteAccessChecker.check(auth, membreSiteId);
    }
}
