package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.auth.AdminAuthResponseDTO;
import be.ephec.padelmanager.dto.auth.AdminLoginDTO;
import be.ephec.padelmanager.dto.auth.AuthResponseDTO;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.auth.RefreshResponseDTO;
import be.ephec.padelmanager.dto.auth.RegisterRequest;

public interface IAuthService {
    AuthResponseDTO login(LoginRequest dto);
    AuthResponseDTO register(RegisterRequest dto);
    AdminAuthResponseDTO adminLogin(AdminLoginDTO dto);
    RefreshResponseDTO refreshToken(String refreshToken);
    void logout(String matricule);
}
