package be.ephec.padelmanager.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String matricule;

    @NotBlank
    private String motDePasse;
}
