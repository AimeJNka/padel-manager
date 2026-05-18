package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.MatchPadelDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface IMatchPadelService {
    MatchPadelDTO creerMatchPrive(Integer dispoId, Authentication auth);
    MatchPadelDTO creerMatchPublic(Integer dispoId, Authentication auth);
    void ajouterJoueur(Integer idMatch, String matriculeJoueur, Authentication auth);
    void sInscrireMatchPublic(Integer idMatch, Authentication auth);
    void annulerMatch(Integer idMatch, Authentication auth);

    int basculerMatchesIncomplets();

    /**
     * Job 3 — Calcule le solde dû par l'organisateur pour les places vides
     * au démarrage du match (CF-M-007) et marque le match comme DEMARRE.
     * Hérite du @Transactional de classe (propagation REQUIRED).
     * Idempotent : seuls les matchs EN_ATTENTE dont le créneau a démarré
     * sont interrogés ; après traitement ils passent à DEMARRE et ne sont
     * plus retraités.
     * <p>
     * <b>Règle métier :</b> placesVides = 4 − count(participations CONFIRME).
     * Le soldeDu de l'organisateur est incrémenté de (placesVides × 15€).
     * La place de l'organisateur lui-même est comptée comme vide s'il n'a
     * pas confirmé sa participation (pas d'exception, conforme CF-M-007).
     * </p>
     * <p>
     * <b>Note transactionnelle :</b> traitement all-or-nothing.
     * Si un match parmi le batch lève une exception, toute la transaction
     * est rollbackée et aucun match n'est marqué DEMARRE ce tick-ci. Une
     * isolation par-match (REQUIRES_NEW via bean helper) serait une
     * amélioration mais sort du périmètre de Sprint 2C.
     * </p>
     *
     * @return le nombre de matchs effectivement traités (transitionnés vers DEMARRE)
     */
    int traiterSoldeMatchesDemarres();
    Page<MatchPadelDTO> listerMatchs(Integer siteId, String statut, String type, Boolean mine, Pageable pageable, Authentication auth);
    MatchPadelDTO getMatch(Integer idMatch, Authentication auth);
}
