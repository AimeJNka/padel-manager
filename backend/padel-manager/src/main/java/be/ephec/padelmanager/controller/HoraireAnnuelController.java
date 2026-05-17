package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.HoraireAnnuelDTO;
import be.ephec.padelmanager.service.IHoraireAnnuelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites/{idSite}/horaires")
@RequiredArgsConstructor
public class HoraireAnnuelController {

    private final IHoraireAnnuelService horaireService;

    @GetMapping
    public ResponseEntity<List<HoraireAnnuelDTO>> getBySite(@PathVariable Integer idSite) {
        return ResponseEntity.ok(horaireService.findBySite(idSite));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<HoraireAnnuelDTO> create(@PathVariable Integer idSite, @Valid @RequestBody HoraireAnnuelDTO dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horaireService.create(idSite, dto, authentication));
    }
}
