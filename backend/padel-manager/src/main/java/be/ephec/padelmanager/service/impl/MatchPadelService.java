package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.DisponibiliteDTO;
import be.ephec.padelmanager.dto.MatchPadelDTO;
import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.dto.PersonneDTO;
import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.dto.TerrainDTO;
import be.ephec.padelmanager.dto.TypeMembreDTO;
import be.ephec.padelmanager.exception.BadRequestException;
import be.ephec.padelmanager.exception.ConflictException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.IMatchPadelService;
import be.ephec.padelmanager.service.IPaiementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    @Override
    public MatchPadelDTO creerMatchPrive(Integer dispoId, Authentication auth) {
        Membre organisateur = resolveMembre(auth);
        Disponibilite dispo = resolveDisponibilite(dispoId);
        verifierConditionsCreation(organisateur, dispo);
        return creerMatch(organisateur, dispo, "PRIVE");
    }

    @Override
    public MatchPadelDTO creerMatchPublic(Integer dispoId, Authentication auth) {
        Membre organisateur = resolveMembre(auth);
        Disponibilite dispo = resolveDisponibilite(dispoId);
        verifierConditionsCreation(organisateur, dispo);
        return creerMatch(organisateur, dispo, "PUBLIC");
    }

    @Override
    public void ajouterJoueur(Integer idMatch, String matriculeJoueur, Authentication auth) {
        MatchPadel match = resolveMatch(idMatch);
        if ("ANNULE".equals(match.getStatut())) {
            throw new BadRequestException("Le match est annulé");
        }
        if (match.getOrganisateur() == null
                || !match.getOrganisateur().getMatricule().equals(auth.getName())) {
            throw new ForbiddenException("Seul l'organisateur peut ajouter un joueur");
        }
        Membre joueur = membreRepo.findById(matriculeJoueur)
                .orElseThrow(() -> new NotFoundException("Membre introuvable : " + matriculeJoueur));

        if (penaliteRepo.existsByMembreMatriculeAndDateFinAfter(joueur.getMatricule(), LocalDateTime.now())) {
            throw new ForbiddenException("Le joueur a une pénalité active");
        }
        if (joueur.getSoldeDu() != null && joueur.getSoldeDu().compareTo(BigDecimal.ZERO) > 0) {
            throw new ForbiddenException("Le joueur a un solde dû");
        }
        if (participationRepo.existsByMatchPadelIdMatchAndMembreMatricule(idMatch, joueur.getMatricule())) {
            throw new ConflictException("Le joueur est déjà inscrit au match");
        }
        if (participationRepo.countByMatchPadelIdMatchAndStatutNot(idMatch, "ANNULEE") >= 4) {
            throw new BadRequestException("Le match est complet (4 joueurs maximum)");
        }

        Participation participation = new Participation();
        participation.setMatchPadel(match);
        participation.setMembre(joueur);
        participation.setStatut("EN_ATTENTE");
        participation.setDateInscription(LocalDateTime.now());
        participationRepo.save(participation);
        paiementService.creerPourParticipation(participation);
    }

    @Override
    public void sInscrireMatchPublic(Integer idMatch, Authentication auth) {
        MatchPadel match = resolveMatch(idMatch);
        if (!"PUBLIC".equals(match.getTypeMatch())) {
            throw new BadRequestException("Le match n'est pas public");
        }
        if ("ANNULE".equals(match.getStatut())) {
            throw new BadRequestException("Le match est annulé");
        }
        Membre membre = resolveMembre(auth);

        if (penaliteRepo.existsByMembreMatriculeAndDateFinAfter(membre.getMatricule(), LocalDateTime.now())) {
            throw new ForbiddenException("Vous avez une pénalité active");
        }
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
        if (participationRepo.countByMatchPadelIdMatchAndStatutNot(idMatch, "ANNULEE") >= 4) {
            throw new BadRequestException("Le match est complet (4 joueurs maximum)");
        }

        Participation participation = new Participation();
        participation.setMatchPadel(match);
        participation.setMembre(membre);
        participation.setStatut("EN_ATTENTE");
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
        if ("ANNULE".equals(match.getStatut())) {
            throw new BadRequestException("Le match est déjà annulé");
        }

        Disponibilite dispo = match.getDisponibilite();
        if (dispo == null || dispo.getDateHeureDebut() == null) {
            throw new BadRequestException("Créneau invalide");
        }
        long heuresRestantes = ChronoUnit.HOURS.between(LocalDateTime.now(), dispo.getDateHeureDebut());
        long delaiRequis = "PUBLIC".equals(match.getTypeMatch()) ? 24 : 48;
        if (heuresRestantes < delaiRequis) {
            throw new BadRequestException(
                    "Annulation impossible : délai minimum de " + delaiRequis + " heures non respecté");
        }

        dispo.setStatut("LIBRE");
        disponibiliteRepo.save(dispo);

        List<Participation> participations = participationRepo.findByMatchPadelIdMatch(idMatch);
        for (Participation p : participations) {
            p.setStatut("ANNULEE");
            paiementService.annulerPourParticipation(p);
        }
        participationRepo.saveAll(participations);

        match.setStatut("ANNULE");
        matchPadelRepo.save(match);
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
        if (!"LIBRE".equals(dispo.getStatut())) {
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
        dispo.setStatut("RESERVE");
        disponibiliteRepo.save(dispo);

        MatchPadel match = new MatchPadel();
        match.setDisponibilite(dispo);
        match.setOrganisateur(organisateur);
        match.setTypeMatch(typeMatch);
        match.setStatut("EN_ATTENTE");
        match.setMontantTotal(new BigDecimal("60.00"));
        match.setDateCreation(LocalDateTime.now());
        MatchPadel saved = matchPadelRepo.save(match);

        Participation participation = new Participation();
        participation.setMatchPadel(saved);
        participation.setMembre(organisateur);
        participation.setStatut("EN_ATTENTE");
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
}
