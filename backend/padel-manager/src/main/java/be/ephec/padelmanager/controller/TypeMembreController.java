package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.TypeMembreDTO;
import be.ephec.padelmanager.service.ITypeMembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/types-membres")
@RequiredArgsConstructor
public class TypeMembreController {

    private final ITypeMembreService typeMembreService;

    @GetMapping
    public ResponseEntity<List<TypeMembreDTO>> getAll() {
        return ResponseEntity.ok(typeMembreService.findAll());
    }
}
