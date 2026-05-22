package be.ephec.padelmanager.scheduler;

import be.ephec.padelmanager.service.IMatchPadelService;
import be.ephec.padelmanager.service.IParticipationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchSchedulerTest {

    @Mock IParticipationService participationService;
    @Mock IMatchPadelService    matchPadelService;
    @InjectMocks MatchScheduler scheduler;

    @Test
    void traiterMatchesHoraire_executesJobsInCorrectOrder() {
        var order = inOrder(participationService, matchPadelService);

        scheduler.traiterMatchesHoraire();

        order.verify(participationService).libererPlacesNonPayees();
        order.verify(matchPadelService).basculerMatchesIncomplets();
        order.verify(matchPadelService).traiterSoldeMatchesDemarres();
        order.verify(matchPadelService).marquerMatchesEffectues();
    }

    @Test
    void traiterMatchesHoraire_whenJob2Throws_job1StillRuns() {
        doThrow(new RuntimeException("DB timeout")).when(participationService).libererPlacesNonPayees();

        scheduler.traiterMatchesHoraire();

        verify(matchPadelService).basculerMatchesIncomplets();
    }

    @Test
    void traiterMatchesHoraire_whenJob1Throws_doesNotPropagate() {
        doThrow(new RuntimeException("TX failure")).when(matchPadelService).basculerMatchesIncomplets();

        assertThatNoException().isThrownBy(() -> scheduler.traiterMatchesHoraire());
    }

    @Test
    void traiterMatchesHoraire_whenJob1Throws_job3StillRuns() {
        doThrow(new RuntimeException("TX failure")).when(matchPadelService).basculerMatchesIncomplets();

        scheduler.traiterMatchesHoraire();

        verify(matchPadelService).traiterSoldeMatchesDemarres();
    }

    @Test
    void traiterMatchesHoraire_whenJob3Throws_job4StillRuns() {
        doThrow(new RuntimeException("TX failure")).when(matchPadelService).traiterSoldeMatchesDemarres();

        scheduler.traiterMatchesHoraire();

        verify(matchPadelService).marquerMatchesEffectues();
    }

    @Test
    void traiterMatchesHoraire_whenJob4Throws_doesNotPropagate() {
        doThrow(new RuntimeException("DB error")).when(matchPadelService).marquerMatchesEffectues();

        assertThatNoException().isThrownBy(() -> scheduler.traiterMatchesHoraire());
    }
}
