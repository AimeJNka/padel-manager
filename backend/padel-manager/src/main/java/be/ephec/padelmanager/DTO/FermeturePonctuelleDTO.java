package be.ephec.padelmanager.DTO;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FermeturePonctuelleDTO {
    private Integer idFermeturePonctuel;
    private SiteDTO site;
    private LocalDate dateFermeture;
    private String motif;
}
