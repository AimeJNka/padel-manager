package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
@Data
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiement")
    private Integer idPaiement;

    @OneToOne
    @JoinColumn(name="id_participation")
    private Participation participation;

    @Column(name = "montant")
    private BigDecimal montant;

    @Column(name = "solde_inclus")
    private BigDecimal soldeInclus;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "statut")
    private String statut;
}
