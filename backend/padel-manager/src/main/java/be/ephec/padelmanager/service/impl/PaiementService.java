package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.config.Role;
import be.ephec.padelmanager.dto.PaiementDTO;
import be.ephec.padelmanager.exception.ConflictException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.PaiementStatus;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.ParticipationStatus;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PaiementRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.service.IPaiementService;
import be.ephec.padelmanager.service.SiteAccessChecker;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lock acquisition order (prevents deadlocks):
 *  1. Paiement (PESSIMISTIC_WRITE via findByIdForUpdate)
 *  2. Membre  (via MembreRepo.save — last in transaction)
 * Never acquire Paiement lock after writing to Membre in the same transaction.
 *
 * Note (SECURITY finding #4 — accepted accounting risk):
 * Late cancellation (< 24h) transitions PAYE -> ANNULE without a dedicated refund step.
 * Accepted design decision (Q1, README.md M8_plan).
 * Risk: audit trail does not distinguish "never paid" from "paid and absorbed as penalty".
 * Deferred to M9: introduce paiement_audit log or ABSORBE_PENALITE status.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaiementService implements IPaiementService {

    private final PaiementRepo paiementRepo;
    private final MembreRepo membreRepo;
    private final ParticipationRepo participationRepo;
    private final SiteAccessChecker siteAccessChecker;

    @Override
    public void creerPourParticipation(Participation participation) {
        BigDecimal montant = participation.getMatchPadel().getMontantTotal()
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        Paiement p = new Paiement();
        p.setParticipation(participation);
        p.setMontant(montant);
        p.setSoldeInclus(BigDecimal.ZERO);
        p.setStatut(PaiementStatus.EN_ATTENTE);
        paiementRepo.save(p);
    }

    @Override
    public void annulerPourParticipation(Participation participation) {
        // SECURITY #12 — pessimistic lock: prevents concurrent match-cancel / late-cancel race
        paiementRepo.findByParticipationForUpdate(participation).ifPresent(p -> {
            if (!PaiementStatus.REMBOURSE.equals(p.getStatut()) && !PaiementStatus.ANNULE.equals(p.getStatut())) {
                p.setStatut(PaiementStatus.PAYE.equals(p.getStatut()) ? PaiementStatus.REMBOURSE : PaiementStatus.ANNULE);
                paiementRepo.save(p);
            }
        });
    }

    /**
     * Note (SECURITY finding #8 — timezone):
     * Uses LocalDateTime.now() without explicit timezone.
     * Acceptable for local single-server deployment (academic scope).
     * Risk: clock skew or TZ mismatch in multi-server deployment corrupts 24h boundary.
     * Deferred to M9: migrate to Instant/OffsetDateTime + TIMESTAMPTZ columns.
     */
    @Override
    public PaiementDTO payerParMembre(Integer idPaiement, Authentication auth) {
        // SECURITY #2 — pessimistic lock against double-payment race
        Paiement paiement = paiementRepo.findByIdForUpdate(idPaiement)
                .orElseThrow(() -> new NotFoundException("Paiement introuvable"));

        // SECURITY #1 — String à gauche de equals (jamais entité.equals)
        String ownerMatricule = paiement.getParticipation().getMembre().getMatricule();
        if (!auth.getName().equals(ownerMatricule)) {
            throw new ForbiddenException("Accès refusé");
        }

        if (!PaiementStatus.EN_ATTENTE.equals(paiement.getStatut())) {
            throw new ConflictException("Paiement déjà traité");
        }

        Participation participation = paiement.getParticipation();
        Membre membre = participation.getMembre();

        paiement.setStatut(PaiementStatus.PAYE);
        paiement.setDatePaiement(LocalDateTime.now());

        BigDecimal soldeDu = membre.getSoldeDu() != null ? membre.getSoldeDu() : BigDecimal.ZERO;
        if (soldeDu.compareTo(BigDecimal.ZERO) > 0) {
            paiement.setSoldeInclus(soldeDu);
            membre.setSoldeDu(BigDecimal.ZERO);
            membreRepo.save(membre); // lock order: paiement acquired first (#5)
        }

        participation.setStatut(ParticipationStatus.CONFIRME);
        participationRepo.save(participation); // @OneToOne sans cascade → save explicite

        paiementRepo.save(paiement);
        return PaiementDTO.from(paiement);
    }

    @Override
    public PaiementDTO rembourserPaiement(Integer idPaiement, Authentication auth) {
        Paiement paiement = paiementRepo.findById(idPaiement)
                .orElseThrow(() -> new NotFoundException("Paiement introuvable"));

        // SECURITY #3 — site-scope check
        checkPaiementSiteAccess(paiement, auth);

        if (!PaiementStatus.PAYE.equals(paiement.getStatut())) {
            throw new ConflictException("Paiement non payé, remboursement impossible");
        }

        paiement.setStatut(PaiementStatus.REMBOURSE);
        paiementRepo.save(paiement);

        Participation participation = paiement.getParticipation();
        if (ParticipationStatus.CONFIRME.equals(participation.getStatut())) {
            participation.setStatut(ParticipationStatus.EN_ATTENTE);
            participationRepo.save(participation);
        }

        return PaiementDTO.from(paiement);
    }

    @Override
    public List<PaiementDTO> listerPaiementsMembre(Authentication auth) {
        return paiementRepo
                .findByParticipationMembreMatriculeOrderByDatePaiementDesc(auth.getName())
                .stream().map(PaiementDTO::from).toList();
    }

    @Override
    public Page<PaiementDTO> listerPaiementsAdmin(
            Integer matchId,
            String matricule,
            String statut,
            Integer siteId,
            Pageable pageable,
            Authentication auth
    ) {
        boolean isGlobal = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ADMIN_GLOBAL.authority()));

        Specification<Paiement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Object, Object> participationJoin = root.join("participation");
            Join<Object, Object> matchJoin = participationJoin.join("matchPadel");

            if (matchId != null) {
                predicates.add(cb.equal(matchJoin.get("idMatch"), matchId));
            }
            if (matricule != null && !matricule.isBlank()) {
                Join<Object, Object> membreJoin = participationJoin.join("membre");
                predicates.add(cb.equal(membreJoin.get("matricule"), matricule));
            }
            if (statut != null && !statut.isBlank()) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }

            // SECURITY #3 — site-scope enforcement (fail-closed for ADMIN_SITE without claim)
            Integer effectiveSiteId;
            if (isGlobal) {
                effectiveSiteId = siteId;
            } else {
                effectiveSiteId = (Integer) auth.getDetails();
                if (effectiveSiteId == null) {
                    throw new ForbiddenException("Accès refusé");
                }
            }
            if (effectiveSiteId != null) {
                Join<Object, Object> siteJoin = matchJoin
                        .join("disponibilite")
                        .join("terrain")
                        .join("site");
                predicates.add(cb.equal(siteJoin.get("idSite"), effectiveSiteId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return paiementRepo.findAll(spec, pageable).map(PaiementDTO::from);
    }

    private void checkPaiementSiteAccess(Paiement paiement, Authentication auth) {
        Integer idSite = paiement.getParticipation()
                .getMatchPadel().getDisponibilite().getTerrain().getSite().getIdSite();
        siteAccessChecker.check(auth, idSite);
    }
}
