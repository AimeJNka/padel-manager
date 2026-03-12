package be.ephec.padelmanager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
public class PaiementDTO {

    private Integer idPaiement;
    private ParticipationDTO idParticipation;
    private BigDecimal montant;
    private BigDecimal soldeInclus;
    private LocalDateTime datePaiement;
    private String statut;
}
