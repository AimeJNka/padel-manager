package be.ephec.padelmanager.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String motDePasse;
}
