package be.ephec.padelmanager.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MembreDTO {
    private String matricule;
    private PersonneDTO Personne;
    private TypeMembreDTO typeMembre;
    private SiteDTO Site;
    private LocalDate dateInscription;
    private BigDecimal soldeDu;
}
