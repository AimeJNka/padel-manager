package be.ephec.padelmanager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MatchPadelDTO {
    private Integer idMatch;
    private DisponibiliteDTO disponibilite;
    private MembreDTO organisateur;
    private String typeMatch;
    private String statut;
    private BigDecimal montantTotal;
    private LocalDateTime dateCreation;
    private List<ParticipationDTO> participations = new ArrayList<>();
}
