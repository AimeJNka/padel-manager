package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.service.MembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/membres")
@RequiredArgsConstructor
public class MembreController {

    private final MembreService membreService;

    @GetMapping
    public ResponseEntity<List<Membre>> getAll() {
        return ResponseEntity.ok(membreService.findAll());
    }
}
