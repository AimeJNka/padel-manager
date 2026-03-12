package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.RefreshTokenDTO;

public interface IRefreshTokenService {
    RefreshTokenDTO createRefreshToken(String matricule);
    RefreshTokenDTO verifyRefreshToken(String token);
    void revokeAllByMatricule(String matricule);
}
