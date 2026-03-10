package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;


@Entity
@Table(name = "disponibilite")
@Data
public class Disponibilite {

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
