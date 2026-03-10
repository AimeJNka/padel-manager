package be.ephec.padelmanager.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MatchPadelDTO {
    private Integer idMatch;
    private DisponibiliteDTO disponibilite;
    private MembreDTO membre;
    private String typeMatch;
    private String statut;
    private BigDecimal montantTotal;
    private LocalDateTime dateCreation;
}
