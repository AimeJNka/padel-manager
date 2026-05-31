package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.dto.MembreProfilDTO;
import be.ephec.padelmanager.dto.MembreSearchDTO;
import be.ephec.padelmanager.dto.UpdateMembreRequest;
import be.ephec.padelmanager.service.IMembreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membres")
@RequiredArgsConstructor
public class MembreController {

    private final IMembreService membreService;

    @GetMapping("/me")
    public ResponseEntity<MembreProfilDTO> getProfil(Authentication authentication) {
        String matricule = (String) authentication.getPrincipal();
        return ResponseEntity.ok(membreService.getProfil(matricule));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('GLOBAL', 'SITE')")
    public ResponseEntity<List<MembreSearchDTO>> search(
            @RequestParam("q") String q,
            @RequestParam(value = "siteId", required = false) Integer siteId,
            Authentication auth) {
        return ResponseEntity.ok(membreService.search(q, siteId, auth));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<List<MembreDTO>> getAll(Authentication authentication) {
        return ResponseEntity.ok(membreService.findAll(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<MembreProfilDTO> getOne(@PathVariable String id) {
        return ResponseEntity.ok(membreService.getOne(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MembreProfilDTO> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateMembreRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(membreService.updateMembre(id, request, authentication));
    }
}
