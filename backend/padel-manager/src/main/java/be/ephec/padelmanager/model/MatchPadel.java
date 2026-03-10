package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_padel")
@Data
public class MatchPadel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_match")
    private Integer idMatch;

    @OneToOne
    @JoinColumn(name="id_dispo")
    private Disponibilite disponibilite;

    @ManyToOne
    @JoinColumn(name="matricule_organisateur")
    private Membre organisateur;

    @Column(name= "type_match")
    private String typeMatch;

    @Column(name = "statut")
    private String statut;

    @Column(name = "montant_total")
    private BigDecimal montantTotal;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;
}
