package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "membre")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Membre {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "matricule")
    private String matricule;

    @ManyToOne
    @JoinColumn(name = "id_personne")
    private Personne personne;

    @ManyToOne
    @JoinColumn(name="id_type")
    private TypeMembre typeMembre;

    @ManyToOne
    @JoinColumn(name="id_site")
    private Site site;

    @Column(name = "mot_de_passe")
    private String motDePasse;

    @Column(name = "date_inscription")
    private LocalDate dateInscription;

    @Column(name = "solde_du")
    private BigDecimal soldeDu;


}