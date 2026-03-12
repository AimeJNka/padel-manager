package be.ephec.padelmanager.DTO.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank
    private String matricule;

    @NotBlank
    private String motDePasse;
}
