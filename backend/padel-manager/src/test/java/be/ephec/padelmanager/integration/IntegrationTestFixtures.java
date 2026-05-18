package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.config.MatchPolicy;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.DisponibiliteStatus;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import be.ephec.padelmanager.repository.MatchPadelRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.repository.TerrainRepo;

import java.time.LocalDateTime;

/**
 * Stateless factory helpers for integration tests.
 * Each method receives its required repos as parameters — no Spring context needed.
 * Extracted from MatchSchedulerIntegrationTest to be shared across test classes.
 *
 * V3 seed data assumed present: terrains 1-3 (site 1), 4-5 (site 2);
 * membres G0001, S0001, L0001.
 */
public final class IntegrationTestFixtures {

    private IntegrationTestFixtures() {}

    public static Disponibilite createDisponibilite(
            TerrainRepo terrainRepo,
            DisponibiliteRepo disponibiliteRepo,
            int terrainId,
            LocalDateTime debut) {
        Disponibilite dispo = new Disponibilite();
        dispo.setTerrain(terrainRepo.findById(terrainId).orElseThrow());
        dispo.setDateHeureDebut(debut);
        dispo.setDateHeureFin(debut.plusMinutes(90));
        dispo.setStatut(DisponibiliteStatus.RESERVE);
        return disponibiliteRepo.save(dispo);
    }

    public static MatchPadel createMatch(
            MatchPadelRepo matchPadelRepo,
            Disponibilite dispo,
            Membre organizer,
            String typeMatch,
            String statut) {
        MatchPadel match = new MatchPadel();
        match.setDisponibilite(dispo);
        match.setOrganisateur(organizer);
        match.setTypeMatch(typeMatch);
        match.setStatut(statut);
        match.setMontantTotal(MatchPolicy.PRIX_TOTAL_MATCH);
        match.setDateCreation(LocalDateTime.now());
        return matchPadelRepo.save(match);
    }

    public static Participation createParticipation(
            MembreRepo membreRepo,
            ParticipationRepo participationRepo,
            MatchPadel match,
            String matricule,
            String statut) {
        Membre membre = membreRepo.findById(matricule).orElseThrow();
        Participation p = new Participation();
        p.setMatchPadel(match);
        p.setMembre(membre);
        p.setStatut(statut);
        p.setDateInscription(LocalDateTime.now());
        return participationRepo.save(p);
    }
}
