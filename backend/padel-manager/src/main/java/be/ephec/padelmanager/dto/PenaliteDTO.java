package be.ephec.padelmanager.dto;

import lombok.Data;


import java.time.LocalDateTime;

@Data
public class PenaliteDTO {
    private Integer idPenalite;
    private MembreDTO matricule;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String motif;
}
