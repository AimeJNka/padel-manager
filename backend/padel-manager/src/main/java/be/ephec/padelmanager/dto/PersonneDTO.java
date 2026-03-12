package be.ephec.padelmanager.dto;

import lombok.Data;

@Data
public class PersonneDTO {
    private Integer idPersonne;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
}
