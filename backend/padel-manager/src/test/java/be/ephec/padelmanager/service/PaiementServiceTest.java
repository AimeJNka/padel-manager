package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.PaiementDTO;
import be.ephec.padelmanager.exception.ConflictException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.model.MatchPadel;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Paiement;
import be.ephec.padelmanager.model.Participation;
import be.ephec.padelmanager.model.Personne;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PaiementRepo;
import be.ephec.padelmanager.repository.ParticipationRepo;
import be.ephec.padelmanager.service.impl.PaiementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    @Mock PaiementRepo paiementRepo;
    @Mock MembreRepo membreRepo;
    @Mock ParticipationRepo participationRepo;
    @Mock SiteAccessChecker siteAccessChecker;

    PaiementService service;

    private static final String OWNER = "G0001";
    private static final Integer PAIEMENT_ID = 42;

    @BeforeEach
    void setUp() {
        service = new PaiementService(paiementRepo, membreRepo, participationRepo, siteAccessChecker);
    }

    private Paiement buildPaiement(String statut, BigDecimal soldeDuMembre, String ownerMatricule) {
        Personne personne = new Personne();
        personne.setNom("Dupont");
        personne.setPrenom("Jean");

        Membre membre = new Membre();
        membre.setMatricule(ownerMatricule);
        membre.setPersonne(personne);
        membre.setSoldeDu(soldeDuMembre);

        // Site graph required by checkPaiementSiteAccess in rembourserPaiement
        be.ephec.padelmanager.model.Site site = new be.ephec.padelmanager.model.Site();
        site.setIdSite(1);
        be.ephec.padelmanager.model.Terrain terrain = new be.ephec.padelmanager.model.Terrain();
        terrain.setIdTerrain(1);
        terrain.setSite(site);
        be.ephec.padelmanager.model.Disponibilite dispo = new be.ephec.padelmanager.model.Disponibilite();
        dispo.setIdDispo(10);
        dispo.setTerrain(terrain);

        MatchPadel match = new MatchPadel();
        match.setIdMatch(1);
        match.setMontantTotal(new BigDecimal("60.00"));
        match.setDisponibilite(dispo);

        Participation participation = new Participation();
        participation.setIdParticipation(7);
        participation.setMembre(membre);
        participation.setMatchPadel(match);
        participation.setStatut("EN_ATTENTE");

        Paiement paiement = new Paiement();
        paiement.setIdPaiement(PAIEMENT_ID);
        paiement.setParticipation(participation);
        paiement.setMontant(new BigDecimal("15.00"));
        paiement.setSoldeInclus(BigDecimal.ZERO);
        paiement.setStatut(statut);
        paiement.setDatePaiement(LocalDateTime.now());
        return paiement;
    }

    private Authentication authAs(String matricule) {
        return new TestingAuthenticationToken(matricule, null, "ROLE_GLOBAL");
    }

    // ── payerParMembre ──────────────────────────────

    @Test
    void payerParMembre_success_soldeDuZero() {
        Paiement paiement = buildPaiement("EN_ATTENTE", BigDecimal.ZERO, OWNER);
        when(paiementRepo.findByIdForUpdate(PAIEMENT_ID)).thenReturn(Optional.of(paiement));

        PaiementDTO dto = service.payerParMembre(PAIEMENT_ID, authAs(OWNER));

        assertThat(paiement.getStatut()).isEqualTo("PAYE");
        assertThat(paiement.getParticipation().getStatut()).isEqualTo("CONFIRME");
        assertThat(paiement.getSoldeInclus()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.statut()).isEqualTo("PAYE");
        verify(membreRepo, never()).save(any());
        verify(paiementRepo, times(1)).save(paiement);
        verify(participationRepo, times(1)).save(paiement.getParticipation());
    }

    @Test
    void payerParMembre_success_soldeDuPositive() {
        Paiement paiement = buildPaiement("EN_ATTENTE", new BigDecimal("20.00"), OWNER);
        when(paiementRepo.findByIdForUpdate(PAIEMENT_ID)).thenReturn(Optional.of(paiement));

        service.payerParMembre(PAIEMENT_ID, authAs(OWNER));

        assertThat(paiement.getStatut()).isEqualTo("PAYE");
        assertThat(paiement.getSoldeInclus()).isEqualByComparingTo(new BigDecimal("20.00"));
        Membre membre = paiement.getParticipation().getMembre();
        assertThat(membre.getSoldeDu()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(membreRepo, times(1)).save(membre);
    }

    @Test
    void payerParMembre_wrongOwner() {
        Paiement paiement = buildPaiement("EN_ATTENTE", BigDecimal.ZERO, OWNER);
        when(paiementRepo.findByIdForUpdate(PAIEMENT_ID)).thenReturn(Optional.of(paiement));

        assertThatThrownBy(() -> service.payerParMembre(PAIEMENT_ID, authAs("G9999")))
                .isInstanceOf(ForbiddenException.class);

        verify(paiementRepo, never()).save(any());
        verify(membreRepo, never()).save(any());
    }

    @Test
    void payerParMembre_alreadyPaid() {
        Paiement paiement = buildPaiement("PAYE", BigDecimal.ZERO, OWNER);
        when(paiementRepo.findByIdForUpdate(PAIEMENT_ID)).thenReturn(Optional.of(paiement));

        assertThatThrownBy(() -> service.payerParMembre(PAIEMENT_ID, authAs(OWNER)))
                .isInstanceOf(ConflictException.class);

        verify(paiementRepo, never()).save(any());
        verify(membreRepo, never()).save(any());
    }

    // ── rembourserPaiement ──────────────────────────

    @Test
    void rembourserPaiement_success() {
        Paiement paiement = buildPaiement("PAYE", BigDecimal.ZERO, OWNER);
        when(paiementRepo.findById(PAIEMENT_ID)).thenReturn(Optional.of(paiement));

        Authentication adminGlobal =
                new TestingAuthenticationToken("admin", null, "ROLE_ADMIN_GLOBAL");

        PaiementDTO dto = service.rembourserPaiement(PAIEMENT_ID, adminGlobal);

        assertThat(paiement.getStatut()).isEqualTo("REMBOURSE");
        assertThat(dto.statut()).isEqualTo("REMBOURSE");
        verify(paiementRepo, times(1)).save(paiement);
    }

    @Test
    void rembourserPaiement_notPaid() {
        Paiement paiement = buildPaiement("EN_ATTENTE", BigDecimal.ZERO, OWNER);
        when(paiementRepo.findById(PAIEMENT_ID)).thenReturn(Optional.of(paiement));

        Authentication adminGlobal =
                new TestingAuthenticationToken("admin", null, "ROLE_ADMIN_GLOBAL");

        assertThatThrownBy(() -> service.rembourserPaiement(PAIEMENT_ID, adminGlobal))
                .isInstanceOf(ConflictException.class);

        verify(paiementRepo, never()).save(any());
    }
}
