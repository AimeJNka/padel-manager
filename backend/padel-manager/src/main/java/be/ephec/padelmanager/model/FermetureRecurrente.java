package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fermeture_recurrente")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FermetureRecurrente {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fermeture_rec")
    private Integer idFermetureRecurrente;

    @ManyToOne
    @JoinColumn(name="id_site")
    private Site site;

    @Column(name = "jour_semaine")
    private Integer jourSemaine;

    @Column(name = "motif")
    private String motif;
}
