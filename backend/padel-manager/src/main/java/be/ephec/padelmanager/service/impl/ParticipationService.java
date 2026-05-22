package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.exception.BadRequestException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.PaiementStatus;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.ParticipationStatus;
import be.ephec.padelmanager.model.Penalite;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PaiementRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.IParticipationService;
import be.ephec.padelmanager.config.MatchPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ParticipationService implements IParticipationService {

    private final MatchPadelRepo matchPadelRepo;
    private final ParticipationRepo participationRepo;
    private final PaiementRepo paiementRepo;
    private final MembreRepo membreRepo;
    private final PenaliteRepo penaliteRepo;

    /**
     * Job 2 — Libère les places non payées (CF-M-006).
     * Hérite du @Transactional de classe (propagation REQUIRED).
     * Idempotent : seules les participations EN_ATTENTE sont interrogées ;
     * après libération elles passent à ANNULEE et ne sont plus retraitées.
     * <p>
     * <b>Note transactionnelle :</b> traitement all-or-nothing.
     * Si une participation parmi le batch lève une exception, toute la transaction
     * est rollbackée et aucune participation n'est libérée ce tick-ci. Une isolation
     * par-participation (REQUIRES_NEW via bean helper) serait une amélioration
     * mais sort du périmètre de Sprint 2B.
     * </p>
     */
    @Override
    public int libererPlacesNonPayees() {
        List<Participation> aLiberer = participationRepo.findByStatutAndMatchDispoDebutBefore(
                ParticipationStatus.EN_ATTENTE, LocalDateTime.now().plusHours(MatchPolicy.DELAI_PAIEMENT_H));
        if (aLiberer.isEmpty()) {
            return 0;
        }
        for (Participation p : aLiberer) {
            p.setStatut(ParticipationStatus.ANNULEE);
        }
        participationRepo.saveAll(aLiberer);
        return aLiberer.size();
    }

    @Override
    public void annulerParticipation(Integer idMatch, Authentication auth) {
        String matricule = auth.getName();

        var match = matchPadelRepo.findById(idMatch)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
        if (match.getOrganisateur() != null
                && match.getOrganisateur().getMatricule().equals(matricule)) {
            throw new ForbiddenException(
                    "En tant qu'organisateur, vous ne pouvez pas annuler votre participation seule — annulez le match entier.");
        }

        Participation participation = participationRepo
                .findByMatchPadelIdMatchAndMembreMatricule(idMatch, matricule)
                .orElseThrow(() -> new ForbiddenException(
                        "Accès refusé : vous n'êtes pas inscrit à ce match."));

        if (ParticipationStatus.ANNULEE.equals(participation.getStatut())) {
            throw new BadRequestException("Participation déjà annulée");
        }

        LocalDateTime debut = participation.getMatchPadel().getDisponibilite().getDateHeureDebut();
        LocalDateTime now = LocalDateTime.now();
        if (!debut.isAfter(now)) {
            throw new BadRequestException("Match déjà passé ou en cours");
        }

        Paiement paiement = paiementRepo.findByParticipation(participation)
                .orElseThrow(() -> new BadRequestException(
                        "Paiement introuvable pour participation " + participation.getIdParticipation()));

        long hoursUntilMatch = ChronoUnit.HOURS.between(now, debut);
        boolean late = hoursUntilMatch < 24L;

        participation.setStatut(ParticipationStatus.ANNULEE);

        // Annulation tardive : si le paiement a été effectué, il est
        // absorbé comme frais d'annulation et passe à ANNULE sans
        // ajout de dette. Si le paiement était EN_ATTENTE, il passe
        // à ANNULE et 15€ sont ajoutés au soldeDu.
        if (late) {
            Membre membre = participation.getMembre();
            boolean wasPaid = PaiementStatus.PAYE.equals(paiement.getStatut());
            paiement.setStatut(PaiementStatus.ANNULE);
            if (!wasPaid) {
                BigDecimal current = membre.getSoldeDu() != null ? membre.getSoldeDu() : BigDecimal.ZERO;
                membre.setSoldeDu(current.add(MatchPolicy.PRIX_PLACE_EUR));
                membreRepo.save(membre);
            }
            Penalite pen = new Penalite();
            pen.setMembre(membre);
            pen.setDateDebut(now);
            pen.setDateFin(now.plusDays(MatchPolicy.DUREE_PENALITE_JOURS));
            pen.setMotif("ANNULATION_TARDIVE");
            penaliteRepo.save(pen);
        } else {
            if (PaiementStatus.PAYE.equals(paiement.getStatut())) {
                paiement.setStatut(PaiementStatus.REMBOURSE);
            } else {
                paiement.setStatut(PaiementStatus.ANNULE);
            }
        }

        participationRepo.save(participation);
        paiementRepo.save(paiement);
    }
}
