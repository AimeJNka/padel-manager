package be.ephec.padelmanager.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;


@Entity
@Table(name = "participation")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Participation {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participation")
    private Integer idParticipation;

    @ManyToOne
    @JoinColumn(name="id_match")
    private MatchPadel matchPadel;

    @ManyToOne
    @JoinColumn(name="matricule")
    private Membre membre;

    @Column(name = "statut")
    private String statut;

    @Column(name = "date_inscription")
    private LocalDateTime dateInscription;

    @OneToOne(mappedBy = "participation", fetch = FetchType.LAZY)
    private Paiement paiement;
}
