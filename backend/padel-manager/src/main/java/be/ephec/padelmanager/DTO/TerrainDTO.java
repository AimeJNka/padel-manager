package be.ephec.padelmanager.DTO;

import lombok.Data;

@Data
public class TerrainDTO {
    private Integer idTerrain;
    private SiteDTO site;
    private Integer numero;
    private String statut;
}
