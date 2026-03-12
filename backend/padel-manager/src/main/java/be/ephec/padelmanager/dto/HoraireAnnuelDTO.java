package be.ephec.padelmanager.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class HoraireAnnuelDTO {
    private Integer idHoraire;
    private SiteDTO site;
    private Integer annee;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;
}
