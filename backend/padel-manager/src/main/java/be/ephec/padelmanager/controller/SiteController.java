package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.SiteDTO;
import be.ephec.padelmanager.service.ISiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final ISiteService siteService;

    @GetMapping
    public ResponseEntity<List<SiteDTO>> getAll() {
        return ResponseEntity.ok(siteService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<SiteDTO> create(@Valid @RequestBody SiteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.create(dto));
    }
}
