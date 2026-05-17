package be.ephec.padelmanager.scheduler;

import be.ephec.padelmanager.service.IMatchPadelService;
import be.ephec.padelmanager.service.IParticipationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchScheduler.class);

    private final IParticipationService participationService;
    private final IMatchPadelService    matchPadelService;

    @Scheduled(cron = "0 0 * * * *")
    public void traiterMatchesHoraire() {
        log.info("[Scheduler] Démarrage du traitement horaire des matchs");
        try {
            int liberes = participationService.libererPlacesNonPayees();
            log.info("[Scheduler] Job 2 — {} place(s) non payée(s) libérée(s)", liberes);
        } catch (Exception e) {
            log.error("[Scheduler] Job 2 (libérer places non payées) a échoué — Job 1 va continuer", e);
        }
        try {
            int bascules = matchPadelService.basculerMatchesIncomplets();
            log.info("[Scheduler] Job 1 — {} match(es) privé(s) basculé(s) en PUBLIC", bascules);
        } catch (Exception e) {
            log.error("[Scheduler] Job 1 (basculer matchs incomplets) a échoué", e);
        }
        try {
            int demarres = matchPadelService.traiterSoldeMatchesDemarres();
            log.info("[Scheduler] Job 3 — {} match(es) marqué(s) DEMARRE", demarres);
        } catch (Exception e) {
            log.error("[Scheduler] Job 3 (calcul solde organisateur) a échoué", e);
        }
        log.info("[Scheduler] Traitement horaire terminé");
    }
}
