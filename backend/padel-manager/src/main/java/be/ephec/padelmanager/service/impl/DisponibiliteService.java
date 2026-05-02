package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.FermeturePonctuelle;
import be.ephec.padelmanager.model.FermetureRecurrente;
import be.ephec.padelmanager.model.HoraireAnnuel;
import be.ephec.padelmanager.model.Terrain;
import be.ephec.padelmanager.repository.DisponibiliteRepo;
import be.ephec.padelmanager.repository.FermeturePonctuelleRepo;
import be.ephec.padelmanager.repository.FermetureRecurrenteRepo;
import be.ephec.padelmanager.repository.HoraireAnnuelRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.repository.TerrainRepo;
import be.ephec.padelmanager.service.IDisponibiliteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DisponibiliteService implements IDisponibiliteService {

    private final DisponibiliteRepo disponibiliteRepo;
    private final TerrainRepo terrainRepo;
    private final HoraireAnnuelRepo horaireRepo;
    private final FermeturePonctuelleRepo fermeturePonctuelleRepo;
    private final FermetureRecurrenteRepo fermetureRecurrenteRepo;
    private final SiteRepo siteRepo;

    static final Duration SLOT_DURATION = Duration.ofMinutes(90);
    static final Duration PAUSE = Duration.ofMinutes(15);
    static final Duration SLOT_STEP = SLOT_DURATION.plus(PAUSE); // 105 min between slot starts

    @Override
    public int genererCreneaux(Integer siteId, Integer annee) {
        return genererCreneauxInterne(siteId, annee, Collections.emptySet());
    }

    @Override
    public int regenererCreneaux(Integer siteId, Integer annee) {
        LocalDateTime yearStart = LocalDate.of(annee, 1, 1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.of(annee, 12, 31).atTime(23, 59, 59);

        // Collect RESERVE slot keys before deleting, to avoid unique-constraint collisions on re-insert
        Set<String> reservedKeys = disponibiliteRepo
                .findByTerrainSiteIdSiteAndDateHeureDebutBetween(siteId, yearStart, yearEnd)
                .stream()
                .filter(d -> "RESERVE".equals(d.getStatut()))
                .map(d -> d.getTerrain().getIdTerrain() + "_" + d.getDateHeureDebut())
                .collect(Collectors.toSet());

        disponibiliteRepo.deleteLibreByTerrainSiteAndYearRange(siteId, yearStart, yearEnd);

        return genererCreneauxInterne(siteId, annee, reservedKeys);
    }

    private int genererCreneauxInterne(Integer siteId, Integer annee, Set<String> excludedKeys) {
        if (!siteRepo.existsById(siteId)) {
            throw new NotFoundException("Site introuvable : " + siteId);
        }

        HoraireAnnuel horaire = horaireRepo.findBySiteIdSiteAndAnnee(siteId, annee)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun horaire défini pour le site " + siteId + " en " + annee));

        List<Terrain> terrains = terrainRepo.findBySiteIdSite(siteId);
        if (terrains.isEmpty()) return 0;

        // id_site = NULL means global closure (all sites) — fetch both
        Set<LocalDate> fermeturesPonctuelles = fermeturePonctuelleRepo
                .findBySiteIdSiteOrSiteIsNull(siteId).stream()
                .map(FermeturePonctuelle::getDateFermeture)
                .collect(Collectors.toSet());

        // jour_semaine is 0-indexed starting Monday (matches DayOfWeek.ordinal())
        Set<Integer> fermeturesRecurrentes = fermetureRecurrenteRepo
                .findBySiteIdSiteOrSiteIsNull(siteId).stream()
                .map(FermetureRecurrente::getJourSemaine)
                .collect(Collectors.toSet());

        List<Disponibilite> newDispos = new ArrayList<>();

        for (LocalDate date = LocalDate.of(annee, 1, 1);
             !date.isAfter(LocalDate.of(annee, 12, 31));
             date = date.plusDays(1)) {

            if (fermeturesPonctuelles.contains(date)) continue;
            if (fermeturesRecurrentes.contains(date.getDayOfWeek().ordinal())) continue;

            for (Terrain terrain : terrains) {
                LocalDateTime slotStart = date.atTime(horaire.getHeureOuverture());
                LocalDateTime closeTime = date.atTime(horaire.getHeureFermeture());
                while (!slotStart.plus(SLOT_DURATION).isAfter(closeTime)) {
                    String key = terrain.getIdTerrain() + "_" + slotStart;
                    if (!excludedKeys.contains(key)) {
                        Disponibilite disponibilite = new Disponibilite();
                        disponibilite.setTerrain(terrain);
                        disponibilite.setDateHeureDebut(slotStart);
                        disponibilite.setDateHeureFin(slotStart.plus(SLOT_DURATION));
                        disponibilite.setStatut("LIBRE");
                        newDispos.add(disponibilite);
                    }
                    slotStart = slotStart.plus(SLOT_STEP);
                }
            }
        }

        disponibiliteRepo.saveAll(newDispos);
        return newDispos.size();
    }
}
