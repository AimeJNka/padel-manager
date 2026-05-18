package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.PenaliteDTO;
import be.ephec.padelmanager.service.IPenaliteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/penalites")
@RequiredArgsConstructor
public class PenaliteController {

    private final IPenaliteService penaliteService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('GLOBAL','SITE','LIBRE')")
    public ResponseEntity<List<PenaliteDTO>> mesPenalites(Authentication auth) {
        return ResponseEntity.ok(penaliteService.listerPenalitesMembre(auth));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL','ADMIN_SITE')")
    public ResponseEntity<Page<PenaliteDTO>> lister(
            @RequestParam(required = false) String matricule,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(penaliteService.listerPenalitesAdmin(
                matricule, activeOnly, siteId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateDebut")),
                auth));
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL','ADMIN_SITE')")
    public ResponseEntity<PenaliteDTO> annuler(@PathVariable Integer id, Authentication auth) {
        return ResponseEntity.ok(penaliteService.annulerPenalite(id, auth));
    }
}
