package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.RefreshTokenDTO;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.RefreshToken;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.RefreshTokenRepo;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.exception.UnauthorizedException;
import be.ephec.padelmanager.service.IRefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService implements IRefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;
    private final MembreRepo membreRepo;
    private final long refreshExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepo refreshTokenRepo,
            MembreRepo membreRepo,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs
    ) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.membreRepo = membreRepo;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    @Transactional
    public RefreshTokenDTO createRefreshToken(String matricule) {
        Membre membre = membreRepo.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Membre introuvable"));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setMembre(membre);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setDateExpiration(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        refreshToken.setRevoque(false);
        refreshToken.setDateCreation(LocalDateTime.now());

        return toDTO(refreshTokenRepo.save(refreshToken));
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public RefreshTokenDTO verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepo.findByTokenAndRevoqueFalse(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token invalide ou révoqué"));

        if (refreshToken.getDateExpiration().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoque(true);
            refreshTokenRepo.save(refreshToken);
            throw new UnauthorizedException("Refresh token expiré");
        }

        return toDTO(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllByMatricule(String matricule) {
        refreshTokenRepo.revokeAllByMatricule(matricule);
    }

    private RefreshTokenDTO toDTO(RefreshToken entity) {
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setIdRefreshToken(entity.getIdRefreshToken());
        dto.setToken(entity.getToken());
        dto.setMatricule(entity.getMembre().getMatricule());
        dto.setDateExpiration(entity.getDateExpiration());
        dto.setRevoque(entity.getRevoque());
        dto.setDateCreation(entity.getDateCreation());
        return dto;
    }
}
