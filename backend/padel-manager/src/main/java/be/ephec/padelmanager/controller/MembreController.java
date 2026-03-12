package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.service.IMembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/membres")
@RequiredArgsConstructor
public class MembreController {

    private final IMembreService membreService;

//    @GetMapping
//    public ResponseEntity<List<MembreDTO>> getAll() {
//        return ResponseEntity.ok(membreService.findAll());
//    }
}
