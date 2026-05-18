package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.AjouterJoueurRequest;
import be.ephec.padelmanager.dto.CreerMatchRequest;
import be.ephec.padelmanager.dto.MatchPadelDTO;
import be.ephec.padelmanager.service.IMatchPadelService;
import be.ephec.padelmanager.service.IParticipationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/matchs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GLOBAL', 'SITE')")
public class MatchController {

    private final IMatchPadelService matchPadelService;
    private final IParticipationService participationService;

    @PostMapping("/prive")
    @PreAuthorize("hasAnyRole('GLOBAL', 'SITE')")
    public ResponseEntity<MatchPadelDTO> creerMatchPrive(
            @Valid @RequestBody CreerMatchRequest request,
            Authentication auth) {
        MatchPadelDTO dto = matchPadelService.creerMatchPrive(request.getDispoId(), auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/public")
    @PreAuthorize("hasAnyRole('GLOBAL', 'SITE')")
    public ResponseEntity<MatchPadelDTO> creerMatchPublic(
            @Valid @RequestBody CreerMatchRequest request,
            Authentication auth) {
        MatchPadelDTO dto = matchPadelService.creerMatchPublic(request.getDispoId(), auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/joueurs")
    public ResponseEntity<Map<String, String>> ajouterJoueur(
            @PathVariable Integer id,
            @Valid @RequestBody AjouterJoueurRequest request,
            Authentication auth) {
        matchPadelService.ajouterJoueur(id, request.getMatricule(), auth);
        return ResponseEntity.ok(Map.of("message", "Joueur ajouté avec succès"));
    }

    @PostMapping("/{id}/inscription")
    @PreAuthorize("hasAnyRole('GLOBAL', 'SITE', 'LIBRE')")
    public ResponseEntity<Map<String, String>> sInscrireMatchPublic(
            @PathVariable Integer id,
            Authentication auth) {
        matchPadelService.sInscrireMatchPublic(id, auth);
        return ResponseEntity.ok(Map.of("message", "Inscription confirmée"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annulerMatch(
            @PathVariable Integer id,
            Authentication auth) {
        matchPadelService.annulerMatch(id, auth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idMatch}/participation")
    @PreAuthorize("hasAnyRole('GLOBAL', 'SITE', 'LIBRE')")
    public ResponseEntity<Void> annulerParticipation(
            @PathVariable Integer idMatch,
            Authentication auth) {
        participationService.annulerParticipation(idMatch, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GLOBAL','SITE','LIBRE','ADMIN_GLOBAL','ADMIN_SITE')")
    public ResponseEntity<Page<MatchPadelDTO>> lister(
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(matchPadelService.listerMatchs(
                siteId, statut, type, mine,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateCreation")),
                auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GLOBAL','SITE','LIBRE','ADMIN_GLOBAL','ADMIN_SITE')")
    public ResponseEntity<MatchPadelDTO> getOne(@PathVariable Integer id, Authentication auth) {
        return ResponseEntity.ok(matchPadelService.getMatch(id, auth));
    }
}
