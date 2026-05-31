package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.DisponibiliteDTO;
import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.dto.TerrainDTO;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Disponibilite;
import be.ephec.padelmanager.model.DisponibiliteStatus;
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
import be.ephec.padelmanager.service.SiteAccessChecker;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
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

@Slf4j
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
    private final SiteAccessChecker siteAccessChecker;

    static final Duration SLOT_DURATION = Duration.ofMinutes(90);
    static final Duration PAUSE = Duration.ofMinutes(15);
    static final Duration SLOT_STEP = SLOT_DURATION.plus(PAUSE); // 105 min between slot starts

    @Override
    public int genererCreneaux(Integer siteId, Integer annee, Authentication authentication) {
        siteAccessChecker.check(authentication, siteId);
        return genererCreneauxInterne(siteId, annee, Collections.<SlotKey>emptySet());
    }

    @Override
    public int regenererCreneaux(Integer siteId, Integer annee, Authentication authentication) {
        siteAccessChecker.check(authentication, siteId);
        LocalDateTime yearStart = LocalDate.of(annee, 1, 1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.of(annee, 12, 31).atTime(23, 59, 59);

        // Dispos surviving the bulk DELETE = RESERVE rows + LIBRE rows still referenced by a match.
        // Both must be excluded from re-generation to avoid UNIQUE-constraint collisions on re-insert.
        Set<Integer> matchedDispoIds = disponibiliteRepo.findMatchedDispoIdsForSiteInRange(siteId, yearStart, yearEnd);

        Set<SlotKey> excludedKeys = disponibiliteRepo
                .findByTerrainSiteIdSiteAndDateHeureDebutBetween(siteId, yearStart, yearEnd)
                .stream()
                .filter(d -> DisponibiliteStatus.RESERVE.equals(d.getStatut())
                          || matchedDispoIds.contains(d.getIdDispo()))
                .map(d -> new SlotKey(d.getTerrain().getIdTerrain(), d.getDateHeureDebut()))
                .collect(Collectors.toSet());

        disponibiliteRepo.deleteLibreByTerrainSiteAndYearRange(siteId, yearStart, yearEnd);

        return genererCreneauxInterne(siteId, annee, excludedKeys);
    }

    // Callers must call siteAccessChecker.check() before invoking this method.
    private int genererCreneauxInterne(Integer siteId, Integer annee, Set<SlotKey> excludedKeys) {
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
                    SlotKey key = new SlotKey(terrain.getIdTerrain(), slotStart);
                    if (!excludedKeys.contains(key)) {
                        Disponibilite disponibilite = new Disponibilite();
                        disponibilite.setTerrain(terrain);
                        disponibilite.setDateHeureDebut(slotStart);
                        disponibilite.setDateHeureFin(slotStart.plus(SLOT_DURATION));
                        disponibilite.setStatut(DisponibiliteStatus.LIBRE);
                        newDispos.add(disponibilite);
                    }
                    slotStart = slotStart.plus(SLOT_STEP);
                }
            }
        }

        disponibiliteRepo.saveAll(newDispos);
        return newDispos.size();
    }

    @Override
    public Page<DisponibiliteDTO> listerDisponibilites(Integer siteId, Integer terrainId, LocalDate date, String statut, Pageable pageable) {
        return disponibiliteRepo.findAll(buildListSpec(siteId, terrainId, date, statut), pageable)
                .map(this::toDTO);
    }

    private Specification<Disponibilite> buildListSpec(Integer siteId, Integer terrainId, LocalDate date, String statut) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (siteId != null) {
                predicates.add(cb.equal(root.get("terrain").get("site").get("idSite"), siteId));
            }
            if (terrainId != null) {
                predicates.add(cb.equal(root.get("terrain").get("idTerrain"), terrainId));
            }
            if (date != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateHeureDebut"), date.atStartOfDay()));
                predicates.add(cb.lessThan(root.get("dateHeureDebut"), date.plusDays(1).atStartOfDay()));
            }
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DisponibiliteDTO toDTO(Disponibilite d) {
        DisponibiliteDTO dto = new DisponibiliteDTO();
        dto.setIdDispo(d.getIdDispo());
        dto.setDateHeureDebut(d.getDateHeureDebut());
        dto.setDateHeureFin(d.getDateHeureFin());
        dto.setStatut(d.getStatut());
        if (d.getTerrain() != null) {
            TerrainDTO t = new TerrainDTO();
            t.setIdTerrain(d.getTerrain().getIdTerrain());
            t.setNumero(d.getTerrain().getNumero());
            t.setStatut(d.getTerrain().getStatut());
            if (d.getTerrain().getSite() != null) {
                SiteDTO s = new SiteDTO();
                s.setIdSite(d.getTerrain().getSite().getIdSite());
                s.setNom(d.getTerrain().getSite().getNom());
                s.setAdresse(d.getTerrain().getSite().getAdresse());
                s.setVille(d.getTerrain().getSite().getVille());
                s.setActif(d.getTerrain().getSite().getActif());
                t.setSite(s);
            }
            dto.setTerrain(t);
        }
        return dto;
    }

    // Typed key for excludedKeys matching during regeneration.
    // LocalDateTime.equals compares fields directly — no toString format drift risk.
    private record SlotKey(int terrainId, LocalDateTime time) {}
}
