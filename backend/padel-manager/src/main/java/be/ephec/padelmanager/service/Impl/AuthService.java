package be.ephec.padelmanager.service.Impl;

import be.ephec.padelmanager.DTO.LoginDTO;
import be.ephec.padelmanager.DTO.RegisterDTO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final MembreRepo membreRepo;
    private final PersonneRepo personneRepo;
    private final SiteRepo siteRepo;
    private final TypeMembreRepo typeMembreRepo;
    private final IJwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String login(LoginDTO dto) {
        // 1. Chercher le membre par matricule
        Membre membre = membreRepo.findById(dto.getMatricule())
                .orElseThrow(() -> new RuntimeException("Matricule ou mot de passe incorrect"));

        // 2. Vérifier le mot de passe
        if (!passwordEncoder.matches(dto.getMotDePasse(), membre.getMotDePasse())) {
            throw new RuntimeException("Matricule ou mot de passe incorrect");
        }

        // 3. Générer et retourner le token
        return jwtService.generateToken(membre.getMatricule());
    }

    @Override
    public Map<String, String> register(RegisterDTO dto) {
        // 1. Récupérer le type de membre
        TypeMembre typeMembre = typeMembreRepo.findById(dto.getIdType())
                .orElseThrow(() -> new RuntimeException("Type de membre introuvable"));

        // 2. Générer le matricule automatiquement
        String matricule = generateMatricule(typeMembre.getPrefixe());

        // 3. Créer la personne
        Personne personne = new Personne();
        personne.setNom(dto.getNom());
        personne.setPrenom(dto.getPrenom());
        personne.setEmail(dto.getEmail());
        personne.setTelephone(dto.getTelephone());
        personne = personneRepo.save(personne);

        // 4. Récupérer le site (optionnel pour les membres Global ou Libre)
        Site site = null;
        if (dto.getIdSite() != null) {
            site = siteRepo.findById(dto.getIdSite())
                    .orElseThrow(() -> new RuntimeException("Site introuvable"));
        }

        // 5. Créer le membre avec le mot de passe hashé
        Membre membre = new Membre();
        membre.setMatricule(matricule);
        membre.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        membre.setPersonne(personne);
        membre.setSite(site);
        membre.setTypeMembre(typeMembre);
        membre.setDateInscription(LocalDate.now());
        membre.setSoldeDu(BigDecimal.ZERO);
        membreRepo.save(membre);

        // 6. Retourner le matricule et le token
        String token = jwtService.generateToken(matricule);
        return Map.of("matricule", matricule, "token", token);
    }

    private String generateMatricule(String prefixe) {
        // Chercher le dernier matricule avec ce préfixe (ex: G0003)
        // Si aucun → commencer à 1
        int nextNumber = membreRepo.findLastMatriculeByPrefixe(prefixe)
                .map(last -> Integer.parseInt(last.substring(1)) + 1)
                .orElse(1);

        // Formater : préfixe + compteur sur 4 chiffres (ex: G0004)
        return prefixe + String.format("%04d", nextNumber);
    }
}
