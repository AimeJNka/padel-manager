package be.ephec.padelmanager.DTO;

import lombok.Data;

@Data
public class TypeMembreDTO {
    private Integer idType;
    private String prefixe;
    private String libelle;
    private Integer delaiReservationJours;
    private Boolean peutCreerMatch;
}
