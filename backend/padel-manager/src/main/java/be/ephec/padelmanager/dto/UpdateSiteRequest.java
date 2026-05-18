package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSiteRequest {
    @NotBlank @Size(max = 100)
    private String nom;

    @NotBlank @Size(max = 200)
    private String adresse;

    @NotBlank @Size(max = 100)
    private String ville;

    private Boolean actif;
}
