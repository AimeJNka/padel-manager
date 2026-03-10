package be.ephec.padelmanager.DTO;

import lombok.Data;


import java.time.LocalDateTime;

@Data
public class DisponibiliteDTO {
    private Integer idDisponibilite;
    private TerrainDTO terrain;
    private LocalDateTime heureDebut;
    private LocalDateTime heureFin;
    private String statut;
}
