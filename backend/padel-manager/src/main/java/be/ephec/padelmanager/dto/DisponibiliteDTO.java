package be.ephec.padelmanager.dto;

import lombok.Data;


import java.time.LocalDateTime;

@Data
public class DisponibiliteDTO {
    private Integer idDispo;
    private TerrainDTO terrain;
    private LocalDateTime dateHeureDebut;
    private LocalDateTime dateHeureFin;
    private String statut;
}
