package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.DTO.auth.AuthResponseDTO;
import be.ephec.padelmanager.DTO.auth.LoginDTO;
import be.ephec.padelmanager.DTO.auth.RefreshRequestDTO;
import be.ephec.padelmanager.DTO.auth.RefreshResponseDTO;
import be.ephec.padelmanager.DTO.auth.RegisterDTO;
import be.ephec.padelmanager.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterDTO dto) {
        return ResponseEntity.ok(authService.register(dto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        return ResponseEntity.ok(authService.refreshToken(dto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        String matricule = (String) authentication.getPrincipal();
        authService.logout(matricule);
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }
}
