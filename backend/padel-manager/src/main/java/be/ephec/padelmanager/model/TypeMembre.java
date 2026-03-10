package be.ephec.padelmanager.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "type_membre")
@Data
public class TypeMembre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type")
    private Integer idType;

    @Column(name = "prefixe", columnDefinition = "bpchar")
    private String prefixe;

    @Column(name = "libelle")
    private String libelle;

    @Column(name = "delai_reservation_jours")
    private Integer delaiReservationJours;

    @Column(name = "peut_creer_match")
    private Boolean peutCreerMatch;
}
