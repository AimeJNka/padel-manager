package be.ephec.padelmanager.dto;

import lombok.Data;

/** Lightweight member projection for invitation picker — excludes all sensitive fields. */
@Data
public class MembreSearchDTO {
    private String matricule;
    private String prenom;
    private String nom;
    private String siteNom;  // null for LIBRE members with no site affiliation
}
