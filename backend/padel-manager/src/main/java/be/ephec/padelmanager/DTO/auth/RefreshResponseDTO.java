package be.ephec.padelmanager.DTO.auth;

import lombok.Data;

@Data
public class RefreshResponseDTO {
    private String accessToken;
    private String role;
}
