package be.ephec.padelmanager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MembreDTO {
    private String matricule;
    private PersonneDTO personne;
    private TypeMembreDTO typeMembre;
    private SiteDTO site;
    private LocalDate dateInscription;
    private BigDecimal soldeDu;
}
