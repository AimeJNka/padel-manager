package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.MatchPadelDTO;
import org.springframework.security.core.Authentication;

public interface IMatchPadelService {
    MatchPadelDTO creerMatchPrive(Integer dispoId, Authentication auth);
    MatchPadelDTO creerMatchPublic(Integer dispoId, Authentication auth);
    void ajouterJoueur(Integer idMatch, String matriculeJoueur, Authentication auth);
    void sInscrireMatchPublic(Integer idMatch, Authentication auth);
    void annulerMatch(Integer idMatch, Authentication auth);
}
