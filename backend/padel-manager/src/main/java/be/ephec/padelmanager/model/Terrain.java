package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "terrain")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Terrain {

    @EqualsAndHashCode.Include
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
