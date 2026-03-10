package be.ephec.padelmanager.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ParticipationDTO {
    private Integer idParticipation;
    private MatchPadelDTO idMatch;
    private MembreDTO matricule;
    private String statut;
    private LocalDateTime dateInscription;
}
