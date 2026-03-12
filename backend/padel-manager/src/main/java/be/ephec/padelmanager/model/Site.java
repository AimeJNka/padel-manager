package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "site")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Site {
    @EqualsAndHashCode.Include
    @Id
    @Column(name = "id_site")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSite;

    @Column(name = "nom")
    private String nom;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "ville")
    private String ville;

    @Column(name = "actif")
    private Boolean actif;
}
