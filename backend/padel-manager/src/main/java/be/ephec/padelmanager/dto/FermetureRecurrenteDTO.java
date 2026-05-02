package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FermetureRecurrenteDTO {
    private Integer idFermetureRecurrente;
    private SiteDTO site;

    @NotNull
    @Min(0)
    @Max(6)
    private Integer jourSemaine;

    private String motif;
}
