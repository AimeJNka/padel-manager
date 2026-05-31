package be.ephec.padelmanager.dto;

import lombok.Data;

import java.math.BigDecimal;

/** Lightweight participant view for match details — payment status enables UI badges. */
@Data
public class ParticipationDTO {
    private Integer    idParticipation;
    private String     matricule;
    private String     prenom;
    private String     nom;
    private String     statutParticipation;  // EN_ATTENTE | CONFIRME | ANNULEE
    private String     statutPaiement;       // EN_ATTENTE | PAYE | ANNULE — null if no paiement record
    private BigDecimal montantPaiement;
}
