package be.ephec.padelmanager.dto;

import lombok.Data;

@Data
public class AdminDTO {
    private Integer idAdmin;
    private SiteDTO site;
    private String email;
    private String nom;
    private String prenom;
    private String role;
}
