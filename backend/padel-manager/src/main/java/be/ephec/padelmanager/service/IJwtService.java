package be.ephec.padelmanager.service;

public interface IJwtService {
    String generateToken(String username);
    String extractMatricule(String token);
    boolean isTokenValide(String token, String matricule);
}
