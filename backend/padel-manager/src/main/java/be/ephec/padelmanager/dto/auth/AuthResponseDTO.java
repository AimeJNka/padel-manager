package be.ephec.padelmanager.dto.auth;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String matricule;
    private String role;
}
