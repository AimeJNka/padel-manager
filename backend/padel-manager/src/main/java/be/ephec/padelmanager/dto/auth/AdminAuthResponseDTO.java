package be.ephec.padelmanager.dto.auth;

import lombok.Data;

@Data
public class AdminAuthResponseDTO {
    private String accessToken;
    private Integer idAdmin;
    private String role;
}
