package be.ephec.padelmanager.service;

public interface IJwtService {
    String generateToken(String matricule, String role);
    String extractMatricule(String token);
    String extractRole(String token);
    boolean isTokenValide(String token, String matricule);
}
