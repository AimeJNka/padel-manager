package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Partial update semantics: null fields are left unchanged. */
@Data
public class UpdateMembreRequest {
    @Email
    @Size(max = 150)
    private String email;

    @Pattern(regexp = "^[+0-9 .\\-]{0,20}$", message = "Format de téléphone invalide")
    private String telephone;
}
