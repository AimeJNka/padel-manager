package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.dto.MembreProfilDTO;
import be.ephec.padelmanager.service.IMembreService;
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<List<MembreDTO>> getAll() {
        return ResponseEntity.ok(membreService.findAll());
    }
}
