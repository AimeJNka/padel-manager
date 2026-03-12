package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.auth.AuthResponseDTO;
import be.ephec.padelmanager.dto.auth.LoginDTO;
import be.ephec.padelmanager.dto.auth.RefreshResponseDTO;
import be.ephec.padelmanager.dto.auth.RegisterDTO;

public interface IAuthService {
    AuthResponseDTO login(LoginDTO dto);
    AuthResponseDTO register(RegisterDTO dto);
    RefreshResponseDTO refreshToken(String refreshToken);
    void logout(String matricule);
}
