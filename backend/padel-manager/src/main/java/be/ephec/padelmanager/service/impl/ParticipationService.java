package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.exception.BadRequestException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.Penalite;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PaiementRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.IParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class ParticipationService implements IParticipationService {

    private final MatchPadelRepo matchPadelRepo;
    private final ParticipationRepo participationRepo;
    private final PaiementRepo paiementRepo;
    private final MembreRepo membreRepo;
    private final PenaliteRepo penaliteRepo;

    @Override
    public void annulerParticipation(Integer idMatch, Authentication auth) {
        String matricule = auth.getName();

        matchPadelRepo.findById(idMatch)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));

        Participation participation = participationRepo
                .findByMatchPadelIdMatchAndMembreMatricule(idMatch, matricule)
                .orElseThrow(() -> new ForbiddenException(
                        "Accès refusé : vous n'êtes pas inscrit à ce match."));

        if ("ANNULEE".equals(participation.getStatut())) {
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

        participation.setStatut("ANNULEE");

        // Annulation tardive : si le paiement a été effectué, il est
        // absorbé comme frais d'annulation et passe à ANNULE sans
        // ajout de dette. Si le paiement était EN_ATTENTE, il passe
        // à ANNULE et 15€ sont ajoutés au soldeDu.
        if (late) {
            Membre membre = participation.getMembre();
            boolean wasPaid = "PAYE".equals(paiement.getStatut());
            paiement.setStatut("ANNULE");
            if (!wasPaid) {
                BigDecimal current = membre.getSoldeDu() != null ? membre.getSoldeDu() : BigDecimal.ZERO;
                membre.setSoldeDu(current.add(new BigDecimal("15.00")));
                membreRepo.save(membre);
            }
            Penalite pen = new Penalite();
            pen.setMembre(membre);
            pen.setDateDebut(now);
            pen.setDateFin(now.plusDays(7));
            pen.setMotif("ANNULATION_TARDIVE");
            penaliteRepo.save(pen);
        } else {
            if ("PAYE".equals(paiement.getStatut())) {
                paiement.setStatut("REMBOURSE");
            } else {
                paiement.setStatut("ANNULE");
            }
        }

        participationRepo.save(participation);
        paiementRepo.save(paiement);
    }
}
