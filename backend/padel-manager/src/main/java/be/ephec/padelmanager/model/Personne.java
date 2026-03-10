package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "personne")
@Data
public class Personne {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personne")
    private Integer idPersonne;

    @Column(name = "email")
    private String email;

    @Column(name = "nom")
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "telephone")
    private String telephone;
}
