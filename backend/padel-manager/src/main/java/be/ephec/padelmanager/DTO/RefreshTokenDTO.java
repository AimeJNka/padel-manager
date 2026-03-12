package be.ephec.padelmanager.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RefreshTokenDTO {
    private Long idRefreshToken;
    private String token;
    private String matricule;
    private LocalDateTime dateExpiration;
    private Boolean revoque;
    private LocalDateTime dateCreation;
}
