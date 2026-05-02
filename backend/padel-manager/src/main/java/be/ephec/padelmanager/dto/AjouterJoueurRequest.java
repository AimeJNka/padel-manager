package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AjouterJoueurRequest {
    @NotBlank
    private String matricule;
}
