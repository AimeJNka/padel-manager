package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "terrain")
@Data
public class Terrain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_terrain")
    private Integer idTerrain;

    @ManyToOne
    @JoinColumn(name="id_site")
    private Site site;

    @Column(name = "numero")
    private Integer numero;

    @Column(name = "statut")
    private String statut;
}
