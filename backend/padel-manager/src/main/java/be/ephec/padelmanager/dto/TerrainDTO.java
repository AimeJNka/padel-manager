package be.ephec.padelmanager.dto;

import lombok.Data;

@Data
public class TerrainDTO {
    private Integer idTerrain;
    private SiteDTO site;
    private Integer numero;
    private String statut;
}
