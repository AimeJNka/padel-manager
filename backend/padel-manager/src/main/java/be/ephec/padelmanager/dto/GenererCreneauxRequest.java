package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenererCreneauxRequest {

    @NotNull
    private Integer siteId;

    @NotNull
    private Integer annee;
}
