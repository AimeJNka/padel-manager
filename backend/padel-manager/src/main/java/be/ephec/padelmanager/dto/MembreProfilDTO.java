package be.ephec.padelmanager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MembreProfilDTO {
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String typeMembre;
    private String siteNom;
    private LocalDate dateInscription;
    private BigDecimal soldeDu;
}
