package be.ephec.padelmanager.dto;

import lombok.Data;

@Data
public class FermetureRecurrenteDTO {
    private Integer idFermetureRecurrente;
    private SiteDTO site;
    private Integer jourSemaine;
    private String motif;
}
