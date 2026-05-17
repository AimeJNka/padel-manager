package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.auth.AdminAuthResponseDTO;
import be.ephec.padelmanager.dto.auth.AdminLoginDTO;
import be.ephec.padelmanager.dto.auth.AuthResponseDTO;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.auth.RefreshRequestDTO;
import be.ephec.padelmanager.dto.auth.RefreshResponseDTO;
import be.ephec.padelmanager.dto.auth.RegisterRequest;
import be.ephec.padelmanager.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequest dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AdminAuthResponseDTO> adminLogin(@Valid @RequestBody AdminLoginDTO dto) {
        return ResponseEntity.ok(authService.adminLogin(dto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        return ResponseEntity.ok(authService.refreshToken(dto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_ADMIN"));
        if (!isAdmin) {
            String matricule = (String) authentication.getPrincipal();
            authService.logout(matricule);
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }
}
