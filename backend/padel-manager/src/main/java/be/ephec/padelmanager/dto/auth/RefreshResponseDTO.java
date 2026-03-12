package be.ephec.padelmanager.dto.auth;

import lombok.Data;

@Data
public class RefreshResponseDTO {
    private String accessToken;
    private String role;
}
