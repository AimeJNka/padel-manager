package be.ephec.padelmanager.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FermeturePonctuelleDTO {
    private Integer idFermeturePonctuelle;
    private SiteDTO site;
    private LocalDate dateFermeture;
    private String motif;
}
