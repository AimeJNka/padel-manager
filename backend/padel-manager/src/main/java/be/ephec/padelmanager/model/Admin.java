package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "administrateur")
@Data
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_admin")
    private Integer idAdmin;

    @ManyToOne
    @JoinColumn(name="id_site", nullable = true)
    private Site site;

    @Column(name = "email")
    private String email;

    @Column(name = "nom")
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "mot_de_passe")
    private String motDePasse;

    @Column(name = "role")
    private String role;

}
