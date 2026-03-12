package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "horaire_annuel")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HoraireAnnuel {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horaire")
    private Integer idHoraire;

    @ManyToOne
    @JoinColumn(name="id_site")
    private Site site;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "heure_ouverture")
    private LocalTime heureOuverture;

    @Column(name = "heure_fermeture")
    private LocalTime heureFermeture;
}
