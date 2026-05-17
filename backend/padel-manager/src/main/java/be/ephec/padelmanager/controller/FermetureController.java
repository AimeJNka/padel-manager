package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.FermeturePonctuelleDTO;
import be.ephec.padelmanager.dto.FermetureRecurrenteDTO;
import be.ephec.padelmanager.service.IFermeturePonctuelleService;
import be.ephec.padelmanager.service.IFermetureRecurrenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites/{idSite}/fermetures")
@RequiredArgsConstructor
public class FermetureController {

    private final IFermetureRecurrenteService fermetureRecurrenteService;
    private final IFermeturePonctuelleService fermeturePonctuelleService;

    @GetMapping("/recurrentes")
    public ResponseEntity<List<FermetureRecurrenteDTO>> getRecurrentes(@PathVariable Integer idSite) {
        return ResponseEntity.ok(fermetureRecurrenteService.findBySite(idSite));
    }

    @PostMapping("/recurrentes")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<FermetureRecurrenteDTO> createRecurrente(
            @PathVariable Integer idSite,
            @Valid @RequestBody FermetureRecurrenteDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fermetureRecurrenteService.create(idSite, dto, authentication));
    }

    @GetMapping("/ponctuelles")
    public ResponseEntity<List<FermeturePonctuelleDTO>> getPonctuelles(@PathVariable Integer idSite) {
        return ResponseEntity.ok(fermeturePonctuelleService.findBySite(idSite));
    }

    @PostMapping("/ponctuelles")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<FermeturePonctuelleDTO> createPonctuelle(
            @PathVariable Integer idSite,
            @Valid @RequestBody FermeturePonctuelleDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fermeturePonctuelleService.create(idSite, dto, authentication));
    }
}
