package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.TerrainDTO;
import be.ephec.padelmanager.service.ITerrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites/{idSite}/terrains")
@RequiredArgsConstructor
public class TerrainController {

    private final ITerrainService terrainService;

    @GetMapping
    public ResponseEntity<List<TerrainDTO>> getBySite(@PathVariable Integer idSite) {
        return ResponseEntity.ok(terrainService.findBySite(idSite));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<TerrainDTO> create(
            @PathVariable Integer idSite,
            @Valid @RequestBody TerrainDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(terrainService.create(idSite, dto, authentication));
    }

    @PutMapping("/{idTerrain}")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<TerrainDTO> update(
            @PathVariable Integer idTerrain,
            @Valid @RequestBody TerrainDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(terrainService.update(idTerrain, dto, authentication));
    }

    @DeleteMapping("/{idTerrain}")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer idSite,
            @PathVariable Integer idTerrain,
            Authentication authentication) {
        terrainService.delete(idSite, idTerrain, authentication);
        return ResponseEntity.noContent().build();
    }
}
