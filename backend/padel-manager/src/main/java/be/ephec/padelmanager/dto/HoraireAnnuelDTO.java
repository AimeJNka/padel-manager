package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class HoraireAnnuelDTO {
    private Integer idHoraire;
    private SiteDTO site;

    @NotNull
    private Integer annee;

    @NotNull
    private LocalTime heureOuverture;

    @NotNull
    private LocalTime heureFermeture;
}
