package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.DisponibiliteDTO;
import be.ephec.padelmanager.dto.MatchPadelDTO;
import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.dto.ParticipationDTO;
import be.ephec.padelmanager.dto.PersonneDTO;
import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.dto.TerrainDTO;
import be.ephec.padelmanager.dto.TypeMembreDTO;
import be.ephec.padelmanager.exception.BadRequestException;
import be.ephec.padelmanager.exception.ConflictException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.DisponibiliteStatus;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.MatchStatus;
import be.ephec.padelmanager.model.MatchType;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.ParticipationStatus;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.Personne;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.IMatchPadelService;
import be.ephec.padelmanager.service.IPaiementService;
import be.ephec.padelmanager.service.IPenaliteService;
import be.ephec.padelmanager.config.MatchPolicy;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MatchPadelService implements IMatchPadelService {

    private final MatchPadelRepo matchPadelRepo;
    private final ParticipationRepo participationRepo;
    private final DisponibiliteRepo disponibiliteRepo;
    private final MembreRepo membreRepo;
    private final PenaliteRepo penaliteRepo;
    private final IPaiementService paiementService;
    private final IPenaliteService penaliteService;

    @Override
    public MatchPadelDTO creerMatchPrive(Integer dispoId, Authentication auth) {
        Membre organisateur = resolveMembre(auth);
        Disponibilite dispo = resolveDisponibilite(dispoId);
        verifierConditionsCreation(organisateur, dispo);
        return creerMatch(organisateur, dispo, MatchType.PRIVE);
    }

    @Override
    public MatchPadelDTO creerMatchPublic(Integer dispoId, Authentication auth) {
        Membre organisateur = resolveMembre(auth);
        Disponibilite dispo = resolveDisponibilite(dispoId);
        verifierConditionsCreation(organisateur, dispo);
        return creerMatch(organisateur, dispo, MatchType.PUBLIC);
    }

    @Override
    public void ajouterJoueur(Integer idMatch, String matriculeJoueur, Authentication auth) {
        MatchPadel match = resolveMatch(idMatch);
        if (MatchStatus.ANNULE.equals(match.getStatut())) {
            throw new BadRequestException("Le match est annulé");
        }
        if (match.getOrganisateur() == null
                || !match.getOrganisateur().getMatricule().equals(auth.getName())) {
            throw new ForbiddenException("Seul l'organisateur peut ajouter un joueur");
        }
        if (MatchType.PUBLIC.equals(match.getTypeMatch())) {
            throw new BadRequestException(
                    "CF-M-010 : l'organisateur ne peut pas ajouter directement un joueur à un match public");
        }
        Membre joueur = membreRepo.findById(matriculeJoueur)
                .orElseThrow(() -> new NotFoundException("Membre introuvable : " + matriculeJoueur));

        if (joueur.getSoldeDu() != null && joueur.getSoldeDu().compareTo(BigDecimal.ZERO) > 0) {
            throw new ForbiddenException("Le joueur a un solde dû");
        }
        if (participationRepo.existsByMatchPadelIdMatchAndMembreMatricule(idMatch, joueur.getMatricule())) {
            throw new ConflictException("Le joueur est déjà inscrit au match");
        }
        if (participationRepo.countByMatchPadelIdMatchAndStatutNot(idMatch, ParticipationStatus.ANNULEE) >= MatchPolicy.NB_JOUEURS_MATCH) {
            throw new BadRequestException("Le match est complet (4 joueurs maximum)");
        }

        Participation participation = new Participation();
        participation.setMatchPadel(match);
        participation.setMembre(joueur);
        participation.setStatut(ParticipationStatus.EN_ATTENTE);
        participation.setDateInscription(LocalDateTime.now());
        participationRepo.save(participation);
        paiementService.creerPourParticipation(participation);
    }

    @Override
    public void sInscrireMatchPublic(Integer idMatch, Authentication auth) {
        MatchPadel match = resolveMatch(idMatch);
        if (!MatchType.PUBLIC.equals(match.getTypeMatch())) {
            throw new BadRequestException("Le match n'est pas public");
        }
        if (MatchStatus.ANNULE.equals(match.getStatut())) {
            throw new BadRequestException("Le match est annulé");
        }
        Membre membre = resolveMembre(auth);

        if (membre.getSoldeDu() != null && membre.getSoldeDu().compareTo(BigDecimal.ZERO) > 0) {
            throw new ForbiddenException("Vous avez un solde dû");
        }

        Disponibilite dispo = match.getDisponibilite();
        if (dispo == null || dispo.getTerrain() == null || dispo.getTerrain().getSite() == null) {
            throw new BadRequestException("Créneau ou terrain invalide");
        }
        if (membre.getSite() != null
                && !membre.getSite().getIdSite().equals(dispo.getTerrain().getSite().getIdSite())) {
            throw new ForbiddenException("Vous ne pouvez vous inscrire qu'à un match de votre site");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!dispo.getDateHeureDebut().isAfter(now)) {
            throw new BadRequestException("Le créneau est dans le passé");
        }
        Integer delai = membre.getTypeMembre() != null ? membre.getTypeMembre().getDelaiReservationJours() : null;
        if (delai != null && dispo.getDateHeureDebut().isAfter(now.plusDays(delai))) {
            throw new BadRequestException("Le créneau est en dehors de la fenêtre de réservation");
        }

        if (participationRepo.existsByMatchPadelIdMatchAndMembreMatricule(idMatch, membre.getMatricule())) {
            throw new ConflictException("Vous êtes déjà inscrit à ce match");
        }
        if (participationRepo.countByMatchPadelIdMatchAndStatutNot(idMatch, ParticipationStatus.ANNULEE) >= MatchPolicy.NB_JOUEURS_MATCH) {
            throw new BadRequestException("Le match est complet (4 joueurs maximum)");
        }

        Participation participation = new Participation();
        participation.setMatchPadel(match);
        participation.setMembre(membre);
        participation.setStatut(ParticipationStatus.EN_ATTENTE);
        participation.setDateInscription(LocalDateTime.now());
        participationRepo.save(participation);
        paiementService.creerPourParticipation(participation);
    }

    @Override
    public void annulerMatch(Integer idMatch, Authentication auth) {
        MatchPadel match = resolveMatch(idMatch);
        if (match.getOrganisateur() == null
                || !match.getOrganisateur().getMatricule().equals(auth.getName())) {
            throw new ForbiddenException("Seul l'organisateur peut annuler le match");
        }
        if (MatchStatus.ANNULE.equals(match.getStatut())) {
            throw new BadRequestException("Le match est déjà annulé");
        }

        Disponibilite dispo = match.getDisponibilite();
        if (dispo == null || dispo.getDateHeureDebut() == null) {
            throw new BadRequestException("Créneau invalide");
        }
        long heuresRestantes = ChronoUnit.HOURS.between(LocalDateTime.now(), dispo.getDateHeureDebut());
        long delaiRequis = MatchType.PUBLIC.equals(match.getTypeMatch()) ? MatchPolicy.DELAI_ANNULATION_PUBLIC_H : MatchPolicy.DELAI_ANNULATION_PRIVE_H;
        if (heuresRestantes < delaiRequis) {
            throw new BadRequestException(
                    "Annulation impossible : délai minimum de " + delaiRequis + " heures non respecté");
        }

        dispo.setStatut(DisponibiliteStatus.LIBRE);
        disponibiliteRepo.save(dispo);

        List<Participation> participations = participationRepo.findByMatchPadelIdMatch(idMatch);
        for (Participation p : participations) {
            p.setStatut(ParticipationStatus.ANNULEE);
            paiementService.annulerPourParticipation(p);
        }
        participationRepo.saveAll(participations);

        match.setStatut(MatchStatus.ANNULE);
        matchPadelRepo.save(match);
    }

    /**
     * Job 1 — Bascule les matchs privés incomplets en PUBLIC (CF-M-004/CF-M-005).
     * Hérite du @Transactional de classe (propagation REQUIRED).
     * Idempotent : seuls les matchs PRIVE+EN_ATTENTE sont interrogés ;
     * après bascule ils deviennent PUBLIC et ne sont plus retraités.
     * <p>
     * <b>Note transactionnelle :</b> traitement all-or-nothing.
     * Si un match parmi le batch lève une exception, toute la transaction
     * est rollbackée et aucun match n'est basculé ce tick-ci. Une isolation
     * par-match (REQUIRES_NEW via bean helper) serait une amélioration
     * mais sort du périmètre de Sprint 2B.
     * </p>
     */
    @Override
    public int basculerMatchesIncomplets() {
        List<MatchPadel> candidats = matchPadelRepo.findByTypeMatchAndStatutAndDispoDebutBefore(
                MatchType.PRIVE, MatchStatus.EN_ATTENTE, LocalDateTime.now().plusHours(MatchPolicy.DELAI_PAIEMENT_H));
        int count = 0;
        for (MatchPadel match : candidats) {
            long confirmes = participationRepo.countByMatchIdAndStatut(
                    match.getIdMatch(), ParticipationStatus.CONFIRME);
            if (confirmes < MatchPolicy.NB_JOUEURS_MATCH) {
                match.setTypeMatch(MatchType.PUBLIC);
                // Save explicite pour lisibilité — dirty checking JPA suffirait, flush en fin de @Transactional
                matchPadelRepo.save(match);
                penaliteService.appliquerPenalite(
                        match.getOrganisateur(), MatchPolicy.DUREE_PENALITE_JOURS,
                        String.format("Match privé #%d incomplet", match.getIdMatch()));
                count++;
            }
        }
        return count;
    }

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
    @Override
    public int traiterSoldeMatchesDemarres() {
        List<MatchPadel> eligibles = matchPadelRepo.findStartedMatchesByStatut(
                MatchStatus.EN_ATTENTE, LocalDateTime.now());
        log.info("[Job 3] {} match(es) éligible(s) pour calcul de solde", eligibles.size());
        int count = 0;
        for (MatchPadel match : eligibles) {
            long confirmes = participationRepo.countByMatchIdAndStatut(
                    match.getIdMatch(), ParticipationStatus.CONFIRME);
            int placesVides = Math.max(0, MatchPolicy.NB_JOUEURS_MATCH - (int) confirmes);
            if (placesVides > 0) {
                BigDecimal soldeAjout = MatchPolicy.PRIX_PLACE_EUR.multiply(BigDecimal.valueOf(placesVides));
                Membre organisateur = match.getOrganisateur();
                BigDecimal actuel = organisateur.getSoldeDu() != null
                        ? organisateur.getSoldeDu() : BigDecimal.ZERO;
                organisateur.setSoldeDu(actuel.add(soldeAjout));
                // Save explicite pour lisibilité — dirty checking JPA suffirait, flush en fin de @Transactional
                membreRepo.save(organisateur);
            }
            match.setStatut(MatchStatus.DEMARRE);
            // Save explicite pour lisibilité — dirty checking JPA suffirait, flush en fin de @Transactional
            matchPadelRepo.save(match);
            count++;
        }
        log.info("[Job 3] {} match(es) traité(s), marqués DEMARRE", count);
        return count;
    }

    private void verifierConditionsCreation(Membre membre, Disponibilite dispo) {
        if (membre.getTypeMembre() == null
                || !Boolean.TRUE.equals(membre.getTypeMembre().getPeutCreerMatch())) {
            throw new ForbiddenException("Votre type de membre ne permet pas de créer un match");
        }
        if (penaliteRepo.existsByMembreMatriculeAndDateFinAfter(membre.getMatricule(), LocalDateTime.now())) {
            throw new ForbiddenException("Vous avez une pénalité active");
        }
        if (membre.getSoldeDu() != null && membre.getSoldeDu().compareTo(BigDecimal.ZERO) > 0) {
            throw new ForbiddenException("Vous avez un solde dû");
        }
        if (!DisponibiliteStatus.LIBRE.equals(dispo.getStatut())) {
            throw new ConflictException("Le créneau n'est pas disponible");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!dispo.getDateHeureDebut().isAfter(now)) {
            throw new BadRequestException("Le créneau est dans le passé");
        }
        Integer delai = membre.getTypeMembre().getDelaiReservationJours();
        if (delai != null && dispo.getDateHeureDebut().isAfter(now.plusDays(delai))) {
            throw new BadRequestException("Le créneau est en dehors de la fenêtre de réservation");
        }
        if (dispo.getTerrain() == null || dispo.getTerrain().getSite() == null) {
            throw new BadRequestException("Terrain ou site invalide");
        }
        if (membre.getSite() != null
                && !membre.getSite().getIdSite().equals(dispo.getTerrain().getSite().getIdSite())) {
            throw new ForbiddenException("Vous ne pouvez créer un match que sur votre site");
        }
    }

    private MatchPadelDTO creerMatch(Membre organisateur, Disponibilite dispo, String typeMatch) {
        dispo.setStatut(DisponibiliteStatus.RESERVE);
        disponibiliteRepo.save(dispo);

        MatchPadel match = new MatchPadel();
        match.setDisponibilite(dispo);
        match.setOrganisateur(organisateur);
        match.setTypeMatch(typeMatch);
        match.setStatut(MatchStatus.EN_ATTENTE);
        match.setMontantTotal(MatchPolicy.PRIX_TOTAL_MATCH);
        match.setDateCreation(LocalDateTime.now());
        MatchPadel saved = matchPadelRepo.save(match);

        Participation participation = new Participation();
        participation.setMatchPadel(saved);
        participation.setMembre(organisateur);
        participation.setStatut(ParticipationStatus.EN_ATTENTE);
        participation.setDateInscription(LocalDateTime.now());
        participationRepo.save(participation);
        paiementService.creerPourParticipation(participation);

        return toDTO(saved);
    }

    private Membre resolveMembre(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ForbiddenException("Authentification requise");
        }
        return membreRepo.findById(auth.getName())
                .orElseThrow(() -> new NotFoundException("Membre introuvable : " + auth.getName()));
    }

    private Disponibilite resolveDisponibilite(Integer id) {
        return disponibiliteRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Créneau introuvable : " + id));
    }

    private MatchPadel resolveMatch(Integer id) {
        return matchPadelRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Match introuvable : " + id));
    }

    private MatchPadelDTO toDTO(MatchPadel m) {
        if (m == null) return null;
        MatchPadelDTO dto = new MatchPadelDTO();
        dto.setIdMatch(m.getIdMatch());
        dto.setTypeMatch(m.getTypeMatch());
        dto.setStatut(m.getStatut());
        dto.setMontantTotal(m.getMontantTotal());
        dto.setDateCreation(m.getDateCreation());
        dto.setDisponibilite(toDisponibiliteDTO(m.getDisponibilite()));
        dto.setOrganisateur(toMembreDTO(m.getOrganisateur()));
        return dto;
    }

    private MatchPadelDTO toDTOWithParticipations(MatchPadel m) {
        MatchPadelDTO dto = toDTO(m);
        List<Participation> participations =
                participationRepo.findByMatchPadelIdMatch(m.getIdMatch());
        dto.setParticipations(
            participations.stream()
                .filter(p -> !ParticipationStatus.ANNULEE.equals(p.getStatut()))
                .map(this::toParticipationDTO)
                .toList()
        );
        return dto;
    }

    private ParticipationDTO toParticipationDTO(Participation p) {
        ParticipationDTO dto = new ParticipationDTO();
        dto.setIdParticipation(p.getIdParticipation());
        dto.setMatricule(p.getMembre().getMatricule());
        Personne personne = p.getMembre().getPersonne();
        dto.setPrenom(personne != null ? personne.getPrenom() : null);
        dto.setNom(personne != null ? personne.getNom() : null);
        dto.setStatutParticipation(p.getStatut());
        Paiement pai = p.getPaiement();
        if (pai != null) {
            dto.setStatutPaiement(pai.getStatut());
            dto.setMontantPaiement(pai.getMontant());
        }
        return dto;
    }

    private DisponibiliteDTO toDisponibiliteDTO(Disponibilite d) {
        if (d == null) return null;
        DisponibiliteDTO dto = new DisponibiliteDTO();
        dto.setIdDispo(d.getIdDispo());
        dto.setDateHeureDebut(d.getDateHeureDebut());
        dto.setDateHeureFin(d.getDateHeureFin());
        dto.setStatut(d.getStatut());
        dto.setTerrain(toTerrainDTO(d.getTerrain()));
        return dto;
    }

    private TerrainDTO toTerrainDTO(be.ephec.padelmanager.model.Terrain t) {
        if (t == null) return null;
        TerrainDTO dto = new TerrainDTO();
        dto.setIdTerrain(t.getIdTerrain());
        dto.setNumero(t.getNumero());
        dto.setStatut(t.getStatut());
        dto.setSite(toSiteDTO(t.getSite()));
        return dto;
    }

    private SiteDTO toSiteDTO(be.ephec.padelmanager.model.Site s) {
        if (s == null) return null;
        SiteDTO dto = new SiteDTO();
        dto.setIdSite(s.getIdSite());
        dto.setNom(s.getNom());
        dto.setAdresse(s.getAdresse());
        dto.setVille(s.getVille());
        dto.setActif(s.getActif());
        return dto;
    }

    private MembreDTO toMembreDTO(Membre m) {
        if (m == null) return null;
        MembreDTO dto = new MembreDTO();
        dto.setMatricule(m.getMatricule());
        dto.setDateInscription(m.getDateInscription());
        dto.setSoldeDu(m.getSoldeDu());
        dto.setSite(toSiteDTO(m.getSite()));
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
        return dto;
    }

    @Override
    public Page<MatchPadelDTO> listerMatchs(Integer siteId, String statut, String type, Boolean mine, Pageable pageable, Authentication auth) {
        // Enforce site scope for SITE members — cannot be bypassed by omitting the param
        Integer effectiveSiteId = (auth != null && auth.getDetails() instanceof Integer authSiteId)
                ? authSiteId : siteId;
        return matchPadelRepo.findAll(buildMatchSpec(effectiveSiteId, statut, type, mine, auth), pageable)
                .map(this::toDTO);
    }

    @Override
    public MatchPadelDTO getMatch(Integer idMatch, Authentication auth) {
        return toDTOWithParticipations(resolveMatch(idMatch));
    }

    private Specification<MatchPadel> buildMatchSpec(Integer siteId, String statut, String type, Boolean mine, Authentication auth) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (siteId != null) {
                predicates.add(cb.equal(root.get("disponibilite").get("terrain").get("site").get("idSite"), siteId));
            }
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("typeMatch"), type));
            }
            if (Boolean.TRUE.equals(mine) && auth != null && auth.getName() != null) {
                String matricule = auth.getName();
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<Participation> pRoot = sub.from(Participation.class);
                sub.select(pRoot.get("matchPadel").get("idMatch").as(Integer.class))
                   .where(
                       cb.equal(pRoot.get("membre").get("matricule"), matricule),
                       cb.notEqual(pRoot.get("statut"), ParticipationStatus.ANNULEE)
                   );
                predicates.add(root.get("idMatch").in(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
