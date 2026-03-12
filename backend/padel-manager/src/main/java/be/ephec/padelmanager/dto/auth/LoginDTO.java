package be.ephec.padelmanager.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank
    private String matricule;

    @NotBlank
    private String motDePasse;
}
