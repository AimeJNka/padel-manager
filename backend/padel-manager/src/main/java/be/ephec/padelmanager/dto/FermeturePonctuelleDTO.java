package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FermeturePonctuelleDTO {
    private Integer idFermeturePonctuelle;
    private SiteDTO site;

    @NotNull
    private LocalDate dateFermeture;

    private String motif;
}
