package be.ephec.padelmanager.DTO.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequestDTO {
    @NotBlank
    private String refreshToken;
}
