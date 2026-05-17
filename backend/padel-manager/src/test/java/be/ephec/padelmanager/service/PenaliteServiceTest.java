package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.PenaliteDTO;
import be.ephec.padelmanager.exception.ConflictException;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Penalite;
import be.ephec.padelmanager.model.Personne;
import be.ephec.padelmanager.repository.PenaliteRepo;
import be.ephec.padelmanager.service.impl.PenaliteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PenaliteServiceTest {

    @Mock PenaliteRepo penaliteRepo;
    @Mock SiteAccessChecker siteAccessChecker;

    PenaliteService service;

    private static final Integer PENALITE_ID = 99;

    @BeforeEach
    void setUp() {
        service = new PenaliteService(penaliteRepo, siteAccessChecker);
    }

    private Penalite buildPenalite(LocalDateTime dateFin) {
        Personne personne = new Personne();
        personne.setNom("Martin");
        personne.setPrenom("Sophie");

        Membre membre = new Membre();
        membre.setMatricule("G0042");
        membre.setPersonne(personne);

        Penalite pen = new Penalite();
        pen.setIdPenalite(PENALITE_ID);
        pen.setMembre(membre);
        pen.setDateDebut(LocalDateTime.now().minusDays(7));
        pen.setDateFin(dateFin);
        pen.setMotif("Test motif");
        return pen;
    }

    private Authentication authGlobal() {
        return new TestingAuthenticationToken("admin", null, "ROLE_ADMIN_GLOBAL");
    }

    // ── annulerPenalite ──────────────────────────────

    @Test
    void annulerPenalite_active_setsDateFinToNow() {
        Penalite pen = buildPenalite(LocalDateTime.now().plusDays(7));
        when(penaliteRepo.findById(PENALITE_ID)).thenReturn(Optional.of(pen));

        PenaliteDTO dto = service.annulerPenalite(PENALITE_ID, authGlobal());

        assertThat(pen.getDateFin())
                .isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
        assertThat(dto.active()).isFalse();
        verify(penaliteRepo, times(1)).save(pen);
    }

    @Test
    void annulerPenalite_notFound_throwsNotFoundException() {
        when(penaliteRepo.findById(PENALITE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.annulerPenalite(PENALITE_ID, authGlobal()))
                .isInstanceOf(NotFoundException.class);

        verify(penaliteRepo, never()).save(any());
    }

    @Test
    void annulerPenalite_alreadyExpired_throwsConflictException() {
        Penalite pen = buildPenalite(LocalDateTime.now().minusDays(1));
        when(penaliteRepo.findById(PENALITE_ID)).thenReturn(Optional.of(pen));

        assertThatThrownBy(() -> service.annulerPenalite(PENALITE_ID, authGlobal()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("expirée");

        verify(penaliteRepo, never()).save(any());
    }

    @Test
    void annulerPenalite_siteScope_throwsForbiddenException() {
        Penalite pen = buildPenalite(LocalDateTime.now().plusDays(7));
        when(penaliteRepo.findById(PENALITE_ID)).thenReturn(Optional.of(pen));
        doThrow(new ForbiddenException("Accès refusé"))
                .when(siteAccessChecker).check(any(), any());

        assertThatThrownBy(() -> service.annulerPenalite(PENALITE_ID, authGlobal()))
                .isInstanceOf(ForbiddenException.class);

        verify(penaliteRepo, never()).save(any());
    }

    // ── appliquerPenalite ────────────────────────────

    @Test
    void appliquerPenalite_savesWithCorrectDateFin() {
        Membre membre = new Membre();
        membre.setMatricule("G0042");

        service.appliquerPenalite(membre, 7, "TEST");

        ArgumentCaptor<Penalite> captor = ArgumentCaptor.forClass(Penalite.class);
        verify(penaliteRepo, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDateFin())
                .isCloseTo(LocalDateTime.now().plusDays(7), within(2, ChronoUnit.SECONDS));
    }

    @Test
    void appliquerPenalite_savesWithCorrectMotif() {
        Membre membre = new Membre();
        membre.setMatricule("G0042");

        service.appliquerPenalite(membre, 7, "Match privé #5 incomplet — UC-03");

        ArgumentCaptor<Penalite> captor = ArgumentCaptor.forClass(Penalite.class);
        verify(penaliteRepo, times(1)).save(captor.capture());
        assertThat(captor.getValue().getMotif()).isEqualTo("Match privé #5 incomplet — UC-03");
    }

    @Test
    void appliquerPenalite_savesWithCorrectMembre() {
        Membre membre = new Membre();
        membre.setMatricule("G0099");

        service.appliquerPenalite(membre, 3, "MOTIF");

        ArgumentCaptor<Penalite> captor = ArgumentCaptor.forClass(Penalite.class);
        verify(penaliteRepo, times(1)).save(captor.capture());
        assertThat(captor.getValue().getMembre().getMatricule()).isEqualTo("G0099");
    }
}
