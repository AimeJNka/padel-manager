package be.ephec.padelmanager.service;

import org.springframework.security.core.Authentication;

public interface IParticipationService {

    void annulerParticipation(Integer idMatch, Authentication auth);
}
