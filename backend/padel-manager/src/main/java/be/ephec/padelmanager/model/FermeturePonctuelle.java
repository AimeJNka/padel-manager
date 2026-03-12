package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "fermeture_ponctuelle")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FermeturePonctuelle {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fermeture_ponc")
    private Integer idFermeturePonctuelle;

    @ManyToOne
    @JoinColumn(name="id_site")
    private Site site;

    @Column(name = "date_fermeture")
    private LocalDate dateFermeture;

    @Column(name = "motif")
    private String motif;
}
