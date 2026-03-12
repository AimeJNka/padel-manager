package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;


@Entity
@Table(name = "disponibilite")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Disponibilite {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispo")
    private Integer idDispo;

    @ManyToOne
    @JoinColumn(name="id_terrain")
    private Terrain terrain;

    @Column(name = "date_heure_debut")
    private LocalDateTime dateHeureDebut;

    @Column(name = "date_heure_fin")
    private LocalDateTime dateHeureFin;

    @Column(name = "statut")
    private String statut;
}
