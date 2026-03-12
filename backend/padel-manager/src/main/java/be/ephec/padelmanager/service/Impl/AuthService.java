package be.ephec.padelmanager.service.Impl;

import be.ephec.padelmanager.DTO.auth.AuthResponseDTO;
import be.ephec.padelmanager.DTO.auth.LoginDTO;
import be.ephec.padelmanager.DTO.auth.RefreshResponseDTO;
import be.ephec.padelmanager.DTO.RefreshTokenDTO;
import be.ephec.padelmanager.DTO.auth.RegisterDTO;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Personne;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.model.TypeMembre;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PersonneRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.repository.TypeMembreRepo;
import be.ephec.padelmanager.service.IAuthService;
import be.ephec.padelmanager.service.IJwtService;
import be.ephec.padelmanager.service.IRefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final MembreRepo membreRepo;
    private final PersonneRepo personneRepo;
    private final SiteRepo siteRepo;
    private final TypeMembreRepo typeMembreRepo;
    private final IJwtService jwtService;
    private final IRefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponseDTO login(LoginDTO dto) {
        Membre membre = membreRepo.findById(dto.getMatricule())
                .orElseThrow(() -> new RuntimeException("Matricule ou mot de passe incorrect"));

        if (!passwordEncoder.matches(dto.getMotDePasse(), membre.getMotDePasse())) {
            throw new RuntimeException("Matricule ou mot de passe incorrect");
        }

        refreshTokenService.revokeAllByMatricule(membre.getMatricule());

        String role = membre.getTypeMembre().getLibelle();
        String accessToken = jwtService.generateToken(membre.getMatricule(), role);
        RefreshTokenDTO refreshToken = refreshTokenService.createRefreshToken(membre.getMatricule());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken());
        response.setMatricule(membre.getMatricule());
        response.setRole(role);
        return response;
    }

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterDTO dto) {
        TypeMembre typeMembre = typeMembreRepo.findById(dto.getIdType())
                .orElseThrow(() -> new RuntimeException("Type de membre introuvable"));

        String matricule = generateMatricule(typeMembre.getPrefixe());

        Personne personne = new Personne();
        personne.setNom(dto.getNom());
        personne.setPrenom(dto.getPrenom());
        personne.setEmail(dto.getEmail());
        personne.setTelephone(dto.getTelephone());
        personne = personneRepo.save(personne);

        Site site = null;
        if (dto.getIdSite() != null) {
            site = siteRepo.findById(dto.getIdSite())
                    .orElseThrow(() -> new RuntimeException("Site introuvable"));
        }

        Membre membre = new Membre();
        membre.setMatricule(matricule);
        membre.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        membre.setPersonne(personne);
        membre.setSite(site);
        membre.setTypeMembre(typeMembre);
        membre.setDateInscription(LocalDate.now());
        membre.setSoldeDu(BigDecimal.ZERO);
        membreRepo.save(membre);

        String role = typeMembre.getLibelle();
        String accessToken = jwtService.generateToken(matricule, role);
        RefreshTokenDTO refreshToken = refreshTokenService.createRefreshToken(matricule);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken());
        response.setMatricule(matricule);
        response.setRole(role);
        return response;
    }

    @Override
    @Transactional
    public RefreshResponseDTO refreshToken(String token) {
        RefreshTokenDTO refreshToken = refreshTokenService.verifyRefreshToken(token);

        Membre membre = membreRepo.findById(refreshToken.getMatricule())
                .orElseThrow(() -> new RuntimeException("Membre introuvable"));

        String role = membre.getTypeMembre().getLibelle();
        String accessToken = jwtService.generateToken(membre.getMatricule(), role);

        RefreshResponseDTO response = new RefreshResponseDTO();
        response.setAccessToken(accessToken);
        response.setRole(role);
        return response;
    }

    @Override
    @Transactional
    public void logout(String matricule) {
        refreshTokenService.revokeAllByMatricule(matricule);
    }

    private String generateMatricule(String prefixe) {
        int nextNumber = membreRepo.findLastMatriculeByPrefixe(prefixe)
                .map(last -> Integer.parseInt(last.substring(1)) + 1)
                .orElse(1);
        return prefixe + String.format("%04d", nextNumber);
    }
}
