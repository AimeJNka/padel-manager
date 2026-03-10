package be.ephec.padelmanager.service;

import be.ephec.padelmanager.DTO.LoginDTO;
import be.ephec.padelmanager.DTO.RegisterDTO;

import java.util.Map;

public interface IAuthService {
    String login(LoginDTO dto);
    Map<String, String> register(RegisterDTO dto);
}
