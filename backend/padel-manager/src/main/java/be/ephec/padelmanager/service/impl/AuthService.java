package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.dto.auth.AdminAuthResponseDTO;
import be.ephec.padelmanager.dto.auth.AdminLoginDTO;
import be.ephec.padelmanager.dto.auth.AuthResponseDTO;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.auth.RefreshResponseDTO;
import be.ephec.padelmanager.dto.RefreshTokenDTO;
import be.ephec.padelmanager.dto.auth.RegisterRequest;
import be.ephec.padelmanager.model.Admin;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Personne;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.model.TypeMembre;
import be.ephec.padelmanager.repository.AdminRepo;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PersonneRepo;
import be.ephec.padelmanager.repository.SiteRepo;
import be.ephec.padelmanager.repository.TypeMembreRepo;
import be.ephec.padelmanager.service.IAuthService;
import be.ephec.padelmanager.service.IJwtService;
import be.ephec.padelmanager.service.IRefreshTokenService;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final MembreRepo membreRepo;
    private final PersonneRepo personneRepo;
    private final SiteRepo siteRepo;
    private final TypeMembreRepo typeMembreRepo;
    private final AdminRepo adminRepo;
    private final IJwtService jwtService;
    private final IRefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequest dto) {
        Membre membre = membreRepo.findById(dto.getMatricule())
                .orElseThrow(() -> new UnauthorizedException("Matricule ou mot de passe incorrect"));

        if (!passwordEncoder.matches(dto.getMotDePasse(), membre.getMotDePasse())) {
            throw new UnauthorizedException("Matricule ou mot de passe incorrect");
        }

        refreshTokenService.revokeAllByMatricule(membre.getMatricule());

        String role = membre.getTypeMembre().getLibelle();
        Integer memberSiteId = membre.getSite() != null ? membre.getSite().getIdSite() : null;
        String accessToken = jwtService.generateToken(membre.getMatricule(), role, memberSiteId);
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
    public AuthResponseDTO register(RegisterRequest dto) {
        TypeMembre typeMembre = typeMembreRepo.findById(dto.getIdType())
                .orElseThrow(() -> new NotFoundException("Type de membre introuvable"));

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
                    .orElseThrow(() -> new NotFoundException("Site introuvable"));
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
        Integer memberSiteId = site != null ? site.getIdSite() : null;
        String accessToken = jwtService.generateToken(matricule, role, memberSiteId);
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
    public AdminAuthResponseDTO adminLogin(AdminLoginDTO dto) {
        Admin admin = adminRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(dto.getMotDePasse(), admin.getMotDePasse())) {
            throw new UnauthorizedException("Email ou mot de passe incorrect");
        }

        Integer idSite = admin.getSite() != null ? admin.getSite().getIdSite() : null;
        String accessToken = jwtService.generateAdminToken(
                String.valueOf(admin.getIdAdmin()), admin.getRole(), idSite);

        AdminAuthResponseDTO response = new AdminAuthResponseDTO();
        response.setAccessToken(accessToken);
        response.setIdAdmin(admin.getIdAdmin());
        response.setRole(admin.getRole());
        return response;
    }

    @Override
    @Transactional
    public RefreshResponseDTO refreshToken(String token) {
        RefreshTokenDTO refreshToken = refreshTokenService.verifyRefreshToken(token);

        Membre membre = membreRepo.findById(refreshToken.getMatricule())
                .orElseThrow(() -> new NotFoundException("Membre introuvable"));

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
