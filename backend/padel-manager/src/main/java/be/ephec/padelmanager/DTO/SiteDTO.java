package be.ephec.padelmanager.DTO;

import lombok.Data;

@Data
public class SiteDTO {
    private Integer idSite;
    private String nom;
    private String adresse;
    private String ville;
    private Boolean actif;
}
