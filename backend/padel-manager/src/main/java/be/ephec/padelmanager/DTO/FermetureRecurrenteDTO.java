package be.ephec.padelmanager.DTO;

import lombok.Data;

@Data
public class FermetureRecurrenteDTO {
    private Integer idFermetureRecurrente;
    private SiteDTO site;
    private Integer jourSemaine;
    private String motif;
}
