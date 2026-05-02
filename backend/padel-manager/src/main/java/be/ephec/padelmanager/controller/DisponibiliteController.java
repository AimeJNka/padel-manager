package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.GenererCreneauxRequest;
import be.ephec.padelmanager.service.IDisponibiliteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/creneaux")
@RequiredArgsConstructor
public class DisponibiliteController {

    private final IDisponibiliteService disponibiliteService;

    @PostMapping("/generer")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<Map<String, Integer>> generer(@Valid @RequestBody GenererCreneauxRequest req, Authentication authentication) {
        int count = disponibiliteService.genererCreneaux(req.getSiteId(), req.getAnnee(), authentication);
        return ResponseEntity.ok(Map.of("generated", count));
    }

    @PostMapping("/regenerer")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<Map<String, Integer>> regenerer(@Valid @RequestBody GenererCreneauxRequest req, Authentication authentication) {
        int count = disponibiliteService.regenererCreneaux(req.getSiteId(), req.getAnnee(), authentication);
        return ResponseEntity.ok(Map.of("generated", count));
    }
}
