package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.DisponibiliteDTO;
import be.ephec.padelmanager.service.IDisponibiliteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/disponibilites")
@RequiredArgsConstructor
public class DisponibilitesController {

    private final IDisponibiliteService disponibiliteService;

    @GetMapping
    public ResponseEntity<Page<DisponibiliteDTO>> lister(
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) Integer terrainId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(disponibiliteService.listerDisponibilites(
                siteId, terrainId, date, statut,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dateHeureDebut"))));
    }
}
