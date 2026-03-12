package be.ephec.padelmanager.service;

import be.ephec.padelmanager.DTO.auth.AuthResponseDTO;
import be.ephec.padelmanager.DTO.auth.LoginDTO;
import be.ephec.padelmanager.DTO.auth.RefreshResponseDTO;
import be.ephec.padelmanager.DTO.auth.RegisterDTO;

public interface IAuthService {
    AuthResponseDTO login(LoginDTO dto);
    AuthResponseDTO register(RegisterDTO dto);
    RefreshResponseDTO refreshToken(String refreshToken);
    void logout(String matricule);
}
