package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;


@Entity
@Table(name = "participation")
@Data
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participation")
    private Integer idParticipation;

    @ManyToOne
    @JoinColumn(name="id_match")
    private MatchPadel matchPadel;

    @ManyToOne
    @JoinColumn(name="matricule")
    private Membre matricule;

    @Column(name = "statut")
    private String statut;

    @Column(name = "date_inscription")
    private LocalDateTime dateInscription;
}
